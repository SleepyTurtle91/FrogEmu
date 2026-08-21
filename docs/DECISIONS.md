# 🏛️ FrogEmu Architecture Decision Records (ADRs)

---

## ADR-001: Dedicated `EmulationThread` Core Ownership
- **Context**: Concurrently calling mGBA methods from the Android UI thread, GL surface thread, and audio callbacks caused circle buffer crashes (`_checkIntegrity`) and race conditions.
- **Decision**: Establish `EmulationThread` as the sole owner of `mCore`. Video is published via memcpy back-to-front buffer. Audio is handed off via `ArrayBlockingQueue`. Input is read via atomic bitmask.
- **Status**: ACCEPTED & IMPLEMENTED.

---

## ADR-002: On-Demand Link Socket Subsystem
- **Context**: Link multiplayer requires sockets/Bluetooth, but emulators are single-player by default.
- **Decision**: When Link is `OFF`, no network or Bluetooth socket may be created, retained, or polled. Sockets are opened strictly on-demand.
- **Status**: ACCEPTED & IMPLEMENTED.

---

## ADR-003: Physical Key Reference-Counting & SOCD Neutralization
- **Context**: When multiple physical inputs map to the same GBA action, releasing one button cleared the bit even if the secondary button was held (button shadowing). Simultaneous opposing cardinal directions (Left+Right) crashed GBA game engines.
- **Decision**: Track physical key codes with `int[10] mGbaKeyRefCounts`. Implement hardware SOCD neutralization (Left+Right -> Neutral, Up+Down -> Neutral).
- **Status**: ACCEPTED & IMPLEMENTED.

---

## ADR-004: Packed 32-Bit Atomic Keymask with 30 Hz Turbo Interleaving
- **Context**: Separating normal keys and turbo keys into multiple atomic integers caused potential 1-frame race conditions. Simple bit-toggling broke charge-shot mechanics.
- **Decision**: Pack standard keys (bits 0..9) and turbo keys (bits 16..17) into a single 32-bit `AtomicInteger`. In `EmulationThread`, compute `effectiveKeyMask = normalMask | (turboPhase ? turboMask : 0)` so charging shots is never interrupted.
- **Status**: ACCEPTED & IMPLEMENTED.

---

## ADR-005: Provider-Adapter Decoupling for Extensibility
- **Context**: Cheat databases (Libretro) and future game assets must not pollute the low-level CPU execution pipeline.
- **Decision**: "Providers provide data. Adapters translate it. EmulationThread executes it." Cheat providers parse `.cht` files into abstract `CheatItem`s; system-specific adapters bridge `CheatItem`s into native core devices.
- **Status**: ACCEPTED & IMPLEMENTED.

---

## ADR-006: Presentation Decoupling & Exact Integer Scaling Geometry
- **Context**: Running shaders with fuzzy Euclidean distance checks (such as naive Scale2x) on high-DPI displays created an "oil paint" smeared effect. Furthermore, non-integer scaling (6.75x) caused pixel shimmer and non-uniform pixel widths.
- **Decision**: Decouple display presentation from core execution (`mGBA = execution`, `FrogEmu = presentation`). Provide exact integer scaling viewports (`6×` 1440×960, `5×` 1200×800, `4×` 960×640) with black pillarboxing. Replace fuzzy color matching with strict EPX color equality to guarantee 100% hard edges and color fidelity.
- **Status**: ACCEPTED & IMPLEMENTED (v1.3.0).

---

## ADR-007: Binary Memory State Snapshot Serialization on Frame Loop Boundary
- **Context**: Capturing or restoring emulator state asynchronously from UI threads can cause race conditions with VRAM updates, audio ring buffers, or active cheat instruction hooks (`GBACheatHook`).
- **Decision**: Queue state save/load requests in a zero-lock `ConcurrentLinkedQueue<StateTask>` and execute them strictly at frame loop boundary step 4.6 on `EmulationThread`. Directly call mGBA's byte-exact binary serialization `g_core->saveState()` and `g_core->loadState()` (~384 KB buffer), returning completion callbacks to the UI thread via `Handler`.
- **Status**: ACCEPTED & IMPLEMENTED (v1.4.0).
