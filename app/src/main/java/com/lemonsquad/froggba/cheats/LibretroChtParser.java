package com.lemonsquad.froggba.cheats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream parser for Libretro (.cht) cheat database files.
 */
public class LibretroChtParser {

    private static class RawEntry {
        String desc = null;
        String code = null;
        boolean enable = false;
    }

    public static List<CheatItem> parse(InputStream is, EmulationSystem system, String providerName) throws IOException {
        if (is == null) return Collections.emptyList();

        Map<Integer, RawEntry> entryMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }
            if (!line.startsWith("cheat")) {
                continue;
            }

            int underscoreIdx = line.indexOf('_', 5);
            if (underscoreIdx == -1) {
                continue;
            }

            String indexStr = line.substring(5, underscoreIdx);
            int index;
            try {
                index = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                continue;
            }

            int eqIdx = line.indexOf('=', underscoreIdx);
            if (eqIdx == -1) {
                continue;
            }

            String key = line.substring(underscoreIdx + 1, eqIdx).trim().toLowerCase();
            String val = line.substring(eqIdx + 1).trim();

            // Strip surrounding quotes
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }

            RawEntry entry = entryMap.get(index);
            if (entry == null) {
                entry = new RawEntry();
                entryMap.put(index, entry);
            }

            switch (key) {
                case "desc":
                    entry.desc = val;
                    break;
                case "code":
                    entry.code = val;
                    break;
                case "enable":
                    entry.enable = "true".equalsIgnoreCase(val) || "1".equals(val);
                    break;
            }
        }

        List<CheatItem> result = new ArrayList<>();
        int maxIndex = -1;
        for (int idx : entryMap.keySet()) {
            if (idx > maxIndex) maxIndex = idx;
        }

        for (int i = 0; i <= maxIndex; ++i) {
            RawEntry entry = entryMap.get(i);
            if (entry == null || entry.desc == null || entry.code == null) {
                continue;
            }

            // Split multi-line codes separated by '+'
            String[] rawCodes = entry.code.split("\\+");
            List<String> codeLines = new ArrayList<>();
            for (String c : rawCodes) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty()) {
                    codeLines.add(trimmed);
                }
            }

            if (!codeLines.isEmpty()) {
                String id = "cheat_" + i;
                result.add(new CheatItem(
                        id,
                        entry.desc,
                        "",
                        system != null ? system : EmulationSystem.GBA,
                        providerName != null ? providerName : "Libretro",
                        codeLines,
                        entry.enable
                ));
            }
        }

        return result;
    }
}
