# CHECKPOINT 1086 — read me first, then TASKS.md

**Written:** 2026-11-26T20:32:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `e2fabad` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha13 (ime code 24) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/awesome-bell-gmcf0i, inputs {"full_build":"true","note":"R34: translating switches itself off when you leave the box (another app, tab or box, or the keyboard hides); a glide starts at once and no stray first letter is typed when a second finger or a resting thumb touches while you start a glide (a flick with no move events is a glide too); e-mail boxes are recognised by their hint, label or name, addresses you type are learned in every ordinary box and when the focus moves on (login forms), new screen My e-mail addresses, and after the @ the strip offers outlook.com, gmail.com, proton.me and the other popular domains"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha13), then:  bash tools/record-release.sh swoop-v0.7.0-alpha13 "R34: translating switches itself off when you leave the box (another app, tab or box, or the keyboard hides); a glide starts at once and no stray first letter is typed when a second finger or a resting thumb touches while you start a glide (a flick with no move events is a glide too); e-mail boxes are recognised by their hint, label or name, addresses you type are learned in every ordinary box and when the focus moves on (login forms), new screen My e-mail addresses, and after the @ the strip offers outlook.com, gmail.com, proton.me and the other popular domains"
NEVER ship swoop-v0.7.0-alpha13 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e2fabad ckpt 1085: R34b R34c DONE (e-mail boxes recognised by hint/label, learning in every ordi
  002111e ckpt 1069: R34d DONE in code+tests: KeysView no longer types the first letter when a sec
  d5ce1d3 ckpt 1063: R34e DONE in code+tests: translation switches itself off on a new input sessi
  1bc8768 ckpt 1057: R34 written into TASKS.md in the owner's words (5 items: glide engine, e-mail
  4c2c91a ckpt 1054: R33 DONE: swoop-v0.7.0-alpha12 released (https://github.com/tjshea90/Keyboard
  3a86baf ckpt 1048: alpha12 (ime code 23, swoop-v0.7.0-alpha12) gated and pushed; android.yml DIS
  441d4b0 ckpt 1047: gated swoop-v0.7.0-alpha12 (ime code 23) and pushed it: handoff self-test, un
  f9936c8 ckpt 1046: pre-ship: R33: a word on the strip that is spelled like the glided word and f
  a20ef6e ckpt 1036: R33 code: look-alike-fix rule (GlideSession.looksLikeFix + SpellingNear + eng
  00d6ec0 ckpt 1022: R33 written into TASKS.md (owner: ave still wins over and, tapping and adds i
```
