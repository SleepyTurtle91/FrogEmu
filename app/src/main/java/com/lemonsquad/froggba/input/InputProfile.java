package com.lemonsquad.froggba.input;

import android.view.KeyEvent;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Immutable configuration defining physical gamepad key bindings and analog sensitivity.
 */
public class InputProfile {

    public enum Preset {
        STANDARD_GBA("Standard GBA (Nintendo)"),
        SNES_RETRO("SNES Retro Layout (Y=B, B=A)"),
        CUSTOM("Custom Layout");

        private final String displayName;
        Preset(String name) { this.displayName = name; }
        public String getDisplayName() { return displayName; }
    }

    // GBA Key bit constants
    public static final int GBA_KEY_A      = 1 << 0;
    public static final int GBA_KEY_B      = 1 << 1;
    public static final int GBA_KEY_SELECT = 1 << 2;
    public static final int GBA_KEY_START  = 1 << 3;
    public static final int GBA_KEY_RIGHT  = 1 << 4;
    public static final int GBA_KEY_LEFT   = 1 << 5;
    public static final int GBA_KEY_UP     = 1 << 6;
    public static final int GBA_KEY_DOWN   = 1 << 7;
    public static final int GBA_KEY_R      = 1 << 8;
    public static final int GBA_KEY_L      = 1 << 9;

    public static final int GBA_TURBO_A    = 1 << 0;
    public static final int GBA_TURBO_B    = 1 << 1;

    public static final int[] ALL_GBA_KEYS = {
        GBA_KEY_A, GBA_KEY_B, GBA_KEY_SELECT, GBA_KEY_START,
        GBA_KEY_UP, GBA_KEY_DOWN, GBA_KEY_LEFT, GBA_KEY_RIGHT,
        GBA_KEY_L, GBA_KEY_R
    };

    private final Preset preset;
    private final Map<Integer, Integer> keyMap;      // KeyCode -> GBA Key bitmask
    private final Map<Integer, Integer> turboMap;    // KeyCode -> GBA Turbo bitmask
    private final float deadzone;                    // 0.10f to 0.90f

    public InputProfile(Preset preset, Map<Integer, Integer> keyMap, Map<Integer, Integer> turboMap, float deadzone) {
        this.preset = preset;
        this.keyMap = Collections.unmodifiableMap(new HashMap<>(keyMap));
        this.turboMap = Collections.unmodifiableMap(new HashMap<>(turboMap));
        this.deadzone = Math.max(0.10f, Math.min(0.90f, deadzone));
    }

    public Preset getPreset() { return preset; }
    public Map<Integer, Integer> getKeyMap() { return keyMap; }
    public Map<Integer, Integer> getTurboMap() { return turboMap; }
    public float getDeadzone() { return deadzone; }

    public int getGbaKey(int keyCode) {
        Integer mask = keyMap.get(keyCode);
        return mask != null ? mask : 0;
    }

    public int getTurboKey(int keyCode) {
        Integer mask = turboMap.get(keyCode);
        return mask != null ? mask : 0;
    }

    public boolean isMapped(int keyCode) {
        return keyMap.containsKey(keyCode) || turboMap.containsKey(keyCode);
    }

    // ── Built-in Presets ────────────────────────────────────────────

    public static InputProfile createStandardGba(float deadzone) {
        Map<Integer, Integer> km = new HashMap<>();
        km.put(KeyEvent.KEYCODE_BUTTON_A,      GBA_KEY_A);
        km.put(KeyEvent.KEYCODE_BUTTON_B,      GBA_KEY_B);
        km.put(KeyEvent.KEYCODE_BUTTON_SELECT, GBA_KEY_SELECT);
        km.put(KeyEvent.KEYCODE_BUTTON_START,  GBA_KEY_START);
        km.put(KeyEvent.KEYCODE_DPAD_UP,       GBA_KEY_UP);
        km.put(KeyEvent.KEYCODE_DPAD_DOWN,     GBA_KEY_DOWN);
        km.put(KeyEvent.KEYCODE_DPAD_LEFT,     GBA_KEY_LEFT);
        km.put(KeyEvent.KEYCODE_DPAD_RIGHT,    GBA_KEY_RIGHT);
        km.put(KeyEvent.KEYCODE_BUTTON_L1,     GBA_KEY_L);
        km.put(KeyEvent.KEYCODE_BUTTON_R1,     GBA_KEY_R);

        Map<Integer, Integer> tm = new HashMap<>();
        tm.put(KeyEvent.KEYCODE_BUTTON_X, GBA_TURBO_A);
        tm.put(KeyEvent.KEYCODE_BUTTON_Y, GBA_TURBO_B);

        return new InputProfile(Preset.STANDARD_GBA, km, tm, deadzone);
    }

