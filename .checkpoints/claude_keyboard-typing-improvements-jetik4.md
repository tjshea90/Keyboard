# CHECKPOINT 432 — read me first, then TASKS.md

**Written:** 2026-11-18T01:42:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `4fe1557` (this checkpoint is the commit after it)

## Just done
R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newline, . ! ? … through closing quotes/brackets, not after e.g./i.e./Mr./Dr./initials), caps re-evaluated after the cursor moves; new core CaseBook (days, months, countries, languages, ~800 names/brands + may/march after date words) + PersonalModel.addCase/caseForm learned from mid-sentence capitals (saved, merged, exported as 'k' lines), taken back with one Backspace; presenter properNoun autofix, glide commit/swap/strip forms; presenter applies caps settings BEFORE startInput (bug found). core 437+ and ime 207 tests green

## Do this next
R18a next: core GlideTyping.alternatives(word, prev2, prev, next, limit, done): ideal path of the word through GlideKeys (Fingerprint.ofWord style polyline), previewDecoder.decode -> path-close words, re-rank with next-word bigram, merge Suggester typo fixes; InputController.wordAround() (left+right of cursor) + replaceWordAround(); presenter edit mode (foreign cursor move or cursor inside a word -> strip alternatives; tap replaces the whole word keeping case, no trailing space, learns confusion only if similar); tests in core + KeyboardPresenterTest; then R18d (measure with PredictionYardstickTest), then release 0.6.0-alpha1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  docs/KEYBOARD-EXPORT.md

## Last ten checkpoints
```
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
