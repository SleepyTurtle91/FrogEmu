package com.lemonsquad.froggba;
import android.util.Log;

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
        if (pressed) {
            mKeyMask |= keyBit;
        } else {
            mKeyMask &= ~keyBit;
        }
        Log.d("FroggBA_Input", "Sending key mask: " + mKeyMask);
        mActivity.setKeysJNI(mKeyMask);
    }
}
