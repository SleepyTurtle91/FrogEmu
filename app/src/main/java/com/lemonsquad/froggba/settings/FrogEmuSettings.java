package com.lemonsquad.froggba.settings;

import android.content.Context;
import android.content.SharedPreferences;
import com.lemonsquad.froggba.EmulatorRenderer;
import com.lemonsquad.froggba.input.InputProfile;
import com.lemonsquad.froggba.link.LinkManager;

/**
 * Centralized, persistent settings repository for FrogEmu.
 */
public class FrogEmuSettings {

    private static final String PREF_NAME = "frogemu_settings";

    // Display
    private static final String KEY_UPSCALER = "display_upscaler";
    private static final String KEY_INTEGER_SCALING = "display_integer_scaling";

    // Controls
    public enum TouchMode { AUTO, ALWAYS, NEVER }
    private static final String KEY_TOUCH_MODE = "controls_touch_mode";
    private static final String KEY_INPUT_PRESET = "controls_input_preset";
    private static final String KEY_ANALOG_DEADZONE = "controls_analog_deadzone";
    private static final String KEY_CUSTOM_PROFILE_JSON = "controls_custom_profile_json";

    // Audio
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";
    private static final String KEY_AUDIO_VOLUME = "audio_volume";

    // Link Multiplayer
    private static final String KEY_LINK_MODE = "link_mode";
    private static final String KEY_PLAYER_ID = "link_player_id";
    private static final String KEY_PLAYER_NAME = "link_player_name";
    private static final String KEY_NUM_DEVICES = "link_num_devices";

    private final SharedPreferences mPrefs;
    private static FrogEmuSettings sInstance;

    public static synchronized FrogEmuSettings getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FrogEmuSettings(context.getApplicationContext());
        }
        return sInstance;
    }

    private FrogEmuSettings(Context context) {
        mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Display ─────────────────────────────────────────────────────

    public EmulatorRenderer.Upscaler getUpscaler() {
        String name = mPrefs.getString(KEY_UPSCALER, EmulatorRenderer.Upscaler.NEAREST.name());
        try {
            return EmulatorRenderer.Upscaler.valueOf(name);
        } catch (Exception e) {
            return EmulatorRenderer.Upscaler.NEAREST;
        }
    }

    public void setUpscaler(EmulatorRenderer.Upscaler upscaler) {
        mPrefs.edit().putString(KEY_UPSCALER, upscaler.name()).apply();
    }

    public boolean isIntegerScaling() {
        return mPrefs.getBoolean(KEY_INTEGER_SCALING, false);
    }

    public void setIntegerScaling(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_INTEGER_SCALING, enabled).apply();
    }

    // ── Controls & Input Profile ────────────────────────────────────

    public TouchMode getTouchMode() {
        String name = mPrefs.getString(KEY_TOUCH_MODE, TouchMode.AUTO.name());
        try {
            return TouchMode.valueOf(name);
        } catch (Exception e) {
            return TouchMode.AUTO;
        }
    }

    public void setTouchMode(TouchMode mode) {
        mPrefs.edit().putString(KEY_TOUCH_MODE, mode.name()).apply();
    }

    public InputProfile.Preset getInputPreset() {
        String name = mPrefs.getString(KEY_INPUT_PRESET, InputProfile.Preset.STANDARD_GBA.name());
        try {
            return InputProfile.Preset.valueOf(name);
        } catch (Exception e) {
            return InputProfile.Preset.STANDARD_GBA;
        }
    }

    public void setInputPreset(InputProfile.Preset preset) {
        mPrefs.edit().putString(KEY_INPUT_PRESET, preset.name()).apply();
    }

    public float getAnalogDeadzone() {
        return mPrefs.getFloat(KEY_ANALOG_DEADZONE, 0.40f);
    }

    public void setAnalogDeadzone(float deadzone) {
        mPrefs.edit().putFloat(KEY_ANALOG_DEADZONE, Math.max(0.10f, Math.min(0.90f, deadzone))).apply();
    }

    public String getCustomProfileJson() {
        return mPrefs.getString(KEY_CUSTOM_PROFILE_JSON, "");
    }

    public void setCustomProfileJson(String json) {
        mPrefs.edit().putString(KEY_CUSTOM_PROFILE_JSON, json).apply();
    }

    /** Load active profile based on preset selection and stored custom mapping. */
    public InputProfile loadActiveInputProfile() {
        float deadzone = getAnalogDeadzone();
        InputProfile.Preset preset = getInputPreset();
        switch (preset) {
            case SNES_RETRO:
                return InputProfile.createSnesRetro(deadzone);
            case CUSTOM:
                String json = getCustomProfileJson();
                return InputProfile.fromJson(json, deadzone);
            case STANDARD_GBA:
            default:
                return InputProfile.createStandardGba(deadzone);
        }
    }

    // ── Audio ───────────────────────────────────────────────────────

    public boolean isAudioEnabled() {
        return mPrefs.getBoolean(KEY_AUDIO_ENABLED, true);
    }

    public void setAudioEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply();
    }

    public int getAudioVolume() {
        return mPrefs.getInt(KEY_AUDIO_VOLUME, 100);
    }

    public void setAudioVolume(int volume) {
        mPrefs.edit().putInt(KEY_AUDIO_VOLUME, Math.max(0, Math.min(100, volume))).apply();
    }

    // ── Link Multiplayer ────────────────────────────────────────────

    public LinkManager.Mode getLinkMode() {
        String name = mPrefs.getString(KEY_LINK_MODE, LinkManager.Mode.DISCONNECTED.name());
        try {
            return LinkManager.Mode.valueOf(name);
        } catch (Exception e) {
            return LinkManager.Mode.DISCONNECTED;
        }
    }

    public void setLinkMode(LinkManager.Mode mode) {
        mPrefs.edit().putString(KEY_LINK_MODE, mode.name()).apply();
    }

    public int getLinkPlayerId() {
        return mPrefs.getInt(KEY_PLAYER_ID, 0);
    }

    public void setLinkPlayerId(int id) {
        mPrefs.edit().putInt(KEY_PLAYER_ID, Math.max(0, Math.min(3, id))).apply();
    }

    public String getLinkPlayerName() {
        return mPrefs.getString(KEY_PLAYER_NAME, "FrogEmu Player");
    }

    public void setLinkPlayerName(String name) {
        mPrefs.edit().putString(KEY_PLAYER_NAME, name).apply();
    }

    public int getLinkNumDevices() {
        return mPrefs.getInt(KEY_NUM_DEVICES, 2);
    }

    public void setLinkNumDevices(int count) {
        mPrefs.edit().putInt(KEY_NUM_DEVICES, Math.max(2, Math.min(4, count))).apply();
    }
}
