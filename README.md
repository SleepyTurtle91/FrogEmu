# 🐸 FrogEmu

> **Multi-System Handheld Emulator for Android**  
> Engineered for dedicated Android handhelds (Anbernic RG556) with cycle-accurate core emulation, decoupled multi-threading architecture, low-latency audio, modular shader pipelines, cheat engine, instant save states, and on-demand local link cable multiplayer.

---

## 🌟 Features

### 🟢 Active & Built
- **GBA Core**: Headless **mGBA v0.11-dev** engine integration (MPL-2.0).
- **Concurrency Architecture**: Dedicated `EmulationThread` exclusively owns the mGBA core, decoupling 59.73 Hz GBA timing from Android display refresh rate.
- **Display Pipeline (Phase 4)**:
  - **Exact Integer Scaling**: `Integer 6× (1440×960)`, `Integer 5× (1200×800)`, `Integer 4× (960×640)`, and `Aspect Fit 3:2 (1620×1080)`.
  - **Modular Shaders**: `Pixel-Perfect Nearest` (100% authentic color & hard edges), `Authentic GBA LCD Matrix Grid`, `Retro Scanlines`, `Clean EPX / Scale2x`, and `Bilinear`.
- **Physical Controls & Custom Mapping (Phase 2)**:
  - Multi-key reference counting, hardware SOCD neutralization, analog deadzone hysteresis, and 30 Hz turbo pulsing.
  - Standard GBA, SNES Retro, and Custom Press-to-Bind profiles.
- **Audio Streaming**: Real-time 16-bit stereo PCM audio pipeline at 32.7 kHz using thread-safe ring-buffer handoff to `AudioTrack`.
- **Native Cheat Engine (Phase 3)**:
  - Virtualization bridge for `struct mCheatDevice` (GameShark Advance, CodeBreaker, Action Replay v3, Raw).
  - Libretro `.cht` stream parser & 5-tier ROM matcher.
  - Built-in Online Libretro Cheats Downloader (500+ game databases) + 14 bundled offline databases.
- **Instant Save States (Phase 8)**:
  - Native binary snapshots via `g_core->saveState()` and `g_core->loadState()`.
  - 5 dedicated slots per title (`Slot 0 Quick Save` + `Slots 1..4`) with real-time timestamps and file size badges.
- **Link Multiplayer Subsystem**: Hardware GBA Serial I/O (`GBASIODriver`) hook with in-process **Loopback Transport** and real-time live **Link Diagnostics**.
- **Settings Control Plane**: Landscape two-pane configuration dashboard with persistent storage.

### 🔵 Planned Roadmap
- **Systems**: Game Boy (GB), Game Boy Color (GBC), PlayStation (PS1), PlayStation Portable (PSP)
- **Connectivity**: On-Demand Wi-Fi LAN / Hotspot and Bluetooth Socket Transports
- **Library**: Multi-directory ROM Scanner with Box Art Metadata

---

## 🏛️ Core Architecture & Invariants

```text
┌────────────────────────────────────────────────────────┐
│                   Android UI Thread                    │
│   - Game View (Clean, Immersive 3:2 Display)           │
│   - Settings ⚙️ Control Plane (FrogEmuSettings)        │
│   - ROM Picker & File Management                       │
└───────────────────────────┬────────────────────────────┘
                            │ (Atomic / Preferences / Queues)
                            ▼
┌────────────────────────────────────────────────────────┐
│                    EmulationThread                     │
│   - Sole Owner of mCore lifecycle                      │
│   - Independent Clock Timing (~59.7275 Hz)             │
│   - Polls Input Bitmask (Normal + 30Hz Turbo Blending) │
│   - Executes stepFrameJNI()                            │
│   - Publishes Display Framebuffer (memcpy back→front)  │
│   - Pushes Audio Chunks to Queue                       │
│   - Mediates SIO Transfer Injections                   │
│   - Executes Native Cheat Hooks & Save State Tasks     │
└───────┬───────────────────┬────────────────────┬───────┘
        │                   │                    │
 (Display Front-Buffer) (Audio Queue)   (Link SIO Hand-off)
        ▼                   ▼                    ▼
┌──────────────┐    ┌──────────────┐     ┌──────────────┐
│  GL Thread   │    │ AudioThread  │     │ LinkManager  │
│  - Render    │    │ - AudioTrack │     │ (On-Demand)  │
│  - Shaders   │    │ - 32.7 kHz   │     └──────┬───────┘
│  - Viewports │    └──────────────┘            │
└──────────────┘                         ┌──────┴──────┐
                                         ▼             ▼
                                     Wi-Fi LAN     Bluetooth
                                     (Sockets      (RFCOMM
                                      when ON)     when ON)
```

1. **C/C++ ABI Alignment**: Wrapper and `libmgba` share synchronized compiler flags.
2. **Core Ownership & Double-Buffering**: `EmulationThread` solely owns the core; GL reads the front buffer; neither thread accesses the other's active buffer.
3. **On-Demand Link Subsystem**: Link networking is zero-overhead when `OFF` (no background sockets, no polling threads).
4. **Provider-Adapter Decoupling**: "Providers provide data. Adapters translate it. EmulationThread executes it."

---

## 🛠️ Building

### Prerequisites
- Android Studio / Android SDK (API 34)
- Android NDK (r25c or newer)
- CMake 3.22.1+

### Clone & Build
```bash
git clone --recursive https://github.com/SleepyTurtle91/FrogEmu.git
cd FrogEmu
./gradlew assembleDebug
```

---

## 📜 Licenses & Attribution

- **mGBA Core Engine**: [Mozilla Public License 2.0 (MPL-2.0)](https://github.com/mgba-emu/mgba/blob/master/LICENSE) — Copyright © 2013–2024 Jeffrey Pfau & contributors.
- **AndroidX & Google Material Components**: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
- **Third-Party Subcomponents** (`libpng`, `zlib`, `lzma`): Permissive zlib/libpng/BSD Licenses.

---

*Developed by LemonSquad • Built for Anbernic RG556 & Android Handhelds*
