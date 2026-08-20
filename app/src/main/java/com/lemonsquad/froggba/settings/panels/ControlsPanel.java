package com.lemonsquad.froggba.settings.panels;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.input.InputProfile;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class ControlsPanel implements SettingsPanel {

    private static class ActionDef {
        final String name;
        final boolean isTurbo;
        final int bitmask;

        ActionDef(String name, boolean isTurbo, int bitmask) {
            this.name = name;
            this.isTurbo = isTurbo;
            this.bitmask = bitmask;
        }
    }

    private static final ActionDef[] ACTIONS = new ActionDef[] {
        new ActionDef("GBA A Button",    false, InputProfile.GBA_KEY_A),
        new ActionDef("GBA B Button",    false, InputProfile.GBA_KEY_B),
        new ActionDef("Turbo A (30Hz)",  true,  InputProfile.GBA_TURBO_A),
        new ActionDef("Turbo B (30Hz)",  true,  InputProfile.GBA_TURBO_B),
        new ActionDef("L Shoulder",      false, InputProfile.GBA_KEY_L),
        new ActionDef("R Shoulder",      false, InputProfile.GBA_KEY_R),
        new ActionDef("Start",           false, InputProfile.GBA_KEY_START),
        new ActionDef("Select",          false, InputProfile.GBA_KEY_SELECT),
        new ActionDef("D-Pad Up",        false, InputProfile.GBA_KEY_UP),
        new ActionDef("D-Pad Down",      false, InputProfile.GBA_KEY_DOWN),
        new ActionDef("D-Pad Left",      false, InputProfile.GBA_KEY_LEFT),
        new ActionDef("D-Pad Right",     false, InputProfile.GBA_KEY_RIGHT)
    };

    @Override
    public String getId() { return "controls"; }

    @Override
    public String getTitle() { return "Controls & Input"; }

    @Override
    public String getIcon() { return "🎮"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_controls, container, false);

        // Gamepad hardware detection
        TextView txtHardware = view.findViewById(R.id.txt_hardware_controller);
        String detectedName = getConnectedGamepadName();
        if (detectedName != null) {
            txtHardware.setText("Detected: " + detectedName + " ✅");
            txtHardware.setTextColor(0xFF00FF88);
        } else {
            txtHardware.setText("No physical gamepad detected (Touch fallback active)");
            txtHardware.setTextColor(0xFFAAAAAA);
        }

        // Preset selector
        RadioGroup rgPreset = view.findViewById(R.id.rg_input_preset);
        InputProfile.Preset currentPreset = settings.getInputPreset();
        if (currentPreset == InputProfile.Preset.SNES_RETRO) {
            ((RadioButton) view.findViewById(R.id.rb_preset_snes_retro)).setChecked(true);
        } else if (currentPreset == InputProfile.Preset.CUSTOM) {
            ((RadioButton) view.findViewById(R.id.rb_preset_custom)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_preset_standard_gba)).setChecked(true);
        }

        LinearLayout mappingContainer = view.findViewById(R.id.ll_mapping_rows_container);
        refreshMappingRows(context, mappingContainer, settings, listener);

        rgPreset.setOnCheckedChangeListener((group, checkedId) -> {
            InputProfile.Preset selected = InputProfile.Preset.STANDARD_GBA;
            if (checkedId == R.id.rb_preset_snes_retro) selected = InputProfile.Preset.SNES_RETRO;
            else if (checkedId == R.id.rb_preset_custom) selected = InputProfile.Preset.CUSTOM;

            settings.setInputPreset(selected);
            InputProfile newProfile = settings.loadActiveInputProfile();
            if (listener != null) listener.onInputProfileChanged(newProfile);
            refreshMappingRows(context, mappingContainer, settings, listener);
        });

        // Analog Deadzone Slider
        SeekBar seekDeadzone = view.findViewById(R.id.seek_analog_deadzone);
        TextView txtDeadzoneVal = view.findViewById(R.id.txt_deadzone_val);
        float currentDeadzone = settings.getAnalogDeadzone();
        int progress = Math.round((currentDeadzone - 0.10f) * 100.0f);
        seekDeadzone.setProgress(Math.max(0, Math.min(80, progress)));
        txtDeadzoneVal.setText(Math.round(currentDeadzone * 100) + "%");

        seekDeadzone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                float deadzone = 0.10f + (p / 100.0f);
                txtDeadzoneVal.setText(Math.round(deadzone * 100) + "%");
                settings.setAnalogDeadzone(deadzone);
                InputProfile profile = settings.loadActiveInputProfile();
                if (listener != null) listener.onInputProfileChanged(profile);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Touch mode radio buttons
        RadioGroup rgTouch = view.findViewById(R.id.rg_touch_mode);
        FrogEmuSettings.TouchMode touchMode = settings.getTouchMode();
        if (touchMode == FrogEmuSettings.TouchMode.ALWAYS) {
            ((RadioButton) view.findViewById(R.id.rb_touch_always)).setChecked(true);
        } else if (touchMode == FrogEmuSettings.TouchMode.NEVER) {
            ((RadioButton) view.findViewById(R.id.rb_touch_never)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_touch_auto)).setChecked(true);
        }

        rgTouch.setOnCheckedChangeListener((group, checkedId) -> {
            FrogEmuSettings.TouchMode mode = FrogEmuSettings.TouchMode.AUTO;
            if (checkedId == R.id.rb_touch_always) mode = FrogEmuSettings.TouchMode.ALWAYS;
            else if (checkedId == R.id.rb_touch_never) mode = FrogEmuSettings.TouchMode.NEVER;
            settings.setTouchMode(mode);
            if (listener != null) listener.onTouchModeChanged(mode);
        });

        return view;
    }

    private void refreshMappingRows(Context context, LinearLayout container,
                                    FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        container.removeAllViews();
        InputProfile profile = settings.loadActiveInputProfile();
        Map<Integer, Integer> keyMap = profile.getKeyMap();
        Map<Integer, Integer> turboMap = profile.getTurboMap();

        for (final ActionDef action : ACTIONS) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 12, 16, 12);
            row.setBackgroundColor(0x11FFFFFF);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 8);
            row.setLayoutParams(rowParams);

            TextView txtAction = new TextView(context);
            txtAction.setText(action.name);
            txtAction.setTextColor(Color.WHITE);
            txtAction.setTextSize(13);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            txtAction.setLayoutParams(actionParams);

            // Find keycode mapped to this action
            int mappedKeyCode = -1;
            if (action.isTurbo) {
                for (Map.Entry<Integer, Integer> entry : turboMap.entrySet()) {
                    if ((entry.getValue() & action.bitmask) != 0) {
                        mappedKeyCode = entry.getKey();
                        break;
                    }
                }
            } else {
                for (Map.Entry<Integer, Integer> entry : keyMap.entrySet()) {
                    if ((entry.getValue() & action.bitmask) != 0) {
                        mappedKeyCode = entry.getKey();
                        break;
                    }
                }
            }

            Button btnKey = new Button(context);
            String keyName = mappedKeyCode != -1 ? InputProfile.getHumanKeyName(mappedKeyCode) : "[ Unmapped ]";
            btnKey.setText(keyName);
            btnKey.setTextSize(11);
            btnKey.setTextColor(0xFF00FF66);
            btnKey.setBackgroundColor(0x3300FF66);
            btnKey.setAllCaps(false);
            btnKey.setPadding(20, 8, 20, 8);

            btnKey.setOnClickListener(v -> showRemapDialog(context, action, settings, listener, () -> {
                refreshMappingRows(context, container, settings, listener);
            }));

            row.addView(txtAction);
            row.addView(btnKey);
            container.addView(row);
        }
    }

    private void showRemapDialog(Context context, ActionDef action, FrogEmuSettings settings,
                                 SettingsDialog.OnSettingsChangedListener listener, Runnable onDone) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (event.getRepeatCount() > 0) return true;
                // Discard volume keys so audio adjustment works
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    return super.onKeyDown(keyCode, event);
                }
                applyRemappedKey(keyCode, action, settings, listener);
                dismiss();
                if (onDone != null) onDone.run();
                return true;
            }
        };

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xEE121212);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(context);
        title.setText("🎮 Remapping: " + action.name);
        title.setTextColor(0xFF00FF66);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);

        TextView msg = new TextView(context);
        msg.setText("Press any physical button on your RG556 or Controller...\n\n(Auto-closing in 10 seconds)");
        msg.setTextColor(Color.WHITE);
        msg.setTextSize(14);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 0, 0, 30);

        Button btnCancel = new Button(context);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.WHITE);
        btnCancel.setBackgroundColor(0x44FFFFFF);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        layout.addView(title);
        layout.addView(msg);
        layout.addView(btnCancel);

        dialog.setContentView(layout);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        // Auto-dismiss timeout
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable autoDismiss = dialog::dismiss;
        handler.postDelayed(autoDismiss, 10000);
        dialog.setOnDismissListener(d -> handler.removeCallbacks(autoDismiss));

        dialog.show();
    }

    private void applyRemappedKey(int keyCode, ActionDef action, FrogEmuSettings settings,
                                  SettingsDialog.OnSettingsChangedListener listener) {
        InputProfile current = settings.loadActiveInputProfile();
        Map<Integer, Integer> keyMap = new HashMap<>(current.getKeyMap());
        Map<Integer, Integer> turboMap = new HashMap<>(current.getTurboMap());

        if (action.isTurbo) {
            // Remove previous binding for this turbo action
            keyMap.remove(keyCode);
            turboMap.values().removeIf(mask -> (mask & action.bitmask) != 0);
            turboMap.put(keyCode, action.bitmask);
        } else {
            // Remove previous binding for this normal action
            turboMap.remove(keyCode);
            keyMap.values().removeIf(mask -> (mask & action.bitmask) != 0);
            keyMap.put(keyCode, action.bitmask);
        }

        InputProfile newProfile = new InputProfile(
                InputProfile.Preset.CUSTOM,
                keyMap,
                turboMap,
                settings.getAnalogDeadzone()
        );

        settings.setInputPreset(InputProfile.Preset.CUSTOM);
        settings.setCustomProfileJson(newProfile.toJson());

        if (listener != null) {
            listener.onInputProfileChanged(newProfile);
        }
    }

    private String getConnectedGamepadName() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev != null && (dev.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                return dev.getName();
            }
        }
        return null;
    }
}
