package com.lemonsquad.froggba.input;

import org.junit.Test;
import static org.junit.Assert.*;

public class InputProfileSerializationTest {

    @Test
    public void testStandardGbaPreset() {
        InputProfile profile = InputProfile.createStandardGba(0.40f);
        assertEquals(InputProfile.Preset.STANDARD_GBA, profile.getPreset());
        assertEquals(0.40f, profile.getDeadzone(), 0.001f);
        assertEquals(InputProfile.GBA_KEY_A, profile.getGbaKey(96)); // BUTTON_A
        assertEquals(InputProfile.GBA_KEY_B, profile.getGbaKey(97)); // BUTTON_B
        assertEquals(InputProfile.GBA_TURBO_A, profile.getTurboKey(99)); // BUTTON_X
        assertEquals(InputProfile.GBA_TURBO_B, profile.getTurboKey(100)); // BUTTON_Y
    }

    @Test
    public void testSnesRetroPreset() {
        InputProfile profile = InputProfile.createSnesRetro(0.50f);
        assertEquals(InputProfile.Preset.SNES_RETRO, profile.getPreset());
        assertEquals(InputProfile.GBA_KEY_A, profile.getGbaKey(97)); // RG556 BUTTON_B -> GBA A
        assertEquals(InputProfile.GBA_KEY_B, profile.getGbaKey(100)); // RG556 BUTTON_Y -> GBA B
        assertEquals(InputProfile.GBA_TURBO_A, profile.getTurboKey(96)); // RG556 BUTTON_A -> Turbo A
        assertEquals(InputProfile.GBA_TURBO_B, profile.getTurboKey(99)); // RG556 BUTTON_X -> Turbo B
    }

    @Test
    public void testJsonRoundtrip() {
        InputProfile original = InputProfile.createStandardGba(0.35f);
        String json = original.toJson();
        assertNotNull(json);
        assertTrue(json.contains("keyMap"));

        InputProfile parsed = InputProfile.fromJson(json, 0.40f);
        assertNotNull(parsed);
        assertEquals(original.getDeadzone(), parsed.getDeadzone(), 0.001f);
        assertEquals(original.getGbaKey(96), parsed.getGbaKey(96));
        assertEquals(original.getTurboKey(99), parsed.getTurboKey(99));
    }

    @Test
    public void testCorruptedJsonFallback() {
        InputProfile fallback = InputProfile.fromJson("{ invalid_json ]", 0.45f);
        assertNotNull(fallback);
        assertEquals(InputProfile.Preset.STANDARD_GBA, fallback.getPreset());
        assertEquals(0.45f, fallback.getDeadzone(), 0.001f);
    }
}
