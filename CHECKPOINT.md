# CHECKPOINT 1329 — read me first, then TASKS.md

**Written:** 2026-11-28T15:37:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `936d206` (this checkpoint is the commit after it)

## Just done
STATE: Swoop alpha18 RELEASED AND RECORDED (https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha18, run 46159028756 green, BUILDLOG line written). Emulator smoke on a real Android: API 29 run 44 GREEN; API 36 run 45 FAILED before typing (no Setup text field: smoke.sh scrolled with a fixed swipe from y=1500, off the 1280 px API 36 screen, and my longer 'Fix the word before' text pushed the try-it box below the fold) -> tools/emu/smoke.sh now scrolls with the real screen size (834c46d), API 36 smoke RE-DISPATCHED (check it). Browser (own branch claude/bold-knuth-nn3ywo, pushed ckpt 3613 95c93622, green handoff 100/100; Browser tests here: core 2297 tests 1 load-flake (PhoneBackendTest, 19/19 alone), app 516 + new MiniNavTest rare-look-alike test 0 failed, mutation-checked): Kestrel alpha117 (gecko code 218) DISPATCHED: Browser android.yml run 46160819337 (run_number 319); note saved in scratchpad/browser-note.txt (same text as the dispatch). alpha116 (code 217) of the other Browser session is published but UNRECORDED in the Browser BUILDLOG

## Do this next
next: (1) mcp__github__actions_get get_workflow_run resource_id 46160819337 on tjshea90/Browser (or get_release_by_tag kestrel-g-v0.3.0-alpha117); when green: cd-free: bash /home/user/Browser/tools/record-release.sh kestrel-g-v0.3.0-alpha117 "$(cat scratchpad/browser-note.txt)" then git -C /home/user/Browser push (the record commit goes on my Browser branch only); if red read get_job_logs failed_only and fix on my Browser branch (never main); (2) check the API 36 smoke re-run on this repo (list_workflow_runs emu-smoke.yml); (3) tick TASKS R40 + R40-6d and the Browser TASKS KBR40d with the Release link; (4) tell the owner in plain words (what was fixed, research, what is NOT proven on the tablet, the R41 decision, alpha116 unrecorded)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f4f0a89 ckpt 1324: alpha19 prep (NOT released): core GlideLearning.confirm + KeptGlide = the sha
  2ff9b78 ckpt 1318: alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93b
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
