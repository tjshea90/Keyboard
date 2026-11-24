# CHECKPOINT 1014 — read me first, then TASKS.md

**Written:** 2026-11-24T08:15:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-euler-w64u7s` · **builds on:** `9a86224` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha11 (ime code 22) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/bold-euler-w64u7s, inputs {"full_build":"true","note":"R32: punctuation swap (space before , . ! ? taken back, Backspace restores), digit long-press fractions/superscripts, e-mail address suggestions in e-mail boxes, strip tap always replaces the glided word, common-word guard against rare first guesses"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha11), then:  bash tools/record-release.sh swoop-v0.7.0-alpha11 "R32: punctuation swap (space before , . ! ? taken back, Backspace restores), digit long-press fractions/superscripts, e-mail address suggestions in e-mail boxes, strip tap always replaces the glided word, common-word guard against rare first guesses"
NEVER ship swoop-v0.7.0-alpha11 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9a86224 ckpt 1013: pre-ship: R32: punctuation swap (space before , . ! ? taken back, Backspace r
  ab28919 ckpt 1000: R32e CommonGuard (gap 5 nats, margin 3, measured on real swipes: +8 all / +11
  5fd5f44 ckpt 992: R32b digit long-press row (ImeLayout.digitHolds, KeysView.openDigitPopup, 7 vi
  90d27ce ckpt 978: R32a core: weak-space swap (typed/strip/glide space + , . ! ? takes the space,
  6f8e218 ckpt 956: R32 written into TASKS.md (a-e) in the owner's words; session branch claude/bo
  991a420 ckpt 953: R30 DONE: alpha10 released (https://github.com/tjshea90/Keyboard/releases/tag/
  a40cab1 ckpt 951: R30 alpha10 shipped to GitHub: ship.sh green (handoff test, 290 ime + core tes
  e13f2a7 ckpt 950: gated swoop-v0.7.0-alpha10 (ime code 21) and pushed it: handoff self-test, uni
  cc1cf34 ckpt 947: R30 step 4b: ime tests 290 green (KeysViewTest timed-glide test, safe-mode eng
  7391048 ckpt 942: R30 step 4a: :core ShapeRescorer (hybrid score+0.7*logP, gate 3.5, letter-box 
```
