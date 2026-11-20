# CHECKPOINT 872 — read me first, then TASKS.md

**Written:** 2026-11-20T07:55:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9df989c8-ryoul9` · **builds on:** `e680385` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-9df989c8-ryoul9, inputs {"full_build":"true","note":"0.7.0-alpha8: a glide starts the moment the finger moves (2.5 dp, was 5); a glide that starts on the top edge of the top row (t, e, o, p...) no longer lands on the suggestion strip and dies; a second thumb that lands before the first glide ends now takes over instead of being dropped; on big keys (tablet) what the keyboard learned reaches less far, so a learned lift can no longer overrule a clean path (the -> there); Diagnostics has a touch line"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha8), then:  bash tools/record-release.sh swoop-v0.7.0-alpha8 "0.7.0-alpha8: a glide starts the moment the finger moves (2.5 dp, was 5); a glide that starts on the top edge of the top row (t, e, o, p...) no longer lands on the suggestion strip and dies; a second thumb that lands before the first glide ends now takes over instead of being dropped; on big keys (tablet) what the keyboard learned reaches less far, so a learned lift can no longer overrule a clean path (the -> there); Diagnostics has a touch line"
NEVER ship swoop-v0.7.0-alpha8 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
  4fbb3a2 ckpt 862: touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouch
  93919bb ckpt 856: R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite gr
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
  4ed7b4c ckpt 840: swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on 
  8d33f7d ckpt 839: gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit
  bc6e202 ckpt 838: pre-ship: 0.7.0-alpha7: the next glided word can fix the word before it when t
  ff86187 ckpt 802: R26a measured on slice A: post-correction +0.82 first-choice at margin 1 (43 m
```
