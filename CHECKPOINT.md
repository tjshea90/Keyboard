# CHECKPOINT 297 — read me first, then TASKS.md

**Written:** 2026-11-17T15:58:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `59b102c` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"full_build":"true","note":"0.5.0-alpha1: the keyboard now matches the Gboard photo: four rows, round ?123 at the bottom left and round Enter at the bottom right, digit hints on the top row, teal colours, toolbar spread evenly, symbols on two pages, number row off by default; the Kestrel browser panel can open it floating"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.5.0-alpha1), then:  bash tools/record-release.sh swoop-v0.5.0-alpha1 "0.5.0-alpha1: the keyboard now matches the Gboard photo: four rows, round ?123 at the bottom left and round Enter at the bottom right, digit hints on the top row, teal colours, toolbar spread evenly, symbols on two pages, number row off by default; the Kestrel browser panel can open it floating"
NEVER ship swoop-v0.5.0-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
```
