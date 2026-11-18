# CHECKPOINT 726 — read me first, then TASKS.md

**Written:** 2026-11-18T20:56:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `bfef0d3` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha4 (code 15) DISPATCHED ONCE: android.yml run 44396244888 on sha bfef0d3 (queued 2026-11-18T20:56Z). Do NOT dispatch again. R24a/R24b/R24c ticked.

## Do this next
wait for run 44396244888 to finish green (mcp__github__actions_get get_workflow_run), confirm Release swoop-v0.7.0-alpha4 has the APK (mcp__github__get_release_by_tag), then: bash tools/record-release.sh swoop-v0.7.0-alpha4 with the IDENTICAL note from ship (see CHECKPOINT ckpt 725), then give the owner the Release link on its own line. If the run is red: read failed job logs (get_job_logs failed_only run_id), fix, bump to 0.7.0-alpha5 code 16, ship again. Owner-facing open items: Kestrel browser sync of core/input not done (GlideParams/GlideKeys changed; /home/user/Browser absent); tablet side of forPitchDp has no real-swipe evidence; owner to press Test translation connection in Diagnostics on the tablet and read out the Result line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bfef0d3 ckpt 725: gated swoop-v0.7.0-alpha4 (ime code 15) and pushed it: handoff self-test, unit
  defb85a ckpt 716: R24c presenter tests 75/75 green; R24a emulator probe on Android 10 AND 16 emu
  a6cc912 ckpt 698: R24c translate toggle fixed in KeyboardPresenter (off always works; Enter neve
  a195801 ckpt 686: R24a code done + unit-tested (core SafeReason/Kind DROPPED,BLOCKED,SECURE + en
  295f46b ckpt 670: wrote owner's R24a (translation SocketException) + R24b (more glide research) 
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
  06f1fac ckpt 657: gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit
  9995f5d ckpt 656: pre-ship: 0.7.0-alpha3: hold Backspace to delete faster then whole words, clip
```
