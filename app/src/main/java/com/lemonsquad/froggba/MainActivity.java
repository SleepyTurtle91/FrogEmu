package com.lemonsquad.froggba;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mGLView = new GLSurfaceView(this);
        mGLView.setEGLContextClientVersion(2);
        EmulatorRenderer renderer = new EmulatorRenderer(this);
        mGLView.setRenderer(renderer);
        setContentView(mGLView);
        
        String romPath = extractAsset("test.gba");
        if (romPath != null) {
            ByteBuffer buffer = initEmulatorJNI(romPath);
            if (buffer != null) {
                renderer.setFrameBuffer(buffer);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
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
}
