package com.lemonsquad.froggba.cheats;

/**
 * Encapsulates a cheat state mutation to be safely executed on EmulationThread.
 */
public class CheatCommand {

    public enum Type {
        LOAD_ALL,
        SET_ENABLED,
        CLEAR_ALL
    }

    public final Type type;
    public final int index;
    public final boolean enabled;
    public final String[] names;
    public final String[][] codeLines;
    public final boolean[] enabledFlags;

    public static CheatCommand loadAll(String[] names, String[][] codeLines, boolean[] enabledFlags) {
        return new CheatCommand(Type.LOAD_ALL, 0, false, names, codeLines, enabledFlags);
    }

    public static CheatCommand setEnabled(int index, boolean enabled) {
        return new CheatCommand(Type.SET_ENABLED, index, enabled, null, null, null);
    }

    public static CheatCommand clearAll() {
        return new CheatCommand(Type.CLEAR_ALL, 0, false, null, null, null);
    }

    private CheatCommand(Type type, int index, boolean enabled,
                         String[] names, String[][] codeLines, boolean[] enabledFlags) {
        this.type = type;
        this.index = index;
        this.enabled = enabled;
        this.names = names;
        this.codeLines = codeLines;
        this.enabledFlags = enabledFlags;
    }
}
