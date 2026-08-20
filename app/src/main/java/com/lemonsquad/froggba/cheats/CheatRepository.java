package com.lemonsquad.froggba.cheats;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.lemonsquad.froggba.EmulationThread;
import com.lemonsquad.froggba.settings.FrogEmuSettings;

/**
 * Manages cheat definitions, per-game persistence, master code dependencies,
 * downloaded database lookups, and synchronization with EmulationThread.
 */
public class CheatRepository {

    private static final String TAG = "FrogEmu_Cheats";

    private final Context mContext;
    private final EmulationThread mEmuThread;
    private final FrogEmuSettings mSettings;

    private RomMatcher.RomMetadata mActiveRom = null;
    private File mActiveRomFile = null;
    private List<CheatItem> mCurrentCheats = new ArrayList<>();

    public CheatRepository(Context context, EmulationThread emuThread) {
        mContext = context.getApplicationContext();
        mEmuThread = emuThread;
        mSettings = FrogEmuSettings.getInstance(context);
    }

    public synchronized RomMatcher.RomMetadata getActiveRom() {
        return mActiveRom;
    }

    public synchronized List<CheatItem> getCurrentCheats() {
        return Collections.unmodifiableList(mCurrentCheats);
    }

    public synchronized void reloadActiveRomCheats() {
        if (mActiveRomFile != null) {
            onRomLoaded(mActiveRomFile);
        }
    }

    /**
     * Called when a new ROM is loaded. Identifies the game, finds matching .cht,
     * restores saved toggle states, and dispatches to EmulationThread.
     */
    public synchronized void onRomLoaded(File romFile) {
        mActiveRomFile = romFile;
        mActiveRom = RomMatcher.inspectRom(romFile);
        Log.i(TAG, "Loaded ROM: " + mActiveRom);

        List<CheatItem> loaded = findAndParseCheats(mActiveRom, romFile);
        if (loaded.isEmpty()) {
            mCurrentCheats.clear();
            mEmuThread.queueCheatCommand(CheatCommand.clearAll());
            return;
        }

        // Restore saved toggle states from preferences
        String saveKey = "cheats_state_" + mActiveRom.gameCode;
        JSONObject savedStates = null;
        try {
            String val = mContext.getSharedPreferences("frogemu_cheats", Context.MODE_PRIVATE)
                    .getString(saveKey, "{}");
            savedStates = new JSONObject(val);
        } catch (Exception ignored) {}

        List<CheatItem> synchronizedList = new ArrayList<>();
        boolean hasActiveChild = false;

        for (CheatItem item : loaded) {
            boolean enabled = item.isEnabled();
            if (savedStates != null && savedStates.has(item.getId())) {
                enabled = savedStates.optBoolean(item.getId(), false);
            }
            if (!item.isMasterCode() && enabled) {
                hasActiveChild = true;
            }
            synchronizedList.add(item.withEnabled(enabled));
        }

        // Enforce master code dependency: if any child is enabled, master must be on
        if (hasActiveChild) {
            for (int i = 0; i < synchronizedList.size(); ++i) {
                CheatItem item = synchronizedList.get(i);
                if (item.isMasterCode() && !item.isEnabled()) {
                    synchronizedList.set(i, item.withEnabled(true));
                }
            }
        }

        mCurrentCheats = synchronizedList;
        pushAllToEmulationThread();
    }

    public synchronized void onRomUnloaded() {
        mActiveRom = null;
        mActiveRomFile = null;
        mCurrentCheats.clear();
        mEmuThread.queueCheatCommand(CheatCommand.clearAll());
    }

    /**
     * Toggle a cheat's enabled status with master code dependency resolution.
     */
    public synchronized boolean setCheatEnabled(int index, boolean enabled) {
        if (index < 0 || index >= mCurrentCheats.size()) return false;

        CheatItem target = mCurrentCheats.get(index);

        // If user tries to turn OFF a Master Code while child cheats are active, prevent it
        if (target.isMasterCode() && !enabled) {
            boolean anyChildActive = false;
            for (CheatItem item : mCurrentCheats) {
                if (!item.isMasterCode() && item.isEnabled()) {
                    anyChildActive = true;
                    break;
                }
            }
            if (anyChildActive) {
                Log.w(TAG, "Cannot disable Master Code while dependent cheats are active.");
                return false;
            }
        }

        mCurrentCheats.set(index, target.withEnabled(enabled));

        // If turning ON a child cheat, ensure master code is also ON
        if (!target.isMasterCode() && enabled) {
            for (int i = 0; i < mCurrentCheats.size(); ++i) {
                CheatItem item = mCurrentCheats.get(i);
                if (item.isMasterCode() && !item.isEnabled()) {
                    mCurrentCheats.set(i, item.withEnabled(true));
                    mEmuThread.queueCheatCommand(CheatCommand.setEnabled(i, true));
                }
            }
        }

        mEmuThread.queueCheatCommand(CheatCommand.setEnabled(index, enabled));
        saveCurrentToggleStates();
        return true;
    }

