# CHECKPOINT 17 — read me first, then TASKS.md

**Written:** 2026-11-16T23:40:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `03cfa89` (this checkpoint is the commit after it)

## Just done
KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) builds, lint green; android.yml + setup action + publish script written; core/input + glide ported with all tests (129 green); en.klx byte-identical; self-test 65/65

## Do this next
1) dispatch .github/workflows/android.yml via mcp__github__actions_run_trigger (owner tjshea90, repo Keyboard, workflow_id android.yml, ref ccr-c57dc9d3-149psl, inputs full_build=false) and wait for it green -> tick KB-001 with the run id. 2) KB-004 docs: docs/ARCHITECTURE.md (plan section 6), docs/IF-THE-KEYBOARD-BREAKS.md (plain words). 3) KB-003 prep: tools/signing-key.sh (create|unlock, openssl-encrypted PKCS12, one secret SWOOP_KEYSTORE_PASSPHRASE) + dispatch-only .github/workflows/make-signing-key.yml; needs the owner's yes on Q-KB003 in TASKS.md first. 4) then Phase 1: KB-100 manifest/method.xml/KeysService/Setup activity.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
