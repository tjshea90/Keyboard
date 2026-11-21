# CHECKPOINT 942 — read me first, then TASKS.md

**Written:** 2026-11-21T22:51:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `39d6663` (this checkpoint is the commit after it)

## Just done
R30 step 4a: :core ShapeRescorer (hybrid score+0.7*logP, gate 3.5, letter-box canvas) + GlideTyping.shapeModel/loadShape/warmShape/auto-off + KeysView touch times + presenter/Settings switch 'Smarter glide reading' + weights asset ime/assets/glide/swipe_encoder.bin + MIT notice; ShapeRescorerTest/ShapeNetParityTest pass

## Do this next
step 4b: run full :core:test + :ime:testDebugUnitTest + lint (bash ship.sh will); KeysViewTest for timed onGlideEnd; update Browser-link hash note; Diagnostics wording; tablet-speed unknown (auto-off at 700 ms x3); then bump ime/build.gradle.kts alpha10 + bash ship.sh + dispatch android.yml ONCE + record-release; write final plain answer

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5bfe37a ckpt 932: R30 step 3c DONE: three disjoint real-swipe sets incl. FUTO TEST split: hybrid
  881fdb5 ckpt 930: R30 step 3 RESULT: hybrid (shipped score + 0.5*net log P over the top-40) = +4
  e9c055f ckpt 927: R30 step 3a: RealSwipe.read now keeps swipe times as ms since the first point 
  fa2d033 ckpt 925: R30 step 2 DONE: core/.../glide/ShapeNet.kt (pure Kotlin forward pass of the o
  f635ef6 ckpt 921: R30 step 1c: the open encoder is fully specified in research/R30-trained-shape
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
