# CHECKPOINT 992 — read me first, then TASKS.md

**Written:** 2026-11-24T07:43:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-euler-w64u7s` · **builds on:** `b69ba38` (this checkpoint is the commit after it)

## Just done
R32b digit long-press row (ImeLayout.digitHolds, KeysView.openDigitPopup, 7 view tests); R32c EmailBook + EmailLearner + typedEmail/pickAddress + presenter strip + Settings switch/forget (core 636 tests green, ime presenter+view 145 green)

## Do this next
R32e rare words: check how to get the real-swipe corpus (research/R24-glide-next.md, REAL_SWIPES=1 harness; huggingface fetch may be blocked), measure ave/fur vs and/for on tap path (Suggester) and glide path (GlideDecoder rarity), then tick R32a-d in TASKS via tools/claim.sh and run lint (:ime:lintDebug) before ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  90d27ce ckpt 978: R32a core: weak-space swap (typed/strip/glide space + , . ! ? takes the space,
  6f8e218 ckpt 956: R32 written into TASKS.md (a-e) in the owner's words; session branch claude/bo
  991a420 ckpt 953: R30 DONE: alpha10 released (https://github.com/tjshea90/Keyboard/releases/tag/
  a40cab1 ckpt 951: R30 alpha10 shipped to GitHub: ship.sh green (handoff test, 290 ime + core tes
  e13f2a7 ckpt 950: gated swoop-v0.7.0-alpha10 (ime code 21) and pushed it: handoff self-test, uni
  cc1cf34 ckpt 947: R30 step 4b: ime tests 290 green (KeysViewTest timed-glide test, safe-mode eng
  7391048 ckpt 942: R30 step 4a: :core ShapeRescorer (hybrid score+0.7*logP, gate 3.5, letter-box 
  5bfe37a ckpt 932: R30 step 3c DONE: three disjoint real-swipe sets incl. FUTO TEST split: hybrid
  881fdb5 ckpt 930: R30 step 3 RESULT: hybrid (shipped score + 0.5*net log P over the top-40) = +4
  e9c055f ckpt 927: R30 step 3a: RealSwipe.read now keeps swipe times as ms since the first point 
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
