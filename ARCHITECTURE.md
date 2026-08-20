# FroggBA Architecture Document

## 0. Core Invariants

### Invariant 1: C/C++ ABI Alignment
> **CRITICAL RULE**: The JNI wrapper (`native-lib.cpp`) and the `libmgba` static/shared library MUST be compiled with the exact same mGBA configuration and compile definitions (e.g., `M_CORE_GBA`, `ENABLE_VFS`).
Failure to do so causes a C/C++ ABI mismatch where `sizeof(struct mCore)` differs between the wrapper and the library, resulting in memory corruption and immediate `SEGV_MAPERR` crashes when executing function pointers like `core->init()`.

### Invariant 2: Thread Ownership & Framebuffer Publication Contract
> **CRITICAL RULE**: `EmulationThread` is the sole owner of the `mGBA` native core. No other thread (UI, GL Render, Audio, or Network) may directly call JNI methods that mutate core state.
> - **Video**: `EmulationThread` writes to an internal back buffer (`g_videoBuffer`). Upon completing a frame, it atomically publishes the frame data to the front buffer (`g_displayBuffer`). The GL Render thread only reads from `g_displayBuffer`. Neither thread accesses the other's active buffer.
> - **Input**: UI and Gamepad inputs are updated atomically (`AtomicInteger`) in `InputManager` and polled once per frame by `EmulationThread` before `runFrame()`.
> - **Audio**: `EmulationThread` drains audio frames from mGBA into a thread-safe `ArrayBlockingQueue<short[]>`, which the dedicated `AudioThread` consumes without locking the core or the renderer.
> - **Link SIO**: Native `GBASIODriver` intercepts GBA serial transfers. Transfer completion (`GBASIOMultiplayerFinishTransfer()`) is invoked exclusively on `EmulationThread`.

### Invariant 3: On-Demand Link Networking Contract
> **CRITICAL RULE**: Link networking is a built-in on-demand subsystem. No network or Bluetooth socket may be created, retained, or polled while Link Multiplayer is disabled.
> When Link is `OFF`, all transports are stopped, sockets closed, and background threads terminated. FroggBA operates as a zero-overhead single-player emulator with zero network CPU/battery consumption.

---

## 1. Concurrency & Control Plane Model

```text
┌────────────────────────────────────────────────────────┐
│                   Android UI Thread                    │
│   - Game View (Clean, Immersive 3:2 Display)           │
│   - Settings ⚙️ Control Plane (FroggBASettings)         │
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

---

## 2. Video Pipeline & Modular Shaders
- **Resolution**: Native GBA display is 240×160 (3:2 aspect ratio).
- **Double Buffering**: Native layer maintains `g_videoBuffer` (written by `mCore`) and `g_displayBuffer` (read by GLES).
- **Modular Upscaler Architecture**:
  - `Nearest`: Reference / diagnostic baseline (pixel-perfect 1:1).
  - `Scale2x`: Edge-detection sub-pixel interpolation shader.
  - `HQ2x` / `xBRZ`: Future visual enhancement filters.
  - *Invariant*: Shader compilation failures gracefully fall back to `Nearest` without crashing.

---

## 3. Audio Pipeline
- `mAudioBuffer` in mGBA is drained at the end of each frame execution.
- Samples are handed off to `AudioThread` via an `ArrayBlockingQueue`.
- The `AudioTrack` runs in streaming mode at the native core sample rate.
- Stopping or pausing emulation cleanly halts and joins `AudioThread` to prevent dangling references.

---

## 4. Hardware Input & RG556 Controller Integration
- Physical controls (D-Pad, A/B/X/Y, L1/R1, Start/Select, Analog Sticks) map to standard Android `KeyEvent` and `MotionEvent` sources (`SOURCE_GAMEPAD`, `SOURCE_JOYSTICK`).
- Touch controls are grouped in `touch_controls` and auto-hidden when physical gamepad hardware is detected (configured via Settings).
- Key states are combined into the standard GBA bitmask format before being passed into `core->setKeys()`.

---

## 5. Built-in Subsystems vs. Modular Plugins

### Built-in Subsystems
- **Link Multiplayer Subsystem**: On-demand socket engine with built-in transports:
  - `LoopbackTransport`: In-process testing & virtual slave validation.
  - `WifiLanTransport`: Local Wi-Fi router / Android Hotspot broadcast.
  - `WifiHotspotTransport`: Direct peer-to-peer Wi-Fi hotspot mode.
  - `BluetoothTransport`: RFCOMM / L2CAP direct pairing.

### Modular Plugin Framework
- **Display Plugins** (`DisplayProvider`): `NearestFilter`, `Scale2xFilter`, `ScanlineFilter`, `CrtFilter`, `Hq2xFilter`, `XbrzFilter`.
- **Input Plugins** (`InputProvider`): `GamepadMappingProvider`, `TouchOverlayProvider`.
- **Cheat Plugins** (`CheatProvider`): `CheatsDatabase` (SQLite/Libretro parser).

*Invariant*: Plugins communicate strictly through FroggBA Java interfaces and never touch `mCore` directly.

---

## 6. Execution Workflow Milestones & 10-Phase Roadmap

### Foundation Phase (Completed)
- [x] **mGBA Core Integration & ABI Invariant**
- [x] **Concurrency Refactor (`EmulationThread` Core Ownership)**
- [x] **Double-Buffered Frame Publication Contract**
- [x] **Frame Timing Clock (~59.73 Hz)**
- [x] **Audio Streaming Pipeline (32.7 kHz 16-bit PCM)**
- [x] **Real-Game Stability on Anbernic RG556**
- [x] **Native SIO Link Adapter (`GBASIODriver`) & Loopback Transport**
- [x] **FroggBA Settings v1 (Persistent Preferences & Clean Gameplay UI)**

---

### Expansion Phase (Roadmap)

| Phase | Milestone | Status | Description |
| :---: | :-------- | :----: | :---------- |
| **1** | **Settings Architecture Hardening** | 🔜 | Extensible modular category sub-panels |
| **2** | **RG556 Controller Custom Mapping** | ⏳ | Custom button remapping & stick deadzones |
| **3** | **Display Filter Framework** | ⏳ | Scanline, CRT, LCD Grid, HQ2x, xBRZ shaders |
| **4** | **Real-Game SIO Handshake Validation** | ⏳ | Transaction logging & protocol verification |
| **5** | **Wi-Fi LAN / Hotspot Transport** | ⏳ | Zero-config on-demand socket multiplayer |
| **6** | **Bluetooth Transport** | ⏳ | On-demand RFCOMM socket pairing |
| **7** | **Cheat Plugin & `cheats.db`** | ⏳ | Libretro/SQLite cheat repository & mGBA hook |
| **8** | **Save-State Plugin** | ⏳ | Instant state snapshots, slots, and thumbnails |
| **9** | **ROM Library & Box Art** | ⏳ | Multi-directory scanner, cover art, metadata |
| **10**| **Plugin Discovery & Management** | ⏳ | Dynamic extension loading & settings registration |
