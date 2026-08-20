package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.lemonsquad.froggba.EmulatorRenderer;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
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
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_display, container, false);

        // 1. Shaders & Display Filters
        RadioGroup rgShader = view.findViewById(R.id.rg_shader_filter);
        EmulatorRenderer.Upscaler currentUpscaler = settings.getUpscaler();

        switch (currentUpscaler) {
            case LCD_GRID:
                ((RadioButton) view.findViewById(R.id.rb_shader_lcd_grid)).setChecked(true);
                break;
            case SCANLINES:
                ((RadioButton) view.findViewById(R.id.rb_shader_scanlines)).setChecked(true);
                break;
            case SCALE2X:
                ((RadioButton) view.findViewById(R.id.rb_shader_scale2x)).setChecked(true);
                break;
            case BILINEAR:
                ((RadioButton) view.findViewById(R.id.rb_shader_bilinear)).setChecked(true);
                break;
            case NEAREST:
            default:
                ((RadioButton) view.findViewById(R.id.rb_shader_nearest)).setChecked(true);
                break;
        }

        rgShader.setOnCheckedChangeListener((group, checkedId) -> {
            EmulatorRenderer.Upscaler upscaler;
            if (checkedId == R.id.rb_shader_lcd_grid) {
                upscaler = EmulatorRenderer.Upscaler.LCD_GRID;
            } else if (checkedId == R.id.rb_shader_scanlines) {
                upscaler = EmulatorRenderer.Upscaler.SCANLINES;
            } else if (checkedId == R.id.rb_shader_scale2x) {
                upscaler = EmulatorRenderer.Upscaler.SCALE2X;
            } else if (checkedId == R.id.rb_shader_bilinear) {
                upscaler = EmulatorRenderer.Upscaler.BILINEAR;
            } else {
                upscaler = EmulatorRenderer.Upscaler.NEAREST;
            }
            settings.setUpscaler(upscaler);
            if (listener != null) listener.onUpscalerChanged(upscaler);
        });

        // 2. Scaling & Geometry
        RadioGroup rgScale = view.findViewById(R.id.rg_scaling_mode);
        EmulatorRenderer.ScalingMode currentScaling = settings.getScalingMode();

        switch (currentScaling) {
            case INTEGER_MAX:
                ((RadioButton) view.findViewById(R.id.rb_scale_int_max)).setChecked(true);
                break;
            case INTEGER_5X:
                ((RadioButton) view.findViewById(R.id.rb_scale_int_5x)).setChecked(true);
                break;
            case INTEGER_4X:
                ((RadioButton) view.findViewById(R.id.rb_scale_int_4x)).setChecked(true);
                break;
            case FULLSCREEN:
                ((RadioButton) view.findViewById(R.id.rb_scale_fullscreen)).setChecked(true);
                break;
            case FIT_3_2:
            default:
                ((RadioButton) view.findViewById(R.id.rb_scale_fit_3_2)).setChecked(true);
                break;
        }

        rgScale.setOnCheckedChangeListener((group, checkedId) -> {
            EmulatorRenderer.ScalingMode mode;
            if (checkedId == R.id.rb_scale_int_max) {
                mode = EmulatorRenderer.ScalingMode.INTEGER_MAX;
            } else if (checkedId == R.id.rb_scale_int_5x) {
                mode = EmulatorRenderer.ScalingMode.INTEGER_5X;
            } else if (checkedId == R.id.rb_scale_int_4x) {
                mode = EmulatorRenderer.ScalingMode.INTEGER_4X;
            } else if (checkedId == R.id.rb_scale_fullscreen) {
                mode = EmulatorRenderer.ScalingMode.FULLSCREEN;
            } else {
                mode = EmulatorRenderer.ScalingMode.FIT_3_2;
            }
            settings.setScalingMode(mode);
            if (listener != null) listener.onScalingModeChanged(mode);
        });

        return view;
    }
}
