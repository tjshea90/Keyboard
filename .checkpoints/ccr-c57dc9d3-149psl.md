# CHECKPOINT 33 — read me first, then TASKS.md

**Written:** 2026-11-16T23:48:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `a80cbe6` (this checkpoint is the commit after it)

## Just done
KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + method.xml + KeysService + KeysView skeleton + SetupActivity + adaptive icon; 8 Robolectric tests (API 29) green; lint green

## Do this next
KB-101: claim it (bash tools/claim.sh KB-101); write core/src/main/kotlin/app/kestrel/core/input/editor/{EditorBridge,FieldInfo,InputController}.kt (design in plan 6.4: window snapshot, composing for tap words, lastGlide, pending-expected-selection queue, verify-before-replace) and core/src/test/.../editor/{FakeEditor,InputControllerTest,InputControllerFuzzTest}.kt (>=40 tests incl. randomized fuzz with delayed selection updates and external edits). Then wire KeysService to InputController (:ime EditorBridge over InputConnection).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
