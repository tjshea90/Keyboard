# CHECKPOINT 275 — read me first, then TASKS.md

**Written:** 2026-11-17T04:59:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `4bc527c` (this checkpoint is the commit after it)

## Just done
suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only when 20x more likely); emulator smoke geometry updated for the new bottom row and hold-space; core 287 ime 127 lint ok

## Do this next
dispatch emu-smoke.yml (api 29) and read get_job_logs: expect OK on typing hi/Hi, Backspace, glide, hold-space no crash, floating; then 0.3.0-alpha1 (versionCode 4): bump ime/build.gradle.kts, README bullets (no 123 key, hold full stop for symbols, hold space for picker), ship.sh, android.yml, record-release, tell the owner (also: the research summary and the question which research items to build first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/smoke.sh

## Last ten checkpoints
```
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
