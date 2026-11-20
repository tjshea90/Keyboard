# CHECKPOINT 856 — read me first, then TASKS.md

**Written:** 2026-11-20T07:08:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9df989c8-ryoul9` · **builds on:** `b6ce8f2` (this checkpoint is the commit after it)

## Just done
R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite green (start 2.5 dp, strip/keys boundary, second-thumb takeover, preview gate)

## Do this next
R28a: add Diagnostics counters (glide-start stats: second-thumb takeovers, first-move points, glides per... numbers only) in core GlideSupport stats + DiagnosticsActivity; then decide the learned-lift guard (GlideDecoder.applyPatterns/applyCorrections: no lift for a word 2+ letters longer unless its perLetter fit is within 0.25 of the first); write research/R28-short-vs-long-and-glide-start.md (flat-keys study FlatKeysStudyTest done: simulator finds NO short->long on realistic tablet geometry); then bump version 0.7.0-alpha8 (code 19) in ime/build.gradle.kts and bash ship.sh

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
  4ed7b4c ckpt 840: swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on 
  8d33f7d ckpt 839: gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit
  bc6e202 ckpt 838: pre-ship: 0.7.0-alpha7: the next glided word can fix the word before it when t
  ff86187 ckpt 802: R26a measured on slice A: post-correction +0.82 first-choice at margin 1 (43 m
  0a3f557 ckpt 800: wrote owner's R27a (short words read as long words: the->there, and->access) a
  1440ffd ckpt 796: wrote owner's R26a (optional research: next-word post-correction + dwell/speed
  2b63d15 ckpt 792: R25 family CONFIRMED FIXED by the owner (2026-11-19): restarting the tablet ma
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
