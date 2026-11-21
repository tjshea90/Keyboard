# CHECKPOINT 951 — read me first, then TASKS.md

**Written:** 2026-11-21T23:06:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `e13f2a7` (this checkpoint is the commit after it)

## Just done
R30 alpha10 shipped to GitHub: ship.sh green (handoff test, 290 ime + core tests), android.yml full_build DISPATCHED ONCE on branch claude/linkmeter-keyboard-launcher-fixes-fsk78t for tag swoop-v0.7.0-alpha10

## Do this next
wait ~6 min (bounded until loop), then actions_list list_workflow_runs android.yml branch=claude/... event workflow_dispatch perPage 1 -> jobs -> Release swoop-v0.7.0-alpha10 has the APK -> bash tools/record-release.sh swoop-v0.7.0-alpha10 with the identical note -> tick R30e in TASKS -> final plain answer with the Release link. NEVER dispatch twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e13f2a7 ckpt 950: gated swoop-v0.7.0-alpha10 (ime code 21) and pushed it: handoff self-test, uni
  cc1cf34 ckpt 947: R30 step 4b: ime tests 290 green (KeysViewTest timed-glide test, safe-mode eng
  7391048 ckpt 942: R30 step 4a: :core ShapeRescorer (hybrid score+0.7*logP, gate 3.5, letter-box 
  5bfe37a ckpt 932: R30 step 3c DONE: three disjoint real-swipe sets incl. FUTO TEST split: hybrid
  881fdb5 ckpt 930: R30 step 3 RESULT: hybrid (shipped score + 0.5*net log P over the top-40) = +4
  e9c055f ckpt 927: R30 step 3a: RealSwipe.read now keeps swipe times as ms since the first point 
  fa2d033 ckpt 925: R30 step 2 DONE: core/.../glide/ShapeNet.kt (pure Kotlin forward pass of the o
  f635ef6 ckpt 921: R30 step 1c: the open encoder is fully specified in research/R30-trained-shape
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
```
