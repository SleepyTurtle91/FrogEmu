package com.lemonsquad.froggba;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Translates Android input events into a GBA key bitmask.
 *
 * This class never calls JNI directly.  It updates an AtomicInteger
 * that the EmulationThread reads once per frame.
 */
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

    private final AtomicInteger mKeyMask = new AtomicInteger(0);
    private final EmulationThread mEmuThread;

    public InputManager(EmulationThread emuThread) {
        mEmuThread = emuThread;
    }

    /** Set or clear a single key bit and push the result to the emu thread. */
    public void setKeyPressed(int keyBit, boolean pressed) {
        int mask;
        if (pressed) {
            mask = mKeyMask.updateAndGet(old -> old | keyBit);
        } else {
            mask = mKeyMask.updateAndGet(old -> old & ~keyBit);
        }
        mEmuThread.setInputMask(mask);
    }

    /** Handle a physical gamepad key event.  Returns true if consumed. */
    public boolean handleGamepadKeyEvent(KeyEvent event) {
        boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
        int keyCode = event.getKeyCode();

        int gbaKey = mapKeyCode(keyCode);
        if (gbaKey != 0) {
            setKeyPressed(gbaKey, pressed);
            return true;
        }
        return false;
    }

    /** Handle analog stick / hat events.  Returns true if consumed. */
    public boolean handleGamepadMotionEvent(MotionEvent event) {
        float x    = event.getAxisValue(MotionEvent.AXIS_X);
        float y    = event.getAxisValue(MotionEvent.AXIS_Y);
        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        float threshold = 0.5f;
        setKeyPressed(GBA_KEY_LEFT,  x < -threshold || hatX < -threshold);
        setKeyPressed(GBA_KEY_RIGHT, x >  threshold || hatX >  threshold);
        setKeyPressed(GBA_KEY_UP,    y < -threshold || hatY < -threshold);
        setKeyPressed(GBA_KEY_DOWN,  y >  threshold || hatY >  threshold);

        return true;
    }

    private static int mapKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:      return GBA_KEY_A;
            case KeyEvent.KEYCODE_BUTTON_B:      return GBA_KEY_B;
            case KeyEvent.KEYCODE_DPAD_UP:       return GBA_KEY_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:     return GBA_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:     return GBA_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:    return GBA_KEY_RIGHT;
            case KeyEvent.KEYCODE_BUTTON_L1:     return GBA_KEY_L;
            case KeyEvent.KEYCODE_BUTTON_R1:     return GBA_KEY_R;
            case KeyEvent.KEYCODE_BUTTON_START:  return GBA_KEY_START;
            case KeyEvent.KEYCODE_BUTTON_SELECT: return GBA_KEY_SELECT;
            default: return 0;
        }
    }
}
