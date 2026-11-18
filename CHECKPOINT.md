# CHECKPOINT 753 — read me first, then TASKS.md

**Written:** 2026-11-18T23:09:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `8b45308` (this checkpoint is the commit after it)

## Just done
R25: measured table (Android 10 emulator, background app): Data Saver -> ECONNREFUSED, unrestricted list fixes it; battery saver / doze -> timeouts; standby rare works; NONE give EPERM/EACCES. So messages fixed: new Kind/ErrorCode REFUSED (usually Data Saver), BLOCKED (EPERM/EACCES) message no longer blames Data Saver; new probe step 'Internet access group' (/proc/self/status Groups has 3003, below API 33) with verdict 'restart; reinstall after export'; docs/TRANSLATE.md R25 table; net_probe scenarios now opt-in (emu-smoke.yml input scenarios); tests green (translate + engine + diagnostics)

## Do this next
dispatch emu-smoke.yml mode=net-probe (scenarios false, quick ~4 min) and read probe-emulator (29) log: the 'Internet access group' step MUST be OK on a healthy Android 10 (else the check false-alarms: remove it); API 36 must show nothing for it; then full gradle suite + ship 0.7.0-alpha5 code 16; ask owner for the exact FAIL line of Test translation connection

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/emu-smoke.yml
     M docs/TRANSLATE.md
     M tools/emu/net_probe.sh

## Last ten checkpoints
```
  837228c ckpt 746: R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered W
  31d7b81 ckpt 742: R25: first emulator run was INVALID (phase B reused old log lines: am start on
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
