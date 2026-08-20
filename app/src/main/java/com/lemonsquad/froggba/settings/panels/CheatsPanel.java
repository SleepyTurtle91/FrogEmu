package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class CheatsPanel implements SettingsPanel {

    @Override
    public String getId() { return "cheats"; }

    @Override
    public String getTitle() { return "Cheats Engine"; }

    @Override
    public String getIcon() { return "🧪"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        return inflater.inflate(R.layout.panel_settings_cheats, container, false);
    }
}
