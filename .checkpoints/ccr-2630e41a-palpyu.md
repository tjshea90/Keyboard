# CHECKPOINT 784 — read me first, then TASKS.md

**Written:** 2026-11-19T00:21:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `d9aa4cd` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha6 RELEASED AND RECORDED (run 44426423457 green; Release swoop-v0.7.0-alpha6 has swoop-0.7.0-alpha6.apk, cert verified; BUILDLOG line written). R25 still open: waits for the owner's power-off/on, Settings>Apps>Swoop switches, then export + reinstall result.

## Do this next
nothing is running. Owner: (1) full power-off/on, (2) Settings > Apps > Swoop network switches + VPN/firewall/ad-block apps, (3) Setup > Export what it learned, uninstall, install swoop-v0.7.0-alpha6, Import; then Diagnostics > Test translation connection and paste the lines. If socket creation is STILL refused after a clean reinstall: it is a tablet-maker per-app network block or profile (ask for the Settings > Apps > Swoop screens, what 'Digital wellbeing / parental / school / security' apps exist); the foreground-service workaround would NOT help (socket creation is refused, not background data) so do not build it. Also open: Kestrel browser sync of core/input (/home/user/Browser absent), tablet side of GlideParams.forPitchDp unvalidated (SMALL_PITCH_DP = LARGE_PITCH_DP for one setting), optional research next-word post-correction + dwell cue

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M OUTBOX.md

## Last ten checkpoints
```
  7d2c909 ckpt 780: swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on 
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
  21947b4 ckpt 772: R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPE
  c4b6a87 ckpt 768: R25c: owner's exact Test translation connection output received (group OK, bat
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
  7c4dd02 ckpt 757: pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refu
  66682b9 ckpt 753: R25: measured table (Android 10 emulator, background app): Data Saver -> ECONN
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
