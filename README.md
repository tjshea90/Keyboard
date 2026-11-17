# Swoop

A small, fast Android keyboard with **glide typing that keeps learning from you**. Built for one old tablet (Android 10 Go, 2 GB RAM, no Google services) and an Android 16 phone, so it is light on purpose: no accounts, no ads, no telemetry, no AI features, no network access at all in the current version.

**Status: early alpha (0.1.x).** It builds and its logic is tested, but it is not yet a daily-driver keyboard. Keep your current keyboard installed and switch back whenever you like (tap the globe key).

## What works today
- Tap typing and glide typing (slide over the letters; a strip shows other guesses; one Backspace removes the whole glided word).
- Learns your words, word pairs, corrections and how your thumb lands, only on your device. Export / import of what it learned (including the file the Kestrel browser's keyboard exports).
- Text, URL, e-mail, number and phone layouts; symbols page; hold a top-row letter for its digit; slide on the space bar to move the cursor; two-thumb typing.
- Learns nothing in password fields or when an app asks for incognito typing.
- A crash-loop "safe layout" and a globe key, so a bug can never lock you out of typing.
- Diagnostics screen (numbers only, nothing you typed) with a Copy button.

## Install
1. Open the latest **Release** of this repository on the device and tap the `.apk` file (allow installs from your browser when Android asks).
2. Open the Swoop app: it explains how to switch the keyboard on and choose it.

Every Release is built and signed by GitHub Actions with one private signing key, so later versions update in place. Check the SHA-256 in the Release text against the file if you want to be sure.

## Privacy
The app asks for **no permissions** (CI fails if that ever changes without a decision). It does not use the internet. What it learns stays in its private storage; the only way data leaves is the Export button you press. See `docs/IF-THE-KEYBOARD-BREAKS.md` for plain-words help.

## For developers
- `core/` pure Kotlin (glide decoder, learning, the input state machine); `ime/` the Android app; `tools/` shell tooling and the word-list compiler; `eval/` the accuracy lab.
- `./gradlew :core:test :ime:testDebugUnitTest :ime:lintDebug` runs the tests (no emulator needed). `.github/workflows/emu-smoke.yml` runs the keyboard on a real Android emulator.
- Plan and decisions: `research/KEYBOARD-APP-PLAN.md`; job list `TASKS.md`; where work stopped `CHECKPOINT.md`.
- The glide engine and the English word list come from the Kestrel browser project (word-list licences in `docs/DICTIONARY.md` and `ime/src/main/assets/glide/LICENSE.txt`).
