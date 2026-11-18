# CHECKPOINT 490 — read me first, then TASKS.md

**Written:** 2026-11-18T03:34:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `978c082` (this checkpoint is the commit after it)

## Just done
R19c done+tested (docked two-axis resize via bar arrows, window-tall during drag, dockH setting, Full size pill/double-tap restore height, overlay stale-height fix in KeyboardRoot.onMeasure): KeyboardRootTest 36 + FloatingMathTest pass. NB: :ime tests need ONLINE gradle (no --offline) in this container

## Do this next
add KeyboardPresenterTest cases for R19a/b/d end-to-end (glide after '.', deleted space stays, glide never says no word fits); docs FLOATING.md/README/SetupActivity note+label; then full :core:test :ime:testDebugUnitTest :ime:lintDebug; bump ime/build.gradle.kts to 0.6.0-alpha2 code 11; bash ship.sh; dispatch android.yml

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  60344cb ckpt 480: R19a/b/d core done+tested: always-answer relaxed decode pass (GlideDecoder.alw
  02723ef ckpt 469: wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)
  3839c16 ckpt 466: handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings bl
  c028463 ckpt 464: 0.6.0-alpha1 RELEASED AND RECORDED: run 44220063374 green (check, test-jvm inc
  37209dd ckpt 462: 0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ON
  6e52915 ckpt 461: gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit
  2b21971 ckpt 446: R18a done: tap any earlier word -> strip offers words with close glide pattern
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
