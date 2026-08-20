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
                            │ (Atomic / Preferences)
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
│   - Executes Native Cheat Hooks                        │
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

---

## 2. Video Pipeline & Modular Shaders

- Screen resolution: GBA Native `240x160` (3:2 Aspect Ratio).
- Anbernic RG556 display: `1920x1080` (16:9 OLED).
- DirectByteBuffer is shared across JNI boundary once during `initCoreJNI()`.
- Double-buffered: Back buffer is filled by mGBA, front buffer is read by OpenGL ES.
- Display filters implement `EmulatorRenderer.Upscaler`:
  - `NEAREST`: Strict pixel-perfect 1:1 baseline scaling.
  - `SCALE2X`: Sub-pixel directional edge-scaling algorithm in GLSL fragment shader.

---

## 3. Audio Architecture

- Sampling rate: ~32.7 kHz (mGBA native GBA clock output).
- Format: 16-bit Signed Stereo PCM.
- `AudioThread` reads from an `ArrayBlockingQueue<short[]>` capacity 16 and streams to `android.media.AudioTrack` in `MODE_STREAM`.
- Thread lifecycle: Stopped, drained, and joined cleanly on `onPause()` and ROM switching to prevent buffer underruns and crashes.

---

## 4. Input & Physical Controller Architecture

- Physical controls (D-Pad, A/B/X/Y, L1/R1, Start/Select, Analog Sticks) map to standard Android `KeyEvent` and `MotionEvent` sources (`SOURCE_GAMEPAD`, `SOURCE_JOYSTICK`).
- **Reference Counting**: Physical key ref-counting (`int[10]` and `int[2]`) prevents button shadowing bugs when multiple keys map to the same action.
- **Hardware SOCD Neutralization**: Left+Right and Up+Down simultaneous inputs resolve to neutral.
- **Analog Stick Hysteresis**: 5% margin around deadzone prevents potentiometer jitter.
- **Packed 32-bit Atomic Keymask**: Standard keys (bits 0..9) and Turbo keys (bits 16..17) are packed into a single atomic integer.
- **Frame-Accurate 30 Hz Turbo**: `EmulationThread` pulses turbo keys on alternating frame ticks.

---

## 5. Architectural Component Classification

| Component | FrogEmu Architectural Tier |
| :--- | :--- |
| **mGBA Core Engine** | **Core** |
| **RG556 Controller & Custom Mapping** | **Core Subsystem** |
| **Audio PCM Streaming Pipeline** | **Core Subsystem** |
| **Link Multiplayer** | **On-Demand Socket Subsystem** *(Zero overhead when OFF)* |
| **Settings Control Plane** | **Core Control Plane** |
| **Display Filters (Nearest / Scale2x / CRT)** | **Pluggable Enhancement** |
| **Cheat Engine (Provider-Adapter)** | **Pluggable Feature** |
| **Save States & Thumbnails** | **Pluggable Feature** |
| **Future Multi-System Cores (GB, GBC, PS1, PSP)**| **Additional Multi-System Cores** |

---

## 6. Execution Milestones & Roadmap

### Foundation Phase (Completed ✅)
- [x] **mGBA Core Integration & ABI Invariant**
- [x] **Concurrency Refactor (`EmulationThread` Core Ownership)**
- [x] **Double-Buffered Frame Publication Contract (~59.73 Hz)**
- [x] **Audio Streaming Pipeline (32.7 kHz 16-bit PCM)**
- [x] **Real-Game Stability on Anbernic RG556**
- [x] **Native SIO Link Adapter (`GBASIODriver`) & Loopback Transport**
- [x] **FrogEmu Settings v1 (Landscape Two-Pane Settings Control Plane)**
- [x] **Phase 1: Settings Architecture Hardening (Modular Panels)**
- [x] **Phase 2: RG556 Controller Custom Mapping (Press-to-Bind & Presets)**

---

### Expansion Phase (Roadmap 🔜)

| Phase | Milestone | Status | Description |
| :---: | :-------- | :----: | :---------- |
| **3** | **Cheat Engine (Provider-Adapter)** | 📐 | Libretro `.cht` parser, 5-tier ROM matcher & native `mCheatDevice` bridge |
| **4** | **Display Filter Framework** | ⏳ | Scanline, CRT, LCD Grid, HQ2x, xBRZ shaders |
| **5** | **Real-Game SIO Handshake Validation** | ⏳ | Transaction logging & timing verification in commercial games |
| **6** | **Wi-Fi LAN / Hotspot Transport** | ⏳ | Zero-config on-demand socket multiplayer |
| **7** | **Bluetooth Transport** | ⏳ | On-demand RFCOMM socket pairing |
| **8** | **Save-State Plugin** | ⏳ | Instant state snapshots, slots, and preview thumbnails |
| **9** | **ROM Library & Box Art** | ⏳ | Multi-directory scanner, cover art, metadata |
| **10**| **Multi-System Expansion (GB/GBC)** | ⏳ | Dedicated core adapters for Game Boy & Game Boy Color |
