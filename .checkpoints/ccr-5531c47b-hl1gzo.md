# CHECKPOINT 298 — read me first, then TASKS.md

**Written:** 2026-11-17T16:09:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `89bb406` (this checkpoint is the commit after it)

## Just done
Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav bar covering the bottom row (edge to edge since Android 15); KeyboardRoot now reads the navigation-bar inset on API 35+ and pads the docked panel / keeps the floating one above it (KeyboardRootInsetsTest, sdk 35); smoke.sh accepts mSelectedMethodId and the smart-space outcome; ImeGeometryAccuracyTest (photo layout top-1 97.3 vs 96.5 reference); Kestrel branch pushed (8676c1c6: tests core 1616 app 365 green); docs; version 0.5.0-alpha2 code 7

## Do this next
dispatch emu-smoke.yml api 36 AND api 29 on this head, look at the API 36 screenshot (keys above the nav bar?); if good: bash ship.sh 'note', android.yml full_build=true, wait tag+green, get_release_by_tag, record-release swoop-v0.5.0-alpha2; Browser: gecko compile running locally (log scratchpad gecko2.log) then merge origin/ccr-e89b9dfe-pl3lk5 into the Browser branch when alpha65 is out; heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M docs/LAYOUT.md
     M ime/build.gradle.kts

## Last ten checkpoints
```
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
