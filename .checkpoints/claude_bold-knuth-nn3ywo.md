# CHECKPOINT 1316 — read me first, then TASKS.md

**Written:** 2026-11-28T15:05:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `f35281b` (this checkpoint is the commit after it)

## Just done
worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime tests+lint green; ReviseAuditLabTest (R40_LAB=1) results in research/R40-correction-policies.md 5.3 (everyday sentences: 0 spoiled, guard keeps good fixes); RareGuardEvalTest re-measured on 3768 real swipes = decode-time CommonGuard at its plateau, unchanged (5.5); AOSP-style rule: names (capital mid-sentence) and capitals-only words are never auto-fixed (KeyboardPresenter.looksLikeNameOrAcronym, 3 presenter tests, mutation-checked); setting text 'Fix the word before' updated; version bumped to 0.7.0-alpha18 (code 29); TASKS R40-1..R40-5 ticked with evidence (R40 itself and R40-6 stay open). The first ship.sh gate run was KILLED by the restart (log stopped at 'running the unit suites'); nothing was pushed as a release, no tag exists. Browser facts: newest tip origin/claude/elegant-darwin-h7dlay (other session pre-shipping alpha116) has KEYBOARD_PIN 4ed7b4c (Swoop alpha7); Swoop since added the 2.5 MB shape network ime/src/main/assets/glide/swipe_encoder.bin that the Browser lacks; Browser host glue is MiniNav.startGlideEngine (GlideTyping(loadMain, glideExec, loadPersonal, savePersonal, previewExecutor) with NO loadShape/loadDictionary/saveOwn); Browser sync.sh auto-merges my branch's 'ckpt N:' commits into other sessions

## Do this next
next: (1) bash ship.sh run DETACHED: setsid nohup bash ship.sh "<note>" > scratchpad/ship18.log (gates :core:test ~15 min + :ime ~2 min, then pushes and writes the next-step card); (2) dispatch android.yml ref claude/bold-knuth-nn3ywo inputs full_build=true note=...; wait; mcp__github__get_release_by_tag swoop-v0.7.0-alpha18 shows the APK; bash tools/record-release.sh swoop-v0.7.0-alpha18 "<same note>"; tick TASKS R40; (3) R40-6 Browser, own branch claude/bold-knuth-nn3ywo only via git -C /home/user/Browser: merge origin/claude/elegant-darwin-h7dlay, bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard, port the host glue for the new engine (loadShape asset copy, loadDictionary/saveOwn, settings defaults) so the built-in keyboard decodes like Swoop, run -p /home/user/Browser :core:test :app:testDebugUnitTest, push; never release while alpha116 ships; automatic freshness = tools/sync-browser.sh + resume.sh/test_handoff warning when the Browser pin is behind Swoop

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
  55b0b1e ckpt 1248: gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, un
  ec2b546 ckpt 1247: R38-1..4 ticked; version bumped to code 28 / 0.7.0-alpha17; full suites green
  a540b09 ckpt 1245: R39 DONE: repo settings put Google's Maven Central copy before mavenCentral (
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
