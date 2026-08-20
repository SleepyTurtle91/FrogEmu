# FroggBA Architecture Document

## 0. Build Invariants
> **CRITICAL RULE**: The JNI wrapper (`native-lib.cpp`) and the `libmgba` static/shared library MUST be compiled with the exact same mGBA configuration and compile definitions (e.g., `M_CORE_GBA`, `ENABLE_VFS`). 
Failure to do so causes a C/C++ ABI mismatch where `sizeof(struct mCore)` differs between the wrapper and the library, resulting in memory corruption and immediate `SEGV_MAPERR` crashes when executing function pointers like `core->init()`.

## 1. Emulator Initialization
We will initialize the mGBA core using the headless frontend approach:
```c
struct mCore* core = mCoreCreate(mPLATFORM_GBA);
core->init(core);
mCoreInitConfig(core, "FroggBA");
mCoreLoadConfig(core);
```

## 2. ROM Loading (via VFS)
ROMs will be loaded by passing a Virtual File (`VFile`) to the core:
```c
struct VFile* romFile = VFileOpen(romPath, O_RDONLY);
if (romFile) {
    core->loadROM(core, romFile);
}
```

## 3. Video Pipeline
mGBA outputs pixels to a framebuffer. We will set the framebuffer via `core->setVideoBuffer()`.
```c
unsigned width, height;
core->baseVideoSize(core, &width, &height); // 240x160 for GBA
// Assuming 32-bit color format (mCOLOR_XBGR8 or similar) depending on COLOR_16_BIT definition
uint32_t* videoBuffer = malloc(width * height * sizeof(uint32_t));
core->setVideoBuffer(core, videoBuffer, width);
```
- **Initial Milestone**: Output raw pixels (240x160) to a basic Android `SurfaceView` or `GLSurfaceView` without enhancements.
- **Enhancement Phase**: Implement a GLSL Fragment Shader for upscaling.

## 4. Input Handling
Input state is pushed as a bitmask matching mGBA's internal keys (e.g. `GBA_KEY_A = 0`, so `1 << 0`).
```c
uint32_t keyMask = 0; // Set bits for A, B, UP, DOWN, etc.
core->setKeys(core, keyMask);
```
Android touch events will map to an `InputManager` maintaining the key state, which pushes to native.

## 5. Audio Pipeline
mGBA uses a ring buffer for audio, defined in `mgba-util/audio-buffer.h`.
```c
core->setAudioBufferSize(core, 2048);
struct mAudioBuffer* audioBuf = core->getAudioBuffer(core);
```
The Android frontend will pull samples via `mAudioBufferRead()` and send them to an Android `AudioTrack` stream.

## 6. Cheats Database (`cheats.db`)
*Verification Required*: mGBA parses libretro/mGBA text formats natively via `mCheatParseFile()`. If `cheats.db` is an SQLite file, we will need a JNI bridge to query the SQLite DB and push individual cheats into mGBA via `mCheatAddLine()`. 
For now, we assume `cheats.db` implies a bundled file we will extract from `assets` and parse.

## 7. Execution Workflow Milestones
To maintain stability, FroggBA will be implemented in the following phases:

| Milestone                    | Status                     |
| ---------------------------- | -------------------------- |
| Milestone 1: Core alive      | ✅                          |
| Milestone 2: ROM loaded      | ✅                          |
| Milestone 3: Frame execution | ✅                          |
| Milestone 4: First frame     | ✅                          |
| Milestone 5: Input + controls| 🔜 **Now**                 |
| Milestone 6: Audio           | ⏳                          |
| Milestone 7: Real ROM testing| ⏳                          |
| Milestone 8: Link multiplayer| 🔬 Research                |
| Upscaler                     | ⏳                          |
| Cheats                       | ⏳                          |

## 8. Multiplayer (Local Link Cable)
FroggBA will implement a deterministic Link Cable networking layer for local offline multiplayer:
- **Link Interface**: The core's GBA serial port will be hooked into an abstracted JNI transport.
- **Transports**: 
  - Wi-Fi (Primary, Local LAN/Hotspot)
  - Bluetooth (Secondary fallback)
- **Roadmap**:
  1. Core architecture & single-device link abstraction.
  2. Wi-Fi transport (LAN discovery, TCP/UDP sockets).
  3. Bluetooth transport.
  4. Multiplayer UX (Lobby, Room codes, latency monitoring).
