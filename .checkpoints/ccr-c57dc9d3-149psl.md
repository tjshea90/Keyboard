# CHECKPOINT 249 — read me first, then TASKS.md

**Written:** 2026-11-17T03:23:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `b05c02b` (this checkpoint is the commit after it)

## Just done
RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned certificate verified) and recorded; N1-N9 all done; floating keyboard + tall-window fix proven on an Android 10 emulator (runs 44007433798, 44008340097)

## Do this next
WAIT for the owner's feedback from the tablet and the phone on 0.2.1: trail speed, key pop-up, number row, suggestions, learning, settings screen, floating (long press the glowing bar). Possible next work, only on the owner's say: KB-107 Android 16 phone pass (emulator smoke api 36: dispatch emu-smoke.yml api=36), G1 numbers from Diagnostics, translation (KB-600+, needs Q-LINKMETER), emoji/clipboard, KB-101b. Open owner questions: Q-HEARTBEAT, Q-LINKMETER.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
