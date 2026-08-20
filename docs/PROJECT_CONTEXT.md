# 🐸 FrogEmu — Project Context & Engineering Memory

> **System**: FrogEmu (Multi-System Handheld Emulator for Android)  
> **Target Device**: Anbernic RG556 Handheld Gaming Console (1080p 16:9 OLED, Unisoc T820, Android 13)  
> **Repository**: [https://github.com/SleepyTurtle91/FrogEmu](https://github.com/SleepyTurtle91/FrogEmu)  
> **Latest Release**: `v1.1.0-preview`  
> **License**: Mozilla Public License 2.0 (MPL-2.0) for mGBA core components; Apache 2.0 / Proprietary for frontend layers.

---

## 🏛️ Threading & Concurrency Model (The 4 Invariants)

1. **C/C++ ABI Alignment**: Wrapper and `libmgba` share exact synchronized compilation flags.
2. **Core Ownership & Double-Buffering**: `EmulationThread` solely owns `mCore` at ~59.73 Hz. GL thread only reads from `g_displayBuffer`. Audio reads into `ArrayBlockingQueue`.
3. **On-Demand Link Subsystem**: Zero background threads or sockets when Link is `OFF`.
4. **Provider-Adapter Decoupling**: "Providers provide data. Adapters translate it. EmulationThread executes it."

---

## 🎮 Subsystems & Platform State

### 1. Core Emulation
- Headless **mGBA v0.11-dev** ARM7TDMI cycle-accurate core.
- Battery SRAM saving verified (`/data/user/0/com.lemonsquad.froggba/files/`).

### 2. Display & Video
- OpenGL ES 2.0 / 3.0 double-buffered renderer.
- 3:2 aspect ratio preservation on 16:9 widescreen.
- Shaders: **Nearest (1:1 Reference)** & **Scale2x** retro sub-pixel edge scaler.

### 3. Audio Pipeline
- 16-bit stereo PCM at ~32.7 kHz.
- Thread-safe ring buffer handoff to `android.media.AudioTrack`. Zero buffer crashes across background/resume cycles.

### 4. Input & Physical Controls (Phase 2 Completed)
- Native Anbernic RG556 physical gamepad support.
- Multi-key reference counting (`mGbaKeyRefCounts`) eliminates button shadowing.
- Hardware SOCD neutralization (Left+Right -> Neutral, Up+Down -> Neutral).
- 5% deadzone hysteresis band for analog sticks.
- 32-bit packed atomic keymask with non-destructive 30 Hz turbo pulsing.
- Presets: Standard GBA, SNES Retro (`Y=B`, `B=A`, `A=Turbo A`, `X=Turbo B`), and Custom Press-to-Bind UI.

### 5. Settings Control Plane (Phase 1 Completed)
- Landscape two-pane layout tailored for RG556.
- Left-hand category navigation (Display, Controls, Audio, Link, Saves, Cheats, About).
- Persistent `FrogEmuSettings` repository.

### 6. Link Cable Engine
- Native `struct GBASIODriver` attached to `mPERIPH_GBA_LINK_PORT`.
- 4-player binary protocol (`LinkPacket`) + in-process `LoopbackTransport`.
- Real-time 500ms Link Diagnostics UI.

### 7. Cheat Subsystem Blueprint (Phase 3 Planned)
- Provider-Adapter model (`CheatProvider` -> `CheatRepository` -> `mGBA Cheat Adapter` -> `struct mCheatDevice`).
- 5-tier ROM matching cascade (CRC32 -> Game Code + Version -> 4-char Game Code -> Sanitized Title -> Adjacent File).
- Libretro `.cht` format parsing with multi-line `+` code expansion.
