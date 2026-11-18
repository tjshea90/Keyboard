# CHECKPOINT 772 — read me first, then TASKS.md

**Written:** 2026-11-18T23:55:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `2ad259b` (this checkpoint is the commit after it)

## Just done
R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPERM connection-log entries; glide healthy on tablet). Deduction: SocketException not ConnectException => socket CREATION refused. SafeReason now says 'socket creation refused' for 'socket failed', probe verdict names it as a permission-record problem and says power the tablet fully off/on first; tests green; not released

## Do this next
owner does, in this order, and reports after each: (1) full power-off/on then Diagnostics > Test translation connection (look for 'socket creation refused' only in the NEXT release; alpha5 still says 'not permitted'), (2) Battery saver off, (3) Settings > Apps > Swoop network/mobile-data switches, VPN, firewall/ad-block apps, (4) Setup > Export what it learned (999 words!) then uninstall + reinstall from the Release. Then ship 0.7.0-alpha6 code 17 (bash ship.sh) with the R25c/R25d wording

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  c4b6a87 ckpt 768: R25c: owner's exact Test translation connection output received (group OK, bat
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
  7c4dd02 ckpt 757: pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refu
  66682b9 ckpt 753: R25: measured table (Android 10 emulator, background app): Data Saver -> ECONN
  837228c ckpt 746: R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered W
  31d7b81 ckpt 742: R25: first emulator run was INVALID (phase B reused old log lines: am start on
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
