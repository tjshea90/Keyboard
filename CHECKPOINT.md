# CHECKPOINT 930 — read me first, then TASKS.md

**Written:** 2026-11-21T22:43:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `bf4562b` (this checkpoint is the commit after it)

## Just done
R30 step 3 RESULT: hybrid (shipped score + 0.5*net log P over the top-40) = +4 pts top1 on two disjoint real-swipe slices (A 87.9->91.9, B 88.8->92.3, top3 +1); net alone ~ shipped; 15-22 ms/forward warm on x86 JVM; research/R30-trained-shape-model.md section 6

## Do this next
step 3c: extend ShapeNetEvalTest: s2l short-word counts, confidence gate (skip net when shipped margin big), test split (futo test.jsonl) as final held-out; then step 4 integration (ShapeRescorer in :core + weights asset + IME hook off-UI-thread + Settings switch + NOTICE)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/R30-trained-shape-model.md

## Last ten checkpoints
```
  e9c055f ckpt 927: R30 step 3a: RealSwipe.read now keeps swipe times as ms since the first point 
  fa2d033 ckpt 925: R30 step 2 DONE: core/.../glide/ShapeNet.kt (pure Kotlin forward pass of the o
  f635ef6 ckpt 921: R30 step 1c: the open encoder is fully specified in research/R30-trained-shape
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
  f3e4471 ckpt 899: swoop-v0.7.0-alpha9 (code 20) DISPATCHED ONCE: android.yml run 44943874370 (ru
  3da4478 ckpt 898: gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
