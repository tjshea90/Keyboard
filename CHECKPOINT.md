# CHECKPOINT 1124 — read me first, then TASKS.md

**Written:** 2026-11-26T21:20:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `c332c24` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/awesome-bell-gmcf0i, inputs {"full_build":"true","note":"R34d (second half): on big keys (the tablet) a short flick to the next key is now a glide and no longer types the letter it began on (the 'a thumb rolled' and 'too short to be a glide' limits were measured in key widths, 41 dp in landscape; now capped at 8 dp and 16 dp, phone keys unchanged); holding an e-mail domain on the strip no longer asks to remove it from the dictionary; checked on an Android 10 emulator"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha15), then:  bash tools/record-release.sh swoop-v0.7.0-alpha15 "R34d (second half): on big keys (the tablet) a short flick to the next key is now a glide and no longer types the letter it began on (the 'a thumb rolled' and 'too short to be a glide' limits were measured in key widths, 41 dp in landscape; now capped at 8 dp and 16 dp, phone keys unchanged); holding an e-mail domain on the strip no longer asks to remove it from the dictionary; checked on an Android 10 emulator"
NEVER ship swoop-v0.7.0-alpha15 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c332c24 ckpt 1123: pre-ship: R34d (second half): on big keys (the tablet) a short flick to the n
  2a86173 ckpt 1118: alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
  1c1b1f5 ckpt 1109: gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, un
  237e3ca ckpt 1106: R34a2 offline lab PracticeEval + PracticeEvalTest (simulated tablet glides ro
  dcae6cc ckpt 1103: alpha13 RELEASED and recorded (run 45892015689 green); R34a2 glide practice c
  23bde72 ckpt 1092: R34a2 Glide practice written into TASKS (after the measured ceiling R34a-meas
  fc5913f ckpt 1091: R34a measured (docs only so far): word-pair model, weight grid and miss class
  7c25fd6 ckpt 1087: alpha13 (ime code 24, swoop-v0.7.0-alpha13) gated, pushed and android.yml DIS
```