    public static InputProfile createSnesRetro(float deadzone) {
        Map<Integer, Integer> km = new HashMap<>();
        km.put(KeyEvent.KEYCODE_BUTTON_B,      GBA_KEY_A);      // RG556 B -> GBA A
        km.put(KeyEvent.KEYCODE_BUTTON_Y,      GBA_KEY_B);      // RG556 Y -> GBA B
        km.put(KeyEvent.KEYCODE_BUTTON_SELECT, GBA_KEY_SELECT);
        km.put(KeyEvent.KEYCODE_BUTTON_START,  GBA_KEY_START);
        km.put(KeyEvent.KEYCODE_DPAD_UP,       GBA_KEY_UP);
        km.put(KeyEvent.KEYCODE_DPAD_DOWN,     GBA_KEY_DOWN);
        km.put(KeyEvent.KEYCODE_DPAD_LEFT,     GBA_KEY_LEFT);
        km.put(KeyEvent.KEYCODE_DPAD_RIGHT,    GBA_KEY_RIGHT);
        km.put(KeyEvent.KEYCODE_BUTTON_L1,     GBA_KEY_L);
        km.put(KeyEvent.KEYCODE_BUTTON_R1,     GBA_KEY_R);

        Map<Integer, Integer> tm = new HashMap<>();
        tm.put(KeyEvent.KEYCODE_BUTTON_A, GBA_TURBO_A);         // RG556 A -> Turbo A
        tm.put(KeyEvent.KEYCODE_BUTTON_X, GBA_TURBO_B);         // RG556 X -> Turbo B

        return new InputProfile(Preset.SNES_RETRO, km, tm, deadzone);
    }

    // ── JSON Serialization / Deserialization ────────────────────────

    public String toJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("preset", preset.name());
            root.put("deadzone", (double) deadzone);

            JSONObject kmObj = new JSONObject();
            for (Map.Entry<Integer, Integer> entry : keyMap.entrySet()) {
                kmObj.put(String.valueOf(entry.getKey()), entry.getValue().intValue());
            }
            root.put("keyMap", kmObj);

            JSONObject tmObj = new JSONObject();
            for (Map.Entry<Integer, Integer> entry : turboMap.entrySet()) {
                tmObj.put(String.valueOf(entry.getKey()), entry.getValue().intValue());
            }
            root.put("turboMap", tmObj);

            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static InputProfile fromJson(String jsonStr, float defaultDeadzone) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return createStandardGba(defaultDeadzone);
        }
        try {
            JSONObject root = new JSONObject(jsonStr);
            String presetStr = root.optString("preset", Preset.CUSTOM.name());
            Preset preset = Preset.valueOf(presetStr);
            float deadzone = (float) root.optDouble("deadzone", defaultDeadzone);

            Map<Integer, Integer> km = new HashMap<>();
            JSONObject kmObj = root.optJSONObject("keyMap");
            if (kmObj != null) {
                Iterator<String> keys = kmObj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    km.put(Integer.parseInt(k), kmObj.getInt(k));
                }
            }

            Map<Integer, Integer> tm = new HashMap<>();
            JSONObject tmObj = root.optJSONObject("turboMap");
            if (tmObj != null) {
                Iterator<String> keys = tmObj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    tm.put(Integer.parseInt(k), tmObj.getInt(k));
                }
            }

            if (km.isEmpty()) {
                return createStandardGba(deadzone);
            }

            return new InputProfile(preset, km, tm, deadzone);
        } catch (Exception e) {
            return createStandardGba(defaultDeadzone);
        }
    }

    public static String getHumanKeyName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:      return "Button A (South)";
            case KeyEvent.KEYCODE_BUTTON_B:      return "Button B (East)";
            case KeyEvent.KEYCODE_BUTTON_X:      return "Button X (North)";
            case KeyEvent.KEYCODE_BUTTON_Y:      return "Button Y (West)";
            case KeyEvent.KEYCODE_BUTTON_L1:     return "L1 Shoulder";
            case KeyEvent.KEYCODE_BUTTON_R1:     return "R1 Shoulder";
            case KeyEvent.KEYCODE_BUTTON_L2:     return "L2 Trigger";
            case KeyEvent.KEYCODE_BUTTON_R2:     return "R2 Trigger";
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return "L3 (Left Stick Click)";
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return "R3 (Right Stick Click)";
            case KeyEvent.KEYCODE_BUTTON_START:  return "Start";
            case KeyEvent.KEYCODE_BUTTON_SELECT: return "Select";
            case KeyEvent.KEYCODE_DPAD_UP:       return "D-Pad Up";
            case KeyEvent.KEYCODE_DPAD_DOWN:     return "D-Pad Down";
            case KeyEvent.KEYCODE_DPAD_LEFT:     return "D-Pad Left";
            case KeyEvent.KEYCODE_DPAD_RIGHT:    return "D-Pad Right";
            case KeyEvent.KEYCODE_BACK:          return "Back Button";
            default:
                String name = KeyEvent.keyCodeToString(keyCode);
                return name.replace("KEYCODE_", "");
        }
    }
}
