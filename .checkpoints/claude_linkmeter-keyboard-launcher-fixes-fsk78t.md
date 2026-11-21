# CHECKPOINT 950 — read me first, then TASKS.md

**Written:** 2026-11-21T23:05:42Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `8c4c68c` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha10 (ime code 21) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/linkmeter-keyboard-launcher-fixes-fsk78t, inputs {"full_build":"true","note":"0.7.0-alpha10: Smarter glide reading: a trained shape network (open FUTO-design model, MIT, runs on the device) takes a second look at glides the keyboard is not sure about; on real swipes the right word is first about 4 times more often in 100 and short words read as long ones fall by two thirds (Settings: Smarter glide reading; Diagnostics shows its use and speed)"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha10), then:  bash tools/record-release.sh swoop-v0.7.0-alpha10 "0.7.0-alpha10: Smarter glide reading: a trained shape network (open FUTO-design model, MIT, runs on the device) takes a second look at glides the keyboard is not sure about; on real swipes the right word is first about 4 times more often in 100 and short words read as long ones fall by two thirds (Settings: Smarter glide reading; Diagnostics shows its use and speed)"
NEVER ship swoop-v0.7.0-alpha10 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cc1cf34 ckpt 947: R30 step 4b: ime tests 290 green (KeysViewTest timed-glide test, safe-mode eng
  7391048 ckpt 942: R30 step 4a: :core ShapeRescorer (hybrid score+0.7*logP, gate 3.5, letter-box 
  5bfe37a ckpt 932: R30 step 3c DONE: three disjoint real-swipe sets incl. FUTO TEST split: hybrid
  881fdb5 ckpt 930: R30 step 3 RESULT: hybrid (shipped score + 0.5*net log P over the top-40) = +4
  e9c055f ckpt 927: R30 step 3a: RealSwipe.read now keeps swipe times as ms since the first point 
  fa2d033 ckpt 925: R30 step 2 DONE: core/.../glide/ShapeNet.kt (pure Kotlin forward pass of the o
  f635ef6 ckpt 921: R30 step 1c: the open encoder is fully specified in research/R30-trained-shape
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
