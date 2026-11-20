# CHECKPOINT 873 — read me first, then TASKS.md

**Written:** 2026-11-20T07:56:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9df989c8-ryoul9` · **builds on:** `c308df8` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on ccr-9df989c8-ryoul9 (queued 07:55Z)

## Do this next
do NOT dispatch again. Wait for run 44698572358 green (mcp__github__actions_get get_workflow_run), then mcp__github__get_release_by_tag swoop-v0.7.0-alpha8 shows the APK, then bash tools/record-release.sh swoop-v0.7.0-alpha8 with the note from the ship.sh output (same text as the dispatch inputs), then tell the owner the Release link. If red: read ::error:: annotations, fix, bump to alpha9 (code 20)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
  4fbb3a2 ckpt 862: touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouch
  93919bb ckpt 856: R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite gr
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
  4ed7b4c ckpt 840: swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on 
  8d33f7d ckpt 839: gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit
  bc6e202 ckpt 838: pre-ship: 0.7.0-alpha7: the next glided word can fix the word before it when t
```
