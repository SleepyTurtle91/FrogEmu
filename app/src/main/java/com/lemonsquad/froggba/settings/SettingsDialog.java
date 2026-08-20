package com.lemonsquad.froggba.settings;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import com.lemonsquad.froggba.EmulatorRenderer;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.link.LinkManager;
import com.lemonsquad.froggba.settings.panels.AboutPanel;
import com.lemonsquad.froggba.settings.panels.AudioPanel;
import com.lemonsquad.froggba.settings.panels.CheatsPanel;
import com.lemonsquad.froggba.settings.panels.ControlsPanel;
import com.lemonsquad.froggba.settings.panels.DisplayPanel;
import com.lemonsquad.froggba.settings.panels.LinkPanel;
import com.lemonsquad.froggba.settings.panels.SavesPanel;

public class SettingsDialog {

    public interface OnSettingsChangedListener {
        void onUpscalerChanged(EmulatorRenderer.Upscaler upscaler);
        void onTouchModeChanged(FroggBASettings.TouchMode mode);
        void onLinkModeChanged(LinkManager.Mode mode);
        void onAudioChanged(boolean enabled);
    }

    private final Context mContext;
    private final LinkManager mLinkManager;
    private final OnSettingsChangedListener mListener;
    private final FroggBASettings mSettings;
    private final List<SettingsPanel> mPanels = new ArrayList<>();
    private final List<Button> mSidebarButtons = new ArrayList<>();

    private Dialog mDialog;
    private FrameLayout mContentContainer;
    private TextView mTxtTitle;
    private SettingsPanel mActivePanel;
    private Handler mHandler;
    private Runnable mTickRunnable;

    public SettingsDialog(Context context, LinkManager linkManager, OnSettingsChangedListener listener) {
        mContext = context;
        mLinkManager = linkManager;
        mListener = listener;
        mSettings = FroggBASettings.getInstance(context);

        // Register modular panels
        mPanels.add(new DisplayPanel());
        mPanels.add(new ControlsPanel());
        mPanels.add(new AudioPanel());
        mPanels.add(new LinkPanel(linkManager));
        mPanels.add(new SavesPanel());
        mPanels.add(new CheatsPanel());
        mPanels.add(new AboutPanel());
    }

    public void show() {
        mDialog = new Dialog(mContext, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_settings, null);
        mDialog.setContentView(view);

        Window window = mDialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        // Close button
        view.findViewById(R.id.btn_close_settings).setOnClickListener(v -> mDialog.dismiss());

        mContentContainer = view.findViewById(R.id.panel_content_container);
        mTxtTitle = view.findViewById(R.id.txt_active_category_title);

        LinearLayout sidebar = view.findViewById(R.id.sidebar_category_container);
        sidebar.removeAllViews();
        mSidebarButtons.clear();

        // Build sidebar navigation buttons
        for (int i = 0; i < mPanels.size(); ++i) {
            final SettingsPanel panel = mPanels.get(i);
            Button btn = new Button(mContext);
            btn.setText(panel.getIcon() + "  " + panel.getTitle());
            btn.setTextSize(13);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setPadding(24, 20, 24, 20);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            btn.setAllCaps(false);

            final int index = i;
            btn.setOnClickListener(v -> selectPanel(index));

            sidebar.addView(btn);
            mSidebarButtons.add(btn);
        }

        // Select first panel by default (Display)
        selectPanel(0);

        // Periodic ticker for live panels (e.g. Link Diagnostics)
        mHandler = new Handler(Looper.getMainLooper());
        mTickRunnable = new Runnable() {
            @Override
            public void run() {
                if (mDialog.isShowing()) {
                    if (mActivePanel != null) {
                        mActivePanel.onTick();
                    }
                    mHandler.postDelayed(this, 500);
                }
            }
        };
        mHandler.post(mTickRunnable);

        mDialog.setOnDismissListener(dialog -> {
            if (mActivePanel != null) {
                mActivePanel.onDestroyView();
                mActivePanel = null;
            }
            if (mHandler != null && mTickRunnable != null) {
                mHandler.removeCallbacks(mTickRunnable);
            }
        });

        mDialog.show();
    }

    private void selectPanel(int index) {
        if (index < 0 || index >= mPanels.size()) return;

        if (mActivePanel != null) {
            mActivePanel.onDestroyView();
        }

        // Update sidebar button highlights
        for (int i = 0; i < mSidebarButtons.size(); ++i) {
            Button btn = mSidebarButtons.get(i);
            if (i == index) {
                btn.setTextColor(0xFF00FF66);
                btn.setBackgroundColor(0x3300FF66);
            } else {
                btn.setTextColor(Color.WHITE);
                btn.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        mActivePanel = mPanels.get(index);
        mTxtTitle.setText(mActivePanel.getIcon() + "  " + mActivePanel.getTitle());

        mContentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View panelView = mActivePanel.createView(mContext, inflater, mContentContainer, mSettings, mListener);
        if (panelView != null) {
            mContentContainer.addView(panelView);
        }
    }
}
