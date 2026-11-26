# CHECKPOINT 1174 — read me first, then TASKS.md

**Written:** 2026-11-26T22:52:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `3b001d5` (this checkpoint is the commit after it)

## Just done
alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DISPATCHED ONCE on claude/awesome-bell-gmcf0i (full_build true); R36 docs + TASKS written; lint green; core 100 % + ime 361 tests green

## Do this next
next: WAIT for the android.yml run (mcp__github__actions_list list_workflow_runs android.yml branch claude/awesome-bell-gmcf0i; never dispatch again); when green and the Release swoop-v0.7.0-alpha16 shows the APK: bash tools/record-release.sh swoop-v0.7.0-alpha16 'Glide training (Settings > Help): glide 8 everyday paragraphs and look-alike word lists several times, the keyboard learns where your thumb lands, one file to attach; boxes that learn nothing no longer move the thumb habit'; tick R36-4 with tools/claim.sh R36 --done; optional emu-smoke.yml dispatch on the tag (API 29); then final plain answer to the owner (practice file result, would-it-work, what was built, what was NOT run on tablet, open items R30f R32f R33f R34f R36f)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
  426fe05 ckpt 1130: Android 16 emulator smoke PASSED on the released alpha15 tag (run 45900554490
  ea77773 ckpt 1128: R34 DONE: alpha13, alpha14, alpha15 released and recorded (runs 45892015689, 
  72e6f77 ckpt 1125: alpha14 RELEASED and recorded (run 45895826479 green); emulator smoke API 29 
  d3e7cf9 ckpt 1124: gated swoop-v0.7.0-alpha15 (ime code 26) and pushed it: handoff self-test, un
```
