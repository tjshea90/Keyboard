# CHECKPOINT 104 — read me first, then TASKS.md

**Written:** 2026-11-17T00:30:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `75580c6` (this checkpoint is the commit after it)

## Just done
KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (never roll the believed cursor back on a late report; one expected report per arrow key; ending a tapped word is an expected report; oldest-first matching; ring 160): :core 225 + :ime 73 tests green, lint green; known rare lag-race recorded as KB-101b

## Do this next
1) dispatch android.yml full_build=true (mcp__github__actions_run_trigger, owner tjshea90 repo Keyboard, ref ccr-c57dc9d3-149psl, inputs full_build true note 'CI chain check, debug APK artifact only') and wait: expect debug-signed test APK artifact, no Release (signing/CERT_SHA256 is UNSET). 2) KB-106 safety: crash-loop guard (counter file, 3 crashes in 60 s -> minimal fallback layout without decoders), globe/next-keyboard key always visible (InputMethodService.switchToNextInputMethod / InputMethodManager.showInputMethodPicker), Setup button 'go back to my previous keyboard' (switchToPreviousInputMethod), tests. 3) KB-107 first Release needs the OWNER first: Q-KB003 in TASKS.md (secret SWOOP_KEYSTORE_PASSPHRASE) - then dispatch make-signing-key.yml once, merge its commit, bump nothing, ship.sh, dispatch full_build, install on tablet+phone, Android 16 inset check, paste Diagnostics. Heartbeat question Q-HEARTBEAT also open.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(27 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
