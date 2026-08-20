package com.lemonsquad.froggba;

import android.view.KeyEvent;
import android.view.MotionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.lemonsquad.froggba.input.InputProfile;

/**
 * Translates Android input events into a packed 32-bit GBA key bitmask.
 *
 * Bit layout in mPackedKeyMask:
 *   Bits  0..9  : Normal GBA Keys (A, B, Select, Start, Right, Left, Up, Down, R, L)
 *   Bits 16..17 : Turbo Keys (Turbo A, Turbo B)
 *
 * Thread-safety:
 *   - Only writes to mPackedKeyMask via atomic updates.
 *   - Uses reference counting on physical keys to prevent button shadowing bugs.
 *   - EmulationThread queries mPackedKeyMask once per frame.
 */
public class InputManager {

    public static final int GBA_KEY_A      = InputProfile.GBA_KEY_A;
    public static final int GBA_KEY_B      = InputProfile.GBA_KEY_B;
    public static final int GBA_KEY_SELECT = InputProfile.GBA_KEY_SELECT;
    public static final int GBA_KEY_START  = InputProfile.GBA_KEY_START;
    public static final int GBA_KEY_RIGHT  = InputProfile.GBA_KEY_RIGHT;
    public static final int GBA_KEY_LEFT   = InputProfile.GBA_KEY_LEFT;
    public static final int GBA_KEY_UP     = InputProfile.GBA_KEY_UP;
    public static final int GBA_KEY_DOWN   = InputProfile.GBA_KEY_DOWN;
    public static final int GBA_KEY_R      = InputProfile.GBA_KEY_R;
    public static final int GBA_KEY_L      = InputProfile.GBA_KEY_L;

    public static final int GBA_TURBO_A    = InputProfile.GBA_TURBO_A;
    public static final int GBA_TURBO_B    = InputProfile.GBA_TURBO_B;

    private final AtomicInteger mPackedKeyMask = new AtomicInteger(0);
    private final EmulationThread mEmuThread;

    private volatile InputProfile mProfile;

    // Physical key tracking (synchronized on this)
    private final Set<Integer> mPressedKeyCodes = new HashSet<>();
    private final int[] mGbaKeyRefCounts = new int[10];     // for bits 0..9
    private final int[] mTurboKeyRefCounts = new int[2];    // for turbo bits 0..1

    // Analog stick states with hysteresis
    private boolean mStickLeft = false;
    private boolean mStickRight = false;
    private boolean mStickUp = false;
    private boolean mStickDown = false;

    public InputManager(EmulationThread emuThread, InputProfile initialProfile) {
        mEmuThread = emuThread;
        mProfile = initialProfile != null ? initialProfile : InputProfile.createStandardGba(0.40f);
    }

    public void setProfile(InputProfile profile) {
        synchronized (this) {
            mProfile = profile != null ? profile : InputProfile.createStandardGba(0.40f);
            resetKeyStatesLocked();
        }
    }

    public InputProfile getProfile() {
        return mProfile;
    }

    /** Reset all physical key states and clear emulator key mask (lifecycle safety). */
    public void resetKeyStates() {
        synchronized (this) {
            resetKeyStatesLocked();
        }
    }

    private void resetKeyStatesLocked() {
        mPressedKeyCodes.clear();
        Arrays.fill(mGbaKeyRefCounts, 0);
        Arrays.fill(mTurboKeyRefCounts, 0);
        mStickLeft = false;
        mStickRight = false;
        mStickUp = false;
        mStickDown = false;
        mPackedKeyMask.set(0);
        mEmuThread.setInputMask(0);
    }

    /** Set or clear a single key bit directly (e.g. from touch screen overlay). */
    public void setTouchKeyPressed(int keyBit, boolean pressed) {
        synchronized (this) {
            int bitIndex = Integer.numberOfTrailingZeros(keyBit);
            if (bitIndex >= 0 && bitIndex < 10) {
                if (pressed) {
                    mGbaKeyRefCounts[bitIndex]++;
                } else {
                    mGbaKeyRefCounts[bitIndex] = Math.max(0, mGbaKeyRefCounts[bitIndex] - 1);
                }
            }
            recomputePackedMaskLocked();
        }
    }

    /** Handle a physical gamepad key event. Returns true if consumed. */
    public boolean handleGamepadKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        InputProfile profile = mProfile;

