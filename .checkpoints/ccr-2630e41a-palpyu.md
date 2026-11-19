# CHECKPOINT 796 — read me first, then TASKS.md

**Written:** 2026-11-19T00:34:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `f8cb67b` (this checkpoint is the commit after it)

## Just done
wrote owner's R26a (optional research: next-word post-correction + dwell/speed cue, measured on real swipes) and R26b (sync latest Swoop into the Kestrel browser) into TASKS.md

## Do this next
R26b first (cheap, explicit): read docs/KESTREL-LINK.md + docs/KESTREL-PORT-R22.md, add_repo tjshea90/Browser (access push), clone to /home/user/Browser, find the previous session's branch (claude/keyboard-caps-space-resi...) and its sync script tools/sync-keyboard-core.sh; then R26a: extend core/src/test/.../RealSwipeEval.kt with a post-correction measurement (consecutive swipes of one sentence, candidates with max=5, rescoring term lastLm * ln(.6 p_uni(next)+.4 p_bi(cand,next)), margins 0.5/1/2) and a dwell/speed measurement using the t field

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  2b63d15 ckpt 792: R25 family CONFIRMED FIXED by the owner (2026-11-19): restarting the tablet ma
  b8af559 ckpt 784: swoop-v0.7.0-alpha6 RELEASED AND RECORDED (run 44426423457 green; Release swoo
  7d2c909 ckpt 780: swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on 
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
  21947b4 ckpt 772: R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPE
  c4b6a87 ckpt 768: R25c: owner's exact Test translation connection output received (group OK, bat
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
