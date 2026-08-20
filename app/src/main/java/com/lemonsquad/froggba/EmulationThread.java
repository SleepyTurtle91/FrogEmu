package com.lemonsquad.froggba;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import com.lemonsquad.froggba.link.LinkManager;

/**
 * Dedicated thread that exclusively owns the mGBA core.
 *
 * Architectural rule:
 *   ONLY this thread may call native methods that touch the emulator core.
 *   No other thread (UI, GL, Audio, or Network) may invoke JNI on the core.
 *
 * Responsibilities:
 *   - Core lifecycle  (init / destroy)
 *   - Input           (setKeys)
 *   - Frame execution (runFrame)
 *   - Video publish   (memcpy back→front in native)
 *   - Audio read      (mAudioBufferRead in native)
 *   - Frame timing    (~59.73 Hz)
 *   - Link Cable SIO  (setLinkConfig, getPendingOut, completeLinkTransfer)
 */
public class EmulationThread extends Thread {

    static { System.loadLibrary("mygbaemulator"); }

    private static final String TAG = "FrogEmu_Emu";

    // GBA master clock / (228 scanlines × 1232 dots) ≈ 59.7275 Hz
    private static final long FRAME_TIME_NS = 16_742_706L;

    // ── State flags ─────────────────────────────────────────────────
    private volatile boolean mRunning = true;
    private volatile boolean mPaused  = false;
    private volatile String  mPendingRomPath = null;
    private volatile String  mPendingRomName = null;

    // ── Input ───────────────────────────────────────────────────────
    private final AtomicInteger mInputMask = new AtomicInteger(0);
    private long mFrameCount = 0;

    // ── Video ───────────────────────────────────────────────────────
    private volatile ByteBuffer mDisplayBuffer = null;

    // ── Audio hand-off to AudioThread ───────────────────────────────
    private final ArrayBlockingQueue<short[]> mAudioQueue =
            new ArrayBlockingQueue<>(16);
    private final short[] mAudioScratch = new short[2048 * 2]; // stereo
    private AudioThread mAudioThread;

    // ── Link Cable Manager & Cached Config ──────────────────────────
    private final LinkManager mLinkManager = new LinkManager();
    private boolean mLastLinkConnected = false;
    private int mLastLinkPlayerId = -1;
    private int mLastLinkNumDevices = -1;

    // ── Callback ────────────────────────────────────────────────────
    public interface Callback {
        void onRomLoaded(ByteBuffer displayBuffer, String romName);
        void onRomLoadFailed();
    }
    private volatile Callback mCallback;

    // ── Public API (called from UI thread) ──────────────────────────

    public void setCallback(Callback cb) { mCallback = cb; }

    public LinkManager getLinkManager() { return mLinkManager; }

    /** Atomically update the GBA key bitmask. */
    public void setInputMask(int mask) { mInputMask.set(mask); }

    /** Request a ROM load. Will be picked up on the next loop iteration. */
    public void loadRom(String path, String displayName) {
        mPendingRomName = displayName;
        mPendingRomPath = path;   // volatile write — must be last
    }

    public void pauseEmulation()  { mPaused = true;  }
    public void resumeEmulation() { mPaused = false; }

    public void stopEmulation() {
        mRunning = false;
        this.interrupt();          // wake from any sleep
    }

    /** Safe for the GL thread to call — returns the front buffer. */
    public ByteBuffer getDisplayBuffer() { return mDisplayBuffer; }

    // ── Main loop ───────────────────────────────────────────────────

