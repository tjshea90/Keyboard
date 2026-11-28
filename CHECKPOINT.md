# CHECKPOINT 1349 — read me first, then TASKS.md

**Written:** 2026-11-28T16:23:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `9f99ceb` (this checkpoint is the commit after it)

## Just done
R40-7 run 1 (46162924215) was VACUOUS: one Backspace right after a glide takes the whole glided word off (design), so no half word was left (rows said Backspaces left ''), picks were next-word chips of an empty field. Rewrote scene_halfword: tap the first letters (composing word) after an optional undone glide, two pages: text and NEW resetting (labserver.py: a page that re-sets the field value 40 ms after every change = ends the composing word on purpose, React/Flutter/Compose pattern); KeyboardPresenter debug line now ends with lost=<InputStats.composingLost> (the old regex could never match: the line is written AFTER the report is handled); composing_lost_reports reads that counter; :ime:compileDebugKotlin green

## Do this next
next: (1) dispatch daily-sim.yml on this branch (run_workflow ref claude/bold-knuth-nn3ywo inputs browser_ref=claude/bold-knuth-nn3ywo suite=typing net=full quick=true); (2) when the artifact daily-sim-typing exists (list_workflow_run_artifacts <run id>, cheap) download_workflow_run_artifact + python3 -B -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <empty dir under scratchpad>; read report.md rows 'half word [text]' and 'half word [resetting]': OK = one clean word; FAIL = half word stuck in Gecko = fix core InputController.pickWord / composingEndedByApp then re-dispatch; INFO no chip = say so; summary rows give the lost= count (0 on [resetting] = the page did not end the composing word, so that page proves nothing about R40-3); (3) write the result into research/R40-correction-policies.md section 6, tick R40-7 and the R40 parent box in TASKS.md, ckpt; (4) final plain-words report to the owner: Swoop alpha18 https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha18 and Kestrel G alpha117 https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha117; NOT run on tablet or phone; R41 decision; soak seeds KB-101b; alpha116 recorded by hand; how to move Swoop's learned words into Kestrel's panel today: Swoop Export what the keyboard learned, Kestrel Settings Import what the keyboard learned

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  40dddef ckpt 1340: R40-7 daily-sim DISPATCHED on this repo: run 46162924215 (run_number 17, suit
  0cd4ae8 ckpt 1339: R40-6d DONE: Browser alpha117 released and recorded (https://github.com/tjshe
  084d839 ckpt 1329: STATE: Swoop alpha18 RELEASED AND RECORDED (https://github.com/tjshea90/Keybo
  f4f0a89 ckpt 1324: alpha19 prep (NOT released): core GlideLearning.confirm + KeptGlide = the sha
  2ff9b78 ckpt 1318: alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93b
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
