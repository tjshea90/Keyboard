# CHECKPOINT 446 — read me first, then TASKS.md

**Written:** 2026-11-18T01:53:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `3132cb3` (this checkpoint is the commit after it)

## Just done
R18a done: tap any earlier word -> strip offers words with close glide patterns. core Alternatives (ideal-path decode + tap-typo fixes, ranked by Fingerprint shape distance + context incl. the NEXT word; yardstick 37/40 first, 38/40 in top 3), InputController.wordAround/contextAround/replaceWordAround, GlideTyping.alternatives, TypingLearner.replaced (slip -> unlearn old + correction), presenter edit mode (userMoved/inside-a-word; not in password/URL; keeps case; old word stays on strip). core 450 + ime 213 green; lint not runnable offline (CI)

## Do this next
R18d next: (a) PersonalModel sentence starters ('b word weight' lines; learned in TypingLearner.finishWord / GlideSession.confirmPrevious when InputController.startsSentence(head); Suggester.nextWords(prev==null) uses them) measured in SuggesterLearningTest (first-word hit rate before/after); (b) adaptive autocorrect margin from undo rate ('f made undone' line; Suggester AUTO_MARGIN + adjust); (c) GlideSession.onDecoded no-match -> retype watch so a typed word teaches the failed glide shape; tests; then docs/user text, bump ime/build.gradle.kts to 0.6.0-alpha1 code 10, bash ship.sh, dispatch android.yml once, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
