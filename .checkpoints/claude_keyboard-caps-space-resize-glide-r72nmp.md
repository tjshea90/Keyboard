# CHECKPOINT 623 — read me first, then TASKS.md

**Written:** 2026-11-18T06:15:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `9367619` (this checkpoint is the commit after it)

## Just done
wrote owner's third batch R23a-e into TASKS.md (backspace hold accelerates to whole words; never learn from clipboard; the/there gesture-length; number pad for number fields; smaller minimum keyboard height). R22 (Kestrel port) is IN PROGRESS: MiniKeyboardModel.smartText + insertWord + shared InputController companion rules written in core, MiniKeyboardSmartTest written; full :core:test was running

## Do this next
ORDER: (1) read the full-core result in scratchpad core_full.log (started before insertWord was added; rerun MiniKeyboardSmartTest), commit; (2) R23 core work: R23c decoder gesture-length term (GlideDecoder.careful + GlideParams + eval on prefix pairs), R23b paste barrier (InputController.paste + TypingLearner/context), R23a InputController.deleteWord + KeysView hold acceleration + Listener.onDeleteWord, R23d ImeLayout number pad + FieldInfos, R23e FloatingMath/Settings min height; tests; ship 0.7.0-alpha3 code 14; (3) THEN Kestrel R22: bash /home/user/browser/tools/sync-keyboard-core.sh /home/user/Keyboard (after commit+push), Prefs.glideHabit 24 floats, MiniNav smartText + strip + backspace/number pad parity, tests, push my Browser branch (never release Kestrel while ccr-587430ac-royssd ships alpha70)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  4d9bb2b ckpt 615: R22 plan written: docs/KESTREL-PORT-R22.md. Browser repo cloned shallow at /ho
  ca9737d ckpt 613: wrote owner's R22 'put all the updates into kestrel keyboard' into TASKS.md. s
  9844fd5 ckpt 610: swoop-v0.7.0-alpha2 RELEASED AND RECORDED: run 44244608905 green (check, test-
  f0361bc ckpt 604: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/ke
  74fd792 ckpt 603: gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
