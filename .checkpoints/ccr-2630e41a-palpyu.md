# CHECKPOINT 734 — read me first, then TASKS.md

**Written:** 2026-11-18T22:34:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `2716d92` (this checkpoint is the commit after it)

## Just done
wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EACCES) into TASKS.md; hypothesis: Data Saver (on by default on Android Go) + metered phone hotspot + background IME process

## Do this next
1) add debug-only probe_delay extra (SetupActivity->DiagnosticsActivity) + tools/emu/net_probe.sh phase B: cmd netpolicy set restrict-background true + set metered-network AndroidWifi true, probe in background (HOME), expect EPERM; phase C: add restrict-background-whitelist <uid>, expect OK; dispatch emu-smoke.yml mode=net-probe api 29 and 36; 2) NetworkLimits step in ConnectionProbe (getRestrictBackgroundStatus, isPowerSaveMode, isDeviceIdleMode, isBackgroundRestricted, standby bucket; no new permission) + verdict + Diagnostics buttons ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS; 3) ship 0.7.0-alpha5 code 16

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
