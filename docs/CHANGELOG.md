# 📝 FrogEmu Changelog

All notable changes to **FrogEmu** are documented in this file.

---

## [v1.1.0-preview] — 2026-08-20

### Added
- **Phase 2: RG556 Controller Custom Mapping**:
  - Interactive press-to-bind UI in Settings Control Plane.
  - Built-in controller presets: `Standard GBA`, `SNES Retro Layout`, and `Custom Mapping`.
  - Multi-key reference counting (`int[10] mGbaKeyRefCounts`) preventing button shadowing bugs.
  - Hardware SOCD neutralization (Left+Right -> Neutral, Up+Down -> Neutral).
  - Analog stick deadzone tuning slider with 5% anti-jitter hysteresis.
  - 32-bit packed atomic keymask with non-destructive 30 Hz turbo engine on `EmulationThread`.
- **Cheat Subsystem Architecture**:
  - Provider-Adapter model documented in `docs/cheats/CHEAT_ARCHITECTURE.md`.
  - 5-tier ROM matching cascade (CRC32 -> GameCode -> Title).
- **Unit Test Suite**:
  - `InputProfileSerializationTest` covering presets, JSON roundtrip, and corrupted JSON fallback (100% pass rate).

### Changed
- Rebranded entire project to **FrogEmu** across UI, Settings, and Logcat tags (`FrogEmu_Emu`, `FrogEmu_Render`, `FrogEmu_Link`).
- Renamed settings repository to `FrogEmuSettings.java`.

---

## [v1.0.0-preview] — 2026-08-20

### Added
- **Initial Public Release of FrogEmu**:
  - Headless **mGBA v0.11-dev** core integration (MPL-2.0).
  - Dedicated `EmulationThread` owning `mCore` at ~59.73 Hz.
  - Double-buffered zero-copy GLES video pipeline with 3:2 aspect ratio preservation.
  - Modular shader pipeline: **Nearest (1:1 Reference)** & **Scale2x** retro edge scaler.
  - Low-latency 16-bit PCM audio pipeline at 32.7 kHz streaming to `AudioTrack`.
  - Anbernic RG556 physical gamepad support with auto-hiding touch overlay.
  - Built-in SIO Link Adapter (`GBASIODriver`) with in-process **Loopback Transport** and real-time live Link Diagnostics.
  - Two-pane landscape **Settings Control Plane** with modular panels.
