# CHECKPOINT 879 — read me first, then TASKS.md

**Written:** 2026-11-20T08:08:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9df989c8-ryoul9` · **builds on:** `2ebcc99` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha8 RELEASED AND RECORDED (run 44698572358 green, Release swoop-v0.7.0-alpha8 has the signed APK, BUILDLOG line pushed); R28a + R28b done; Kestrel browser NOT re-synced (Browser repo not in this session)

## Do this next
nothing running: wait for the owner's alpha8 report (Diagnostics 'touch:' line plus 'learning changed the first guess'); do NOT re-dispatch swoop-v0.7.0-alpha8; optional later: sync core to Kestrel (bash <Browser>/tools/sync-keyboard-core.sh on its own branch via git -C) when the Browser repo is available

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  182d67f ckpt 873: swoop-v0.7.0-alpha8 (code 19) DISPATCHED ONCE: android.yml run 44698572358 on 
  c308df8 ckpt 872: gated swoop-v0.7.0-alpha8 (ime code 19) and pushed it: handoff self-test, unit
  e680385 ckpt 871: R28a+R28b DONE in code: learnedReach scaling (GlideParams), glide-start fixes,
  4fbb3a2 ckpt 862: touch-stream Diagnostics numbers added (InputStats.touchLine, KeysView.onTouch
  93919bb ckpt 856: R28b glide-start fixes in KeysView.kt + 4 new KeysViewTest tests, ime suite gr
  92daed7 ckpt 851: wrote owner's R28a (short->long again) and R28b (glide start sensitivity) into
  8b6946d ckpt 848: handoff saved (.handoff/handoff.md, CLAUDE.md and AGENTS.md learnings blocks r
  f27f15a ckpt 845: swoop-v0.7.0-alpha7 RELEASED AND RECORDED (run 44436640730 green; Release swoo
  4ed7b4c ckpt 840: swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on 
  8d33f7d ckpt 839: gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
