# CHECKPOINT 307 — read me first, then TASKS.md

**Written:** 2026-11-17T17:31:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `5c015e8` (this checkpoint is the commit after it)

## Just done
android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE before it built (no tag, no Release exists): the Android 10 day-mode screenshot of run 44136202959 with the new Dark default showed two visual bugs: the docked keyboard's bar row (the glowing bar with the squeeze arrows) is drawn on the IME WINDOW background, which is white in day mode, above the dark keys; and the Setup / Dictionary / Diagnostics screens show a white title bar over a dark body. Fixing both, then re-ship the same tag swoop-v0.5.0-alpha3 once.

## Do this next
fix (1) ime KeyboardRoot.applyMode: docked panel background must ALWAYS be ColorDrawable(keys.backgroundColor) (today null unless the system bar insets > 0); fix (2) ime SettingsUi.kt add settingsTheme(mode) (1 dark = android.R.style.Theme_DeviceDefault, 2 light = Theme_DeviceDefault_Light, 0 = Theme_DeviceDefault_DayNight) and call setTheme(settingsTheme(Settings(this).theme)) BEFORE super.onCreate in SetupActivity, DictionaryActivity, DiagnosticsActivity; SetupActivity's Colours row must recreate() the activity instead of build() so the title bar follows; add Robolectric tests (the panel background equals the keys background in day mode with the dark default; the activity theme follows the setting); run ./gradlew --offline -q :core:test :ime:testDebugUnitTest; then emu-smoke api 29 (day mode: symbols.png must show a dark bar row and a dark title bar) and api 36; then bash ship.sh <the same note as ckpt 305>, dispatch android.yml ONCE (run 44137544253 was cancelled, the tag swoop-v0.5.0-alpha3 does not exist: check git ls-remote --tags origin first), record-release.sh after green; then the final plain-words reply + amcheckpoint

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
