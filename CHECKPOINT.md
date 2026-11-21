# CHECKPOINT 920 — read me first, then TASKS.md

**Written:** 2026-11-21T22:35:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `548dc93` (this checkpoint is the commit after it)

## Just done
R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth Parekh, branch master; near-verbatim, NOT byte-exact) to scratchpad r30 incl. the repo's own Kotlin forward pass SwipeNet.kt (501 lines), model.py, preprocess.py, layout.py, export_weights.py; weights file header = magic SWEN, u32 version=1, u32 count=69, then per tensor name+dims+float32 (first tensor 'savgol' 3x7)

## Do this next
read scratchpad r30 SwipeNet.kt + ml__swipe__model.py + ml__swipe__preprocess.py + ml__swipe__layout.py (scratchpad is LOST on a cold resume: refetch with a general-purpose helper and WebFetch of raw.githubusercontent.com/xxparthparekhxx/keyboard/master/...), write the architecture into research/R30-trained-shape-model.md section 5, then port to core/src/main/kotlin/app/kestrel/core/input/glide/ShapeNet.kt (pure Kotlin, attribution header) + golden test

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2af4ffc ckpt 918: R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.b
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
  f3e4471 ckpt 899: swoop-v0.7.0-alpha9 (code 20) DISPATCHED ONCE: android.yml run 44943874370 (ru
  3da4478 ckpt 898: gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit
  cc287f0 ckpt 885: R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only le
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
