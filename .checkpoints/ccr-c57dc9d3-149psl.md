# CHECKPOINT 61 — read me first, then TASKS.md

**Written:** 2026-11-17T00:04:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `0851108` (this checkpoint is the commit after it)

## Just done
KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/keypad), 24 touch + 8 pixel tests, service wired (onCursor, action label, reset)

## Do this next
KB-103: read core/src/main/kotlin/app/kestrel/core/input/glide/GlideSupport.kt (GlideTyping API: decode/preview callbacks, learning calls, export/import) and the browser's glide glue in /home/user/Browser/app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt (glideDone/previewDone/pickCandidate/confirmGlide/prepareGlide, read-only absolute paths). Then: (1) KeysView: glide arming/trail/candidates strip (STRIP_H 30dp, chips, Listener.onGlide* seam); (2) KeysService owns GlideTyping with a worker Executor, loads ime assets glide/en.klx + personal file in filesDir, warm-up decode, calls controller.commitGlide/swapGlide, learning only when field.learningAllowed; (3) Export/Import via KeyboardFlow port (MediaStore Downloads). Tests: JVM for glue logic, Robolectric for the view.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
