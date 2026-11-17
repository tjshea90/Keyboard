# CHECKPOINT 204 — read me first, then TASKS.md

**Written:** 2026-11-17T02:26:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `be5d9d8` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-c57dc9d3-149psl, inputs {"full_build":"true","note":"0.2.0-alpha1: number row, instant glide start and faster trail, quicker key pop-up, word suggestions while tapping, learning from typing (words, pairs, corrections, erase and retype), automatic typo fix with undo, options (space after punctuation, double space, capitals, themes), redesigned settings screen"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.2.0-alpha1), then:  bash tools/record-release.sh swoop-v0.2.0-alpha1 "0.2.0-alpha1: number row, instant glide start and faster trail, quicker key pop-up, word suggestions while tapping, learning from typing (words, pairs, corrections, erase and retype), automatic typo fix with undo, options (space after punctuation, double space, capitals, themes), redesigned settings screen"
NEVER ship swoop-v0.2.0-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
```
