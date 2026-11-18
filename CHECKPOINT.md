# CHECKPOINT 415 — read me first, then TASKS.md

**Written:** 2026-11-18T01:25:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `7724deb` (this checkpoint is the commit after it)

## Just done
R18c done: smooth resize (view-transform preview while dragging corner/arrow/pinch, ONE real layout on lift, exact KeysView.mulFor inverse, no shadow/measure per event) + visible 'Full size' pill on the grip bar (docked squeezed or floating) that snaps back to full-width docked with one tap; KeyboardRootTest +5, KeysViewTest +1 green

## Do this next
R18b next: core InputController.autoCaps (empty TEXT field + sentence end through closing quotes/brackets, abbreviation exceptions e.g. i.e. mr. dr.) + presenter autoFixFor proper-noun capitalisation: new core CaseBook (days, months, countries, languages, common names asset + learned mid-sentence capitals in PersonalModel) used by Suggester.shape / GlideSession commitGlide; tests in AutoCapsTest + SuggesterTest; then R18a (GlideTyping.alternatives via ideal-path decode + InputController.wordAroundCursor/replaceWordAround + presenter edit mode), R18d

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
