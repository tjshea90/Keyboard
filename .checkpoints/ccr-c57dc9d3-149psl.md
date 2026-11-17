# CHECKPOINT 130 — read me first, then TASKS.md

**Written:** 2026-11-17T01:06:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `a7df414` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-c57dc9d3-149psl, inputs {"full_build":"true","note":"First alpha: glide and tap typing, learning, setup and diagnostics screens, safe layout"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.1.0-alpha1), then:  bash tools/record-release.sh swoop-v0.1.0-alpha1 "First alpha: glide and tap typing, learning, setup and diagnostics screens, safe layout"
NEVER ship swoop-v0.1.0-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
```
