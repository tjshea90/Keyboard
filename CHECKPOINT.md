# CHECKPOINT 716 — read me first, then TASKS.md

**Written:** 2026-11-18T20:42:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `8dcdf9d` (this checkpoint is the commit after it)

## Just done
R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emulators: every stage OK vs real services (IPv6-unreachable now not a failure); R24b: real-swipe harness (RealSwipe*.kt, FUTO corpus, 2 slices of donors) -> first real accuracy 86.8/93.8 (A) 88.0/95.0 (B); adopted GlideParams.forPitchDp (shapeW 1.0 + visitCap 5 for phone pitch <=45dp, slides to old 2.0/3.0 at >=70dp) worth ~+0.8 first/+1 top3 averaged over both slices; research/R24-glide-next.md written; full gradle suite running in background (/tmp/claude-0/sweeps/FULL.log has EXITCODE)

## Do this next
1) read /tmp/claude-0/sweeps/FULL.log until EXITCODE 0 (core+ime+lint with forPitchDp + pitch tests); 2) push, dispatch emu-smoke.yml mode=smoke api=29 and mode=net-probe (ref ccr-2630e41a-palpyu) and read results; 3) tick R24a/R24b/R24c in TASKS.md with evidence (bash tools/claim.sh <ID> --done), 4) bump ime/build.gradle.kts to 0.7.0-alpha4 code 15, bash ship.sh note, dispatch android.yml full_build=true, wait for tag swoop-v0.7.0-alpha4, bash tools/record-release.sh; 5) tell owner: Kestrel browser sync of core/input NOT done (GlideParams/GlideKeys changed), tablet side of forPitchDp has no real-swipe evidence

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
  06f1fac ckpt 657: gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit
  9995f5d ckpt 656: pre-ship: 0.7.0-alpha3: hold Backspace to delete faster then whole words, clip
  74e5d4d ckpt 649: R23c gesture-length term evaluated (research/R23c-gesture-length.md, GlideLeng
  0db3f9a ckpt 639: R23a-e core+views done: held Backspace speeds up then deletes words (KeysView+
```

(17 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
