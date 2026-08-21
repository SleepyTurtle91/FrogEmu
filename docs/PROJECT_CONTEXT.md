# 🐸 FrogEmu — Project Context & Engineering Memory

> **System**: FrogEmu (Multi-System Handheld Emulator for Android)  
> **Target Device**: Anbernic RG556 Handheld Gaming Console (1080p 16:9 OLED, Unisoc T820, Android 13)  
> **Repository**: [https://github.com/SleepyTurtle91/FrogEmu](https://github.com/SleepyTurtle91/FrogEmu)  
> **Latest Release**: `v1.4.0`  
> **License**: Mozilla Public License 2.0 (MPL-2.0) for mGBA core components; Apache 2.0 / Proprietary for frontend layers.

---

## 🏛️ Threading & Concurrency Model (The 4 Invariants)

1. **C/C++ ABI Alignment**: Wrapper and `libmgba` share exact synchronized compilation flags (`-fPIC`, ARM64/ARMv7 NEON).
2. **Core Ownership & Double-Buffering**: `EmulationThread` solely owns `mCore` at ~59.73 Hz. GL thread only reads from `g_displayBuffer`. Audio reads into `ArrayBlockingQueue`. No external thread touches `mCore`.
3. **On-Demand Link Subsystem**: Zero background threads or sockets when Link is `OFF`.
4. **Provider-Adapter Decoupling**: *"Providers provide data. Adapters translate it. EmulationThread executes it."*

---

## 🎮 Subsystems & Platform State

### 1. Core Emulation (Mature GBA Milestone)
- Headless **mGBA v0.11-dev** ARM7TDMI cycle-accurate core on dedicated `EmulationThread`.
- Battery SRAM saving verified (`/data/user/0/com.lemonsquad.froggba/files/`).

### 2. Display & Video Pipeline (Phase 4 Completed — v1.3.0)
- **Architecture**: `mGBA = execution`, `FrogEmu = presentation`.
- **Integer Scaling Geometry**:
  - `Aspect Fit 3:2` (1620×1080 on 16:9 RG556 display)
  - `Integer Scaling 6×` (1440×960 — Uniform pixel grid, zero shimmer/distortion)
  - `Integer Scaling 5×` (1200×800)
  - `Integer Scaling 4×` (960×640)
  - `Full Screen Stretch` (1920×1080)
- **Modular Shaders & Filters**:
  - `Pixel-Perfect Nearest` (Strict raw GBA pixels, 100% color accuracy, hard edges, zero oil-paint blur)
  - `Authentic GBA LCD Matrix Grid` (Sub-pixel LCD grid matrix)
  - `Scanlines` (Retro handheld & CRT scanline shader)
  - `Clean EPX / Scale2x` (Pure color-matching pixel expansion without oil-paint blur)
  - `Bilinear` (Smooth edge interpolation)

### 3. Audio Pipeline
- 16-bit stereo PCM at ~32.7 kHz.
- Thread-safe ring buffer handoff to `android.media.AudioTrack`. Zero buffer crashes across background/resume cycles.

### 4. Input & Physical Controls (Phase 2 Completed — v1.1.0)
- Native Anbernic RG556 physical gamepad support.
- Multi-key reference counting (`mGbaKeyRefCounts`) eliminates button shadowing.
- Hardware SOCD neutralization (Left+Right -> Neutral, Up+Down -> Neutral).
- 5% deadzone hysteresis band for analog sticks.
- 32-bit packed atomic keymask with non-destructive 30 Hz turbo pulsing.
- Presets: Standard GBA, SNES Retro (`Y=B`, `B=A`, `A=Turbo A`, `X=Turbo B`), and Custom Press-to-Bind UI.

### 5. Settings Control Plane (Phase 1 Completed)
- Landscape two-pane layout tailored for RG556.
- Modular category navigation (`Display`, `Controls`, `Audio`, `Link`, `Saves`, `Cheats`, `About`).
- Persistent `FrogEmuSettings` repository.

### 6. Link Cable Engine
- Native `struct GBASIODriver` attached to `mPERIPH_GBA_LINK_PORT`.
- 4-player binary protocol (`LinkPacket`) + in-process `LoopbackTransport`.
- Real-time 500ms Link Diagnostics UI.

### 7. Native Cheat Subsystem (Phase 3 Completed — v1.2.0 / v1.2.1 / v1.2.2)
- Direct JNI virtualization to `struct mCheatDevice` (GameShark Advance, CodeBreaker, Action Replay v3, Raw memory).
- Stream-based Libretro `.cht` parser with multi-line `+` code support.
- 5-Tier ROM Matching Cascade (CRC32 -> Game Code + Version -> 4-char Game Code -> Sanitized Title -> Adjacent File).
- Master Code Dependency Manager: auto-enables master code when child cheats turn on, blocks disabling master code while child cheats remain active.
- **Built-in Online Libretro Cheats DB Downloader**: Streams and extracts 500+ GBA `.cht` files directly from `buildbot.libretro.com` into internal storage.
- 14 pre-packaged bundled offline databases for major commercial games.

### 8. Instant Save State Subsystem (Phase 8 Completed — v1.4.0)
- Direct binary snapshot serialization via `g_core->saveState()` and `g_core->loadState()` (~384 KB).
- 5 dedicated slots per game (`Slot 0 Quick Save`, `Slot 1`, `Slot 2`, `Slot 3`, `Slot 4`).
- Non-blocking `mStateQueue` processed at frame boundary step 4.6 on `EmulationThread`.
- Real-time slot metadata (timestamps, file sizes) with `[ 💾 Save ]`, `[ 📂 Load ]`, and `[ 🗑️ Delete ]` actions.

---

## 🗺️ Roadmap & Remaining Milestones

```text
┌────────────────────────────────────────────────────────┐
│               FrogEmu Roadmap Overview                 │
├───────────────────────────────────┬────────────────────┤
│ Milestone                         │ Status             │
├───────────────────────────────────┼────────────────────┤
│ Phase 1: Two-Pane Settings Plane  │ ✅ Completed (v1.0)│
│ Phase 2: RG556 Controller Mapping │ ✅ Completed (v1.1)│
│ Phase 3: Native Cheat Engine & DB │ ✅ Completed (v1.2)│
│ Phase 4: Display Scaling & Shaders│ ✅ Completed (v1.3)│
│ Phase 8: Save States & Slots      │ ✅ Completed (v1.4)│
│ Phase 5: Real-Game SIO Validation │ ⏳ Queued          │
│ Phase 6: Wi-Fi LAN / Hotspot Link │ ⏳ Queued          │
│ Phase 7: Bluetooth Transport      │ ⏳ Queued          │
│ Multi-System: GB / GBC Cores      │ ⏳ Future          │
│ Multi-System: PS1 / PSP Cores     │ ⏳ Future          │
└───────────────────────────────────┴────────────────────┘
```
