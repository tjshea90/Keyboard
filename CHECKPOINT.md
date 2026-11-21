# CHECKPOINT 921 — read me first, then TASKS.md

**Written:** 2026-11-21T22:36:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `e074535` (this checkpoint is the commit after it)

## Just done
R30 step 1c: the open encoder is fully specified in research/R30-trained-shape-model.md section 5 (file format, features, net, layout-agnostic head, golden-vector fixture, cost 35M MACs)

## Do this next
(1) extend R30FetchTest default list with https://raw.githubusercontent.com/xxparthparekhxx/keyboard/master/app/src/test/resources/swipe_reference.bin -> core/build/r30/swipe_reference.bin; (2) write core/src/main/kotlin/app/kestrel/core/input/glide/ShapeNet.kt (load SWEN, features, forward, ctcLogP(word)); (3) ShapeNetParityTest vs swipe_reference.bin (skip if missing); then (4) RealSwipeEval-style harness: shipped decoder candidates + net rescoring, tune weight on one slice, test on another

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/R30-trained-shape-model.md

## Last ten checkpoints
```
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
  f3e4471 ckpt 899: swoop-v0.7.0-alpha9 (code 20) DISPATCHED ONCE: android.yml run 44943874370 (ru
  3da4478 ckpt 898: gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit
  cc287f0 ckpt 885: R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only le
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
```
