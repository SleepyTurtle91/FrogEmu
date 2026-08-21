# 🐸 FrogEmu Architecture Document

## 0. Core Invariants

### Invariant 1: C/C++ ABI Alignment
> **CRITICAL RULE**: The JNI wrapper (`native-lib.cpp`) and the `libmgba` static/shared library MUST be compiled with the exact same mGBA configuration and compile definitions (e.g., `M_CORE_GBA`, `ENABLE_VFS`).
> Failure to do so causes a C/C++ ABI mismatch where `sizeof(struct mCore)` differs between the wrapper and the library, resulting in memory corruption and immediate `SEGV_MAPERR` crashes when executing function pointers like `core->init()`.

### Invariant 2: Thread Ownership & Framebuffer Publication Contract
> **CRITICAL RULE**: `EmulationThread` is the sole owner of the emulator core (`mCore`). No other thread (UI, GL Render, Audio, or Network) may directly call JNI methods that mutate core state.
> - **Video**: `EmulationThread` writes to an internal back buffer (`g_videoBuffer`). Upon completing a frame, it atomically publishes the frame data to the front buffer (`g_displayBuffer`). The GL Render thread only reads from `g_displayBuffer`. Neither thread accesses the other's active buffer.
> - **Input**: UI and Gamepad inputs are updated atomically via a packed 32-bit `AtomicInteger` in `InputManager` and polled once per frame by `EmulationThread` before `runFrame()`.
> - **Audio**: `EmulationThread` drains audio frames from mGBA into a thread-safe `ArrayBlockingQueue<short[]>`, which the dedicated `AudioThread` consumes without locking the core or the renderer.
> - **Link SIO**: Native `GBASIODriver` intercepts GBA serial transfers. Transfer completion (`GBASIOMultiplayerFinishTransfer()`) is invoked exclusively on `EmulationThread`.
> - **Cheats & State**: Drained from `mCheatQueue` and `mStateQueue` on frame boundary steps 4.5 and 4.6 on `EmulationThread`.

### Invariant 3: On-Demand Link Networking Contract
> **CRITICAL RULE**: Link networking is a built-in on-demand subsystem. No network or Bluetooth socket may be created, retained, or polled while Link Multiplayer is disabled.
> When Link is `OFF`, all transports are stopped, sockets closed, and background threads terminated. FrogEmu operates as a zero-overhead single-player emulator with zero network CPU/battery consumption.

### Invariant 4: Provider-Adapter Decoupling
> **CRITICAL RULE**: External feature databases (Cheats, Box Art, Mappings) MUST be decoupled from execution cores.
> **"Providers provide data. Adapters translate it. EmulationThread executes it."**
> Core virtualization layers never depend on file formats (e.g., `.cht` schemas), and data providers never touch CPU instruction pipelines.

---

## 1. Concurrency & Control Plane Model

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

---

## 2. Video Pipeline & Modular Shaders (Phase 4 — v1.3.0)

- Screen resolution: GBA Native `240x160` (3:2 Aspect Ratio).
- Anbernic RG556 display: `1920x1080` (16:9 OLED).
- **Exact Integer Scaling Modes**:
  - `Aspect Fit 3:2` (`1620×1080`)
  - `Integer Scaling 6×` (`1440×960` — Zero pixel shimmer/distortion)
  - `Integer Scaling 5×` (`1200×800`)
  - `Integer Scaling 4×` (`960×640`)
  - `Full Screen Stretch` (`1920×1080`)
- **Display Shaders**:
  - `NEAREST`: Strict pixel-perfect 1:1 baseline scaling with hard edges and vibrant GBA colors.
  - `LCD_GRID`: Authentic GBA sub-pixel LCD screen matrix simulation.
  - `SCANLINES`: Retro handheld & CRT horizontal scanline shading.
  - `SCALE2X`: Clean EPX color-equality pixel expansion.
  - `BILINEAR`: Smooth edge interpolation.

---

## 3. Audio Architecture

- Sampling rate: ~32.7 kHz (mGBA native GBA clock output).
- Format: 16-bit Signed Stereo PCM.
- `AudioThread` reads from an `ArrayBlockingQueue<short[]>` capacity 16 and streams to `android.media.AudioTrack` in `MODE_STREAM`.
- Thread lifecycle: Stopped, drained, and joined cleanly on `onPause()` and ROM switching to prevent buffer underruns and crashes.

---

## 4. Input & Physical Controller Architecture (Phase 2 — v1.1.0)

