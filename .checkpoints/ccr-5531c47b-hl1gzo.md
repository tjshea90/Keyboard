# CHECKPOINT 305 — read me first, then TASKS.md

**Written:** 2026-11-17T17:28:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `15b8c1e` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"full_build":"true","note":"0.5.0-alpha3: dark colours by default, like the photo (Settings, Colours: Automatic follows the phone, or Light); the space and the capital after a question mark, full stop or exclamation mark now also work in the keyboard own Setup and Dictionary screens on Android 10; otherwise the same as alpha2: photo layout, clipboard, translate, dictionary, squeeze"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.5.0-alpha3), then:  bash tools/record-release.sh swoop-v0.5.0-alpha3 "0.5.0-alpha3: dark colours by default, like the photo (Settings, Colours: Automatic follows the phone, or Light); the space and the capital after a question mark, full stop or exclamation mark now also work in the keyboard own Setup and Dictionary screens on Android 10; otherwise the same as alpha2: photo layout, clipboard, translate, dictionary, squeeze"
NEVER ship swoop-v0.5.0-alpha3 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
