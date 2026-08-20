package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class SavesPanel implements SettingsPanel {

    @Override
    public String getId() { return "saves"; }

    @Override
    public String getTitle() { return "Saves & State"; }

    @Override
    public String getIcon() { return "💾"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_saves, container, false);
        TextView txtSaveDir = view.findViewById(R.id.txt_save_directory);
        txtSaveDir.setText("SRAM Path: " + context.getFilesDir().getAbsolutePath() + "/");
        return view;
    }
}
