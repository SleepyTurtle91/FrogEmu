package com.lemonsquad.froggba.cheats;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

public class LibretroChtParserTest {

    @Test
    public void testParseStandardLibretroCht() throws Exception {
        String data = "cheats = 2\n" +
                "\n" +
                "# Comment line\n" +
                "cheat0_desc = \"Master Code (Must Be On)\"\n" +
                "cheat0_code = \"00006FA7+000A+82000000+0000\"\n" +
                "cheat0_enable = false\n" +
                "\n" +
                "cheat1_desc = \"Infinite Money ($999,999)\"\n" +
                "cheat1_code = \"82025BC4+E0FF+82025BC6+05F5\"\n" +
                "cheat1_enable = true\n";

        InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        List<CheatItem> items = LibretroChtParser.parse(is, EmulationSystem.GBA, "Libretro");

        assertEquals(2, items.size());

        CheatItem master = items.get(0);
        assertEquals("Master Code (Must Be On)", master.getName());
        assertTrue(master.isMasterCode());
        assertEquals(4, master.getCodes().size());
        assertEquals("00006FA7", master.getCodes().get(0));
        assertEquals("000A", master.getCodes().get(1));
        assertEquals("82000000", master.getCodes().get(2));
        assertEquals("0000", master.getCodes().get(3));
        assertFalse(master.isEnabled());

        CheatItem money = items.get(1);
        assertEquals("Infinite Money ($999,999)", money.getName());
        assertFalse(money.isMasterCode());
        assertEquals(4, money.getCodes().size());
        assertTrue(money.isEnabled());
    }

    @Test
    public void testParseOutOfOrderAndMalformed() throws Exception {
        String data = "cheat1_enable = true\n" +
                "cheat1_desc = \"Max HP\"\n" +
                "cheat1_code = \"82001234+03E7\"\n" +
                "cheat0_desc = \"Enable Code\"\n" +
                "cheat0_code = \"98765432+12345678\"\n" +
                "cheat0_enable = false\n";

        InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        List<CheatItem> items = LibretroChtParser.parse(is, EmulationSystem.GBA, "Test");

        assertEquals(2, items.size());
        assertEquals("Enable Code", items.get(0).getName());
        assertTrue(items.get(0).isMasterCode());
        assertEquals("Max HP", items.get(1).getName());
        assertTrue(items.get(1).isEnabled());
    }
}
