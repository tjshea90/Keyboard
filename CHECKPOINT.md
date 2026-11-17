# CHECKPOINT 306 — read me first, then TASKS.md

**Written:** 2026-11-17T17:29:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `d6156b6` (this checkpoint is the commit after it)

## Just done
swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 (run_number 10, queued 17:29:28Z, head d6156b6) with inputs full_build=true and the note kept in the next line. Includes: dark colours by default, the Android 10 inline-report fix in core InputController (Q-SMARTSPACE proven on the Android 10 emulator, run 44136202959), debug-only trace lines. Kestrel's engine copy was re-synced to Keyboard 4644951 and its core tests pass (Browser branch, auto-checkpoint 2db5bbdd; no Kestrel release). Android 16 regression run 44136241139 (emu-smoke api 36) still running at this checkpoint.

## Do this next
wait for android.yml run 44137544253 (list_workflow_jobs about every 4 min; a get_job_logs answers 404 until the job ends); when ALL jobs are green verify the tag (git ls-remote --tags origin swoop-v0.5.0-alpha3) and mcp__github__get_release_by_tag swoop-v0.5.0-alpha3 shows swoop-0.5.0-alpha3.apk, THEN bash tools/record-release.sh swoop-v0.5.0-alpha3 with the IDENTICAL note: 0.5.0-alpha3: dark colours by default, like the photo (Settings, Colours: Automatic follows the phone, or Light); the space and the capital after a question mark, full stop or exclamation mark now also work in the keyboard own Setup and Dictionary screens on Android 10; otherwise the same as alpha2: photo layout, clipboard, translate, dictionary, squeeze . NEVER dispatch this tag twice. Also read emu-smoke api 36 run 44136241139 (artifact emu-smoke-36): it must show 0 failures (the earlier api 36 run failed only on the known 'no text field on the Setup screen' flake). Then the final plain-words reply to the owner (3 release links as plain text lines) + the amcheckpoint the hook asked for

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
