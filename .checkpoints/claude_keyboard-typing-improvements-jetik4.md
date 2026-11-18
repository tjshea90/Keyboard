# CHECKPOINT 409 — read me first, then TASKS.md

**Written:** 2026-11-18T01:19:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `0cae2a4` (this checkpoint is the commit after it)

## Just done
R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no key shapes, icons, lit key; symbols page unaffected); KeysViewLookTest hiddenLetters day+night green

## Do this next
R18c next: ime/KeyboardRoot.kt + core FloatingMath: (1) visible 'full size' button on GripBar when squeezed (gripFullWidth) / floating resized (reset to default size), (2) smooth resize: apply live pixel geometry in doResize (no fraction round trip), one layout per frame (postOnAnimation), anchored edge never moves; tests in KeyboardRootTest + FloatingMathTest; then R18b, R18a, R18d

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  docs/LAYOUT.md
    M  ime/src/main/kotlin/app/swoop/keyboard/SetupActivity.kt

## Last ten checkpoints
```
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