        if (!profile.isMapped(keyCode)) {
            return false;
        }

        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() > 0) {
                return true; // Discard repeat events
            }
            synchronized (this) {
                if (mPressedKeyCodes.add(keyCode)) {
                    int gbaKey = profile.getGbaKey(keyCode);
                    if (gbaKey != 0) {
                        for (int i = 0; i < 10; ++i) {
                            if ((gbaKey & (1 << i)) != 0) mGbaKeyRefCounts[i]++;
                        }
                    }
                    int turboKey = profile.getTurboKey(keyCode);
                    if (turboKey != 0) {
                        for (int i = 0; i < 2; ++i) {
                            if ((turboKey & (1 << i)) != 0) mTurboKeyRefCounts[i]++;
                        }
                    }
                    recomputePackedMaskLocked();
                }
            }
            return true;
        } else if (action == KeyEvent.ACTION_UP) {
            synchronized (this) {
                if (mPressedKeyCodes.remove(keyCode)) {
                    int gbaKey = profile.getGbaKey(keyCode);
                    if (gbaKey != 0) {
                        for (int i = 0; i < 10; ++i) {
                            if ((gbaKey & (1 << i)) != 0) {
                                mGbaKeyRefCounts[i] = Math.max(0, mGbaKeyRefCounts[i] - 1);
                            }
                        }
                    }
                    int turboKey = profile.getTurboKey(keyCode);
                    if (turboKey != 0) {
                        for (int i = 0; i < 2; ++i) {
                            if ((turboKey & (1 << i)) != 0) {
                                mTurboKeyRefCounts[i] = Math.max(0, mTurboKeyRefCounts[i] - 1);
                            }
                        }
                    }
                    recomputePackedMaskLocked();
                }
            }
            return true;
        }
        return false;
    }

    /** Handle analog stick & hat events with deadzone hysteresis. Returns true if consumed. */
    public boolean handleGamepadMotionEvent(MotionEvent event) {
        float x    = event.getAxisValue(MotionEvent.AXIS_X);
        float y    = event.getAxisValue(MotionEvent.AXIS_Y);
        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        float engage = mProfile.getDeadzone();
        float release = Math.max(0.05f, engage - 0.05f);

        synchronized (this) {
            // Left Stick / Hat X
            float posX = Math.abs(hatX) > Math.abs(x) ? hatX : x;
            if (!mStickLeft && posX < -engage) {
                mStickLeft = true;
                mGbaKeyRefCounts[5]++; // GBA_KEY_LEFT bit 5
            } else if (mStickLeft && posX > -release) {
                mStickLeft = false;
                mGbaKeyRefCounts[5] = Math.max(0, mGbaKeyRefCounts[5] - 1);
            }

            if (!mStickRight && posX > engage) {
                mStickRight = true;
                mGbaKeyRefCounts[4]++; // GBA_KEY_RIGHT bit 4
            } else if (mStickRight && posX < release) {
                mStickRight = false;
                mGbaKeyRefCounts[4] = Math.max(0, mGbaKeyRefCounts[4] - 1);
            }

            // Left Stick / Hat Y
            float posY = Math.abs(hatY) > Math.abs(y) ? hatY : y;
            if (!mStickUp && posY < -engage) {
                mStickUp = true;
                mGbaKeyRefCounts[6]++; // GBA_KEY_UP bit 6
            } else if (mStickUp && posY > -release) {
                mStickUp = false;
                mGbaKeyRefCounts[6] = Math.max(0, mGbaKeyRefCounts[6] - 1);
            }

            if (!mStickDown && posY > engage) {
                mStickDown = true;
                mGbaKeyRefCounts[7]++; // GBA_KEY_DOWN bit 7
            } else if (mStickDown && posY < release) {
                mStickDown = false;
                mGbaKeyRefCounts[7] = Math.max(0, mGbaKeyRefCounts[7] - 1);
            }

            recomputePackedMaskLocked();
        }
        return true;
    }

    /** Recompute normal and turbo bitmasks with SOCD resolution and update atomic packed mask. */
    private void recomputePackedMaskLocked() {
        int normalMask = 0;
        for (int i = 0; i < 10; ++i) {
            if (mGbaKeyRefCounts[i] > 0) {
                normalMask |= (1 << i);
            }
        }

        // SOCD resolution (Simultaneous Opposing Cardinal Directions -> Neutral)
        if ((normalMask & (GBA_KEY_LEFT | GBA_KEY_RIGHT)) == (GBA_KEY_LEFT | GBA_KEY_RIGHT)) {
            normalMask &= ~(GBA_KEY_LEFT | GBA_KEY_RIGHT);
        }
        if ((normalMask & (GBA_KEY_UP | GBA_KEY_DOWN)) == (GBA_KEY_UP | GBA_KEY_DOWN)) {
            normalMask &= ~(GBA_KEY_UP | GBA_KEY_DOWN);
        }

        int turboMask = 0;
        for (int i = 0; i < 2; ++i) {
            if (mTurboKeyRefCounts[i] > 0) {
                turboMask |= (1 << i);
            }
        }

        int packed = (turboMask << 16) | normalMask;
        mPackedKeyMask.set(packed);
        mEmuThread.setInputMask(packed);
    }
}
