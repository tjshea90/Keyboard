# CHECKPOINT 110 — read me first, then TASKS.md

**Written:** 2026-11-17T00:33:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `a58e56e` (this checkpoint is the commit after it)

## Just done
KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core 233 + :ime 78 green, lint green

## Do this next
1) check CI run 43988177865 (full_build=true, ref ccr-c57dc9d3-149psl): mcp__github__actions_list list_workflow_jobs for run 43988177865; if green the 'swoop-test-apk' artifact exists and verify-apk --no-cert passed there. 2) KB-107 is blocked on the OWNER: Q-KB003 (create repo secret SWOOP_KEYSTORE_PASSPHRASE, then say 'secret added'). 3) While waiting: KB-200 TapDecoder start (plan 8.3.5) or KB-201 KGL2 - read plan sections 7/8 first - or KB-703 emulator lab in CI (adapt /home/user/Browser/.github/workflows/emu-lab.yml, read-only) which would prove KB-100/106 on a real Android and is the cheapest way to cut the biggest remaining risk (never run on a device).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2d5db20 ckpt 104: KB-104/105 done; selection-report bookkeeping hardened by a stronger fuzz (nev
  f20bd11 ckpt 76: KB-103 done: glide wired end to end (GlideSession in core; GlideHost, KeyboardP
  c621fd2 ckpt 61: KB-102 done: full-width KeysView + core ImeLayout (letters/symbols/URL/email/ke
  2403b56 ckpt 52: KB-101 done: EditorBridge/FieldInfo/InputController in :core (window snapshot, 
  dd9285f ckpt 33: KB-100 built and unit-proven (not ticked: needs a device/emulator): manifest + 
  81944a8 ckpt 27: KB-003 prepared except the owner's one secret: tools/signing-key.sh (create/unl
  6bc8c57 ckpt 23: KB-001 and KB-004 ticked (CI run 43981339423 green); verify-apk.sh fixed to par
  e5be133 ckpt 17: KB-001 skeleton + KB-002 port done locally: Gradle (:core :ime :tools :eval) bu
  e9b61be ckpt 7: KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
