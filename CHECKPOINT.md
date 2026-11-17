# CHECKPOINT 228 — read me first, then TASKS.md

**Written:** 2026-11-17T02:48:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `1371a73` (this checkpoint is the commit after it)

## Just done
emulator smoke extended: key geometry for the number row + neon bar, assertion that the keyboard window is only as tall as the keyboard (guards the tall-window bug), floating stage (long press the bar, tools/emu/float_check.py checks the IME window is the whole screen, the field behind still takes a tap); owner told NOT to install 0.2.0-alpha1

## Do this next
dispatch .github/workflows/emu-smoke.yml on ref ccr-c57dc9d3-149psl (api 29) with mcp__github__actions_run_trigger; read result via actions_list list_workflow_jobs / get_job_logs after it ends (gradle-cache is off for that job), artifact emu-smoke-29 holds screenshots; fix whatever it shows; then version 0.2.1-alpha1 (code 3), ship.sh, android.yml, record-release, tell owner.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