- Physical controls (D-Pad, A/B/X/Y, L1/R1, Start/Select, Analog Sticks) map to standard Android `KeyEvent` and `MotionEvent` sources.
- **Reference Counting**: Physical key ref-counting (`int[10]` and `int[2]`) prevents button shadowing bugs when multiple keys map to the same action.
- **Hardware SOCD Neutralization**: Left+Right and Up+Down simultaneous inputs resolve to neutral.
- **Analog Stick Hysteresis**: 5% margin around deadzone prevents potentiometer jitter.
- **Packed 32-bit Atomic Keymask**: Standard keys (bits 0..9) and Turbo keys (bits 16..17) are packed into a single atomic integer.
- **Frame-Accurate 30 Hz Turbo**: `EmulationThread` pulses turbo keys on alternating frame ticks.

---

## 5. Cheats & Save States Architecture (Phase 3 & Phase 8)

- **Cheat Engine (v1.2.0 - v1.2.2)**:
  - Native mGBA `struct mCheatDevice` bridge.
  - Libretro `.cht` stream parser with multi-line `+` expansion.
  - 5-Tier ROM matcher (CRC32 -> Game Code + Version -> 4-char Game Code -> Title -> Local file).
  - Master code auto-dependency manager.
  - Built-in Online Libretro Cheats Downloader (streaming zip extraction of 500+ databases).
  - 14 pre-packaged bundled offline databases.
- **Save States Engine (v1.4.0)**:
  - Native `g_core->saveState()` and `g_core->loadState()` binary snapshots (~384 KB).
  - 5 per-game persistent slots (`Slot 0 Quick Save` + `Slots 1..4`).
  - Thread-safe command queue evaluated on `EmulationThread`.
  - Saves control plane with timestamps, file sizes, and Save/Load/Delete actions.

---

## 6. Architectural Component Classification

| Component | FrogEmu Architectural Tier | Status |
| :--- | :--- | :---: |
| **mGBA Core Engine** | **Core** | ✅ Mature (v1.0) |
| **RG556 Controller & Custom Mapping** | **Core Subsystem** | ✅ Certified (v1.1) |
| **Audio PCM Streaming Pipeline** | **Core Subsystem** | ✅ Certified (v1.0) |
| **Settings Control Plane** | **Core Control Plane** | ✅ Certified (v1.0) |
| **Cheat Engine (Provider-Adapter & Downloader)** | **Pluggable Feature** | ✅ Certified (v1.2) |
| **Display Scaling & Shaders (Integer 6x/LCD Grid)**| **Pluggable Enhancement** | ✅ Certified (v1.3) |
| **Save States & Multi-Slot Manager** | **Pluggable Feature** | ✅ Certified (v1.4) |
| **Link Multiplayer (SIO & Loopback)** | **On-Demand Socket Subsystem** | ✅ Loopback Certified |
| **Wi-Fi LAN / Hotspot Link Transport** | **On-Demand Socket Subsystem** | ⏳ Queued |
| **Future Multi-System Cores (GB, GBC, PS1, PSP)**| **Additional Multi-System Cores** | ⏳ Future |

---

## 7. Execution Milestones & Roadmap

### Completed Milestones ✅
- [x] **mGBA Core Integration & ABI Invariant** (`v1.0.0`)
- [x] **Concurrency Refactor (`EmulationThread` Core Ownership)** (`v1.0.0`)
- [x] **Double-Buffered Frame Publication Contract (~59.73 Hz)** (`v1.0.0`)
- [x] **Audio Streaming Pipeline (32.7 kHz 16-bit PCM)** (`v1.0.0`)
- [x] **Native SIO Link Adapter (`GBASIODriver`) & Loopback Transport** (`v1.0.0`)
- [x] **Phase 1: Settings Architecture Hardening (Modular Landscape Panels)** (`v1.0.0`)
- [x] **Phase 2: RG556 Controller Custom Mapping (Press-to-Bind & Presets)** (`v1.1.0`)
- [x] **Phase 3: Native Cheat Engine, Libretro Parser & Online Downloader** (`v1.2.0 - v1.2.2`)
- [x] **Phase 4: Display Scaling Framework & Retro Shaders (Integer 6x, LCD Grid)** (`v1.3.0`)
- [x] **Phase 8: Instant Save State & Multi-Slot Engine** (`v1.4.0`)

---

### Remaining Multi-System & Multiplayer Roadmap 🔜

| Phase | Milestone | Status | Description |
| :---: | :-------- | :----: | :---------- |
| **5** | **Real-Game SIO Handshake Validation** | ⏳ | Transaction logging & timing verification in commercial titles |
| **6** | **Wi-Fi LAN / Hotspot Transport** | ⏳ | Zero-config on-demand socket multiplayer |
| **7** | **Bluetooth Transport** | ⏳ | On-demand RFCOMM socket pairing |
| **9** | **ROM Library & Metadata Scanner** | ⏳ | Multi-directory scanner, cover art, metadata |
| **10**| **Multi-System Expansion (GB/GBC)** | ⏳ | Dedicated core adapters for Game Boy & Game Boy Color |
| **11**| **Multi-System Expansion (PS1/PSP)** | ⏳ | Additional multi-system hardware acceleration cores |
