package com.lemonsquad.froggba;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("mygbaemulator");
    }

    private GLSurfaceView mGLView;
    private InputManager mInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mInputManager = new InputManager(this);
        
        mGLView = new GLSurfaceView(this);
        mGLView.setEGLContextClientVersion(2);
        EmulatorRenderer renderer = new EmulatorRenderer(this);
        mGLView.setRenderer(renderer);
        
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
        
        String romPath = extractAsset("test.gba");
        if (romPath != null) {
            ByteBuffer buffer = initEmulatorJNI(romPath);
            if (buffer != null) {
                renderer.setFrameBuffer(buffer);
            }
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) mGLView.onResume();
    }

    private String extractAsset(String assetName) {
        File outFile = new File(getFilesDir(), assetName);
        if (outFile.exists()) return outFile.getAbsolutePath();
        
        try (InputStream is = getAssets().open(assetName);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[1024];
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

    public native ByteBuffer initEmulatorJNI(String path);
    public native void runFrameJNI();
    public native void setKeysJNI(int mask);
}
