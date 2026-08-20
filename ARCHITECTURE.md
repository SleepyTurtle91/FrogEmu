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
│   - Handles SIO Transfer Injections                    │
└───────┬───────────────────┬────────────────────┬───────┘
        │                   │                    │
 (Display Front-Buffer) (Audio Queue)   (Link Payload Hand-off)
        ▼                   ▼                    ▼
┌──────────────┐    ┌──────────────┐     ┌──────────────┐
│  GL Thread   │    │ AudioThread  │     │ LinkManager  │
│  - Render    │    │ - AudioTrack │     │ - Session    │
│  - Shader    │    │ - 32.7 kHz   │     │ - Transports │
└──────────────┘    └──────────────┘     └──────┬───────┘
                                                │
                                  ┌─────────────┴─────────────┐
                                  ▼                           ▼
                           LoopbackTransport            WifiTransport (LAN)
```

---

## 2. Video Pipeline & Modular Shaders
- **Resolution**: Native GBA display is 240×160 (3:2 aspect ratio).
- **Double Buffering**: Native layer maintains `g_videoBuffer` (written by `mCore`) and `g_displayBuffer` (read by GLES).
- **Modular Upscaler Architecture**:
  - `Nearest`: Reference / diagnostic baseline (pixel-perfect 1:1).
  - `Scale2x`: Edge-detection sub-pixel interpolation shader.
  - `HQ2x` / `xBRZ`: Future visual enhancement plugins.
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

## 5. Plugin Architecture
FroggBA separates feature implementations into modular plugins managed via the Settings Control Plane:
- **Display Plugins** (`DisplayProvider`): `NearestFilter`, `Scale2xFilter`, `HQ2xFilter`, `XbrzFilter`.
- **Link Transports** (`LinkTransport`): `LoopbackTransport`, `WifiTransport` (LAN/Hotspot), `BluetoothTransport`.
- **Input Providers** (`InputProvider`): `AndroidGamepad`, `TouchInput`.
- **Cheat Providers** (`CheatProvider`): `CheatsDatabase` (SQLite/Libretro parser).

*Invariant*: Plugins communicate strictly through FroggBA Java interfaces and never touch `mCore` directly.

---

## 6. Execution Workflow Milestones

| Area                     | Status | Description |
| ------------------------ | -----: | :---------- |
| mGBA Core Integration    |      ✅ | Headless mGBA running via Android NDK |
| JNI ABI Alignment        |      ✅ | Synchronized CMake flags across wrapper & core |
| Concurrency Refactor     |      ✅ | Dedicated `EmulationThread` core ownership |
| Frame Timing (~59.73 Hz) |      ✅ | Decoupled from display refresh rate |
| GLES Double Buffering    |      ✅ | Safe frame publication contract |
| RG556 Gamepad Auto-Hide  |      ✅ | Touch controls hidden on gamepad detection |
| Scale2x Upscaler Shader  |      ✅ | High-performance retro edge scaling |
| Real-Game Validation     |      ✅ | Proven on commercial ROMs on RG556 |
| FroggBA Settings v1      |      ✅ | Persistent preferences & clean gameplay toolbar |
| Native SIO Link Adapter  |      ✅ | `GBASIODriver` hooked to `mPERIPH_GBA_LINK_PORT` |
| Loopback Transport & Diag|      ✅ | 2-4 player in-process testing & live diagnostics |
| **Real SIO Validation**  |     🔜 | Validate handshake transactions in real games |
| **Wi-Fi LAN Multiplayer**|     🔬 | Socket UDP/TCP local transport without internet |
| **Cheats Database**      |     🔬 | Schema investigation and cheat engine hook |
| Save States              |      ⏳ | Memory snapshots and serialization |
| ROM Library & Box Art    |      ⏳ | Scanning, cover art, and metadata |
