# CHECKPOINT 1047 — read me first, then TASKS.md

**Written:** 2026-11-24T20:40:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-euler-w64u7s` · **builds on:** `f9936c8` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha12 (ime code 23) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/bold-euler-w64u7s, inputs {"full_build":"true","note":"R33: a word on the strip that is spelled like the glided word and far likelier (and for ave, for for fur) now REPLACES it from any box; a glided rare word that is only kept teaches the keyboard only lightly, and picking the right word takes the wrong one back, so ave stops feeding itself"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha12), then:  bash tools/record-release.sh swoop-v0.7.0-alpha12 "R33: a word on the strip that is spelled like the glided word and far likelier (and for ave, for for fur) now REPLACES it from any box; a glided rare word that is only kept teaches the keyboard only lightly, and picking the right word takes the wrong one back, so ave stops feeding itself"
NEVER ship swoop-v0.7.0-alpha12 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f9936c8 ckpt 1046: pre-ship: R33: a word on the strip that is spelled like the glided word and f
  a20ef6e ckpt 1036: R33 code: look-alike-fix rule (GlideSession.looksLikeFix + SpellingNear + eng
  00d6ec0 ckpt 1022: R33 written into TASKS.md (owner: ave still wins over and, tapping and adds i
  aed8fb7 ckpt 1019: R32 DONE: swoop-v0.7.0-alpha11 released (https://github.com/tjshea90/Keyboard
  ac950c8 ckpt 1015: alpha11 (ime code 22, swoop-v0.7.0-alpha11) gated and pushed; android.yml DIS
  10618d9 ckpt 1014: gated swoop-v0.7.0-alpha11 (ime code 22) and pushed it: handoff self-test, un
  9a86224 ckpt 1013: pre-ship: R32: punctuation swap (space before , . ! ? taken back, Backspace r
  ab28919 ckpt 1000: R32e CommonGuard (gap 5 nats, margin 3, measured on real swipes: +8 all / +11
  5fd5f44 ckpt 992: R32b digit long-press row (ImeLayout.digitHolds, KeysView.openDigitPopup, 7 vi
  90d27ce ckpt 978: R32a core: weak-space swap (typed/strip/glide space + , . ! ? takes the space,
```
