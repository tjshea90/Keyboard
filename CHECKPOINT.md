# CHECKPOINT 76 — read me first, then TASKS.md

**Written:** 2026-11-17T00:12:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `b7492a1` (this checkpoint is the commit after it)

## Just done
KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardPresenter, glide KeysView with trail+strip+previews, KeyboardFlow export/import, Setup switch); :core 212 + :ime 67 tests green; R8 release APK 1.3 MB verified

## Do this next
KB-104: table tests for FieldInfos/Enter/caps (inputType x imeOptions matrix, including IME_FLAG_NO_ENTER_ACTION, noPersonalizedLearning) in ime/src/test/.../FieldInfosTest.kt and Enter rules; auto-capitalisation from EditorInfo cap flags + getCursorCapsMode belongs to KB-203. Then KB-105 Diagnostics screen (GlideStats.line() + memory + versions + Copy button), KB-106 safety (crash-loop guard -> fallback layout, globe/next-keyboard key, Setup button 'go back to my previous keyboard', docs already in docs/IF-THE-KEYBOARD-BREAKS.md), then KB-107 first Release (needs owner Q-KB003 secret first). Also pending: tick/verify KB-100 on a device (CI emulator lab KB-703 or tablet).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
