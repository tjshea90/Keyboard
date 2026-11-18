# CHECKPOINT 739 — read me first, then TASKS.md

**Written:** 2026-11-18T22:37:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `f7fee83` (this checkpoint is the commit after it)

## Just done
R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/standby bucket, no permission), probe step 'Android limits' + verdict naming Data Saver and the fix, Diagnostics buttons (Unrestricted data page, Swoop app settings), blocked-socket message names Data Saver, debug-only probe_delay; tools/emu/net_probe.sh phases B (Data Saver + metered wifi + app in background) and C (whitelisted) to REPRODUCE the owner's EPERM; focused tests green; emulator run not yet read

## Do this next
dispatch emu-smoke.yml mode=net-probe (ref ccr-2630e41a-palpyu), read the two probe-emulator job logs (get_job_logs job ids from list_workflow_jobs): look for 'R25 REPRODUCED' / 'R25 not reproduced', probe-B and probe-C lines; if netpolicy metered-network is not supported adapt tools/emu/net_probe.sh; then ship 0.7.0-alpha5 code 16

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
