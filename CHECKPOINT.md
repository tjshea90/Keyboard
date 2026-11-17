# CHECKPOINT 244 — read me first, then TASKS.md

**Written:** 2026-11-17T03:16:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `a8bdc64` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-c57dc9d3-149psl, inputs {"full_build":"true","note":"0.2.1-alpha1: floating keyboard (press and hold the glowing bar, drag, resize by the corners, double-tap to dock) and a fix for the keyboard window being as tall as the screen (found by my own tests; use this build instead of 0.2.0). Proven on an Android 10 emulator in CI: typing, glide, floating, the app behind still tappable"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.2.1-alpha1), then:  bash tools/record-release.sh swoop-v0.2.1-alpha1 "0.2.1-alpha1: floating keyboard (press and hold the glowing bar, drag, resize by the corners, double-tap to dock) and a fix for the keyboard window being as tall as the screen (found by my own tests; use this build instead of 0.2.0). Proven on an Android 10 emulator in CI: typing, glide, floating, the app behind still tappable"
NEVER ship swoop-v0.2.1-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
