# CHECKPOINT 746 — read me first, then TASKS.md

**Written:** 2026-11-18T22:56:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `98dd1ef` (this checkpoint is the commit after it)

## Just done
R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered Wi-Fi + app in background -> every connect fails ECONNREFUSED (NOT EPERM); app on Unrestricted data list -> works. So the owner's EPERM/EACCES is a DIFFERENT limit (DROP-type: battery saver / doze / app standby / firewall or VPN app / missing permission); my 'most likely Data Saver' text for EPERM is WRONG and must change. net_probe.sh now runs scenarios datasaver, datasaver-unrestricted, batterysaver, doze, standby-rare, standby-restricted and prints which errno each gives

## Do this next
dispatch emu-smoke.yml mode=net-probe, read probe-emulator (29) AND (36) job logs for 'R25 summary' / 'SCENARIO' lines; then (1) HttpsTransport.classify ECONNREFUSED -> Kind.REFUSED + ErrorCode.REFUSED + message 'refused: usually Data Saver: Unrestricted data' (evidence: scenario datasaver), (2) rewrite the BLOCKED (EPERM/EACCES) message + ConnectionProbe verdict per which scenario gave EPERM, (3) tests, ship 0.7.0-alpha5 code 16; ask the owner for the exact FAIL line of Test translation connection (which stage + errno)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  31d7b81 ckpt 742: R25: first emulator run was INVALID (phase B reused old log lines: am start on
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
