# 📋 FrogEmu Engineering Backlog & Issues

---

## 🖥️ Display & Video Pipeline

### Issue: Display Enhancement #001 — Preserve GBA pixel/color fidelity during integer scaling
- **Priority**: High
- **Status**: ✅ RESOLVED in v1.3.0
- **Resolution**:
  - Replaced fuzzy distance calculations in Scale2x with strict EPX color equality matching (eliminating the oil-paint effect).
  - Added pure `Pixel-Perfect Nearest`, `Authentic LCD Grid`, `Scanlines`, and `Bilinear` shaders.
  - Implemented exact integer scaling geometry (`6×` 1440×960, `5×` 1200×800, `4×` 960×640) with black pillarboxing.

---

## 🔗 Link Multiplayer Subsystem

### Task: Phase 5 — Real-Game SIO Handshake Validation
- **Status**: ⏳ Queued
- **Objectives**: Capture and measure commercial game SIO packet timing over loopback before wire transport protocol selection (TCP vs UDP).

---

## 💾 Save States & Snapshots

### Task: Phase 8 — Save State Slots & Preview Thumbnails
- **Status**: ⏳ Queued
- **Objectives**: Instant state serialization/deserialization, slot management, and GL framebuffer thumbnail capture.
