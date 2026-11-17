# CHECKPOINT 134 — read me first, then TASKS.md

**Written:** 2026-11-17T01:10:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `e7d908d` (this checkpoint is the commit after it)

## Just done
emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifact; waiting for release run 43992969251 build job

## Do this next
poll get_release_by_tag swoop-v0.1.0-alpha1 until it exists (404 = not yet); confirm run 43992969251 success; bash tools/record-release.sh swoop-v0.1.0-alpha1 'First alpha: glide and tap typing, learning, setup and diagnostics screens, safe layout'; tell the owner the link. Then rerun emu-smoke.yml and read the end of its log (run 43993097582 failed on something other than the 2 known script bugs? check)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
