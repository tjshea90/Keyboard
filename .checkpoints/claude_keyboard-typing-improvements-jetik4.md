# CHECKPOINT 404 — read me first, then TASKS.md

**Written:** 2026-11-18T01:13:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `44259ca` (this checkpoint is the commit after it)

## Just done
R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his words

## Do this next
R18 plan: read ime KeyboardPresenter/KeysView/KeyboardRoot/Settings + core Suggester/TypingLearner/GlideDecoder; do R18e (hide letters, easiest), R18c (snap + smooth resize), R18b (auto capitalise), R18a (tap a past word -> glide-close alternatives), R18d (smarter learning, measure with PredictionYardstickTest); release 0.6.0-alpha1 (ime code 10) via bash ship.sh then dispatch android.yml once

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