    private void pushAllToEmulationThread() {
        int count = mCurrentCheats.size();
        String[] names = new String[count];
        String[][] codeLines = new String[count][];
        boolean[] enabledFlags = new boolean[count];

        for (int i = 0; i < count; ++i) {
            CheatItem item = mCurrentCheats.get(i);
            names[i] = item.getName();
            codeLines[i] = item.getCodes().toArray(new String[0]);
            enabledFlags[i] = item.isEnabled();
        }

        mEmuThread.queueCheatCommand(CheatCommand.loadAll(names, codeLines, enabledFlags));
    }

    private void saveCurrentToggleStates() {
        if (mActiveRom == null) return;
        JSONObject obj = new JSONObject();
        try {
            for (CheatItem item : mCurrentCheats) {
                obj.put(item.getId(), item.isEnabled());
            }
            String saveKey = "cheats_state_" + mActiveRom.gameCode;
            mContext.getSharedPreferences("frogemu_cheats", Context.MODE_PRIVATE)
                    .edit()
                    .putString(saveKey, obj.toString())
                    .apply();
        } catch (Exception ignored) {}
    }

    private List<CheatItem> findAndParseCheats(RomMatcher.RomMetadata meta, File romFile) {
        // 1. Check for adjacent .cht file next to ROM on storage
        if (romFile != null && romFile.getParent() != null) {
            String baseName = romFile.getName();
            int dot = baseName.lastIndexOf('.');
            String nameNoExt = dot > 0 ? baseName.substring(0, dot) : baseName;
            File adjacentCht = new File(romFile.getParent(), nameNoExt + ".cht");
            if (adjacentCht.exists() && adjacentCht.isFile()) {
                try (InputStream is = new FileInputStream(adjacentCht)) {
                    List<CheatItem> res = LibretroChtParser.parse(is, EmulationSystem.GBA, "Local Adjacent File");
                    if (!res.isEmpty()) return res;
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading adjacent .cht file: " + adjacentCht, e);
                }
            }
        }

        // 2. Check downloaded cheats database in internal storage (cheats/gba/)
        File downloadedGbaDir = new File(mContext.getFilesDir(), "cheats/gba");
        if (downloadedGbaDir.exists() && downloadedGbaDir.isDirectory()) {
            File match = findMatchingChtFile(downloadedGbaDir, meta, romFile);
            if (match != null && match.exists()) {
                try (InputStream is = new FileInputStream(match)) {
                    List<CheatItem> res = LibretroChtParser.parse(is, EmulationSystem.GBA, "Libretro DB");
                    if (!res.isEmpty()) return res;
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading downloaded .cht file: " + match, e);
                }
            }
        }

        // 3. Check bundled asset database by Game Code (e.g. "BPEE.cht")
        AssetManager am = mContext.getAssets();
        String gameCodeAsset = "cheats/gba/" + meta.gameCode.toUpperCase() + ".cht";
        try (InputStream is = am.open(gameCodeAsset)) {
            List<CheatItem> res = LibretroChtParser.parse(is, EmulationSystem.GBA, "Bundled Pack");
            if (!res.isEmpty()) return res;
        } catch (Exception ignored) {}

        return Collections.emptyList();
    }

    private File findMatchingChtFile(File dir, RomMatcher.RomMetadata meta, File romFile) {
        // Direct GameCode match (e.g. "BPEE.cht")
        File directCode = new File(dir, meta.gameCode + ".cht");
        if (directCode.exists()) return directCode;

        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".cht"));
        if (files == null || files.length == 0) return null;

        // Clean tokens from ROM title and filename
        String title = meta.title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", " ").trim();
        String filename = romFile != null ? romFile.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", " ").trim() : "";

        String[] titleTokens = title.split("\\s+");
        String[] filenameTokens = filename.split("\\s+");

        File bestMatch = null;
        int bestScore = 0;

        for (File f : files) {
            String fname = f.getName().toLowerCase(Locale.ROOT);
            int score = 0;

            for (String token : titleTokens) {
                if (token.length() >= 3 && fname.contains(token)) {
                    score += 2;
                }
            }
            for (String token : filenameTokens) {
                if (token.length() >= 3 && fname.contains(token)) {
                    score += 1;
                }
            }

            if (score > bestScore && score >= 2) {
                bestScore = score;
                bestMatch = f;
            }
        }

        return bestMatch;
    }
}
