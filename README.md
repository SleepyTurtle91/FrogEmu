# 🐸 FrogEmu

> **Multi-System Handheld Emulator for Android**  
> Engineered for dedicated Android handhelds (Anbernic RG556) with cycle-accurate core emulation, decoupled multi-threading architecture, low-latency audio, modular shader pipelines, and on-demand local link cable multiplayer.

---

## 🌟 Features

### 🟢 Active & Built
- **GBA Core**: Headless **mGBA v0.11-dev** engine integration (MPL-2.0).
- **Concurrency Architecture**: Dedicated `EmulationThread` exclusively owns the mGBA core, decoupling 59.73 Hz GBA timing from Android display refresh rate.
- **Display Pipeline**: Double-buffered zero-copy GLES rendering with modular shader hot-swapping (**Nearest 1:1** reference and **Scale2x** retro sub-pixel edge scaler).
- **Physical Controls**: First-class support for **Anbernic RG556** hardware controls with automatic on-screen touch overlay auto-hiding.
- **Audio Streaming**: Real-time 16-bit stereo PCM audio pipeline at 32.7 kHz using thread-safe ring-buffer handoff to `AudioTrack`.
- **Link Multiplayer Subsystem**: Hardware GBA Serial I/O (`GBASIODriver`) hook with in-process **Loopback Transport** and real-time live **Link Diagnostics**.
- **Settings Control Plane**: Landscape two-pane configuration dashboard with persistent `SharedPreferences` storage.

### 🔵 Planned Roadmap
- **Systems**: Game Boy (GB), Game Boy Color (GBC), PlayStation (PS1), PlayStation Portable (PSP)
- **Graphics**: **Vulkan Renderer Backend**, Scanlines, CRT, LCD Pixel Grid, HQ2x, xBRZ
- **Connectivity**: On-Demand Wi-Fi LAN / Hotspot and Bluetooth Socket Transports
- **Tools**: Multi-slot Save States with snapshot previews, Libretro `cheats.db` engine, Cover Art ROM Library

---

## 🏛️ Core Architecture & Invariants

```text
┌────────────────────────────────────────────────────────┐
│                   Android UI Thread                    │
│   - Game View (Clean, Immersive 3:2 Display)           │
│   - Settings ⚙️ Control Plane (FrogEmuSettings)         │
│   - ROM Picker & File Management                       │
└───────────────────────────┬────────────────────────────┘
                            │ (Atomic / Preferences)
                            ▼
┌────────────────────────────────────────────────────────┐
│                    EmulationThread                     │
│   - Sole Owner of mCore lifecycle                      │
│   - Independent Clock Timing (~59.7275 Hz)             │
│   - Polls Input Bitmask                                │
│   - Executes stepFrameJNI()                            │
│   - Publishes Display Framebuffer (memcpy back→front)  │
│   - Pushes Audio Chunks to Queue                       │
│   - Mediates SIO Transfer Injections                   │
└───────┬───────────────────┬────────────────────┬───────┘
        │                   │                    │
 (Display Front-Buffer) (Audio Queue)   (Link SIO Hand-off)
        ▼                   ▼                    ▼
┌──────────────┐    ┌──────────────┐     ┌──────────────┐
│  GL Thread   │    │ AudioThread  │     │ LinkManager  │
│  - Render    │    │ - AudioTrack │     │ (On-Demand)  │
│  - Shaders   │    │ - 32.7 kHz   │     └──────┬───────┘
└──────────────┘    └──────────────┘            │
                                         ┌──────┴──────┐
                                         ▼             ▼
                                     Wi-Fi LAN     Bluetooth
                                     (Sockets      (RFCOMM
                                      when ON)     when ON)
```

1. **C/C++ ABI Alignment**: Wrapper and `libmgba` share synchronized compiler flags.
2. **Core Ownership & Double-Buffering**: `EmulationThread` solely owns the core; GL reads the front buffer; neither thread accesses the other's active buffer.
3. **On-Demand Link Subsystem**: Link networking is zero-overhead when `OFF` (no background sockets, no polling threads).

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
