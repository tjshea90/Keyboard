# CHECKPOINT 1245 — read me first, then TASKS.md

**Written:** 2026-11-27T04:45:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-turing-tyvm1e` · **builds on:** `db46c91` (this checkpoint is the commit after it)

## Just done
R39 DONE: repo settings put Google's Maven Central copy before mavenCentral (plugins + libraries, CI and SWOOP_NO_MIRROR keep Central), tools/gradle-central-mirror.init.gradle, prewarm.sh retries on 429, self-test check, CLAUDE.md + research/R39-maven-429.md; environment setup script updated (saved, runs in the NEXT fresh container); cold-cache proof 0 Central downloads

## Do this next
R38 finish: full2.log in scratchpad = :core:test :ime:testDebugUnitTest :ime:lintDebug on the final tree (run via retry.sh); then bump ime/build.gradle.kts to code 28 / 0.7.0-alpha17, tick R38-1..5 (bash tools/claim.sh R38 --done), bash ship.sh 'note', dispatch android.yml (mcp__github__actions_run_trigger run_workflow ref claude/peaceful-turing-tyvm1e inputs full_build=true note), wait, bash tools/record-release.sh swoop-v0.7.0-alpha17, plain-words answer: R38 (merged state, file numbers, edges/one-letter causes, bigger erase key), R39 (429 fix + what he must still do: nothing, script already saved)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e97ab52 ckpt 1236: R39 (owner mid-session: Maven 429 keeps happening, thought a Google mirror in
  b04be3e ckpt 1231: R38 code written, not yet verified: KeysView (erase key bigger via widenErase
  b1d3f23 ckpt 1216: R38 (owner 2026-11-27: training file alpha16, glide edges/one-letter, bigger 
  e66717b ckpt 1205: R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
