# CHECKPOINT 742 — read me first, then TASKS.md

**Written:** 2026-11-18T22:47:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `639a8e1` (this checkpoint is the commit after it)

## Just done
R25: first emulator run was INVALID (phase B reused old log lines: am start only fronted the old task); found Data Saver status is unreadable without ACCESS_NETWORK_STATE (probe says 'unknown'), so the blocked-socket verdict now says MOST LIKELY Data Saver + the fix; net_probe.sh fixed (am start -S, count finished probes); re-dispatching

## Do this next
dispatch emu-smoke.yml mode=net-probe again and read probe-emulator (29) log for 'R25 REPRODUCED'/'phase B'/'phase C'; then ship 0.7.0-alpha5 code 16

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M ime/src/main/kotlin/app/swoop/keyboard/translate/AndroidLimits.kt
     M ime/src/main/kotlin/app/swoop/keyboard/translate/ConnectionProbe.kt
     M tools/emu/net_probe.sh

## Last ten checkpoints
```
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
