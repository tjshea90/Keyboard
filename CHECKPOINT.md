# CHECKPOINT 802 — read me first, then TASKS.md

**Written:** 2026-11-19T00:43:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `a02cc70` (this checkpoint is the commit after it)

## Just done
R26a measured on slice A: post-correction +0.82 first-choice at margin 1 (43 mended, 5 spoiled of 4628), dwell cue no signal (AUC ~0.5); RealSwipe harness made self-contained (own MiniJson) so it compiles inside the Kestrel browser copy

## Do this next
re-sync Kestrel (bash /home/user/browser/tools/sync-keyboard-core.sh /home/user/Keyboard), rerun its :core:test in background (/tmp/claude-0/sweeps/BROWSER-CORE.log), then post-correction on slice B (REAL_POST=1 REAL_SWIPES_FILE=build/real-swipes-B.jsonl) to see if the gain holds

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0a3f557 ckpt 800: wrote owner's R27a (short words read as long words: the->there, and->access) a
  1440ffd ckpt 796: wrote owner's R26a (optional research: next-word post-correction + dwell/speed
  2b63d15 ckpt 792: R25 family CONFIRMED FIXED by the owner (2026-11-19): restarting the tablet ma
  b8af559 ckpt 784: swoop-v0.7.0-alpha6 RELEASED AND RECORDED (run 44426423457 green; Release swoo
  7d2c909 ckpt 780: swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on 
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
  21947b4 ckpt 772: R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPE
  c4b6a87 ckpt 768: R25c: owner's exact Test translation connection output received (group OK, bat
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
