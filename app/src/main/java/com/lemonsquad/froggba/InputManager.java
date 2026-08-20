package com.lemonsquad.froggba;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.InputDevice;

public class InputManager {

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

    private int mKeyMask = 0;
    private final MainActivity mActivity;

    public InputManager(MainActivity activity) {
        mActivity = activity;
    }

    public void setKeyPressed(int keyBit, boolean pressed) {
        int oldMask = mKeyMask;
        if (pressed) {
            mKeyMask |= keyBit;
        } else {
            mKeyMask &= ~keyBit;
        }
        
        if (oldMask != mKeyMask) {
            mActivity.setKeysJNI(mKeyMask);
        }
    }

    public boolean handleGamepadKeyEvent(KeyEvent event) {
        boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
        int keyCode = event.getKeyCode();
        
        Log.d("FroggBA_Gamepad", "KeyEvent: " + keyCode + " (" + KeyEvent.keyCodeToString(keyCode) + ") action: " + event.getAction());
        
        int gbaKey = -1;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A: gbaKey = GBA_KEY_A; break;
            case KeyEvent.KEYCODE_BUTTON_B: gbaKey = GBA_KEY_B; break;
            case KeyEvent.KEYCODE_DPAD_UP: gbaKey = GBA_KEY_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: gbaKey = GBA_KEY_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: gbaKey = GBA_KEY_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: gbaKey = GBA_KEY_RIGHT; break;
            case KeyEvent.KEYCODE_BUTTON_L1: gbaKey = GBA_KEY_L; break;
            case KeyEvent.KEYCODE_BUTTON_R1: gbaKey = GBA_KEY_R; break;
            case KeyEvent.KEYCODE_BUTTON_START: gbaKey = GBA_KEY_START; break;
            case KeyEvent.KEYCODE_BUTTON_SELECT: gbaKey = GBA_KEY_SELECT; break;
        }
        
        if (gbaKey != -1) {
            setKeyPressed(gbaKey, pressed);
            return true;
        }
        return false;
    }

    public boolean handleGamepadMotionEvent(MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);
        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        
        // Log analog sticks to verify what RG556 sends for D-pad/stick
        Log.d("FroggBA_Gamepad", "MotionEvent X: " + x + ", Y: " + y + " HAT_X: " + hatX + " HAT_Y: " + hatY);
        
        float threshold = 0.5f;
        setKeyPressed(GBA_KEY_LEFT, x < -threshold || hatX < -threshold);
        setKeyPressed(GBA_KEY_RIGHT, x > threshold || hatX > threshold);
        setKeyPressed(GBA_KEY_UP, y < -threshold || hatY < -threshold);
        setKeyPressed(GBA_KEY_DOWN, y > threshold || hatY > threshold);
        
        return true;
    }
}
