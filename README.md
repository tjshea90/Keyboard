# Swoop

A small, fast Android keyboard with **glide typing that keeps learning from you**. Built for one old tablet (Android 10 Go, 2 GB RAM, no Google services) and an Android 16 phone, so it is light on purpose: no accounts, no ads, no telemetry, no AI features, no network access at all in the current version.

**Status: alpha (0.6.x).** It builds, its logic is tested (unit tests, a simulated thumb, a real Android 10 emulator on GitHub), and the owner's tablet and phone are the real test. Keep your current keyboard installed and switch back whenever you like (hold the space bar, or use Android's keyboard switcher).

## What works today
- Tap typing and glide typing (slide over the letters; a glowing trail follows your finger and a glide starts the moment you move; a strip shows other guesses; one Backspace removes the whole glided word).
- The layout of the Gboard photo the owner chose (docs/LAYOUT.md): ten letter keys across with a small digit hint on each of the top row (hold a key to type its digit), the home row indented half a key, Shift at the left and Backspace at the right of the bottom letter row, and under it ?123 (round, bottom LEFT), comma, a very wide blank space bar, full stop and Enter (round, bottom RIGHT). Nothing under the space bar: no emoji, voice or globe key. Hold the full stop or the comma for a grid of special characters; ?123 opens two pages of symbols, and one special character typed brings the letters back. A number row is in Settings (off by default).
- A toolbar above the keys: settings (cog), clipboard, translate (ES), then the suggested words. Long press a suggested word to remove it for good. A word with an emoji (clown) offers the emoji; tapping it replaces the word.
- Clipboard like Gboard's: what you copy while the keyboard is open (kept an hour), tap to paste, pin to keep, hold for Pin / Delete, Clear all.
- Translate: switch it on with the ES icon, type English, press Enter to turn the box into Mexican Spanish, press Enter again to send. The only internet use in the keyboard (MyMemory, then Google Translate, same method as the LinkMeter app).
- My dictionary (Settings): add words (offered when you glide their pattern) and remove words (never offered again). Rare words lose to common words and to words you use.
- Hide the letters: one solid blank panel, no key shapes at all, that still types and glides. Glide trail on/off.
- Capital letters where they belong: the first word of a box, after . ? ! and a new line (also through a closing quote or bracket, and not after e.g. / Mr. / Dr.), days, months, countries, languages, common names and brands, and the names YOU write with a capital (type one twice and it is yours). One Backspace takes a capital back and the keyboard remembers.
- Tap any word you already typed, even several words ago, and the suggestion strip offers words with a close glide pattern in its place (a neighbouring key, two letters swapped, a letter too many or too few), ranked with the words around it. Tap one and exactly that word is replaced.
- The suggestion strip (docs/STRIP.md): what you just copied is the first box (tap to paste; it shows dots in password boxes); after a glide the middle box is the predicted next word and the boxes at its sides are other guesses of the glide; a lone i is always a capital I (also i'm, i'll, i've, i'd, also for glided words).
- Resize the docked keyboard both ways: pinch the glowing bar with two fingers to make it narrower, or drag an arrow at either end of the bar like a corner (sideways = narrower or wider, UP = taller, down = shorter; the keyboard stays on the bottom edge); drag the bar sideways to move it. While the keyboard is squeezed, resized or floating the bar shows a **Full size** button: one tap puts the full-width, normal-height keyboard back (double-tapping the bar still works). Resizing is smooth: only a picture of the keyboard moves while your finger is down, and the keys are laid out once when you let go.
- A floating keyboard: a glowing bar on top of the keyboard; press and hold it and drag to lift the keyboard off the bottom and put it anywhere, drag a corner to resize it, double-tap the bar (or drop it at the bottom) to dock it again. The app behind is never pushed or covered except by the keyboard itself. Checked on an Android 10 emulator in CI; not yet on the real tablet or phone.
- Word suggestions while you tap, with glide typing on or off: completions, the likely next word, and an automatic fix for an obvious typo (Backspace takes it back and the keyboard never changes that word again).
- It learns you in every app: your words, the words you use together, how you begin sentences, your capital letters, your corrections (including the word you swapped in by tapping an old one), a word you erased and retyped (also after a glide that matched nothing), how often you undo an automatic fix (the keyboard then fixes less or more), and where your thumb lands. Only on your device. Export / import of what it learned (including the file the Kestrel browser's keyboard exports), and a Forget button.
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
- The Kestrel browser ALWAYS types through this keyboard when it is installed (owner request 17): every Kestrel text box (address bar, Home box, find bar, web page fields) takes Android's normal input path, so there is ONE learned memory and one set of settings for both apps and nothing to keep in sync. Kestrel's address box tells Swoop what it is (the browser layout with slash, .com and Go, words and glide still work, nothing is auto-capitalised, spaced or fixed in an address), private tabs make Swoop learn nothing, the navigation panel opens Swoop floating over the page, and one tap in Kestrel's Settings hands what Kestrel's own small keyboard learned before to Swoop ("Add to Swoop" screen, nothing is added without a tap). The small keyboard in Kestrel is only the fallback when Swoop is not installed (a pinned, checked copy of `core/.../input`). See `docs/KESTREL-LINK.md`. The daily-use simulation (`.github/workflows/daily-sim.yml`, `tools/sim/`) plays both apps together on an Android 10 tablet-shaped emulator.
