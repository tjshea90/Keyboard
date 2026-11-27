# CHECKPOINT 1248 — read me first, then TASKS.md

**Written:** 2026-11-27T04:48:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-turing-tyvm1e` · **builds on:** `ec2b546` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/peaceful-turing-tyvm1e, inputs {"full_build":"true","note":"R38: glides that start at the screen edge are no longer taken by Android's back swipe, the next word's thumb is kept when the first lifts early, touches beside a squeezed keyboard count, erase pressed with the other thumb mid-glide works, erase key bigger (R39: Gradle asks Google's Maven Central copy first)"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha17), then:  bash tools/record-release.sh swoop-v0.7.0-alpha17 "R38: glides that start at the screen edge are no longer taken by Android's back swipe, the next word's thumb is kept when the first lifts early, touches beside a squeezed keyboard count, erase pressed with the other thumb mid-glide works, erase key bigger (R39: Gradle asks Google's Maven Central copy first)"
NEVER ship swoop-v0.7.0-alpha17 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ec2b546 ckpt 1247: R38-1..4 ticked; version bumped to code 28 / 0.7.0-alpha17; full suites green
  a540b09 ckpt 1245: R39 DONE: repo settings put Google's Maven Central copy before mavenCentral (
  e97ab52 ckpt 1236: R39 (owner mid-session: Maven 429 keeps happening, thought a Google mirror in
  b04be3e ckpt 1231: R38 code written, not yet verified: KeysView (erase key bigger via widenErase
  b1d3f23 ckpt 1216: R38 (owner 2026-11-27: training file alpha16, glide edges/one-letter, bigger 
  e66717b ckpt 1205: R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
```
