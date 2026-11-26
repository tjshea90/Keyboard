# CHECKPOINT 1167 — read me first, then TASKS.md

**Written:** 2026-11-26T22:38:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `e2ea95f` (this checkpoint is the commit after it)

## Just done
R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unasked words): thumb lesson +1.5 phone/+0.3..0.6 tablet, shape level on phone (+1.8 on practised words) and -0.1/-1.0 on tablet, confusion -2.1/-5.1 on tablet -> keyboard teaches thumb always, shape only pitch<=45dp, confusion never (GlideTeaching.Parts.forPitchDp); shipped variant never worse (+1.3 phone, +0.4/+0.2 tablet); no-learning boxes move no thumb habit (GlideSessionTest)

## Do this next
next: (1) full :core:test + :ime:testDebugUnitTest + :ime:lintDebug + bash tools/test_handoff.sh (2) research/R36-glide-training.md (numbers above + owner file replay: habit 1.9/1.7 dp, taught=plain 38/40) + TASKS R36 ticks/R36f + docs/STRIP? no (3) bump ime/build.gradle.kts 0.7.0-alpha16 code 27, bash ship.sh, dispatch android.yml once (ref claude/awesome-bell-gmcf0i, full_build true), wait, bash tools/record-release.sh swoop-v0.7.0-alpha16, emu-smoke dispatch optional (4) final plain answer

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M ime/src/main/res/values/strings.xml

## Last ten checkpoints
```
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
  426fe05 ckpt 1130: Android 16 emulator smoke PASSED on the released alpha15 tag (run 45900554490
  ea77773 ckpt 1128: R34 DONE: alpha13, alpha14, alpha15 released and recorded (runs 45892015689, 
  72e6f77 ckpt 1125: alpha14 RELEASED and recorded (run 45895826479 green); emulator smoke API 29 
  d3e7cf9 ckpt 1124: gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, un
  c332c24 ckpt 1123: pre-ship: R34d (second half): on big keys (the tablet) a short flick to the n
  2a86173 ckpt 1118: alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
