package com.lemonsquad.froggba;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.provider.OpenableColumns;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.lemonsquad.froggba.cheats.CheatRepository;
import com.lemonsquad.froggba.link.LinkManager;
import com.lemonsquad.froggba.link.LoopbackTransport;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;

public class MainActivity extends AppCompatActivity implements SettingsDialog.OnSettingsChangedListener {

    private static final int PICK_ROM_REQUEST = 1;

    private GLSurfaceView     mGLView;
    private EmulatorRenderer   mRenderer;
    private EmulationThread    mEmuThread;
    private InputManager       mInputManager;
    private FrogEmuSettings    mSettings;
    private CheatRepository    mCheatRepo;

    private View    mTouchControls;
    private TextView mTxtRomTitle;

    // ── Lifecycle ───────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSettings = FrogEmuSettings.getInstance(this);

        // Immersive fullscreen
        applyImmersiveMode();

        // Start the emulation thread (idles until a ROM is loaded)
        mEmuThread = new EmulationThread();
        mCheatRepo = new CheatRepository(this, mEmuThread);
        mEmuThread.setCallback(new EmulationThread.Callback() {
            @Override
            public void onRomLoaded(final ByteBuffer displayBuffer, final String romName) {
                runOnUiThread(() -> {
                    mRenderer.setFrameBuffer(displayBuffer);
                    if (mTxtRomTitle != null && romName != null) {
                        mTxtRomTitle.setText(romName);
                        mTxtRomTitle.setVisibility(View.VISIBLE);
                    }
                });
            }
            @Override
            public void onRomLoadFailed() {
                // Could show a Toast here
            }
        });
        mEmuThread.start();

        // Apply initial link setting
        applyInitialLinkMode();

        mInputManager = new InputManager(mEmuThread, mSettings.loadActiveInputProfile());

        // GL Surface
        mGLView = new GLSurfaceView(this);
        mGLView.setEGLContextClientVersion(2);
        mRenderer = new EmulatorRenderer();
        mRenderer.setUpscaler(mSettings.getUpscaler());       // Load saved shader
        mRenderer.setScalingMode(mSettings.getScalingMode()); // Load saved scaling geometry
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        FrameLayout glContainer = findViewById(R.id.gl_container);
        glContainer.addView(mGLView);

        // Touch controls container
        mTouchControls = findViewById(R.id.touch_controls);

        // Setup touch buttons
        setupButton(R.id.btn_a,      InputManager.GBA_KEY_A);
        setupButton(R.id.btn_b,      InputManager.GBA_KEY_B);
        setupButton(R.id.btn_select, InputManager.GBA_KEY_SELECT);
        setupButton(R.id.btn_start,  InputManager.GBA_KEY_START);
        setupButton(R.id.btn_up,     InputManager.GBA_KEY_UP);
        setupButton(R.id.btn_down,   InputManager.GBA_KEY_DOWN);
        setupButton(R.id.btn_left,   InputManager.GBA_KEY_LEFT);
        setupButton(R.id.btn_right,  InputManager.GBA_KEY_RIGHT);
        setupButton(R.id.btn_l,      InputManager.GBA_KEY_L);
        setupButton(R.id.btn_r,      InputManager.GBA_KEY_R);

