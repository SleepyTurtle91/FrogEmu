package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.lemonsquad.froggba.EmulatorRenderer;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FroggBASettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class DisplayPanel implements SettingsPanel {

    @Override
    public String getId() { return "display"; }

    @Override
    public String getTitle() { return "Display & Video"; }

    @Override
    public String getIcon() { return "🖥️"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FroggBASettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_display, container, false);

        RadioGroup rgUpscaler = view.findViewById(R.id.rg_upscaler);
        if (settings.getUpscaler() == EmulatorRenderer.Upscaler.SCALE2X) {
            ((RadioButton) view.findViewById(R.id.rb_upscaler_scale2x)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_upscaler_nearest)).setChecked(true);
        }

        rgUpscaler.setOnCheckedChangeListener((group, checkedId) -> {
            EmulatorRenderer.Upscaler upscaler = (checkedId == R.id.rb_upscaler_scale2x)
                    ? EmulatorRenderer.Upscaler.SCALE2X
                    : EmulatorRenderer.Upscaler.NEAREST;
            settings.setUpscaler(upscaler);
            if (listener != null) listener.onUpscalerChanged(upscaler);
        });

        CheckBox chkInteger = view.findViewById(R.id.chk_integer_scaling);
        chkInteger.setChecked(settings.isIntegerScaling());
        chkInteger.setOnCheckedChangeListener((btn, isChecked) -> {
            settings.setIntegerScaling(isChecked);
        });

        return view;
    }
}
