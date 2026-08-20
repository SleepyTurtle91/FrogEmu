# 📝 FrogEmu Changelog

All notable changes to **FrogEmu** are documented in this file.

---

## [v1.2.2] — 2026-08-20

### Added
- **Expanded Bundled Offline Cheats Database**:
  - Added pre-packaged cheat databases for top-tier GBA commercial titles:
    - *Pokémon Ruby Version* (`AXVE.cht`)
    - *Pokémon Sapphire Version* (`AXPE.cht`)
    - *Pokémon LeafGreen Version* (`BPGE.cht`)
    - *Super Mario Advance 4: Super Mario Bros. 3* (`AX4E.cht`)
    - *Castlevania: Aria of Sorrow* (`AANE.cht`)
    - *The Legend of Zelda: A Link to the Past & Four Swords* (`AZLE.cht`)
    - *Golden Sun* (`AGSE.cht`)
    - *Kirby & The Amazing Mirror* (`AKFE.cht`)
  - All bundled databases work instantly out-of-the-box offline without requiring an online download.

---

## [v1.2.1] — 2026-08-20

### Added
- **Built-in Online Cheats DB Downloader (Libretro)**:
  - Download official Libretro cheat bundle (`https://buildbot.libretro.com/assets/frontend/cheats.zip`) directly within FrogEmu.
  - Streaming zip extractor extracting 500+ GBA cheat databases directly into internal storage (`files/cheats/gba/`).
  - Prepares `files/cheats/gb/` and `files/cheats/gbc/` for future multi-system expansions.
  - Interactive Downloader UI card in **Settings ⚙️ → Cheats Engine** with live percentage progress bar, bytes counter, and automatic game re-matching upon completion.
  - Added `android.permission.INTERNET` to `AndroidManifest.xml`.

---

## [v1.2.0] — 2026-08-20

### Added
- **Phase 3: Mature GBA Milestone — Native Cheat Subsystem**:
  - **Native mGBA Cheat Virtualization**: Native JNI bridge to `struct mCheatDevice` with full support for GameShark Advance, CodeBreaker, Action Replay v3, and VBA/Raw codes.
  - **Provider-Adapter Architecture**: System-agnostic common model (`CheatItem`, `EmulationSystem`) decoupled from file formats.
  - **Libretro `.cht` Stream Parser**: High-performance parser reading directly from `InputStream` (supporting compressed APK assets, content URIs, and local storage) with `+` multi-line code expansion.
  - **5-Tier Fast ROM Matcher**: Cartridge header inspector extracting Game Code (e.g. `BPEE` for Emerald, `BPRE` for FireRed), Version byte, and full-ROM CRC32 with truncated binary guard.
  - **Master Code Dependency Manager**: Automatic activation of required master codes (`(Must Be On)`) when child cheats are toggled on, and safety lock preventing disabling master codes while dependent cheats remain active.
  - **Curated Bundled Cheat Databases**: Pre-packaged starter databases in `assets/cheats/gba/` for *Pokémon Emerald*, *Pokémon FireRed*, *Zelda: The Minish Cap*, *Metroid Fusion*, *Metroid Zero Mission*, and *Mario Kart: Super Circuit*.
  - **Interactive Cheats UI**: Real-time switch list in **Settings ⚙️ → Cheats Engine** with game badges, master code highlights, and "Disable All" button.
  - **Thread-Safe Command Queue**: Zero-lock `ConcurrentLinkedQueue<CheatCommand>` on `EmulationThread` frame boundary step 4.5.
- **Unit Test Suite**:
  - `LibretroChtParserTest` (standard format, comments, out-of-order keys, malformed files).
  - `RomMatcherTest` (valid GBA header extraction, truncated binary bounds safety).

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
