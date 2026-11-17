# CHECKPOINT 135 — read me first, then TASKS.md

**Written:** 2026-11-17T01:12:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `df9bd25` (this checkpoint is the commit after it)

## Just done
release run 43992969251: build+verify (pinned dev-key certificate) PASSED on GitHub, but 'Publish the Release' failed after 1 s (reason not visible: only cleanup noise in the log tail); publish script runs fine locally with a stub gh; added an ERR trap annotation to publish-release.sh and turned the gradle cache off for the build job so the next log is quiet

## Do this next
re-dispatch android.yml full_build=true note 'First alpha: glide and tap typing, learning, setup and diagnostics screens, safe layout' (same version, NOT yet recorded, tag does not exist: get_release_by_tag swoop-v0.1.0-alpha1 returned 404); read the end of the build job log with get_job_logs tail_lines 40; fix the publish failure (suspects: token permission, gh create flags); then record-release + tell the owner the link

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/scripts/publish-release.sh
     M .github/workflows/android.yml

## Last ten checkpoints
```
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
```
