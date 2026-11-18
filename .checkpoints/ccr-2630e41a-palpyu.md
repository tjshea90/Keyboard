# CHECKPOINT 768 — read me first, then TASKS.md

**Written:** 2026-11-18T23:53:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `d9a8267` (this checkpoint is the commit after it)

## Just done
R25c: owner's exact Test translation connection output received (group OK, battery saver ON, DNS OK, connect IPv4 EPERM in 3 ms for both services, with the test screen in front); verdict no longer rules battery saver out, names it and says switch it off; Android limits step counts battery saver/doze; translate BLOCKED message says switch Battery saver off; tests incl. his exact output green; NOT released yet (wording only: ship together with whatever his retest shows)

## Do this next
owner retests with Battery saver OFF (Settings > Battery) in alpha5 and reports whether the test still says EPERM. If it WORKS: tick R25c/R25b, release 0.7.0-alpha6 code 17 with the new wording (bash ship.sh). If EPERM stays: ask what firewall/VPN/ad-block/parental/school/vendor 'block app internet' items exist (Settings > Apps > Swoop, Settings > Network > VPN, installed apps), consider the vendor/OEM per-app network switch; foreground-service workaround needs the owner's decision

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M ime/src/test/kotlin/app/swoop/keyboard/translate/ConnectionProbeTest.kt

## Last ten checkpoints
```
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
  7c4dd02 ckpt 757: pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refu
  66682b9 ckpt 753: R25: measured table (Android 10 emulator, background app): Data Saver -> ECONN
  837228c ckpt 746: R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered W
  31d7b81 ckpt 742: R25: first emulator run was INVALID (phase B reused old log lines: am start on
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
