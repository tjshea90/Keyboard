# CHECKPOINT 1163 — read me first, then TASKS.md

**Written:** 2026-11-26T22:27:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `528bc08` (this checkpoint is the commit after it)

## Just done
R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own-words page, words-that-went-wrong page, per-page scores, v2 file) + GlideTrainingTest 12 green; Practice.Sink returns the asked word + prev/prev2, presenter teaches the engine; TrainingActivity (start/page/break/end, box holds the words so far, switch learn on/off, own words, save, leave-guard) + manifest + strings + Settings row + TrainingActivityTest; PracticeFile helper shared; ime tests green

## Do this next
next: (1) core test that a no-learning field does not move the thumb habit (GlideSessionTest) (2) TrainingLabTest REAL_TABLET=1 (simulated person, learned vs plain hold-out, per lesson) for the honest 'would it work' (3) full :core:test + :ime:testDebugUnitTest + :ime:lintDebug (4) docs research/R36-glide-training.md + TASKS R36 + bump 0.7.0-alpha16 code 27 + bash ship.sh + dispatch android.yml once + record-release + final answer

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
  426fe05 ckpt 1130: Android 16 emulator smoke PASSED on the released alpha15 tag (run 45900554490
  ea77773 ckpt 1128: R34 DONE: alpha13, alpha14, alpha15 released and recorded (runs 45892015689, 
  72e6f77 ckpt 1125: alpha14 RELEASED and recorded (run 45895826479 green); emulator smoke API 29 
  d3e7cf9 ckpt 1124: gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, un
  c332c24 ckpt 1123: pre-ship: R34d (second half): on big keys (the tablet) a short flick to the n
  2a86173 ckpt 1118: alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
