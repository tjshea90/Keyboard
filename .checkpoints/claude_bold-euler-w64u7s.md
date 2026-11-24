# CHECKPOINT 1036 — read me first, then TASKS.md

**Written:** 2026-11-24T20:07:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-euler-w64u7s` · **builds on:** `3ccc5e0` (this checkpoint is the commit after it)

## Just done
R33 code: look-alike-fix rule (GlideSession.looksLikeFix + SpellingNear + engine.contextLn; strip shows such a next word among the replacements; lost-glide rescue extended), kept rare glides learn lightly (keepTrust), recent boost scales with weight, correction unlearns the wrong word (2 uses), rare-word boost capped at 3 uses (PersonalModel.RARE_*); Diagnostics counters; tests KeptRareWordTest + R33 presenter tests + PersonalModelTest; FULL SUITES NOT YET RUN

## Do this next
run ./gradlew :core:test :ime:testDebugUnitTest in ONE background run (no overlap), fix red; run :ime:lintDebug; prove the R33 presenter tests red by switching SpellingNear rule off once; run RareGuardEvalTest on slice A (REAL_SWIPES=1 REAL_RARE=1, file core/build/real-swipes-A.jsonl) to check no regression from the personal-model change (it has no personal model: should be unchanged); then bump to code 23 / 0.7.0-alpha12, ship.sh, dispatch android.yml once, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  00d6ec0 ckpt 1022: R33 written into TASKS.md (owner: ave still wins over and, tapping and adds i
  aed8fb7 ckpt 1019: R32 DONE: swoop-v0.7.0-alpha11 released (https://github.com/tjshea90/Keyboard
  ac950c8 ckpt 1015: alpha11 (ime code 22, swoop-v0.7.0-alpha11) gated and pushed; android.yml DIS
  10618d9 ckpt 1014: gated swoop-v0.7.0-alpha11 (ime code 22) and pushed it: handoff self-test, un
  9a86224 ckpt 1013: pre-ship: R32: punctuation swap (space before , . ! ? taken back, Backspace r
  ab28919 ckpt 1000: R32e CommonGuard (gap 5 nats, margin 3, measured on real swipes: +8 all / +11
  5fd5f44 ckpt 992: R32b digit long-press row (ImeLayout.digitHolds, KeysView.openDigitPopup, 7 vi
  90d27ce ckpt 978: R32a core: weak-space swap (typed/strip/glide space + , . ! ? takes the space,
  6f8e218 ckpt 956: R32 written into TASKS.md (a-e) in the owner's words; session branch claude/bo
  991a420 ckpt 953: R30 DONE: alpha10 released (https://github.com/tjshea90/Keyboard/releases/tag/
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
