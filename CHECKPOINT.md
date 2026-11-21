# CHECKPOINT 898 — read me first, then TASKS.md

**Written:** 2026-11-21T19:59:42Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `330b5c9` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha9 (ime code 20) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/linkmeter-keyboard-launcher-fixes-fsk78t, inputs {"full_build":"true","note":"0.7.0-alpha9: a kept wrong look-alike (our for or, access for and) is no longer learned as if it were right, so it cannot feed itself; Diagnostics counts when learning changed the first guess; research note on how Gboard predicts and what was measured (a flat price per letter was tried and rejected)"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha9), then:  bash tools/record-release.sh swoop-v0.7.0-alpha9 "0.7.0-alpha9: a kept wrong look-alike (our for or, access for and) is no longer learned as if it were right, so it cannot feed itself; Diagnostics counts when learning changed the first guess; research note on how Gboard predicts and what was measured (a flat price per letter was tried and rejected)"
NEVER ship swoop-v0.7.0-alpha9 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cc287f0 ckpt 885: R29a: LearnGuard (kept look-alike learned lightly, no shape/habit when only le
  e612daa ckpt 879: swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoo
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
  4fbb3a2 ckpt 862: touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouch
  93919bb ckpt 856: R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite gr
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
