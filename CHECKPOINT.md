# CHECKPOINT 657 — read me first, then TASKS.md

**Written:** 2026-11-18T06:55:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `9995f5d` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/keyboard-caps-space-resize-glide-r72nmp, inputs {"full_build":"true","note":"0.7.0-alpha3: hold Backspace to delete faster then whole words, clipboard text never learned, big number pad for number/phone/date fields, keyboard can be shorter, glide reads the=short vs there=long"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha3), then:  bash tools/record-release.sh swoop-v0.7.0-alpha3 "0.7.0-alpha3: hold Backspace to delete faster then whole words, clipboard text never learned, big number pad for number/phone/date fields, keyboard can be shorter, glide reads the=short vs there=long"
NEVER ship swoop-v0.7.0-alpha3 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9995f5d ckpt 656: pre-ship: 0.7.0-alpha3: hold Backspace to delete faster then whole words, clip
  74e5d4d ckpt 649: R23c gesture-length term evaluated (research/R23c-gesture-length.md, GlideLeng
  0db3f9a ckpt 639: R23a-e core+views done: held Backspace speeds up then deletes words (KeysView+
  649cee3 ckpt 623: wrote owner's third batch R23a-e into TASKS.md (backspace hold accelerates to 
  4d9bb2b ckpt 615: R22 plan written: docs/KESTREL-PORT-R22.md. Browser repo cloned shallow at /ho
  ca9737d ckpt 613: wrote owner's R22 'put all the updates into kestrel keyboard' into TASKS.md. s
  9844fd5 ckpt 610: swoop-v0.7.0-alpha2 RELEASED AND RECORDED: run 44244608905 green (check, test-
  f0361bc ckpt 604: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/ke
  74fd792 ckpt 603: gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
```
