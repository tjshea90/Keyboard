# CHECKPOINT 649 — read me first, then TASKS.md

**Written:** 2026-11-18T06:45:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `0bc5aa3` (this checkpoint is the commit after it)

## Just done
R23c gesture-length term evaluated (research/R23c-gesture-length.md, GlideLengthEvalTest: yardstick +1..2 pts top1, long word +3..5, short word unchanged; defaults kept), 7 core tests adjusted for intended changes; R23d/R23b/R23a presenter+FieldInfos tests added; core+ime green

## Do this next
1) full :core:test once more (expect green) and :ime:testDebugUnitTest; 2) :ime:lintDebug; 3) docs: README bullets + docs/ARCHITECTURE.md (length term, Backspace schedule, pad, min height, clipboard provenance) + docs/FLOATING.md note; 4) tick R23a-e in TASKS.md via tools/claim.sh R23x --done; 5) bump ime/build.gradle.kts code 14 / 0.7.0-alpha3, bash ship.sh, dispatch android.yml once, record-release; 6) R22 per docs/KESTREL-PORT-R22.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0db3f9a ckpt 639: R23a-e core+views done: held Backspace speeds up then deletes words (KeysView+
  649cee3 ckpt 623: wrote owner's third batch R23a-e into TASKS.md (backspace hold accelerates to 
  4d9bb2b ckpt 615: R22 plan written: docs/KESTREL-PORT-R22.md. Browser repo cloned shallow at /ho
  ca9737d ckpt 613: wrote owner's R22 'put all the updates into kestrel keyboard' into TASKS.md. s
  9844fd5 ckpt 610: swoop-v0.7.0-alpha2 RELEASED AND RECORDED: run 44244608905 green (check, test-
  f0361bc ckpt 604: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/ke
  74fd792 ckpt 603: gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
