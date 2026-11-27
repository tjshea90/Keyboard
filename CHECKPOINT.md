# CHECKPOINT 1236 — read me first, then TASKS.md

**Written:** 2026-11-27T04:21:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-turing-tyvm1e` · **builds on:** `ab4dcdf` (this checkpoint is the commit after it)

## Just done
R39 (owner mid-session: Maven 429 keeps happening, thought a Google mirror in the setup script fixed it, find the permanent fix) written into TASKS.md. R38 full suites :core:test :ime:testDebugUnitTest :ime:lintDebug passed on retry attempt 2 (attempt 1 failed in lintAnalyzeDebug after 7m, check if 429)

## Do this next
R39-1: read settings.gradle.kts repositories, ~/.gradle/init.d + gradle.properties, env vars (GRADLE_*, MAVEN_*), get_environment (setup script text), research/R35 note about the setup script; then fix; then finish R38 (version bump 28 alpha17, ship.sh, dispatch android.yml)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  TASKS.md

## Last ten checkpoints
```
  b04be3e ckpt 1231: R38 code written, not yet verified: KeysView (erase key bigger via widenErase
  b1d3f23 ckpt 1216: R38 (owner 2026-11-27: training file alpha16, glide edges/one-letter, bigger 
  e66717b ckpt 1205: R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
