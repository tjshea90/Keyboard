# CHECKPOINT 593 — read me first, then TASKS.md

**Written:** 2026-11-18T05:17:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `1d8bf3e` (this checkpoint is the commit after it)

## Just done
wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build run 44242169394 STILL RUNNING (check, test-jvm green; build job in progress)

## Do this next
FIRST finish alpha1: get_workflow_run 44242169394 -> green -> get_release_by_tag swoop-v0.7.0-alpha1 shows APK -> bash tools/record-release.sh swoop-v0.7.0-alpha1 '<note in ckpt 587/588>' -> give owner link. THEN R21: InputController.type() CLOSES_WORD logic + leadForNewWord() NO_SPACE_AFTER: add opening-quote / inside-quotes detection and an autoTrailAt marker for glide/pick trailing spaces; tests in core (QuotesTest) + presenter; release as 0.7.0-alpha2 code 13

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  495b39a ckpt 579: R20d rank 1 done+tested (TouchField plane, persistence, export, docs). All fou
  c1cdf29 ckpt 546: R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide 
  2657834 ckpt 528: R20b built: after a glide the strip is [other guess][NEXT word][other guess] (
  bbb8240 ckpt 524: R20c done+tested (pronoun I everywhere: InputController.capitalizeI/fixLoneI/w
  c72bbee ckpt 514: wrote owner's second 2026-11-18 batch R20a-d into TASKS.md (his words); 0.6.0-
  b4f6a50 ckpt 511: swoop-v0.6.0-alpha2 RELEASED AND RECORDED: run 44232237543 green (check, test-
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
