package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FroggBASettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class AudioPanel implements SettingsPanel {

    @Override
    public String getId() { return "audio"; }

    @Override
    public String getTitle() { return "Audio & Sound"; }

    @Override
    public String getIcon() { return "🔊"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FroggBASettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_audio, container, false);

        CheckBox chkAudio = view.findViewById(R.id.chk_audio_enabled);
        chkAudio.setChecked(settings.isAudioEnabled());
        chkAudio.setOnCheckedChangeListener((btn, isChecked) -> {
            settings.setAudioEnabled(isChecked);
            if (listener != null) listener.onAudioChanged(isChecked);
        });

        SeekBar seekVolume = view.findViewById(R.id.seek_audio_volume);
        TextView txtVolume = view.findViewById(R.id.txt_audio_volume);
        seekVolume.setProgress(settings.getAudioVolume());
        txtVolume.setText(settings.getAudioVolume() + "%");

        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtVolume.setText(progress + "%");
                settings.setAudioVolume(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }
}
