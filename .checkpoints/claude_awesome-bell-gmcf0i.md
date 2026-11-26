# CHECKPOINT 1125 — read me first, then TASKS.md

**Written:** 2026-11-26T21:21:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `d3e7cf9` (this checkpoint is the commit after it)

## Just done
alpha14 RELEASED and recorded (run 45895826479 green); emulator smoke API 29 green on the alpha15 candidate (run 45896625754); alpha15 (ime code 26) gated, pushed and android.yml DISPATCHED once; API 36 smoke dispatched

## Do this next
Next: find the newest android.yml run on claude/awesome-bell-gmcf0i (mcp__github__actions_list list_workflow_runs android.yml), wait for all 3 jobs green, get_release_by_tag swoop-v0.7.0-alpha15, then bash tools/record-release.sh swoop-v0.7.0-alpha15 "<the identical note given to ship.sh: R34d (second half): on big keys (the tablet) a short flick to the next key is now a glide ...; checked on an Android 10 emulator>" ; check the API 36 smoke result too; then tick R34 parent in TASKS and send the FINAL plain answer (links: alpha13, alpha14, alpha15). NEVER dispatch alpha15 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d3e7cf9 ckpt 1124: gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, un
  c332c24 ckpt 1123: pre-ship: R34d (second half): on big keys (the tablet) a short flick to the n
  2a86173 ckpt 1118: alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
  1c1b1f5 ckpt 1109: gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, un
  237e3ca ckpt 1106: R34a2 offline lab PracticeEval + PracticeEvalTest (simulated tablet glides ro
  dcae6cc ckpt 1103: alpha13 RELEASED and recorded (run 45892015689 green); R34a2 glide practice c
  23bde72 ckpt 1092: R34a2 Glide practice written into TASKS (after the measured ceiling R34a-meas
  fc5913f ckpt 1091: R34a measured (docs only so far): word-pair model, weight grid and miss class
```
