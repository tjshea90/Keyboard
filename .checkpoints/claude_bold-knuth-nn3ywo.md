# CHECKPOINT 1317 — read me first, then TASKS.md

**Written:** 2026-11-28T15:16:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `10c0ce1` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/bold-knuth-nn3ywo, inputs {"full_build":"true","note":"Fixes the keyboard rewriting a common word into a rare one (to it -> yo it) and a half-typed word staying in front of a suggestion: weights for rare words, recovery when an app ends the word being typed, names and capitals-only words never auto-fixed"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha18), then:  bash tools/record-release.sh swoop-v0.7.0-alpha18 "Fixes the keyboard rewriting a common word into a rare one (to it -> yo it) and a half-typed word staying in front of a suggestion: weights for rare words, recovery when an app ends the word being typed, names and capitals-only words never auto-fixed"
NEVER ship swoop-v0.7.0-alpha18 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
  55b0b1e ckpt 1248: gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, un
  ec2b546 ckpt 1247: R38-1..4 ticked; version bumped to code 28 / 0.7.0-alpha17; full suites green
```
