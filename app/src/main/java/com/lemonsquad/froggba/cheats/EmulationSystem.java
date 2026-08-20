package com.lemonsquad.froggba.cheats;

/**
 * Supported emulation systems across FrogEmu.
 */
public enum EmulationSystem {
    GBA("Game Boy Advance", "gba"),
    GB("Game Boy", "gb"),
    GBC("Game Boy Color", "gbc"),
    PS1("PlayStation", "ps1"),
    PSP("PlayStation Portable", "psp");

    private final String displayName;
    private final String directoryKey;

    EmulationSystem(String displayName, String directoryKey) {
        this.displayName = displayName;
        this.directoryKey = directoryKey;
    }

    public String getDisplayName() { return displayName; }
    public String getDirectoryKey() { return directoryKey; }
}
