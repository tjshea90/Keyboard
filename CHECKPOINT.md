# CHECKPOINT 139 — read me first, then TASKS.md

**Written:** 2026-11-17T01:17:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `28a1c31` (this checkpoint is the commit after it)

## Just done
FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160), signed with the public dev key (pin ded33b33...), verify-apk passed, recorded in BUILDLOG.md; emulator smoke green on a real Android 10 emulator (run 43993756467); KB-003 and KB-100 ticked; OR2 done

## Do this next
KB-107 / gate G1 needs the OWNER: install the Release APK on tablet and phone, enable+choose Swoop, type, then paste Diagnostics (Settings screen > Diagnostics > Copy) for decode timing and memory; Android 16 phone inset check; then KB-200 TapDecoder / KB-201 KGL2 (plan sections 7-8), or run emu-smoke with api=36 for an Android 16 emulator check (workflow input api). Open owner questions: Q-HEARTBEAT (2-hourly wake), Q-LINKMETER at KB-600.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M CLAUDE.md
     M TASKS.md

## Last ten checkpoints
```
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
  33268f4 ckpt 110: KB-106 done: crash-loop safe mode, globe key everywhere, go-back button; :core
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
