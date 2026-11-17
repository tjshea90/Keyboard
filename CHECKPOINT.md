# CHECKPOINT 129 — read me first, then TASKS.md

**Written:** 2026-11-17T01:05:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `806e7f3` (this checkpoint is the commit after it)

## Just done
owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-dev.p12 (pin ded33b33...), no secrets/passphrase; Gradle release signs with it by default; android.yml rewritten (pin gate, build, verify, publish); passphrase machinery removed; docs/README updated; local release APK built and verify-apk passed (pinned cert); self-test 67 green

## Do this next
bash ship.sh 'first alpha: glide + tap keyboard' (gates + unit suites + pushes), then dispatch android.yml full_build=true note 'first alpha...' on ref ccr-c57dc9d3-149psl, wait for jobs success (list_workflow_jobs), get_release_by_tag swoop-v0.1.0-alpha1 shows the APK, then bash tools/record-release.sh swoop-v0.1.0-alpha1 'SAME note', tell the owner the Release link on its own line. Afterwards: fix the failed emulator smoke run 43991821449 (get_job_logs tail_lines ~400 for job 133734073305; make smoke.sh tee to emu-out/smoke.log).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
