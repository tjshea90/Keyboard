# CHECKPOINT 132 — read me first, then TASKS.md

**Written:** 2026-11-17T01:07:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `9a8e0da` (this checkpoint is the commit after it)

## Just done
emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub runner): install, ime enable, ime set, keyboard shows, real taps typed 'hi', Backspace, a swipe glided 'top ', no crash - all PASSED on a real Android; the 2 failures were script bugs (grep -q SIGPIPE under pipefail, a too-strict glide pattern) now fixed in tools/emu/smoke.sh. Release build run 43992969251 in progress (check gate incl. dev-key pin passed)

## Do this next
poll run 43992969251 (list_workflow_jobs) until build job success; get_release_by_tag swoop-v0.1.0-alpha1 shows the APK; bash tools/record-release.sh swoop-v0.1.0-alpha1 'First alpha: glide and tap typing, learning, setup and diagnostics screens, safe layout'; give the owner the Release link. Then re-run emu-smoke.yml (api 29) to get the fixed script green, tick KB-100 and KB-003 with the run ids, delete trigger trig_014mcDfmVfGAvHjcWEzmH1RY.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
