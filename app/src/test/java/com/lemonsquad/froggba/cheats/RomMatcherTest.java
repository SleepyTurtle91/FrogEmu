package com.lemonsquad.froggba.cheats;

import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class RomMatcherTest {

    @Test
    public void testInspectValidGbaRom() throws Exception {
        File tempRom = File.createTempFile("test_emerald", ".gba");
        tempRom.deleteOnExit();

        byte[] romData = new byte[512];
        // Title at 0xA0
        byte[] titleBytes = "POKEMON EMER".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(titleBytes, 0, romData, 0xA0, titleBytes.length);

        // GameCode at 0xAC
        byte[] codeBytes = "BPEE".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(codeBytes, 0, romData, 0xAC, codeBytes.length);

        // Version at 0xBC
        romData[0xBC] = 0x01;

        try (FileOutputStream fos = new FileOutputStream(tempRom)) {
            fos.write(romData);
        }

        RomMatcher.RomMetadata meta = RomMatcher.inspectRom(tempRom);
        assertEquals("POKEMON EMER", meta.title);
        assertEquals("BPEE", meta.gameCode);
        assertEquals(1, meta.version);
        assertNotEquals(0, meta.crc32);
    }

    @Test
    public void testInspectTruncatedRom() throws Exception {
        File shortRom = File.createTempFile("short", ".bin");
        shortRom.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(shortRom)) {
            fos.write(new byte[]{1, 2, 3});
        }

        RomMatcher.RomMetadata meta = RomMatcher.inspectRom(shortRom);
        assertEquals("SHORT", meta.gameCode);
    }
}
