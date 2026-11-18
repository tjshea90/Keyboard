# CHECKPOINT 763 — read me first, then TASKS.md

**Written:** 2026-11-18T23:39:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `83c8239` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoop-v0.7.0-alpha5 has swoop-0.7.0-alpha5.apk, cert verified; BUILDLOG line written). R25a ticked, R25b waits for the owner.

## Do this next
nothing is running. Owner installs swoop-v0.7.0-alpha5, restarts the tablet once, retries translating, and sends the FAIL lines + Result line of Diagnostics > Test translation connection (R25b). Then: group missing -> restart/reinstall advice worked or not; EPERM with group OK -> ask about firewall/VPN/vendor switch/profile; foreground-service workaround needs the owner's decision (FOREGROUND_SERVICE permission). Also open: Kestrel browser sync of core/input (/home/user/Browser absent), tablet side of GlideParams.forPitchDp unvalidated (set SMALL_PITCH_DP = LARGE_PITCH_DP for one setting), optional research next-word post-correction + dwell cue (research/R24-glide-next.md section 5)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M OUTBOX.md

## Last ten checkpoints
```
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
  7c4dd02 ckpt 757: pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refu
  66682b9 ckpt 753: R25: measured table (Android 10 emulator, background app): Data Saver -> ECONN
  837228c ckpt 746: R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered W
  31d7b81 ckpt 742: R25: first emulator run was INVALID (phase B reused old log lines: am start on
  161e5ff ckpt 739: R25 code: AndroidLimits (Data Saver/battery saver/doze/background restricted/s
  8fd1a22 ckpt 734: wrote owner's R25 (probe says Android refuses the keyboard a socket = EPERM/EA
  b445988 ckpt 731: swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
