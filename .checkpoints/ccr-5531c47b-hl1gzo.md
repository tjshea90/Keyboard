# CHECKPOINT 309 — read me first, then TASKS.md

**Written:** 2026-11-17T17:37:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `e494230` (this checkpoint is the commit after it)

## Just done
Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now also paint the keyboard colour (KeyboardRoot.rootBackground: docked = keys colour, floating = transparent), test extended; smoke.sh: hide_error_dialogs (the Android 16 image showed a launcher ANR dialog 'Quickstep isn't responding' on two runs = the 'no text field on the Setup screen' failures, an emulator flake, not Swoop) and an informational squeeze stage (drag the left arrow in and back out, screenshots squeezed.png / unsqueezed.png). Run 44138416431 (api 29, head ckpt 308) is in progress: its symbols.png shows whether the bar row and the Setup title bar are dark now.

## Do this next
read run 44138416431 (artifact emu-smoke-29: VIEW symbols.png: bar row + Setup title bar must be dark); then dispatch emu-smoke api 29 on the head after this ckpt (it has the squeeze stage: VIEW squeezed.png: the strips beside the squeezed keyboard must be dark) and api 36 (the one queued from fb3afa2 may already be running); if all good: bash ship.sh with the SAME note (see ckpt 305: 0.5.0-alpha3 note), dispatch android.yml ONCE (run 44137544253 was cancelled before it built; the tag swoop-v0.5.0-alpha3 does not exist yet: check git ls-remote --tags origin), record-release.sh after green; final reply + amcheckpoint

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
