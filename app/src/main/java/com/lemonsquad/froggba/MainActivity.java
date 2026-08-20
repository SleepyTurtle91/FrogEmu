package com.lemonsquad.froggba;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("mygbaemulator");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView tv = findViewById(R.id.sample_text);
        
        // 1. Initialize core
        String initResult = stringFromJNI();
        
        // 2. Extract test ROM from assets
        String romPath = extractAsset("test.gba");
        
        // 3. Load ROM
        boolean loaded = false;
        if (romPath != null) {
            loaded = loadRomJNI(romPath);
        }
        
        tv.setText(initResult + "\nROM Loaded: " + loaded);
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

    public native String stringFromJNI();
    public native boolean loadRomJNI(String path);
}