        // Load ROM button
        mTxtRomTitle = findViewById(R.id.txt_rom_title);
        findViewById(R.id.btn_load_rom).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_ROM_REQUEST);
        });

        // Settings ⚙️ button
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            new SettingsDialog(this, mEmuThread.getLinkManager(), mCheatRepo, this).show();
        });

        // Update touch controls based on saved settings and hardware
        updateTouchControlsVisibility();
    }

    private void applyInitialLinkMode() {
        if (mSettings.getLinkMode() == LinkManager.Mode.MASTER) {
            mEmuThread.getLinkManager().attachTransport(
                    new LoopbackTransport(mSettings.getLinkNumDevices()),
                    LinkManager.Mode.MASTER,
                    mSettings.getLinkPlayerId(),
                    mSettings.getLinkNumDevices());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersiveMode();
        if (mGLView != null) mGLView.onResume();
        if (mEmuThread != null) mEmuThread.resumeEmulation();
        updateTouchControlsVisibility();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mInputManager != null) mInputManager.resetKeyStates();
        if (mEmuThread != null) mEmuThread.pauseEmulation();
        if (mGLView != null) mGLView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEmuThread != null) {
            mEmuThread.stopEmulation();
            try { mEmuThread.join(2000); } catch (InterruptedException ignored) {}
        }
    }

    // ── Settings Callbacks ──────────────────────────────────────────

    @Override
    public void onUpscalerChanged(EmulatorRenderer.Upscaler upscaler) {
        if (mRenderer != null) {
            mRenderer.setUpscaler(upscaler);
        }
    }

    @Override
    public void onScalingModeChanged(EmulatorRenderer.ScalingMode mode) {
        if (mRenderer != null) {
            mRenderer.setScalingMode(mode);
        }
    }

    @Override
    public void onTouchModeChanged(FrogEmuSettings.TouchMode mode) {
        updateTouchControlsVisibility();
    }

    @Override
    public void onInputProfileChanged(com.lemonsquad.froggba.input.InputProfile profile) {
        if (mInputManager != null) {
            mInputManager.setProfile(profile);
        }
    }

    @Override
    public void onLinkModeChanged(LinkManager.Mode mode) {
        if (mode == LinkManager.Mode.MASTER) {
            mEmuThread.getLinkManager().attachTransport(
                    new LoopbackTransport(mSettings.getLinkNumDevices()),
                    LinkManager.Mode.MASTER,
                    mSettings.getLinkPlayerId(),
                    mSettings.getLinkNumDevices());
        } else {
            mEmuThread.getLinkManager().detachTransport();
        }
    }

    @Override
    public void onAudioChanged(boolean enabled) {
        // Handled via SharedPreferences flag
    }

    // ── ROM loading ─────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ROM_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String displayName = getDisplayName(uri);
                String tempPath = copyUriToInternalStorage(uri, displayName);
                if (tempPath != null) {
                    mEmuThread.loadRom(tempPath, displayName);
                    if (mCheatRepo != null) {
                        mCheatRepo.onRomLoaded(new File(tempPath));
                    }
                }
            }
        }
    }

    /** Copy the ROM to internal storage using the original filename,
     *  so each game gets its own save file. */
    private String copyUriToInternalStorage(Uri uri, String filename) {
        File outFile = new File(getFilesDir(), filename);
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

    private String getDisplayName(Uri uri) {
        String name = "rom.gba";
        try (Cursor cursor = getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        }
        return name;
    }

    // ── Gamepad input dispatch ───────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_GAMEPAD)  == InputDevice.SOURCE_GAMEPAD  ||
            (source & InputDevice.SOURCE_DPAD)     == InputDevice.SOURCE_DPAD     ||
            (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            if (mInputManager.handleGamepadKeyEvent(event)) return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
             event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mInputManager.handleGamepadMotionEvent(event)) return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    // ── Touch button wiring ─────────────────────────────────────────

    private void setupButton(int viewId, final int keyBit) {
        findViewById(viewId).setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    mInputManager.setTouchKeyPressed(keyBit, true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    mInputManager.setTouchKeyPressed(keyBit, false);
                    break;
            }
            return true;
        });
    }

    // ── UI helpers ──────────────────────────────────────────────────

    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController wic = getWindow().getInsetsController();
            if (wic != null) {
                wic.hide(WindowInsets.Type.systemBars());
                wic.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /** Update on-screen touch controls based on FrogEmuSettings and hardware status. */
    private void updateTouchControlsVisibility() {
        if (mTouchControls == null) return;
        FrogEmuSettings.TouchMode mode = mSettings != null ? mSettings.getTouchMode() : FrogEmuSettings.TouchMode.AUTO;

        if (mode == FrogEmuSettings.TouchMode.ALWAYS) {
            mTouchControls.setVisibility(View.VISIBLE);
            return;
        }
        if (mode == FrogEmuSettings.TouchMode.NEVER) {
            mTouchControls.setVisibility(View.GONE);
            return;
        }

        // AUTO mode: Check if physical gamepad is connected
        boolean hasGamepad = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev != null &&
                (dev.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                hasGamepad = true;
                break;
            }
        }
        mTouchControls.setVisibility(hasGamepad ? View.GONE : View.VISIBLE);
    }
}
