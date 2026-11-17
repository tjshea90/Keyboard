# CHECKPOINT 279 — read me first, then TASKS.md

**Written:** 2026-11-17T05:00:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `b7f8984` (this checkpoint is the commit after it)

## Just done
owner reversed N11 (wants the 123 key back: he needs the special symbols). Code at this commit still has NO 123 key (hold . or , opens a symbols grid); everything else of the 0.3.0 work is done and tested (core 287, ime 127, lint ok): Brave strip-tap fix, no globe by default + hold space = picker, space bar wider, apostrophe restoration, research/PREDICTION-SOURCES.md. ime/build.gradle.kts is already 0.3.0-alpha1 (code 4), README edited, NOT shipped

## Do this next
RESTORE THE 123 KEY: (1) core/.../ImeLayout.kt: re-add the Page.SYMBOLS branch (digits + symbolRows + bottom row with abc key) and put KeySpec('123', Key.ToSymbols, 1.5f) first in bottom(...) for letters (TEXT/URL/EMAIL), space shrinks to 4.9 (TEXT); (2) fix ImeLayoutTest (thereIsNoOneTwoThree... -> 123 present, space weight 4.9, widest key still space) and KeysViewTest (restore theSymbolsKeyPagesInsideTheView... page switch test, delete the no-123 test); KeyboardPresenterTest unaffected; (3) tools/emu/smoke.sh bottom row weights [1.5,1,4.9,1,1.6] and key positions; (4) run :core:test :ime:testDebugUnitTest :ime:lintDebug; (5) the emulator smoke run dispatched at ckpt 275 (check actions_list emu-smoke.yml) used the no-123 layout: re-dispatch after (3); (6) ship.sh 'note' for 0.3.0-alpha1 (README bullet about no 123 key must be rewritten: 123 key stays), android.yml, record-release, tell owner.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
