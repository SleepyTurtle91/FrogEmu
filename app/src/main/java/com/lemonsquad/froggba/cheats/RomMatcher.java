package com.lemonsquad.froggba.cheats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Extracts cartridge metadata and performs 5-tier ROM matching against cheat repositories.
 */
public class RomMatcher {

    public static class RomMetadata {
        public final String title;
        public final String gameCode;      // e.g. "BPEE"
        public final int version;          // e.g. 0x00
        public final long crc32;           // Full ROM CRC32 checksum

        public RomMetadata(String title, String gameCode, int version, long crc32) {
            this.title = title != null ? title : "";
            this.gameCode = gameCode != null ? gameCode : "";
            this.version = version;
            this.crc32 = crc32;
        }

        @Override
        public String toString() {
            return String.format("%s (%s v1.%d) [CRC: %08X]", title, gameCode, version, crc32);
        }
    }

    /**
     * Inspects a GBA ROM header and computes its metadata.
     * Safely bounds-checks against truncated binaries (< 192 bytes).
     */
    public static RomMetadata inspectRom(File romFile) {
        if (romFile == null || !romFile.exists() || !romFile.isFile()) {
            return new RomMetadata("Unknown Game", "UNKNOWN", 0, 0);
        }

        byte[] header = new byte[192];
        int bytesRead = 0;
        long fullCrc = 0;

        try (InputStream is = new FileInputStream(romFile)) {
            bytesRead = is.read(header);

            CRC32 crc = new CRC32();
            crc.update(header, 0, Math.max(0, bytesRead));
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) != -1) {
                crc.update(buf, 0, n);
            }
            fullCrc = crc.getValue();
        } catch (IOException ignored) {}

        if (bytesRead < 192) {
            return new RomMetadata(romFile.getName(), "SHORT", 0, fullCrc);
        }

        // Title at offset 0xA0 (12 bytes ASCII)
        String rawTitle = new String(header, 0xA0, 12, StandardCharsets.US_ASCII).trim();
        String title = sanitizeString(rawTitle);

        // Game code at offset 0xAC (4 bytes ASCII, e.g. "BPEE")
        String rawCode = new String(header, 0xAC, 4, StandardCharsets.US_ASCII).trim();
        String gameCode = sanitizeString(rawCode);

        // Version byte at offset 0xBC
        int version = header[0xBC] & 0xFF;

        return new RomMetadata(
                title.isEmpty() ? romFile.getName() : title,
                gameCode.isEmpty() ? "UNKNOWN" : gameCode,
                version,
                fullCrc
        );
    }

    private static String sanitizeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
