# CHECKPOINT 1340 — read me first, then TASKS.md

**Written:** 2026-11-28T16:02:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `0cd4ae8` (this checkpoint is the commit after it)

## Just done
R40-7 daily-sim DISPATCHED on this repo: run 46162924215 (run_number 17, suite=typing quick=true browser_ref=claude/bold-knuth-nn3ywo, head ckpt 1339 = scene_halfword + chip positions); takes about 15 min

## Do this next
next: (1) mcp__github__actions_get get_workflow_run 46160968809 -> no: 46162924215 on tjshea90/Keyboard; when completed: list_workflow_run_artifacts 46162924215, download_workflow_run_artifact, python3 -B -I /home/user/Browser/tools/emu/fetch_artifact.py <download_url> <empty dir under scratchpad>, read report.md rows 'half word ...' (OK = one clean word; FAIL = doubled prefix in Gecko: fix in core InputController.pickWord / KeyboardPresenter then re-dispatch; INFO no chip = strip shows nothing after the Backspaces: say so); (2) write the result in research/R40-correction-policies.md section 6, tick R40-7 and the R40 parent box in TASKS.md, bash tools/ckpt.sh; (3) final plain-words report to the owner: Swoop alpha18 https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha18 , Kestrel G alpha117 https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha117 ; NOT run on tablet or phone; R41 live shared memory needs the owner's decision; fuzz soak seeds 1388/2321 are the old KB-101b class; alpha116 of the other Browser session was recorded by hand

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0cd4ae8 ckpt 1339: R40-6d DONE: Browser alpha117 released and recorded (https://github.com/tjshe
  084d839 ckpt 1329: STATE: Swoop alpha18 RELEASED AND RECORDED (https://github.com/tjshea90/Keybo
  f4f0a89 ckpt 1324: alpha19 prep (NOT released): core GlideLearning.confirm + KeptGlide = the sha
  2ff9b78 ckpt 1318: alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93b
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
```
