# CHECKPOINT 241 — read me first, then TASKS.md

**Written:** 2026-11-17T03:10:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `20e5a6e` (this checkpoint is the commit after it)

## Just done
N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 failures: docked window 477/1280 px, tap+glide typing, long press on the bar floats it, region insets, field behind still tappable); fixed the 1 px creep found by the diagnostics (measured panel height + float fractions); core 277, ime 120, lint ok

## Do this next
dispatch emu-smoke.yml once more on this head (confirm still 0 failures), then bump ime/build.gradle.kts to versionCode 3 / 0.2.1-alpha1, bash ship.sh 'note', dispatch android.yml full_build=true, wait for tag + green, get_release_by_tag, bash tools/record-release.sh swoop-v0.2.1-alpha1 'same note', give owner the Release link; also update README (floating + the emulator proof) and tell owner 0.2.0 may have had a tall-window bug (the emulator did not confirm it on 0.2.0 itself: it was found by a unit test).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
