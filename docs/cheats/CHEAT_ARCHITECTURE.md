# 🧪 FrogEmu Cheat Engine Architecture

## 1. Overview
This document outlines the architecture for FrogEmu's Cheat Subsystem, connecting Libretro `.cht` databases to mGBA's native `struct mCheatDevice` hardware virtualization engine.

---

## 2. Decoupled 3-Layer System

```text
┌────────────────────────────────────────────────────────┐
│                   FrogEmu Settings UI                  │
│   - Cheats sub-panel (List cheats, Enable/Disable)     │
│   - Add Custom Cheat (GameShark, CodeBreaker, Raw)     │
│   - Database status & active game matching info        │
└───────────────────────────┬────────────────────────────┘
                            │ (Java CheatItem list)
                            ▼
┌────────────────────────────────────────────────────────┐
│                    CheatRepository                     │
│   - 5-Tier ROM Matcher (CRC32 → GameCode → Title)      │
│   - Libretro .cht Parser (splits '+' multi-line codes) │
│   - Per-Game Persistent State (.json / Preferences)    │
└───────────────────────────┬────────────────────────────┘
                            │ (Synchronized on EmulationThread)
                            ▼
┌────────────────────────────────────────────────────────┐
│             EmulationThread & Native mGBA              │
│   - g_core->cheatDevice(g_core)                        │
│   - GBACheatSet & GBACheatHook execution               │
│   - Automated Master Code / IRQ Hook injection         │
│   - Frame-boundary Raw memory writes                   │
└───────────────────────────┘
```

---

## 3. Supported Formats & Protocols

1. **CodeBreaker / GameShark SP**: 12 hex digits (`XXXXXXXX YYYY`), dynamic PRNG tables, master codes, slide/fill, and button jokers.
2. **GameShark Advance / Action Replay v1/v2**: 16 hex digits (`XXXXXXXX YYYYYYYY`), seed encryption, ARM/Thumb opcode interception (`GBACheatHook`).
3. **Action Replay v3 / AR MAX**: 16 hex digits (`XXXXXXXX YYYYYYYY`), PARv3 tables, pointer codes.
4. **VBA / Raw Memory**: Colon-separated or direct hex writes (`02XXXXXX:YYYY` EWRAM, `03XXXXXX:YYYY` IWRAM).

---

## 4. 5-Tier ROM Matching Cascade

1. **Tier 1: Full-ROM CRC32 / SHA-1** (Matches exact revision / hack)
2. **Tier 2: 4-Character Game Code + Version Byte** (e.g. `BPEE` + `0x01` for Emerald v1.1)
3. **Tier 3: 4-Character Game Code** (e.g. `BPEE` for Emerald USA)
4. **Tier 4: Sanitized Game Title** (e.g. `POKEMON EMER`)
5. **Tier 5: Adjacent File** (`<ROM_Name>.cht` adjacent to ROM on storage)

---

## 5. Invariants & Thread Safety

- **Sole Core Owner**: All `mCheatDevice` calls occur strictly on `EmulationThread`.
- **Zero Overhead when OFF**: No hooks installed when cheat list is disabled.
- **Save-State Preservation**: Cheat hooks are temporarily lifted during save-state serialization.
