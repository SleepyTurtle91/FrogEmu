package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.link.LinkManager;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class LinkPanel implements SettingsPanel {

    private final LinkManager mLinkManager;
    private TextView mTxtDiagnostics;

    public LinkPanel(LinkManager linkManager) {
        mLinkManager = linkManager;
    }

    @Override
    public String getId() { return "link"; }

    @Override
    public String getTitle() { return "Link Multiplayer"; }

    @Override
    public String getIcon() { return "🔗"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_link, container, false);

        RadioGroup rgLink = view.findViewById(R.id.rg_link_mode);
        if (settings.getLinkMode() == LinkManager.Mode.MASTER && mLinkManager.isConnected()) {
            ((RadioButton) view.findViewById(R.id.rb_link_loopback)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_link_off)).setChecked(true);
        }

        rgLink.setOnCheckedChangeListener((group, checkedId) -> {
            LinkManager.Mode mode = (checkedId == R.id.rb_link_loopback)
                    ? LinkManager.Mode.MASTER
                    : LinkManager.Mode.DISCONNECTED;
            settings.setLinkMode(mode);
            if (listener != null) listener.onLinkModeChanged(mode);
        });

        mTxtDiagnostics = view.findViewById(R.id.txt_link_diagnostics);
        updateDiagnostics();

        return view;
    }

    @Override
    public void onTick() {
        updateDiagnostics();
    }

    private void updateDiagnostics() {
        if (mTxtDiagnostics != null && mLinkManager != null) {
            mTxtDiagnostics.setText(mLinkManager.getDiagnostics().toString());
        }
    }

    @Override
    public void onDestroyView() {
        mTxtDiagnostics = null;
    }
}
