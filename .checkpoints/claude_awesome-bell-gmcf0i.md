# CHECKPOINT 1128 — read me first, then TASKS.md

**Written:** 2026-11-26T21:37:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `f2b397a` (this checkpoint is the commit after it)

## Just done
R34 DONE: alpha13, alpha14, alpha15 released and recorded (runs 45892015689, 45895826479, 45897535570 green); API 29 emulator smoke green; API 36 emulator smoke fails 3x at the emulator's own Quickstep ANR dialog before any key (control run on tag alpha12 dispatched); R35 setup-script prompt answered

## Do this next
Next: only the final plain answer to the owner: install alpha15 https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha15 ; owner steps R34f (translation off check, stray letter check, e-mail checks, Settings > Help > Glide practice -> Save the file -> attach); then next session: if a practice file arrives: PRACTICE_FILE=<abs path> java ... JUnitCore app.kestrel.core.input.glide.PracticeEval (see research/R34 section 5), tune GlideParams.forPitchDp for big keys on it. Waiting on the owner too: R33f (ave/and), R32f, R30f. Not synced to the Kestrel browser (Browser repo not attached).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  72e6f77 ckpt 1125: alpha14 RELEASED and recorded (run 45895826479 green); emulator smoke API 29 
  d3e7cf9 ckpt 1124: gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, un
  c332c24 ckpt 1123: pre-ship: R34d (second half): on big keys (the tablet) a short flick to the n
  2a86173 ckpt 1118: alpha15 candidate lint green; emulator smoke (API 29) dispatched on HEAD 744b
  2833998 ckpt 1115: R35 (owner asked for a prompt to change the setup script permanently) answere
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
  1c1b1f5 ckpt 1109: gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, un
  237e3ca ckpt 1106: R34a2 offline lab PracticeEval + PracticeEvalTest (simulated tablet glides ro
  dcae6cc ckpt 1103: alpha13 RELEASED and recorded (run 45892015689 green); R34a2 glide practice c
  23bde72 ckpt 1092: R34a2 Glide practice written into TASKS (after the measured ceiling R34a-meas
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
