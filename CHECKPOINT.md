# CHECKPOINT 308 — read me first, then TASKS.md

**Written:** 2026-11-17T17:34:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `e09d17e` (this checkpoint is the commit after it)

## Just done
Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panelBackground(): the docked panel now ALWAYS paints the keyboard colour (before: nothing, so the bar row showed the white IME window above the dark keys) and follows a change of colours (KeysView.onPaletteChanged); (2) settingsTheme(mode) + setTheme before super.onCreate in SetupActivity / DictionaryActivity / DiagnosticsActivity and the Setup title bar is painted in the screen colours (a white title bar over a dark body). Tests: KeyboardRootInsetsTest (panel colour + follows Light), SetupActivityTest (framework theme dark by default); all ime tests green. README mentions the dark default.

## Do this next
dispatch emu-smoke.yml api 29 (and api 36) on this head, fetch artifacts, VIEW symbols.png (day mode, dark default): the bar row above the keys must be dark and the Setup title bar dark; if good: bash ship.sh with the SAME note as before (0.5.0-alpha3 note in ckpt 305/306), dispatch android.yml ONCE (run 44137544253 was cancelled before building, tag swoop-v0.5.0-alpha3 does not exist: check git ls-remote --tags origin), record-release.sh after green; final reply + amcheckpoint

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
