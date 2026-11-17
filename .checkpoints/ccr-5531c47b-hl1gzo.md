# CHECKPOINT 301 — read me first, then TASKS.md

**Written:** 2026-11-17T16:27:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `8e55816` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"full_build":"true","note":"0.5.0-alpha2: fixes the Android 16 phone: the system navigation bar no longer covers the bottom row (?123, space bar, Enter); the clipboard cards no longer run text under the pin; same photo layout as alpha1"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.5.0-alpha2), then:  bash tools/record-release.sh swoop-v0.5.0-alpha2 "0.5.0-alpha2: fixes the Android 16 phone: the system navigation bar no longer covers the bottom row (?123, space bar, Enter); the clipboard cards no longer run text under the pin; same photo layout as alpha1"
NEVER ship swoop-v0.5.0-alpha2 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
