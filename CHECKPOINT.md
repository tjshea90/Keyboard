# CHECKPOINT 1118 — read me first, then TASKS.md

**Written:** 2026-11-26T21:13:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `744b7c4` (this checkpoint is the commit after it)

## Just done
alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b7c4 = run 45896625754 (checks the touch changes on a real Android 10 emulator); alpha14 android.yml run 45895826479 still building

## Do this next
Next: (1) alpha14 run 45895826479 -> when green + Release APK -> record-release.sh swoop-v0.7.0-alpha14 (note in ckpt 1110); (2) read emu-smoke run 45896625754 result (mcp__github__actions_get get_workflow_run; if red read the job log with get_job_logs) - a red smoke on the alpha15 candidate means do NOT ship alpha15 until understood; (3) then bump 0.7.0-alpha15 code 26, bash ship.sh, dispatch android.yml once (+ optionally emu-smoke api 36), record-release; (4) final plain answer

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
  1c1b1f5 ckpt 1109: gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, un
  237e3ca ckpt 1106: R34a2 offline lab PracticeEval + PracticeEvalTest (simulated tablet glides ro
  dcae6cc ckpt 1103: alpha13 RELEASED and recorded (run 45892015689 green); R34a2 glide practice c
  23bde72 ckpt 1092: R34a2 Glide practice written into TASKS (after the measured ceiling R34a-meas
  fc5913f ckpt 1091: R34a measured (docs only so far): word-pair model, weight grid and miss class
  7c25fd6 ckpt 1087: alpha13 (ime code 24, swoop-v0.7.0-alpha13) gated, pushed and android.yml DIS
  59b1eb9 ckpt 1086: gated swoop-v0.7.0-alpha13 (ime code 24) and pushed it: handoff self-test, un
  e2fabad ckpt 1085: R34b R34c DONE (e-mail boxes recognised by hint/label, learning in every ordi
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
