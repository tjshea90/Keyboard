# CHECKPOINT 604 — read me first, then TASKS.md

**Written:** 2026-11-18T05:29:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `74fd792` (this checkpoint is the commit after it)

## Just done
DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/keyboard-caps-space-resize-glide-r72nmp at 05:29Z. Do NOT dispatch again.

## Do this next
WAIT for the newest android.yml run on this branch (mcp__github__actions_list list_workflow_runs, branch filter) to finish green (about 10 min: check, test-jvm, build); confirm mcp__github__get_release_by_tag swoop-v0.7.0-alpha2 shows swoop-0.7.0-alpha2.apk (retry once on a 502); then bash tools/record-release.sh swoop-v0.7.0-alpha2 "0.7.0-alpha2: no spaces between a quotation mark and the word inside it (a glided or picked word after an opening quote, and a closing quote takes the glide space away); words inside quotes and brackets now count as context for the next-word guess" and give the owner the Release link. If it FAILED: read the job logs, fix, bump to 0.7.0-alpha3 code 14, ship again.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  74fd792 ckpt 603: gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  495b39a ckpt 579: R20d rank 1 done+tested (TouchField plane, persistence, export, docs). All fou
  c1cdf29 ckpt 546: R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide 
  2657834 ckpt 528: R20b built: after a glide the strip is [other guess][NEXT word][other guess] (
```
