# CHECKPOINT 601 — read me first, then TASKS.md

**Written:** 2026-11-18T05:21:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `3d1e84f` (this checkpoint is the commit after it)

## Just done
0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.0-alpha1 has swoop-0.7.0-alpha1.apk SHA-256 9c8caf26...). R21 (no spaces inside quotation marks) done+tested; version bumped to 0.7.0-alpha2 code 13, not yet shipped

## Do this next
run lint then ship: ./gradlew :ime:lintDebug; bash ship.sh '0.7.0-alpha2: no spaces between a quotation mark and the word inside it (a glided or picked word after an opening quote, and a closing quote takes the glide space away); words inside quotes and brackets now count as context for the next-word guess'; dispatch android.yml ONCE; wait green; record-release with the identical note

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M ime/build.gradle.kts

## Last ten checkpoints
```
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  495b39a ckpt 579: R20d rank 1 done+tested (TouchField plane, persistence, export, docs). All fou
  c1cdf29 ckpt 546: R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide 
  2657834 ckpt 528: R20b built: after a glide the strip is [other guess][NEXT word][other guess] (
  bbb8240 ckpt 524: R20c done+tested (pronoun I everywhere: InputController.capitalizeI/fixLoneI/w
  c72bbee ckpt 514: wrote owner's second 2026-11-18 batch R20a-d into TASKS.md (his words); 0.6.0-
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
