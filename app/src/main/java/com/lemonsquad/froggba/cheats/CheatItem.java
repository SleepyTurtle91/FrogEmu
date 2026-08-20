package com.lemonsquad.froggba.cheats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable data model representing a single cheat feature or code set.
 */
public class CheatItem {

    private final String id;
    private final String name;
    private final String description;
    private final EmulationSystem system;
    private final String provider;
    private final List<String> codes;
    private final boolean isMasterCode;
    private final boolean enabled;

    public CheatItem(String id, String name, String description,
                     EmulationSystem system, String provider,
                     List<String> codes, boolean enabled) {
        this.id = id;
        this.name = name != null ? name : "Unnamed Cheat";
        this.description = description != null ? description : "";
        this.system = system != null ? system : EmulationSystem.GBA;
        this.provider = provider != null ? provider : "Libretro";
        this.codes = Collections.unmodifiableList(new ArrayList<>(codes != null ? codes : Collections.emptyList()));
        this.isMasterCode = detectMasterCode(this.name);
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public EmulationSystem getSystem() { return system; }
    public String getProvider() { return provider; }
    public List<String> getCodes() { return codes; }
    public boolean isMasterCode() { return isMasterCode; }
    public boolean isEnabled() { return enabled; }

    public CheatItem withEnabled(boolean newEnabled) {
        return new CheatItem(id, name, description, system, provider, codes, newEnabled);
    }

    private static boolean detectMasterCode(String title) {
        if (title == null) return false;
        String lower = title.toLowerCase();
        return lower.contains("must be on") ||
               lower.contains("enable code") ||
               lower.contains("master code") ||
               lower.contains("(m)") ||
               lower.contains("[m]");
    }
}
