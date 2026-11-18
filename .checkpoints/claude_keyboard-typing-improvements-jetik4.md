# CHECKPOINT 461 — read me first, then TASKS.md

**Written:** 2026-11-18T02:00:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `ea8351d` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/keyboard-typing-improvements-jetik4, inputs {"full_build":"true","note":"0.6.0-alpha1: tap any earlier word for close-glide-pattern replacements; auto capitals (sentence starts, names, days) and learned capitals; Full size button + smooth resize; hide-letters is one solid blank panel; smarter learning (sentence starters, adaptive autocorrect, tapped-word corrections)"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.6.0-alpha1), then:  bash tools/record-release.sh swoop-v0.6.0-alpha1 "0.6.0-alpha1: tap any earlier word for close-glide-pattern replacements; auto capitals (sentence starts, names, days) and learned capitals; Full size button + smooth resize; hide-letters is one solid blank panel; smarter learning (sentence starters, adaptive autocorrect, tapped-word corrections)"
NEVER ship swoop-v0.6.0-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2b21971 ckpt 446: R18a done: tap any earlier word -> strip offers words with close glide pattern
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
