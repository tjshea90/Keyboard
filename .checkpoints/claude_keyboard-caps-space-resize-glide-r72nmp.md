# CHECKPOINT 603 — read me first, then TASKS.md

**Written:** 2026-11-18T05:29:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `a2813e6` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/keyboard-caps-space-resize-glide-r72nmp, inputs {"full_build":"true","note":"0.7.0-alpha2: no spaces between a quotation mark and the word inside it (a glided or picked word after an opening quote, and a closing quote takes the glide space away); words inside quotes and brackets now count as context for the next-word guess"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha2), then:  bash tools/record-release.sh swoop-v0.7.0-alpha2 "0.7.0-alpha2: no spaces between a quotation mark and the word inside it (a glided or picked word after an opening quote, and a closing quote takes the glide space away); words inside quotes and brackets now count as context for the next-word guess"
NEVER ship swoop-v0.7.0-alpha2 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  495b39a ckpt 579: R20d rank 1 done+tested (TouchField plane, persistence, export, docs). All fou
  c1cdf29 ckpt 546: R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide 
  2657834 ckpt 528: R20b built: after a glide the strip is [other guess][NEXT word][other guess] (
  bbb8240 ckpt 524: R20c done+tested (pronoun I everywhere: InputController.capitalizeI/fixLoneI/w
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
