# CHECKPOINT 1022 — read me first, then TASKS.md

**Written:** 2026-11-24T19:56:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-euler-w64u7s` · **builds on:** `db063d6` (this checkpoint is the commit after it)

## Just done
R33 written into TASKS.md (owner: ave still wins over and, tapping and adds it behind ave); alpha11 fix R32d only covered and-in-decoder-five

## Do this next
write red presenter tests for: and NOT in the decoder's five guesses but the predicted next word after ave (middle box) and the lost-glide variant; then GlideSession look-alike rule (similar + glided word much rarer) in KeyboardPresenter.onCandidate/renderGlideStrip; then simulate a biased tablet thumb (core/src/test GlideEval) to see why ave beats and

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  aed8fb7 ckpt 1019: R32 DONE: swoop-v0.7.0-alpha11 released (https://github.com/tjshea90/Keyboard
  ac950c8 ckpt 1015: alpha11 (ime code 22, swoop-v0.7.0-alpha11) gated and pushed; android.yml DIS
  10618d9 ckpt 1014: gated swoop-v0.7.0-alpha11 (ime code 22) and pushed it: handoff self-test, un
  9a86224 ckpt 1013: pre-ship: R32: punctuation swap (space before , . ! ? taken back, Backspace r
  ab28919 ckpt 1000: R32e CommonGuard (gap 5 nats, margin 3, measured on real swipes: +8 all / +11
  5fd5f44 ckpt 992: R32b digit long-press row (ImeLayout.digitHolds, KeysView.openDigitPopup, 7 vi
  90d27ce ckpt 978: R32a core: weak-space swap (typed/strip/glide space + , . ! ? takes the space,
  6f8e218 ckpt 956: R32 written into TASKS.md (a-e) in the owner's words; session branch claude/bo
  991a420 ckpt 953: R30 DONE: alpha10 released (https://github.com/tjshea90/Keyboard/releases/tag/
  a40cab1 ckpt 951: R30 alpha10 shipped to GitHub: ship.sh green (handoff test, 290 ime + core tes
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
