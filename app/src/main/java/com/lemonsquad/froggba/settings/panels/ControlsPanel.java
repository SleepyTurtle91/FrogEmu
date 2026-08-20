package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FroggBASettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class ControlsPanel implements SettingsPanel {

    @Override
    public String getId() { return "controls"; }

    @Override
    public String getTitle() { return "Controls & Input"; }

    @Override
    public String getIcon() { return "🎮"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FroggBASettings settings, SettingsDialog.OnSettingsChangedListener listener) {
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

        // Touch mode radio buttons
        RadioGroup rgTouch = view.findViewById(R.id.rg_touch_mode);
        FroggBASettings.TouchMode touchMode = settings.getTouchMode();
        if (touchMode == FroggBASettings.TouchMode.ALWAYS) {
            ((RadioButton) view.findViewById(R.id.rb_touch_always)).setChecked(true);
        } else if (touchMode == FroggBASettings.TouchMode.NEVER) {
            ((RadioButton) view.findViewById(R.id.rb_touch_never)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_touch_auto)).setChecked(true);
        }

        rgTouch.setOnCheckedChangeListener((group, checkedId) -> {
            FroggBASettings.TouchMode mode = FroggBASettings.TouchMode.AUTO;
            if (checkedId == R.id.rb_touch_always) mode = FroggBASettings.TouchMode.ALWAYS;
            else if (checkedId == R.id.rb_touch_never) mode = FroggBASettings.TouchMode.NEVER;
            settings.setTouchMode(mode);
            if (listener != null) listener.onTouchModeChanged(mode);
        });

        return view;
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
