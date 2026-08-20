package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.settings.FroggBASettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class AboutPanel implements SettingsPanel {

    @Override
    public String getId() { return "about"; }

    @Override
    public String getTitle() { return "About FroggBA"; }

    @Override
    public String getIcon() { return "ℹ️"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FroggBASettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        return inflater.inflate(R.layout.panel_settings_about, container, false);
    }
}
