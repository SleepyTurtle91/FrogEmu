package com.lemonsquad.froggba.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Interface for modular Settings category panels.
 */
public interface SettingsPanel {
    String getId();
    String getTitle();
    String getIcon();

    View createView(Context context, LayoutInflater inflater, ViewGroup container,
                    FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener);

    /** Called periodically (e.g. 500ms) while the panel is actively displayed. */
    default void onTick() {}

    /** Called when switching away from this panel. */
    default void onDestroyView() {}
}
