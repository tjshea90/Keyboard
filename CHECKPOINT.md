# CHECKPOINT 1318 — read me first, then TASKS.md

**Written:** 2026-11-28T15:17:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `93bb9c7` (this checkpoint is the commit after it)

## Just done
alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93bb9c7 (gates green here: :core:test + :ime unit suites). Browser side so far (my Browser branch claude/bold-knuth-nn3ywo, fast-forwarded to the newest tip b2dc6b3f = alpha116, which already PUBLISHED (run 318 green, unrecorded in the Browser BUILDLOG: its session was cut off): sync-keyboard-core.sh + check-keyboard-core.sh extended for the shape network (shape_sha256 in KEYBOARD_PIN), engine synced from Keyboard commit 10c0ce1 (word list identical, engine +59 files), :core + :app COMPILE against it, Browser handoff self-test 100 passed, MiniNav.startGlideEngine passes loadShape + shapeModel=true. UNCOMMITTED in the Browser tree (not pushed yet). Found: MiniNav.confirmGlide teaches at FULL weight (old policy, no LearnGuard/keepTrust) = the ave/access/yo loop still lives in the Browser; two vendored tests (ShapeRescorerTest, ShapeNetParityTest) look for the weights/reference only in Swoop's folder layout and would fail in the Browser

## Do this next
next: (1) wait for run 46159028756 (mcp__github__actions_get get_workflow_run; get_release_by_tag swoop-v0.7.0-alpha18 shows the APK) then bash tools/record-release.sh swoop-v0.7.0-alpha18 with the note from CHECKPOINT/ship log; tick TASKS R40; (2) alpha19 prep in core: new GlideLearning.confirm + KeptGlide (shared learning rule extracted from GlideSession.confirmPrevious, same behaviour, tests GlideLearningTest), ShapeRescorerTest/ShapeNetParityTest also look in geckoapp/src/main/assets/glide, tools/sync-browser.sh (draft in the scratchpad), resume.sh status line; (3) Browser: sync again from the alpha19 commit (also copy core/src/test/resources/swipe_reference.bin), MiniNav.confirmGlide -> GlideLearning.confirm, MiniNavTest for a kept rare look-alike, run -p Browser :core:test :app:testDebugUnitTest (full Browser core run in background log scratchpad/browser-coretest.log), update docs/KEYBOARD-LINK.md, ckpt + push the Browser branch, then the Browser release only via its workflow (nothing else running there)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
  55b0b1e ckpt 1248: gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, un
```
