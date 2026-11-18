# CHECKPOINT 462 — read me first, then TASKS.md

**Written:** 2026-11-18T02:00:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `6e52915` (this checkpoint is the commit after it)

## Just done
0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ONCE on branch claude/keyboard-typing-improvements-jetik4 (do NOT dispatch again). R18a-e done and ticked

## Do this next
WAIT for the android.yml run (find id via mcp__github__actions_list list_workflow_runs android.yml, newest on this branch): when all jobs green and 'git ls-remote --tags origin swoop-v0.6.0-alpha1' shows the tag and mcp__github__get_release_by_tag has the APK -> bash tools/record-release.sh swoop-v0.6.0-alpha1 with the IDENTICAL note from ship.sh output (BUILDLOG/CHECKPOINT); then tell the owner the Release link. If the run is red: read job logs (get_job_logs, gradle-cache false), fix, bump to alpha2 (code 11), ship.sh again. Browser repo /home/user/Browser not present this session: Kestrel's pinned engine copy NOT re-synced (it types through Swoop anyway)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6e52915 ckpt 461: gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit
  2b21971 ckpt 446: R18a done: tap any earlier word -> strip offers words with close glide pattern
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
```
