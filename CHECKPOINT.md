# CHECKPOINT 916 — read me first, then TASKS.md

**Written:** 2026-11-21T22:11:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `02d64f3` (this checkpoint is the commit after it)

## Just done
R30a/b DONE: research/R30-trained-shape-model.md (Gboard model not adoptable; open MIT data + encoders found: FUTO swipe corpus 1.04M swipes, xxparthparekhxx swipe_encoder.bin 634K params layout-agnostic TCN+CTC, youcoded 529K, omniglide)

## Do this next
R30c step 1: fetch swipe_encoder.bin (huggingface.co/xxparthparekhxx/compose-keyboard-swipe-encoder/resolve/main/swipe_encoder.bin) + repo ml/ code into core/build/r30/ via a JUnit-side fetch like RealSwipe.fetch (no curl); READ the LICENSE file of both repos first; then numpy reference forward in tools/r30/ and Kotlin port in core/.../glide/ShapeNet.kt

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    ?? research/R30-trained-shape-model.md

## Last ten checkpoints
```
  02d64f3 ckpt 915: R30 written into TASKS in the owner's words (adopt Gboard's trained shape mode
  79719b8 ckpt 901: ckpt: swoop-v0.7.0-alpha9 RELEASED AND RECORDED (android.yml run 44943874370 g
  f3e4471 ckpt 899: swoop-v0.7.0-alpha9 (code 20) DISPATCHED ONCE: android.yml run 44943874370 (ru
  3da4478 ckpt 898: gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit
  cc287f0 ckpt 885: R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only le
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
  4fbb3a2 ckpt 862: touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouch
```
