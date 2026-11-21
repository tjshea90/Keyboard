# CHECKPOINT 925 — read me first, then TASKS.md

**Written:** 2026-11-21T22:38:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `6ba0755` (this checkpoint is the commit after it)

## Just done
R30 step 2 DONE: core/.../glide/ShapeNet.kt (pure Kotlin forward pass of the open FUTO-design encoder: SWEN loader, trajectory resample, features, 5 TCN blocks, layout-agnostic cosine head, CTC word log-likelihood) matches the PyTorch reference vectors: features max diff 9.5e-7, log-emissions 1.3e-5 (ShapeNetParityTest; weights in core/build/r30, git-ignored, not committed); one cold forward 104 ms on the sandbox JVM

## Do this next
step 3: harness core/src/test/.../ShapeNetEvalTest.kt (REAL_SWIPES=1 + core/build/r30): read RealSwipeEval.kt first for how real swipes become phone keys and candidates; for each swipe: ShapeNet.trajectory + emissions with the key centres of the SAME canvas; candidates = shipped decoder top-N (N~40); score = shippedCost + w*(-ctcLogP); report first/top-3 for w in a grid on slice A (offset 0) and the chosen w on slice B (different donors, RealSwipe.fetch offset); also net-alone rank of the true word among candidates

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f635ef6 ckpt 921: R30 step 1c: the open encoder is fully specified in research/R30-trained-shape
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
  f3e4471 ckpt 899: swoop-v0.7.0-alpha9 (code 20) DISPATCHED ONCE: android.yml run 44943874370 (ru
  3da4478 ckpt 898: gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit
  cc287f0 ckpt 885: R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only le
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
