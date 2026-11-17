# CHECKPOINT 245 — read me first, then TASKS.md

**Written:** 2026-11-17T03:17:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `a7d984f` (this checkpoint is the commit after it)

## Just done
swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboard + tall-window fix; emulator smoke runs 44007433798 and 44008340097 green)

## Do this next
wait for the build: until git ls-remote --tags origin swoop-v0.2.1-alpha1 shows the tag, then actions_list list_workflow_runs android.yml green, get_release_by_tag swoop-v0.2.1-alpha1 shows the APK, then bash tools/record-release.sh swoop-v0.2.1-alpha1 with the identical note from the ship.sh output (in CHECKPOINT/ckpt 24x message), then tell the owner: link, what is new, 0.2.0 superseded, honest list of what is NOT tested on their devices.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
```
