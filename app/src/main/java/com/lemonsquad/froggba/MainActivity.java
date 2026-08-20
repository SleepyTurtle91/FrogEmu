package com.lemonsquad.froggba;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.View;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.widget.Button;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("mygbaemulator");
    }

    private static final int PICK_ROM_REQUEST = 1;

    private GLSurfaceView mGLView;
    private EmulatorRenderer mRenderer;
    private InputManager mInputManager;
    private AudioThread mAudioThread;
    private boolean mIsEmulatorRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mInputManager = new InputManager(this);
        
        mGLView = new GLSurfaceView(this);
        mGLView.setEGLContextClientVersion(2);
        mRenderer = new EmulatorRenderer(this);
        mGLView.setRenderer(mRenderer);
        
        FrameLayout glContainer = findViewById(R.id.gl_container);
        glContainer.addView(mGLView);
        
        setupButton(R.id.btn_a, InputManager.GBA_KEY_A);
        setupButton(R.id.btn_b, InputManager.GBA_KEY_B);
        setupButton(R.id.btn_select, InputManager.GBA_KEY_SELECT);
        setupButton(R.id.btn_start, InputManager.GBA_KEY_START);
        setupButton(R.id.btn_up, InputManager.GBA_KEY_UP);
        setupButton(R.id.btn_down, InputManager.GBA_KEY_DOWN);
        setupButton(R.id.btn_left, InputManager.GBA_KEY_LEFT);
        setupButton(R.id.btn_right, InputManager.GBA_KEY_RIGHT);
        setupButton(R.id.btn_l, InputManager.GBA_KEY_L);
        setupButton(R.id.btn_r, InputManager.GBA_KEY_R);
        
        Button btnLoad = findViewById(R.id.btn_load_rom);
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_ROM_REQUEST);
            }
        });
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (source & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD ||
            (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            
            if (mInputManager.handleGamepadKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.getAction() == MotionEvent.ACTION_MOVE) {
            
            if (mInputManager.handleGamepadMotionEvent(event)) {
                return true;
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ROM_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String tempPath = copyUriToTempFile(uri);
                if (tempPath != null) {
                    stopEmulator();
                    
                    ByteBuffer buffer = initEmulatorJNI(tempPath);
                    if (buffer != null) {
                        mRenderer.setFrameBuffer(buffer);
                        startEmulator();
                    }
                }
            }
        }
    }

    private void startEmulator() {
        mIsEmulatorRunning = true;
        if (mAudioThread != null) {
            mAudioThread.halt();
        }
        mAudioThread = new AudioThread();
        mAudioThread.start();
    }

    private void stopEmulator() {
        mIsEmulatorRunning = false;
        if (mAudioThread != null) {
            mAudioThread.halt();
            mAudioThread = null;
        }
    }

    private class AudioThread extends Thread {
        private volatile boolean mRunning = true;
        private AudioTrack mAudioTrack;

        public void halt() {
            mRunning = false;
        }

        @Override
        public void run() {
            int sampleRate = getSampleRateJNI();
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            
            int bufferSize = Math.max(minBufferSize, sampleRate / 10 * 4); // ~100ms buffer
            
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 
                    bufferSize, AudioTrack.MODE_STREAM);
            
            mAudioTrack.play();
            
            int capacityFrames = 1024;
            short[] buffer = new short[capacityFrames * 2]; // 2 channels
            
            while (mRunning) {
                int framesRead = readAudioJNI(buffer, capacityFrames);
                if (framesRead > 0) {
                    mAudioTrack.write(buffer, 0, framesRead * 2);
                } else {
                    try { Thread.sleep(2); } catch (Exception e) { }
                }
            }
            
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    private String copyUriToTempFile(Uri uri) {
        File outFile = new File(getFilesDir(), "current_rom.gba");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupButton(int viewId, final int keyBit) {
        findViewById(viewId).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mInputManager.setKeyPressed(keyBit, true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mInputManager.setKeyPressed(keyBit, false);
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGLView != null) mGLView.onPause();
        stopEmulator();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) mGLView.onResume();
        if (mRenderer != null && mIsEmulatorRunning) {
             startEmulator();
        }
    }

    public native ByteBuffer initEmulatorJNI(String path);
    public native void runFrameJNI();
    public native void setKeysJNI(int mask);
    public native int getSampleRateJNI();
    public native int readAudioJNI(short[] samples, int capacityFrames);
}
