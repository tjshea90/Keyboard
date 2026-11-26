# CHECKPOINT 1181 — read me first, then TASKS.md

**Written:** 2026-11-26T23:05:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `b47b39c` (this checkpoint is the commit after it)

## Just done
R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha16, run 45907515000 green: check, test-jvm, build; APK cert pin verified), emulator smoke green API 29 (45907615226) + API 36 (45907618364), BUILDLOG recorded, TASKS R36 ticked, research/R36-glide-training.md written

## Do this next
nothing running. Waiting on the owner: R36f (alpha16: Settings > Help > Glide training at everyday speed, attach 'Swoop glide training <date>.txt'), R34f, R33f, R32f, R30f. When his training file arrives: from core/ run PRACTICE_FILE=<attached path> java -cp $CP org.junit.runner.JUnitCore app.kestrel.core.input.glide.PracticeEval (prints TRAINING lines: running text vs drills, rank of misses, the word before helps?, replay with teaching) after ./gradlew :core:compileTestKotlin and a cp.env (CP=build/classes/kotlin/main:build/classes/kotlin/test:build/resources/test:kotlin-stdlib:junit:hamcrest), never copy the file into the repo; decide next fix from the misses (language/order = R31 next-word model or context weight; path = reach of remembered shapes). Not synced to the Kestrel browser. R35 setup-script prompt answered (owner may say 'do it'). Final plain answer to the owner is the last thing to send

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
  426fe05 ckpt 1130: Android 16 emulator smoke PASSED on the released alpha15 tag (run 45900554490
  ea77773 ckpt 1128: R34 DONE: alpha13, alpha14, alpha15 released and recorded (runs 45892015689, 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
