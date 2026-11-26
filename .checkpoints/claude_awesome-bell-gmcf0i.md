# CHECKPOINT 1148 — read me first, then TASKS.md

**Written:** 2026-11-26T22:20:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `6b8597e` (this checkpoint is the commit after it)

## Just done
R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNOWN word, never counts/pairs), GlideTyping.teach (executor, serial check, shared thumbMoved), Diagnostics 'Glide training taught N', thumb habit now gated by learnAllowed (no-learning boxes), GlidePractice v2 (contexts, tags, append), PracticeEval.replay; GlideTeachingTest green; his file replay: thumb habit only 1.9/1.7 dp, teaching = plain on his 40 glides

## Do this next
next: (1) core GlideTraining.kt (paragraph pages + drill pages + missed-words page, tokens, scores) + GlideTrainingTest (all words in shipped list); (2) Practice.Sink returns the word to teach + prev/prev2 + serial, presenter calls glide.engine.teach, update KeyboardPresenterTest practice tests; (3) TrainingActivity + manifest + strings + SetupActivity row + test; (4) TrainingLabTest REAL_TABLET=1; (5) alpha16 ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
  426fe05 ckpt 1130: Android 16 emulator smoke PASSED on the released alpha15 tag (run 45900554490
  ea77773 ckpt 1128: R34 DONE: alpha13, alpha14, alpha15 released and recorded (runs 45892015689, 
  72e6f77 ckpt 1125: alpha14 RELEASED and recorded (run 45895826479 green); emulator smoke API 29 
  d3e7cf9 ckpt 1124: gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, un
  c332c24 ckpt 1123: pre-ship: R34d (second half): on big keys (the tablet) a short flick to the n
  2a86173 ckpt 1118: alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
  1c1b1f5 ckpt 1109: gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, un
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
