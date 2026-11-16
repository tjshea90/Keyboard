# CHECKPOINT 23 — read me first, then TASKS.md

**Written:** 2026-11-16T23:41:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `d18b4eb` (this checkpoint is the commit after it)

## Just done
KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to parse minSdk/allowBackup and verified on the real debug APK; ARCHITECTURE + IF-THE-KEYBOARD-BREAKS written. Phase 0 is done except KB-003 (needs the owner).

## Do this next
KB-003 prep (no owner needed yet): write tools/signing-key.sh (create|unlock) and test the round trip locally with a throwaway passphrase in the scratchpad; write dispatch-only .github/workflows/make-signing-key.yml. Then Phase 1: claim KB-100 (bash tools/claim.sh KB-100): ime/src/main/AndroidManifest.xml service + res/xml/method.xml (subtype en_US) + KeysService + SetupActivity + empty KeysView.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
