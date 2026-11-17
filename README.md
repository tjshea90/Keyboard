# Swoop

A small, fast Android keyboard with **glide typing that keeps learning from you**. Built for one old tablet (Android 10 Go, 2 GB RAM, no Google services) and an Android 16 phone, so it is light on purpose: no accounts, no ads, no telemetry, no AI features, no network access at all in the current version.

**Status: early alpha (0.2.x).** It builds and its logic is tested, but it is not yet a daily-driver keyboard. Keep your current keyboard installed and switch back whenever you like (tap the globe key).

## What works today
- Tap typing and glide typing (slide over the letters; a glowing trail follows your finger and a glide starts the moment you move; a strip shows other guesses; one Backspace removes the whole glided word).
- A number row above the letters at all times, and the 123 key for the symbols page. Holding the full stop or the comma also opens a grid of the same symbols (slide to one and let go). The space bar is as wide as the row allows, and there is no keyboard-switch key by default: hold the space bar to switch keyboards (a Settings switch brings the globe key back).
- A floating keyboard: a glowing bar on top of the keyboard; press and hold it and drag to lift the keyboard off the bottom and put it anywhere, drag a corner to resize it, double-tap the bar (or drop it at the bottom) to dock it again. The app behind is never pushed or covered except by the keyboard itself. Checked on an Android 10 emulator in CI; not yet on the real tablet or phone.
- Word suggestions while you tap, with glide typing on or off: completions, the likely next word, and an automatic fix for an obvious typo (Backspace takes it back and the keyboard never changes that word again).
- It learns you in every app: your words, the words you use together, your corrections, a word you erased and retyped, and where your thumb lands. Only on your device. Export / import of what it learned (including the file the Kestrel browser's keyboard exports), and a Forget button.
- Options like other keyboards: space after punctuation, double space for a full stop, capital letters after a full stop, capital I, slide on the space bar to move the cursor, key pop-up, vibration, key sound, key height, light / dark / automatic colours and an accent colour.
- A redesigned settings screen with a live copy of the keyboard that follows every change.
- Text, URL, e-mail, number and phone layouts; two-thumb typing; in address bars that complete what you type inline (Chrome, Brave) a tap on a suggested word still replaces the glided word.
- Learns nothing in password fields or when an app asks for incognito typing.
- A crash-loop "safe layout" and a globe key, so a bug can never lock you out of typing.
- Diagnostics screen (numbers only, nothing you typed) with a Copy button.

Not yet: translation, emoji, clipboard.

## Install
1. Open the latest **Release** of this repository on the device and tap the `.apk` file (allow installs from your browser when Android asks).
2. Open the Swoop app: it explains how to switch the keyboard on and choose it.

Every Release is built and signed by GitHub Actions (with the dev key committed in `signing/dev/`, so later versions update in place). Only install Swoop APKs from this repository's Releases: because the signing key is public, an APK from anywhere else could be signed to look like an update. The SHA-256 is in each Release text.

## Privacy
The app asks for **no permissions** (CI fails if that ever changes without a decision). It does not use the internet. What it learns stays in its private storage; the only way data leaves is the Export button you press. See `docs/IF-THE-KEYBOARD-BREAKS.md` for plain-words help.

## For developers
- `core/` pure Kotlin (glide decoder, learning, the input state machine); `ime/` the Android app; `tools/` shell tooling and the word-list compiler; `eval/` the accuracy lab.
- `./gradlew :core:test :ime:testDebugUnitTest :ime:lintDebug` runs the tests (no emulator needed). `.github/workflows/emu-smoke.yml` runs the keyboard on a real Android emulator.
- Plan and decisions: `research/KEYBOARD-APP-PLAN.md`; job list `TASKS.md`; where work stopped `CHECKPOINT.md`.
- The glide engine and the English word list come from the Kestrel browser project (word-list licences in `docs/DICTIONARY.md` and `ime/src/main/assets/glide/LICENSE.txt`).