    @Override
    public void run() {
        android.os.Process.setThreadPriority(
                android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);

        Log.i(TAG, "EmulationThread started.");

        while (mRunning) {
            // ── 1. Pending ROM load? ────────────────────────────────
            String romPath = mPendingRomPath;
            if (romPath != null) {
                String romName = mPendingRomName;
                mPendingRomPath = null;
                mPendingRomName = null;
                handleRomLoad(romPath, romName);
            }

            // ── 2. Paused or idle? ──────────────────────────────────
            if (mPaused || mDisplayBuffer == null) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                continue;
            }

            // ── 3. Sync Link Cable Configuration on change ─────────
            boolean linkConnected = mLinkManager.isConnected();
            int linkPlayerId = mLinkManager.getPlayerId();
            int linkNumDevices = mLinkManager.getConnectedDevices();
            if (linkConnected != mLastLinkConnected ||
                linkPlayerId != mLastLinkPlayerId ||
                linkNumDevices != mLastLinkNumDevices) {
                setLinkConfigJNI(linkConnected, linkPlayerId, linkNumDevices);
                mLastLinkConnected = linkConnected;
                mLastLinkPlayerId = linkPlayerId;
                mLastLinkNumDevices = linkNumDevices;
            }

            // ── 4. Inject any resolved Link Transfer ───────────────
            short[] resolvedTransfer = mLinkManager.pollResolvedTransfer();
            if (resolvedTransfer != null) {
                completeLinkTransferJNI(resolvedTransfer);
            }

            // ── 5. Step one frame ───────────────────────────────────
            long t0 = System.nanoTime();

            int packed = mInputMask.get();
            int normalMask = packed & 0x3FF;
            int turboMask  = (packed >> 16) & 0x3FF;

            mFrameCount++;
            boolean turboPhase = (mFrameCount & 1) == 0;
            int effectiveKeyMask = normalMask | (turboPhase ? turboMask : 0);

            int audioFrames = stepFrameJNI(effectiveKeyMask, mAudioScratch, 2048);

            // Hand audio to AudioThread (non-blocking — drop if queue full)
            if (audioFrames > 0) {
                short[] copy = new short[audioFrames * 2];
                System.arraycopy(mAudioScratch, 0, copy, 0, audioFrames * 2);
                mAudioQueue.offer(copy);
            }

            // ── 6. Check for Link Transfer Request initiated by game ─
            int pendingOut = getLinkPendingOutJNI();
            if (pendingOut != -1) {
                mLinkManager.onLocalTransferRequest((short) pendingOut);
            }

            // ── 7. Frame timing ─────────────────────────────────────
            long elapsed = System.nanoTime() - t0;
            long sleepNs = FRAME_TIME_NS - elapsed;
            if (sleepNs > 1_000_000L) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L,
                                 (int)(sleepNs % 1_000_000L));
                } catch (InterruptedException e) { break; }
            }
        }

        // ── Cleanup ─────────────────────────────────────────────────
        mLinkManager.detachTransport();
        stopAudioThread();
        destroyCoreJNI();
        mDisplayBuffer = null;
        Log.i(TAG, "EmulationThread exited.");
    }

    // ── ROM loading ─────────────────────────────────────────────────

    private void handleRomLoad(String path, String romName) {
        stopAudioThread();

        ByteBuffer buf = initCoreJNI(path);
        mDisplayBuffer = buf;

        if (buf != null) {
            startAudioThread();
            Callback cb = mCallback;
            if (cb != null) cb.onRomLoaded(buf, romName);
        } else {
            Callback cb = mCallback;
            if (cb != null) cb.onRomLoadFailed();
        }
    }

    // ── Audio thread management ─────────────────────────────────────

    private void startAudioThread() {
        mAudioQueue.clear();
        int sampleRate = getSampleRateJNI();
        mAudioThread = new AudioThread(sampleRate, mAudioQueue);
        mAudioThread.start();
    }

    private void stopAudioThread() {
        if (mAudioThread != null) {
            mAudioThread.halt();
            try { mAudioThread.join(500); } catch (InterruptedException ignored) {}
            mAudioThread = null;
        }
    }

    // ── Inner AudioThread ───────────────────────────────────────────

    private static class AudioThread extends Thread {
        private volatile boolean mRunning = true;
        private final int mSampleRate;
        private final ArrayBlockingQueue<short[]> mQueue;

        AudioThread(int sampleRate, ArrayBlockingQueue<short[]> queue) {
            super("FroggBA-Audio");
            mSampleRate = sampleRate;
            mQueue = queue;
        }

        void halt() { mRunning = false; this.interrupt(); }

        @Override
        public void run() {
            int minBuf = AudioTrack.getMinBufferSize(
                    mSampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int bufSize = Math.max(minBuf, mSampleRate / 10 * 4);

            AudioTrack track = new AudioTrack(
                    AudioManager.STREAM_MUSIC, mSampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize, AudioTrack.MODE_STREAM);
            track.play();

            Log.i(TAG, "AudioThread started at " + mSampleRate + " Hz");

            while (mRunning) {
                try {
                    short[] data = mQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (data != null) {
                        track.write(data, 0, data.length);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }

            track.stop();
            track.release();
            Log.i(TAG, "AudioThread stopped.");
        }
    }

    // ── Native methods — called ONLY from this thread ───────────────
    private native ByteBuffer initCoreJNI(String romPath);
    private native int  stepFrameJNI(int keyMask, short[] audioOut, int capacity);
    private native int  getSampleRateJNI();
    private native void destroyCoreJNI();

    // Link Cable SIO JNI
    private native void setLinkConfigJNI(boolean connected, int deviceId, int numDevices);
    private native int  getLinkPendingOutJNI();
    private native void completeLinkTransferJNI(short[] multiData4);
}
