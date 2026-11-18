# CHECKPOINT 615 — read me first, then TASKS.md

**Written:** 2026-11-18T06:09:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `52456b8` (this checkpoint is the commit after it)

## Just done
R22 plan written: docs/KESTREL-PORT-R22.md. Browser repo cloned shallow at /home/user/browser (lowercase), MY branch claude/keyboard-caps-space-resize-glide-r72nmp created there from ccr-587430ac-royssd tip 337a3f3 (another session is ACTIVE there, preparing alpha70: do not release Kestrel, do not push to its branch). Found: Prefs.glideHabit accepts only 2 floats (would silently stop saving after sync)

## Do this next
STEP A (Keyboard repo, vendored files): add MiniKeyboardModel.smartText (default false) with capital I, sentence capital (not at empty start), glided-word capital after a full stop, no spaces inside quotes, auto-space + deleted-space; tests in core/src/test/.../input (new MiniKeyboardSmartTest); commit+push. STEP B: bash /home/user/browser/tools/sync-keyboard-core.sh /home/user/Keyboard (commit pushed first), then Browser edits via git -C /home/user/browser: Prefs.glideHabit second pref for the plane, MiniNav smartText for page fields + next-word strip + importLearned touch, tests, run ./gradlew :core:test :app:testDebugUnitTest in /home/user/browser via -p? (never cd), push my Browser branch

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    ?? docs/KESTREL-PORT-R22.md

## Last ten checkpoints
```
  ca9737d ckpt 613: wrote owner's R22 'put all the updates into kestrel keyboard' into TASKS.md. s
  9844fd5 ckpt 610: swoop-v0.7.0-alpha2 RELEASED AND RECORDED: run 44244608905 green (check, test-
  f0361bc ckpt 604: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/ke
  74fd792 ckpt 603: gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
