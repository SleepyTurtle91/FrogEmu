# 📋 FrogEmu Engineering Backlog & Issues

---

## 🖥️ Display & Video Pipeline

### Issue: Display Enhancement #001 — Preserve GBA pixel/color fidelity during integer scaling
- **Priority**: High (Next Milestone)
- **Status**: 📝 Queued / Documented
- **Symptoms**: Current Scale2x directional edge shader produces an "oil paint" / softened effect on high-DPI OLED screens rather than crisp, authentic pixel boundaries.
- **Root Cause & Target Architecture**:
  - Keep mGBA core strictly as the execution engine (`mGBA = execution`).
  - Isolate display scaling strictly in the presentation pipeline (`FrogEmu = presentation`).
  - Establish a clean, multi-tier scaling hierarchy:
    1. **Native / Pixel Perfect**: Exact 1:1 GBA pixels with strict nearest-neighbor sampling.
    2. **Clean Integer 2× / 3× / 4×**: Direct integer-scaled nearest-neighbor without smoothing or interpolation.
    3. **Bilinear (Optional)**: Smooth non-integer scaling.
    4. **LCD Grid / Scanlines**: Authentic GBA LCD sub-pixel matrix emulation.
    5. **CRT**: Optional retro curvature/bloom shader.

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
