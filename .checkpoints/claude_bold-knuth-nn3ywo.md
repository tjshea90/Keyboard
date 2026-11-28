# CHECKPOINT 1339 — read me first, then TASKS.md

**Written:** 2026-11-28T16:01:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `797a7ec` (this checkpoint is the commit after it)

## Just done
R40-6d DONE: Browser alpha117 released and recorded (https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha117, run 46160819337 all green, Browser BUILDLOG 574dd44e, pushed); daily-sim run 46160968809 green (real Gecko fields: tap 4/4 exact, glide 10/10, address bar, private tab). Started R40-7: debug-only chip positions in KeysView.keymapLine (chip<i>=x,y, geometry only, :ime:compileDebugKotlin green) + tools/sim/dailysim.py scene_halfword (glide, Backspace to half, tap chip, field must hold ONE word)

## Do this next
next: (1) bash tools/ckpt.sh then dispatch daily-sim.yml on THIS branch (mcp__github__actions_run_trigger run_workflow, workflow daily-sim.yml, ref claude/bold-knuth-nn3ywo, inputs suite=typing quick=true); read the artifact (mcp__github__actions_get download_workflow_run_artifact, then python3 -B -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <empty dir>) report.md rows 'half word ...'; a FAIL row = doubled word in Gecko = fix in core InputController.pickWord / KeyboardPresenter; (2) put the result into research/R40-correction-policies.md section 6 and tick R40-7 + R40 in TASKS.md; (3) final plain-words report to the owner (Swoop alpha18 https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha18, Browser alpha117 link above; not run on tablet/phone; R41 needs the owner's decision; soak seeds KB-101b; alpha116 hand-recorded)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  084d839 ckpt 1329: STATE: Swoop alpha18 RELEASED AND RECORDED (https://github.com/tjshea90/Keybo
  f4f0a89 ckpt 1324: alpha19 prep (NOT released): core GlideLearning.confirm + KeptGlide = the sha
  2ff9b78 ckpt 1318: alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93b
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
