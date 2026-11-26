# CHECKPOINT 1115 — read me first, then TASKS.md

**Written:** 2026-11-26T21:12:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `3a49707` (this checkpoint is the commit after it)

## Just done
R35 (owner asked for a prompt to change the setup script permanently) answered in chat and written into TASKS; alpha15 candidate coded: big-key tap/glide limits in dp (KeysViewTest 70 green) and held domain chip asks nothing (presenter test green)

## Do this next
Next: (1) alpha14 run 45895826479: when green + Release has APK -> bash tools/record-release.sh swoop-v0.7.0-alpha14 with the identical note (see ckpt 1110 / TASKS R34a2); (2) THEN bump ime/build.gradle.kts to 0.7.0-alpha15 code 26 (record BEFORE bumping), full :core:test :ime:testDebugUnitTest :ime:lintDebug, bash ship.sh note (big-key tap/glide limits in dp: a short flick on big keys is a glide not the first letter; a held e-mail domain chip asks nothing), dispatch android.yml once, record-release; (3) final plain answer with alpha13/14/15 links + open items (R34f owner steps incl. Glide practice file, R33f/R32f waiting, Kestrel copy not synced because the Browser repo is not attached here)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  5b98638 ckpt 1110: alpha14 (ime code 25, swoop-v0.7.0-alpha14) gated, pushed and android.yml DIS
  1c1b1f5 ckpt 1109: gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, un
  237e3ca ckpt 1106: R34a2 offline lab PracticeEval + PracticeEvalTest (simulated tablet glides ro
  dcae6cc ckpt 1103: alpha13 RELEASED and recorded (run 45892015689 green); R34a2 glide practice c
  23bde72 ckpt 1092: R34a2 Glide practice written into TASKS (after the measured ceiling R34a-meas
  fc5913f ckpt 1091: R34a measured (docs only so far): word-pair model, weight grid and miss class
  7c25fd6 ckpt 1087: alpha13 (ime code 24, swoop-v0.7.0-alpha13) gated, pushed and android.yml DIS
  59b1eb9 ckpt 1086: gated swoop-v0.7.0-alpha13 (ime code 24) and pushed it: handoff self-test, un
  e2fabad ckpt 1085: R34b R34c DONE (e-mail boxes recognised by hint/label, learning in every ordi
  002111e ckpt 1069: R34d DONE in code+tests: KeysView no longer types the first letter when a sec
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
