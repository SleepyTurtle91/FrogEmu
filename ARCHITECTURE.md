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
- **Reference Renderer**: Output raw pixels (240x160) to a `GLSurfaceView` with Nearest-Neighbor filtering.
- **Upscaler Pipeline**: Exposes a modular shader system (`Nearest`, `Scale2x`, `HQ2x`, `xBRZ`).

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
The Android frontend runs a dedicated `AudioThread` to pull samples via `mAudioBufferRead()` and send them to a blocking `AudioTrack` stream.

## 6. Cheats Database (`cheats.db`)
*STATUS: Research Phase*
We must determine the exact schema, source, and compatibility of `cheats.db` with mGBA's internal cheat device before building an SQLite/JNI bridge.

## 7. Execution Workflow Milestones

| Area                     | Status |
| ------------------------ | -----: |
| mGBA integration         |      ✅ |
| JNI ABI                  |      ✅ |
| ROM loading              |      ✅ |
| Frame execution          |      ✅ |
| GLES rendering           |      ✅ |
| Input                    |      ✅ |
| Audio                    |      ✅ |
| **Real-game validation** |      ✅ |
| **Upscaler (Scale2x)**     |      ✅ |
| Cheats                   |     🔬 |
| Wi-Fi Link               |     🔬 |
| Bluetooth Link           |     🔬 |
| Save states              |      ⏳ |
| Settings                 |      ⏳ |
| About / app identity     |      ⏳ |

## 8. Multiplayer (Local Link Cable)
FroggBA will implement a deterministic Link Cable networking layer for local offline multiplayer:
- **Core Principle**: Two FroggBA instances must behave like two GBA consoles connected by a physical cable, exchanging exact link timing protocols, not just game state.
- **Link Interface**: The core's GBA serial port will be hooked into an abstracted JNI transport.
- **Transports**: 
  - Wi-Fi (Primary, Local LAN/Hotspot)
  - Bluetooth (Secondary fallback)
