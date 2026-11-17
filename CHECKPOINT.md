# CHECKPOINT 300 — read me first, then TASKS.md

**Written:** 2026-11-17T16:23:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `3f461e5` (this checkpoint is the commit after it)

## Just done
smoke.sh: bring the docked keyboard back reliably before the floating stage (unfocus + retry) after the Android 16 run 44123738564 showed the keyboard hidden after the keyboard switch; Kestrel alpha66 dispatched (Browser run queued, note in docs/handoff/ALPHA66_RELEASE_NOTE.txt)

## Do this next
dispatch emu-smoke.yml api 36 on this head; if 0 failures: bash ship.sh for 0.5.0-alpha2 then android.yml full_build=true then record-release; also record kestrel-g-v0.3.0-alpha66 after its Browser run is green (find the run id with actions_list list_workflow_runs android.yml on tjshea90/Browser; bash /home/user/Browser/tools/record-release.sh kestrel-g-v0.3.0-alpha66 with the IDENTICAL note); heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
