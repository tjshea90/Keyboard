# CHECKPOINT 311 — read me first, then TASKS.md

**Written:** 2026-11-17T17:41:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `9cb2b3b` (this checkpoint is the commit after it)

## Just done
swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137544253, was cancelled before it built): android.yml run 44139548579 (run_number 11, queued 17:40:22Z, head 9cb2b3b = ckpt 310), inputs full_build=true + the note in the next line. Head includes: Dark default; docked panel / root / squeezed strips paint the keyboard colour; settings screens use the matching framework theme; Q-SMARTSPACE fix. Proof so far: Android 10 day-mode run 44138416431 (head ckpt 308) 0 failures, symbols.png dark bar row + dark title bar, field 'H? A'. Still running: emu-smoke api 36 (fb3afa2) and api 29 (2631ec0, with the squeeze stage: view squeezed.png / unsqueezed.png).

## Do this next
wait for android.yml run 44139548579 (list_workflow_jobs about every 4 min; get_job_logs answers 404 until the job ends); in the meantime read the two emu-smoke runs (api 36 from head fb3afa2; api 29 from head 2631ec0: VIEW squeezed.png: the strips beside the squeezed keyboard must be dark; unsqueezed.png full width again; smoke.log 0 failures) with python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>; if a run shows a REAL regression cancel the release run 44139548579 before it publishes. When the release run is green: verify git ls-remote --tags origin swoop-v0.5.0-alpha3 and mcp__github__get_release_by_tag swoop-v0.5.0-alpha3 (swoop-0.5.0-alpha3.apk), THEN bash tools/record-release.sh swoop-v0.5.0-alpha3 with the IDENTICAL note: 0.5.0-alpha3: dark colours by default, like the photo (Settings, Colours: Automatic follows the phone, or Light); the space and the capital after a question mark, full stop or exclamation mark now also work in the keyboard own Setup and Dictionary screens on Android 10; otherwise the same as alpha2: photo layout, clipboard, translate, dictionary, squeeze . NEVER dispatch this tag again. Then tick TASKS P6 (claim.sh P6 --done with the evidence), the final plain-words reply (links on their own lines) and the amcheckpoint the hook asked for

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
```
