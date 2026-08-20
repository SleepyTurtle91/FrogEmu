package com.lemonsquad.froggba.link;

import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the Link Cable session, orchestrating data exchange between
 * EmulationThread and the active LinkTransport.
 *
 * Invariant:
 * - Transport events (incoming network packets) publish data to LinkManager.
 * - ONLY EmulationThread polls LinkManager and invokes completeLinkTransferJNI().
 */
public class LinkManager implements LinkTransport.Listener {

    private static final String TAG = "FroggBA_Link";

    public enum Mode {
        DISCONNECTED,
        MASTER,  // Player 0
        SLAVE    // Player 1..3
    }

    // ── Diagnostics Snapshot ────────────────────────────────────────
    public static class Diagnostics {
        public Mode mode = Mode.DISCONNECTED;
        public int playerId = 0;
        public int connectedDevices = 1;
        public long totalTransfers = 0;
        public long errors = 0;
        public int lastSequence = 0;
        public short lastLocalWord = 0;
        public short[] lastMultiData = new short[]{ (short)0xFFFF, (short)0xFFFF, (short)0xFFFF, (short)0xFFFF };

        @Override
        public String toString() {
            return String.format("State: %s (P%d)\nDevices: %d/4\nTransfers: %d\nErrors: %d\nSeq: %d\nLast Out: 0x%04X\nLast Multi: [0x%04X, 0x%04X, 0x%04X, 0x%04X]",
                    mode, playerId, connectedDevices, totalTransfers, errors, lastSequence,
                    lastLocalWord & 0xFFFF,
                    lastMultiData[0] & 0xFFFF, lastMultiData[1] & 0xFFFF,
                    lastMultiData[2] & 0xFFFF, lastMultiData[3] & 0xFFFF);
        }
    }

    private volatile Mode mMode = Mode.DISCONNECTED;
    private final AtomicInteger mPlayerId = new AtomicInteger(0);
    private final AtomicInteger mConnectedDevices = new AtomicInteger(1);
    private final AtomicInteger mSequence = new AtomicInteger(0);
    private final AtomicLong mTotalTransfers = new AtomicLong(0);
    private final AtomicLong mErrors = new AtomicLong(0);
    private volatile short mLastLocalWord = 0;
    private volatile short[] mLastMultiData = new short[]{ (short)0xFFFF, (short)0xFFFF, (short)0xFFFF, (short)0xFFFF };

    private volatile LinkTransport mTransport;

    // Resolved transfer waiting to be injected into the core by EmulationThread
    private final AtomicReference<short[]> mPendingResolvedData = new AtomicReference<>(null);

    public LinkManager() {}

    public void attachTransport(LinkTransport transport, Mode mode, int playerId, int numDevices) {
        if (mTransport != null) {
            mTransport.stop();
        }
        mTransport = transport;
        mMode = mode;
        mPlayerId.set(playerId);
        mConnectedDevices.set(numDevices);
        mTotalTransfers.set(0);
        mErrors.set(0);
        mSequence.set(0);

        if (mTransport != null) {
            mTransport.setListener(this);
            mTransport.start();
        }
        Log.i(TAG, "LinkManager attached: mode=" + mode + ", playerId=" + playerId + ", numDevices=" + numDevices);
    }

    public void detachTransport() {
        if (mTransport != null) {
            mTransport.stop();
            mTransport = null;
        }
        mMode = Mode.DISCONNECTED;
        mPendingResolvedData.set(null);
        Log.i(TAG, "LinkManager detached.");
    }

    public boolean isConnected() {
        return mMode != Mode.DISCONNECTED && mTransport != null;
    }

    public Mode getMode() {
        return mMode;
    }

    public int getPlayerId() {
        return mPlayerId.get();
    }

    public int getConnectedDevices() {
        return mConnectedDevices.get();
    }

    /**
     * Called by EmulationThread when mGBA initiates a transfer.
     */
    public void onLocalTransferRequest(short localWord) {
        if (!isConnected()) return;
        mLastLocalWord = localWord;
        int seq = mSequence.incrementAndGet();
        mTransport.sendTransfer(mPlayerId.get(), seq, localWord);
    }

    /**
     * Called by EmulationThread to retrieve resolved 4-console payload.
     * Returns null if no resolved transfer is waiting.
     */
    public short[] pollResolvedTransfer() {
        short[] data = mPendingResolvedData.getAndSet(null);
        if (data != null) {
            mTotalTransfers.incrementAndGet();
            mLastMultiData = data;
        }
        return data;
    }

    /** Returns an immutable diagnostics snapshot for UI/logging. */
    public Diagnostics getDiagnostics() {
        Diagnostics d = new Diagnostics();
        d.mode = mMode;
        d.playerId = mPlayerId.get();
        d.connectedDevices = mConnectedDevices.get();
        d.totalTransfers = mTotalTransfers.get();
        d.errors = mErrors.get();
        d.lastSequence = mSequence.get();
        d.lastLocalWord = mLastLocalWord;
        d.lastMultiData = mLastMultiData;
        return d;
    }

    // ── LinkTransport.Listener callbacks (runs on transport/network thread) ──

    @Override
    public void onTransferResolved(int sequence, short[] multiData4) {
        if (multiData4 != null && multiData4.length == 4) {
            mPendingResolvedData.set(multiData4);
        } else {
            mErrors.incrementAndGet();
        }
    }

    @Override
    public void onPeerConnected(int playerId) {
        Log.i(TAG, "Peer connected: Player " + playerId);
    }

    @Override
    public void onPeerDisconnected(int playerId) {
        Log.i(TAG, "Peer disconnected: Player " + playerId);
    }
}
