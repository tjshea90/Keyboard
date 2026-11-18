# CHECKPOINT 587 — read me first, then TASKS.md

**Written:** 2026-11-18T05:08:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `9ff3807` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/keyboard-caps-space-resize-glide-r72nmp, inputs {"full_build":"true","note":"0.7.0-alpha1: what you copied is the first box of the suggestion strip (tap to paste); after a glide the middle box predicts the next word and the side boxes are other guesses of the glide; a lone i and i'm, i'll, i've, i'd are always a capital I (also glided); the keyboard learns where your thumb lands on each part of the keyboard, not just one shift"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha1), then:  bash tools/record-release.sh swoop-v0.7.0-alpha1 "0.7.0-alpha1: what you copied is the first box of the suggestion strip (tap to paste); after a glide the middle box predicts the next word and the side boxes are other guesses of the glide; a lone i and i'm, i'll, i've, i'd are always a capital I (also glided); the keyboard learns where your thumb lands on each part of the keyboard, not just one shift"
NEVER ship swoop-v0.7.0-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  495b39a ckpt 579: R20d rank 1 done+tested (TouchField plane, persistence, export, docs). All fou
  c1cdf29 ckpt 546: R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide 
  2657834 ckpt 528: R20b built: after a glide the strip is [other guess][NEXT word][other guess] (
  bbb8240 ckpt 524: R20c done+tested (pronoun I everywhere: InputController.capitalizeI/fixLoneI/w
  c72bbee ckpt 514: wrote owner's second 2026-11-18 batch R20a-d into TASKS.md (his words); 0.6.0-
  b4f6a50 ckpt 511: swoop-v0.6.0-alpha2 RELEASED AND RECORDED: run 44232237543 green (check, test-
  cffd8a8 ckpt 506: DISPATCHED android.yml ONCE for swoop-v0.6.0-alpha2 (code 11) on ref claude/ke
  ae7bda6 ckpt 505: gated swoop-v0.6.0-alpha2 (ime code 11) and pushed it: handoff self-test, unit
```
