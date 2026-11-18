# CHECKPOINT 725 — read me first, then TASKS.md

**Written:** 2026-11-18T20:56:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `767a0bc` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-2630e41a-palpyu, inputs {"full_build":"true","note":"0.7.0-alpha4: translating can always be switched off and never translates in password or address-bar boxes; clearer translation errors and a Test translation connection button in Diagnostics; glide re-tuned on real swipes (about +1 point on phones); keyboard translate network check on Android 10 and 16"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha4), then:  bash tools/record-release.sh swoop-v0.7.0-alpha4 "0.7.0-alpha4: translating can always be switched off and never translates in password or address-bar boxes; clearer translation errors and a Test translation connection button in Diagnostics; glide re-tuned on real swipes (about +1 point on phones); keyboard translate network check on Android 10 and 16"
NEVER ship swoop-v0.7.0-alpha4 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
  06f1fac ckpt 657: gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit
  9995f5d ckpt 656: pre-ship: 0.7.0-alpha3: hold Backspace to delete faster then whole words, clip
  74e5d4d ckpt 649: R23c gesture-length term evaluated (research/R23c-gesture-length.md, GlideLeng
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
