# CHECKPOINT 118 — read me first, then TASKS.md

**Written:** 2026-11-17T00:55:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `b002505` (this checkpoint is the commit after it)

## Just done
owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS): docs/hooks text updated for a public repo; signing key file now uses PBKDF2 with 10 million iterations (public repo => offline guessing must be slow; count stored in signing/KDF_ITER); self-test 75 green; CI run 43988177865 (full build, debug APK artifact) was green

## Do this next
dispatch .github/workflows/make-signing-key.yml on ref ccr-c57dc9d3-149psl (mcp__github__actions_run_trigger run_workflow): if it fails 'secret not set' the owner has not added SWOOP_KEYSTORE_PASSPHRASE: ask them (plain steps in TASKS Q-KB003). If green: git pull the bot commit (signing/swoop.jks.enc, signing/KDF_ITER, signing/CERT_SHA256), run bash ship.sh 'first alpha', dispatch android.yml full_build=true, wait, verify Release APK, bash tools/record-release.sh swoop-v0.1.0-alpha1 same-note, give the owner the link. In parallel write .github/workflows/emu-smoke.yml (reactivecircus/android-emulator-runner, API 29 x86_64, ime list/enable/set, show keyboard, no FATAL in logcat).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
