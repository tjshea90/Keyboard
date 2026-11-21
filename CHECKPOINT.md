# CHECKPOINT 918 — read me first, then TASKS.md

**Written:** 2026-11-21T22:12:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `2b6f7cf` (this checkpoint is the commit after it)

## Just done
R30c step 1a: R30FetchTest (test-side, R30_FETCH=1) downloaded swipe_encoder.bin (2529224 B, sha256 d569395f...) and swipe_words.txt (1721282 B) into core/build/r30 (git-ignored); waiting for the helper that saves the repo's ml/ source to scratchpad/r30

## Do this next
read scratchpad r30 files: LICENSE first line, model def, features, export layout; then numpy reference forward tools/r30/ref_forward.py and golden vectors

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fcbb1d3 ckpt 916: R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; 
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
  f3e4471 ckpt 899: swoop-v0.7.0-alpha9 (code 20) DISPATCHED ONCE: android.yml run 44943874370 (ru
  3da4478 ckpt 898: gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit
  cc287f0 ckpt 885: R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only le
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
