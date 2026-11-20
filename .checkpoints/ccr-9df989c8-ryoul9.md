# CHECKPOINT 862 — read me first, then TASKS.md

**Written:** 2026-11-20T07:25:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9df989c8-ryoul9` · **builds on:** `4425544` (this checkpoint is the commit after it)

## Just done
touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouchNote, presenter, Diagnostics line) + tests written but NOT YET RUN; learned-state lab LearnedShortLongLabTest running (ablations on tablet geometry); first lab result: on tablet-size keys the LEARNED decoder is 1-2 points WORSE than the plain one for a clean finger

## Do this next
1) when the lab finishes (core/build test output, LAB lines, env LAB_GEOS=tablet LAB_VARIANTS=full,nopat,noconf,nofield,nothumb,nopersonal,pure REAL_TABLET=1) pick which learned part to damp on big keys; 2) run ./gradlew :ime:testDebugUnitTest (no --offline: robolectric jars need network) for the new KeysViewTest/DiagnosticsTest tests; 3) write research/R28-short-vs-long-and-glide-start.md; 4) release 0.7.0-alpha8 code 19 via ship.sh

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  93919bb ckpt 856: R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite gr
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
  4ed7b4c ckpt 840: swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on 
  8d33f7d ckpt 839: gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit
  bc6e202 ckpt 838: pre-ship: 0.7.0-alpha7: the next glided word can fix the word before it when t
  ff86187 ckpt 802: R26a measured on slice A: post-correction +0.82 first-choice at margin 1 (43 m
  0a3f557 ckpt 800: wrote owner's R27a (short words read as long words: the->there, and->access) a
  1440ffd ckpt 796: wrote owner's R26a (optional research: next-word post-correction + dwell/speed
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
