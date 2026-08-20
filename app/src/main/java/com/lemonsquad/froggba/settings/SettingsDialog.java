package com.lemonsquad.froggba.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.lemonsquad.froggba.EmulatorRenderer;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.link.LinkManager;
import com.lemonsquad.froggba.link.LoopbackTransport;

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
    private Dialog mDialog;
    private Handler mHandler;
    private Runnable mDiagnosticsUpdater;

    public SettingsDialog(Context context, LinkManager linkManager, OnSettingsChangedListener listener) {
        mContext = context;
        mLinkManager = linkManager;
        mListener = listener;
        mSettings = FroggBASettings.getInstance(context);
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

        // ── 🖥️ Display ───────────────────────────────────────────────
        RadioGroup rgUpscaler = view.findViewById(R.id.rg_upscaler);
        if (mSettings.getUpscaler() == EmulatorRenderer.Upscaler.SCALE2X) {
            ((RadioButton) view.findViewById(R.id.rb_upscaler_scale2x)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_upscaler_nearest)).setChecked(true);
        }
        rgUpscaler.setOnCheckedChangeListener((group, checkedId) -> {
            EmulatorRenderer.Upscaler upscaler = (checkedId == R.id.rb_upscaler_scale2x)
                    ? EmulatorRenderer.Upscaler.SCALE2X
                    : EmulatorRenderer.Upscaler.NEAREST;
            mSettings.setUpscaler(upscaler);
            if (mListener != null) mListener.onUpscalerChanged(upscaler);
        });

        // ── 🎮 Controls ──────────────────────────────────────────────
        RadioGroup rgTouch = view.findViewById(R.id.rg_touch_mode);
        FroggBASettings.TouchMode touchMode = mSettings.getTouchMode();
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
            mSettings.setTouchMode(mode);
            if (mListener != null) mListener.onTouchModeChanged(mode);
        });

        // ── 🔊 Audio ─────────────────────────────────────────────────
        CheckBox chkAudio = view.findViewById(R.id.chk_audio_enabled);
        chkAudio.setChecked(mSettings.isAudioEnabled());
        chkAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mSettings.setAudioEnabled(isChecked);
            if (mListener != null) mListener.onAudioChanged(isChecked);
        });

        // ── 🔗 Link Multiplayer ──────────────────────────────────────
        RadioGroup rgLink = view.findViewById(R.id.rg_link_mode);
        if (mSettings.getLinkMode() == LinkManager.Mode.MASTER && mLinkManager.isConnected()) {
            ((RadioButton) view.findViewById(R.id.rb_link_loopback)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rb_link_off)).setChecked(true);
        }
        rgLink.setOnCheckedChangeListener((group, checkedId) -> {
            LinkManager.Mode mode = (checkedId == R.id.rb_link_loopback)
                    ? LinkManager.Mode.MASTER
                    : LinkManager.Mode.DISCONNECTED;
            mSettings.setLinkMode(mode);
            if (mListener != null) mListener.onLinkModeChanged(mode);
        });

        // Diagnostics updater
        TextView txtDiagnostics = view.findViewById(R.id.txt_link_diagnostics);
        mHandler = new Handler(Looper.getMainLooper());
        mDiagnosticsUpdater = new Runnable() {
            @Override
            public void run() {
                if (mDialog.isShowing()) {
                    txtDiagnostics.setText(mLinkManager.getDiagnostics().toString());
                    mHandler.postDelayed(this, 500);
                }
            }
        };
        mHandler.post(mDiagnosticsUpdater);

        mDialog.setOnDismissListener(dialog -> {
            if (mHandler != null && mDiagnosticsUpdater != null) {
                mHandler.removeCallbacks(mDiagnosticsUpdater);
            }
        });

        mDialog.show();
    }
}
