# CHECKPOINT 1324 — read me first, then TASKS.md

**Written:** 2026-11-28T15:19:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `0a989a5` (this checkpoint is the commit after it)

## Just done
alpha19 prep (NOT released): core GlideLearning.confirm + KeptGlide = the shared glide-learning rule (R29 LearnGuard, R33 keepTrust, unlearn the wrong first guess) extracted from GlideSession.confirmPrevious with unchanged behaviour (GlideSessionTest, PostCorrectionTest, KeptRareWordTest, AdaptiveLearningTest, CommonWordGuardTest all green) + GlideLearningTest 5; ShapeRescorerTest and ShapeNetParityTest also find the weights in geckoapp/src/main/assets/glide (the Browser layout); tools/sync-browser.sh (status / sync / --test) wired into resume.sh (status line), ship.sh (next-step card) and test_handoff.sh. alpha18 build is still running: run 46159028756

## Do this next
next: (1) check run 46159028756 (mcp__github__actions_get get_workflow_run resource_id 46159028756) and record alpha18 when green: bash tools/record-release.sh swoop-v0.7.0-alpha18 with the ship note; (2) Browser: add swipe_reference.bin copy to its sync-keyboard-core.sh, run bash tools/sync-browser.sh --test (or the steps by hand), MiniNav.confirmGlide -> GlideLearning.confirm (+ pendingSerial from GlideResult), MiniNavTest kept-rare-look-alike test, docs/KEYBOARD-LINK.md; (3) Swoop docs/KESTREL-LINK.md + CLAUDE.md rule line + TASKS R40-6 boxes; then ship alpha19 (version bump 0.7.0-alpha19 code 30) so the Browser pin names a RELEASED engine

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2ff9b78 ckpt 1318: alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93b
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
