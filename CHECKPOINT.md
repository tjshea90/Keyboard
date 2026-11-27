# CHECKPOINT 1216 — read me first, then TASKS.md

**Written:** 2026-11-27T03:50:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-turing-tyvm1e` · **builds on:** `550e1b8` (this checkpoint is the commit after it)

## Just done
R38 (owner 2026-11-27: training file alpha16, glide edges/one-letter, bigger erase key) written into TASKS.md and claimed; read KeysView touch code + analysed his file: starts never below x=31.6 px on the left (right edge used up to 403), so the system back swipe at the screen edge is the first suspect; second finger landing mid-glide is dropped; Backspace pressed during a glide is swallowed

## Do this next
R38-1 answer (all earlier branches ARE inside this branch, none on default branch ccr-c57dc9d3-149psl; alpha16 is the latest release); then run PracticeEval on his file (PRACTICE_FILE=/root/.claude/uploads/97e3f906-84af-5886-921b-1000c61ed534/f6302ae1-Swoop_glide_training_2026-11-26_2239.txt, see PracticeEval.kt header for the java command); then KeysView.kt fixes: red tests first in ime/src/test/.../KeysViewTest.kt

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e66717b ckpt 1205: R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
