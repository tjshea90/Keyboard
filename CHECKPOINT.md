# CHECKPOINT 120 — read me first, then TASKS.md

**Written:** 2026-11-17T00:57:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `ce25756` (this checkpoint is the commit after it)

## Just done
emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.yml, KVM runner, API 29): registers/enables/chooses Swoop, shows it in the Setup try-it field, taps real keys (h,i), Backspace, a swipe glide, globe key, logcat crash check. make-signing-key.yml run 43991546290 FAILED at its first check: the repo secret SWOOP_KEYSTORE_PASSPHRASE is not set (or under 20 chars): owner action needed for the first release

## Do this next
1) dispatch emu-smoke.yml (mcp__github__actions_run_trigger, workflow_id emu-smoke.yml, ref ccr-c57dc9d3-149psl, inputs api=29), read the result with list_workflow_jobs, fix tools/emu/smoke.sh until green (artifact emu-smoke-29 holds logcat/ui/dumpsys/screenshots: use mcp__github__actions_get download_workflow_run_artifact only if needed). 2) ask the owner for the secret (TASKS Q-KB003; public repo => 5+ random words as passphrase); when they say 'secret added': dispatch make-signing-key.yml, git pull --no-rebase origin ccr-c57dc9d3-149psl (merge the bot commit), bash ship.sh 'first alpha: glide + tap keyboard', dispatch android.yml full_build=true note, wait, verify Release APK, bash tools/record-release.sh swoop-v0.1.0-alpha1 same-note, tell the owner the Release link.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
