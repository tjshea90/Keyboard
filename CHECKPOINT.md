# CHECKPOINT 885 — read me first, then TASKS.md

**Written:** 2026-11-21T19:03:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `c1b7aff` (this checkpoint is the commit after it)

## Just done
R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only learning put a rarer word before a 20x commoner one) + shadow gap + stat + KeptErrorsLabTest (REAL_TABLET=1); LearnGuardTest green

## Do this next
run lab: REAL_TABLET=1 ./gradlew --offline :core:test --tests '*KeptErrorsLabTest' -i | grep KEPT ; then tune GAP_NATS/SELF_WEIGHT; ime tests+lint; write research/GBOARD_GLIDE_CONTEXT.md + R29 section; tick R29 in TASKS

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
  4fbb3a2 ckpt 862: touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouch
  93919bb ckpt 856: R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite gr
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
  4ed7b4c ckpt 840: swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
