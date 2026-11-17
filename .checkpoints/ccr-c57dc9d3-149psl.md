# CHECKPOINT 236 — read me first, then TASKS.md

**Written:** 2026-11-17T03:03:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `778d1ce` (this checkpoint is the commit after it)

## Just done
first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen and shown; docked IME window = 477 px of 1280 (keyboard-sized: the tall-window fix WORKS on a device); 'hi' typed (field capitalises); NOT yet working on the emulator: key coordinates (window also covers the nav bar), long press on the neon bar did not float it. Added geometry-only SwoopDiag logging (debuggable builds only) and fixed smoke geometry

## Do this next
dispatch emu-smoke.yml again; read get_job_logs tail 160: look at the SwoopDiag block (grip down / long press fired / floating -> / applyWindow / onComputeInsets / layout lines) to see where the long press stops; fix; repeat until docked typing + glide + float all pass; then 0.2.1-alpha1.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
