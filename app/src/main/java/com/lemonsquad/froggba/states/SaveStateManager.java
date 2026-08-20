package com.lemonsquad.froggba.states;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.lemonsquad.froggba.EmulationThread;

/**
 * Manages save state slots (Slots 0..4) and dispatches save/load operations
 * to EmulationThread in a thread-safe manner.
 */
public class SaveStateManager {

    private static final String TAG = "FrogEmu_State";
    public static final int NUM_SLOTS = 5;

    public static class SlotInfo {
        public final int slot;
        public final boolean exists;
        public final String formattedDate;
        public final long sizeBytes;
        public final File file;

        public SlotInfo(int slot, boolean exists, String formattedDate, long sizeBytes, File file) {
            this.slot = slot;
            this.exists = exists;
            this.formattedDate = formattedDate;
            this.sizeBytes = sizeBytes;
            this.file = file;
        }
    }

    private final Context mContext;
    private final EmulationThread mEmuThread;
    private final File mStateDir;

    public SaveStateManager(Context context, EmulationThread emuThread) {
        mContext = context.getApplicationContext();
        mEmuThread = emuThread;
        mStateDir = new File(mContext.getFilesDir(), "states");
        if (!mStateDir.exists()) {
            mStateDir.mkdirs();
        }
    }

    public File getStateFile(String gameId, int slot) {
        String cleanId = (gameId != null && !gameId.trim().isEmpty())
                ? gameId.trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "default_game";
        return new File(mStateDir, cleanId + "_slot" + slot + ".state");
    }

    public SlotInfo getSlotInfo(String gameId, int slot) {
        File file = getStateFile(gameId, slot);
        if (file.exists() && file.isFile()) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault());
            String dateStr = sdf.format(new Date(file.lastModified()));
            return new SlotInfo(slot, true, dateStr, file.length(), file);
        }
        return new SlotInfo(slot, false, "Empty Slot", 0, file);
    }

    public void saveSlot(String gameId, int slot, EmulationThread.StateCallback callback) {
        File file = getStateFile(gameId, slot);
        Log.i(TAG, "Queueing SAVE state to slot " + slot + ": " + file.getAbsolutePath());
        mEmuThread.queueSaveState(file.getAbsolutePath(), callback);
    }

    public void loadSlot(String gameId, int slot, EmulationThread.StateCallback callback) {
        File file = getStateFile(gameId, slot);
        if (!file.exists()) {
            if (callback != null) callback.onComplete(false, "Save state slot is empty.");
            return;
        }
        Log.i(TAG, "Queueing LOAD state from slot " + slot + ": " + file.getAbsolutePath());
        mEmuThread.queueLoadState(file.getAbsolutePath(), callback);
    }

    public boolean deleteSlot(String gameId, int slot) {
        File file = getStateFile(gameId, slot);
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.i(TAG, "Deleted state slot " + slot + ": " + deleted);
            return deleted;
        }
        return false;
    }
}
