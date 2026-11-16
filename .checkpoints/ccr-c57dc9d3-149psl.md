# CHECKPOINT 27 — read me first, then TASKS.md

**Written:** 2026-11-16T23:44:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `8d0f26f` (this checkpoint is the commit after it)

## Just done
KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unlock/fingerprint, AES-256 PBKDF2 600k, refuses overwrite), make-signing-key.yml (dispatch-only, commits only the 2 signing files), android.yml signed path, verify-apk pin; proven locally by signing a real release APK with a throwaway key (never in repo); 9 new tests in test_handoff (74/74 green)

## Do this next
Owner action pending: Q-KB003 in TASKS.md (create repo secret SWOOP_KEYSTORE_PASSPHRASE then say 'secret added'); when told: dispatch make-signing-key.yml ONCE on branch ccr-c57dc9d3-149psl, git pull/merge the bot commit, bump nothing, dispatch android.yml full_build=true, verify the Release APK, then tick KB-003. MEANWHILE continue Phase 1: bash tools/claim.sh KB-100, then ime manifest service + method.xml (subtype en_US) + KeysService + SetupActivity + empty KeysView.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
