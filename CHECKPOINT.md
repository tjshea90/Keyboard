# CHECKPOINT 731 — read me first, then TASKS.md

**Written:** 2026-11-18T21:11:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `de648cc` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha4 RELEASED AND RECORDED (run 44396244888 green: check, test-jvm, build; Release swoop-v0.7.0-alpha4 has swoop-0.7.0-alpha4.apk, cert verified; BUILDLOG line written). R24a/b/c ticked.

## Do this next
nothing is running. Owner installs swoop-v0.7.0-alpha4 on tablet + phone, tries translating OFF, presses Diagnostics > Test translation connection on the tablet and reports the Result line. Next session: fix what the owner reports; if the tablet glide got worse set GlideParams.SMALL_PITCH_DP = LARGE_PITCH_DP (one setting); Kestrel browser sync of core/input (/home/user/Browser absent) is the owner's/other session's call; optional research not done: next-word post-correction and dwell/speed cue (both now measurable on the real-swipe harness, research/R24-glide-next.md section 5)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M OUTBOX.md

## Last ten checkpoints
```
  9892412 ckpt 726: swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on 
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
  06f1fac ckpt 657: gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
