# CHECKPOINT 757 — read me first, then TASKS.md

**Written:** 2026-11-18T23:17:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `5ac4872` (this checkpoint is the commit after it)

## Just done
pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refused connection (Data Saver: button to the Unrestricted data page) from a blocked socket (firewall, VPN, missing internet access) and checks whether Android gave Swoop its internet access; error messages no longer guess

## Do this next
ship.sh gates and releases this (ime)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  66682b9 ckpt 753: R25: measured table (Android 10 emulator, background app): Data Saver -> ECONN
  837228c ckpt 746: R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered W
  31d7b81 ckpt 742: R25: first emulator run was INVALID (phase B reused old log lines: am start on
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
