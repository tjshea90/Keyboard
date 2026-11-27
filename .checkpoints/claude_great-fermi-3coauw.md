# CHECKPOINT 1187 — read me first, then TASKS.md

**Written:** 2026-11-27T00:01:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/great-fermi-3coauw` · **builds on:** `8dbb056` (this checkpoint is the commit after it)

## Just done
R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md and claimed. Findings so far: R37-1 yes, 23 tj-android-kit skills are in the skill list (682 skills from 70 plugin groups, ListPlugins returns empty); R37-2 ANDROID_HOME=/opt/android-sdk, platforms android-29/34/36, build-tools 34.0.0+36.0.0 (+35.0.0 auto-installed by Gradle), environment network policy is unrestricted so NO domain is blocked: the cold-cache build got HTTP 429 (rate limit) from repo.maven.apache.org, :core:compileKotlin passes on retry; full build+tests running in the background; CLAUDE.md block 'Session Learnings' is byte-identical to AGENTS.md and last written 2026-11-19; default branch lacks tools/prewarm.sh so the env setup script only ran gradlew help

## Do this next
R37-3: edit CLAUDE.md: remove duplicate handoff block (lines 51-74, identical in AGENTS.md), fix stale text (SDK platforms are 29/34/36 not 36/37, assigned branch name ccr-c57dc9d3-149psl is stale, 'encrypted key file' is gone: signing/ holds only the public dev key), add the Skills section (repo rules win), add the 429/prewarm learning to Commands. Then: read the build result in the scratchpad file full-summary.log (if lost, rerun ./gradlew --continue --no-parallel --max-workers=2 :core:test :ime:assembleDebug :ime:testDebugUnitTest :ime:lintDebug; a 429 means just rerun), R37-4 list of plugins/skills/connectors to switch off (no installs), tick boxes with bash tools/claim.sh R37 --done evidence, R37-5 final plain-words answer to the owner on branch claude/great-fermi-3coauw

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
  426fe05 ckpt 1130: Android 16 emulator smoke PASSED on the released alpha15 tag (run 45900554490
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
