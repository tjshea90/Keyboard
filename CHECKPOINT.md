# CHECKPOINT 310 — read me first, then TASKS.md

**Written:** 2026-11-17T17:40:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `2631ec0` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"full_build":"true","note":"0.5.0-alpha3: dark colours by default, like the photo (Settings, Colours: Automatic follows the phone, or Light); the space and the capital after a question mark, full stop or exclamation mark now also work in the keyboard own Setup and Dictionary screens on Android 10; otherwise the same as alpha2: photo layout, clipboard, translate, dictionary, squeeze"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.5.0-alpha3), then:  bash tools/record-release.sh swoop-v0.5.0-alpha3 "0.5.0-alpha3: dark colours by default, like the photo (Settings, Colours: Automatic follows the phone, or Light); the space and the capital after a question mark, full stop or exclamation mark now also work in the keyboard own Setup and Dictionary screens on Android 10; otherwise the same as alpha2: photo layout, clipboard, translate, dictionary, squeeze"
NEVER ship swoop-v0.5.0-alpha3 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
