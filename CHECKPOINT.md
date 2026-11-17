# CHECKPOINT 390 — read me first, then TASKS.md

**Written:** 2026-11-17T22:18:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `ae73cdc` (this checkpoint is the commit after it)

## Just done
Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build with R8, certificate verified on the APK, Release published: https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.5.0-alpha4, APK swoop-0.5.0-alpha4.apk 1.97 MB sha256 f129882187bd..., recorded in BUILDLOG.md). Kestrel alpha68 (gecko code 169) gated on my Browser branch ccr-5531c47b-hl1gzo (commit cef3338a: vendored keyboard engine + word list re-synced via tools/sync-keyboard-core.sh, KEYBOARD_PIN, docs/handoff/ALPHA68_RELEASE_NOTE.txt, SIM_FINDINGS_2026-11-17.md) and dispatched ONCE: Browser run 44188402392 (check green, test-js + test-jvm running, then build-gecko). The Browser android.yml had no queued or running run before (the other lane ccr-e89b9dfe-pl3lk5 shipped alpha67 at 18:27Z and is idle). Simulation runs in flight: typing quick with the NEW word list (44187463954), pages full speed (44186817159), pages hotspot (44187266683, queued behind it), film run that now PRESSES PLAY (just dispatched). dailysim.py: per-site/per-film hang recovery, address test restarts from the local page, film scene taps play + reads player lines.

## Do this next
1) WHEN Browser run 44188402392 is green (mcp__github__actions_list list_workflow_jobs, resource_id 44188402392, owner tjshea90 repo Browser): mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha68 must show the APK, then run bash /home/user/Browser/tools/record-release.sh kestrel-g-v0.3.0-alpha68 with the IDENTICAL note = the text of /home/user/Browser/docs/handoff/ALPHA68_RELEASE_NOTE.txt (absolute path, never cd into Browser); if the run FAILS read mcp__github__get_job_logs (failed_only) and fix on my own branch only, never ship a tag twice; 2) read the simulation artifacts (typing 44187463954 = the new list on the emulator; pages 44186817159; hotspot 44187266683; the new film run) and add the numbers to docs/handoff/SIM_FINDINGS_2026-11-17.md in the Browser repo (new file, my branch) and research/LEARNING_RESULTS.md; 3) final plain-words report to the owner with the two Release links on their own lines; end the reply with the amcheckpoint block (health red) because the active-memory hook asked for it.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
