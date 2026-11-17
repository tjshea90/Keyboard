# Swoop

A small, fast Android keyboard with **glide typing that keeps learning from you**. Built for one old tablet (Android 10 Go, 2 GB RAM, no Google services) and an Android 16 phone, so it is light on purpose: no accounts, no ads, no telemetry, no AI features, no network access at all in the current version.

**Status: alpha (0.5.x).** It builds, its logic is tested (unit tests, a simulated thumb, a real Android 10 emulator on GitHub), and the owner's tablet and phone are the real test. Keep your current keyboard installed and switch back whenever you like (hold the space bar, or use Android's keyboard switcher).

## What works today
- Tap typing and glide typing (slide over the letters; a glowing trail follows your finger and a glide starts the moment you move; a strip shows other guesses; one Backspace removes the whole glided word).
- The layout of the Gboard photo the owner chose (docs/LAYOUT.md): ten letter keys across with a small digit hint on each of the top row (hold a key to type its digit), the home row indented half a key, Shift at the left and Backspace at the right of the bottom letter row, and under it ?123 (round, bottom LEFT), comma, a very wide blank space bar, full stop and Enter (round, bottom RIGHT). Nothing under the space bar: no emoji, voice or globe key. Hold the full stop or the comma for a grid of special characters; ?123 opens two pages of symbols, and one special character typed brings the letters back. A number row is in Settings (off by default).
- A toolbar above the keys: settings (cog), clipboard, translate (ES), then the suggested words. Long press a suggested word to remove it for good. A word with an emoji (clown) offers the emoji; tapping it replaces the word.
- Clipboard like Gboard's: what you copy while the keyboard is open (kept an hour), tap to paste, pin to keep, hold for Pin / Delete, Clear all.
- Translate: switch it on with the ES icon, type English, press Enter to turn the box into Mexican Spanish, press Enter again to send. The only internet use in the keyboard (MyMemory, then Google Translate, same method as the LinkMeter app).
- My dictionary (Settings): add words (offered when you glide their pattern) and remove words (never offered again). Rare words lose to common words and to words you use.
- Hide the letters (blank keys that still type), glide trail on/off, capital letter after . ? ! by default.
- Squeeze the docked keyboard: pinch the glowing bar with two fingers or drag the arrows at its ends; drag the bar sideways to move it; double-tap the bar for full width.
- A floating keyboard: a glowing bar on top of the keyboard; press and hold it and drag to lift the keyboard off the bottom and put it anywhere, drag a corner to resize it, double-tap the bar (or drop it at the bottom) to dock it again. The app behind is never pushed or covered except by the keyboard itself. Checked on an Android 10 emulator in CI; not yet on the real tablet or phone.
- Word suggestions while you tap, with glide typing on or off: completions, the likely next word, and an automatic fix for an obvious typo (Backspace takes it back and the keyboard never changes that word again).
- It learns you in every app: your words, the words you use together, your corrections, a word you erased and retyped, and where your thumb lands. Only on your device. Export / import of what it learned (including the file the Kestrel browser's keyboard exports), and a Forget button.
- Options like other keyboards: space after punctuation, double space for a full stop, capital letters after a full stop, capital I, slide on the space bar to move the cursor, key pop-up, vibration, key sound, key height, colours (dark like the photo by default; automatic follows the phone, or light) and an accent colour.
- A redesigned settings screen with a live copy of the keyboard that follows every change.
- Text, URL, e-mail, number and phone layouts; two-thumb typing; in address bars that complete what you type inline (Chrome, Brave) a tap on a suggested word still replaces the glided word.
- Learns nothing in password fields or when an app asks for incognito typing.
- A crash-loop "safe layout" and a globe key, so a bug can never lock you out of typing.
- Diagnostics screen (numbers only, nothing you typed) with a Copy button.


## Install
1. Open the latest **Release** of this repository on the device and tap the `.apk` file (allow installs from your browser when Android asks).
2. Open the Swoop app: it explains how to switch the keyboard on and choose it.

Every Release is built and signed by GitHub Actions (with the dev key committed in `signing/dev/`, so later versions update in place). Only install Swoop APKs from this repository's Releases: because the signing key is public, an APK from anywhere else could be signed to look like an update. The SHA-256 is in each Release text.

## Privacy
The app asks for ONE permission, INTERNET, and only the translate icon uses it (switched off until you turn it on in the toolbar; two hosts only: MyMemory and Google Translate; nothing else in the keyboard touches the network; CI fails if the permission list ever changes without a decision). What it learns stays in its private storage; the only way data leaves is the Export button you press. See `docs/IF-THE-KEYBOARD-BREAKS.md` for what to do if the keyboard misbehaves.

## For developers
- `core/` pure Kotlin (glide decoder, learning, the input state machine); `ime/` the Android app; `tools/` shell tooling and the word-list compiler; `eval/` the accuracy lab.
- `./gradlew :core:test :ime:testDebugUnitTest :ime:lintDebug` runs the tests (no emulator needed). `.github/workflows/emu-smoke.yml` runs the keyboard on a real Android emulator.
- Plan and decisions: `research/KEYBOARD-APP-PLAN.md`; job list `TASKS.md`; where work stopped `CHECKPOINT.md`.
- The glide engine and the English word list come from the Kestrel browser project (word-list licences in `docs/DICTIONARY.md` and `ime/src/main/assets/glide/LICENSE.txt`).
- The Kestrel browser uses this keyboard: its navigation panel opens Swoop floating over the page when Swoop is the chosen keyboard, and the browser's own mini keyboard runs the SAME engine (a pinned, checked copy of `core/.../input`). See `docs/KESTREL-LINK.md`.
