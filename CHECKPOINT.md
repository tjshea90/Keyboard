# CHECKPOINT 232 — read me first, then TASKS.md

**Written:** 2026-11-17T02:56:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `32d2b0d` (this checkpoint is the commit after it)

## Just done
emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when Swoop is not the chosen keyboard (the previous run tested the stock keyboard: its 'too tall' and 'floated' results are INVALID); IME window facts printed into the job log; first valid emulator result still to come

## Do this next
dispatch emu-smoke.yml again (api 29, ref ccr-c57dc9d3-149psl), read get_job_logs tail 160 for the OK/FAIL lines (window height of the DOCKED keyboard must be < 55% of the screen; floating must be >= 85%); if the docked window is tall add logcat diag of view heights in KeyboardRoot.onLayout (debug only) and fix; then 0.2.1-alpha1 release.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
