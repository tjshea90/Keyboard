# CHECKPOINT 839 — read me first, then TASKS.md

**Written:** 2026-11-19T01:24:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `bc6e202` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-2630e41a-palpyu, inputs {"full_build":"true","note":"0.7.0-alpha7: the next glided word can fix the word before it when that was a look-alike (form/from; Settings: Fix the word before); the language keeps more say on clean glides; Diagnostics shows whether your learning changes the first guess and Settings has Forget where my thumb lands; glide settings tuned on real swipes"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha7), then:  bash tools/record-release.sh swoop-v0.7.0-alpha7 "0.7.0-alpha7: the next glided word can fix the word before it when that was a look-alike (form/from; Settings: Fix the word before); the language keeps more say on clean glides; Diagnostics shows whether your learning changes the first guess and Settings has Forget where my thumb lands; glide settings tuned on real swipes"
NEVER ship swoop-v0.7.0-alpha7 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bc6e202 ckpt 838: pre-ship: 0.7.0-alpha7: the next glided word can fix the word before it when t
  ff86187 ckpt 802: R26a measured on slice A: post-correction +0.82 first-choice at margin 1 (43 m
  0a3f557 ckpt 800: wrote owner's R27a (short words read as long words: the->there, and->access) a
  1440ffd ckpt 796: wrote owner's R26a (optional research: next-word post-correction + dwell/speed
  2b63d15 ckpt 792: R25 family CONFIRMED FIXED by the owner (2026-11-19): restarting the tablet ma
  b8af559 ckpt 784: swoop-v0.7.0-alpha6 RELEASED AND RECORDED (run 44426423457 green; Release swoo
  7d2c909 ckpt 780: swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on 
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
  21947b4 ckpt 772: R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPE
```
