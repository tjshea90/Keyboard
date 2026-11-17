# CHECKPOINT 272 — read me first, then TASKS.md

**Written:** 2026-11-17T04:57:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `57ca466` (this checkpoint is the commit after it)

## Just done
N10-N14 done in code and tests: glide survives an address bar's inline completion + verified replaceRecent fallback; no 123 key (hold . or , for a symbols grid); globe off by default (hold space = picker, Settings switch, safe mode on); space bar 6.4 units; research/PREDICTION-SOURCES.md written; core 286, ime 127, lint ok

## Do this next
(1) tools/emu/smoke.sh: bottom row geometry is now [',' 1][space 6.4][. 1][action 1.6]: update bx weights and key space/back positions; replace the globe tap with a hold-space (adb input swipe x y x y 900) then BACK; add a hold-'.' grid check by screenshot only; dispatch emu-smoke.yml; (2) small win from the research: Suggester should restore apostrophes (typed im -> I'm, dont -> don't) as a zero-penalty fix; (3) then version 0.3.0-alpha1 (code 4), ship.sh, android.yml, record, tell the owner; (4) ask the owner which research items to build (harness with real swipes first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
```

(18 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
