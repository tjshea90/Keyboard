# CHECKPOINT 505 — read me first, then TASKS.md

**Written:** 2026-11-18T03:41:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `01a8630` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.6.0-alpha2 (ime code 11) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/keyboard-caps-space-resize-glide-r72nmp, inputs {"full_build":"true","note":"0.6.0-alpha2: a glide always types the closest word (never 'no word fits'); capital after a full stop also for glided and strip words; a space you delete after a full stop stays deleted (example.com); docked keyboard resizes both ways from the bar's arrows, Full size restores both"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.6.0-alpha2), then:  bash tools/record-release.sh swoop-v0.6.0-alpha2 "0.6.0-alpha2: a glide always types the closest word (never 'no word fits'); capital after a full stop also for glided and strip words; a space you delete after a full stop stays deleted (example.com); docked keyboard resizes both ways from the bar's arrows, Full size restores both"
NEVER ship swoop-v0.6.0-alpha2 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  01a8630 ckpt 504: pre-ship: 0.6.0-alpha2: a glide always types the closest word (never 'no word 
  2ddf8c6 ckpt 490: R19c done+tested (docked two-axis resize via bar arrows, window-tall during dr
  60344cb ckpt 480: R19a/b/d core done+tested: always-answer relaxed decode pass (GlideDecoder.alw
  02723ef ckpt 469: wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)
  3839c16 ckpt 466: handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings bl
  c028463 ckpt 464: 0.6.0-alpha1 RELEASED AND RECORDED: run 44220063374 green (check, test-jvm inc
  37209dd ckpt 462: 0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ON
  6e52915 ckpt 461: gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit
  2b21971 ckpt 446: R18a done: tap any earlier word -> strip offers words with close glide pattern
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
```
