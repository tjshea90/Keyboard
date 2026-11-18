# Moving this session's keyboard updates into Kestrel's own keyboard (R22 - owner 2026-11-18)

Owner: "Put all the updates into kestrel keyboard". Kestrel (repo tjshea90/Browser) has TWO keyboards: the real Swoop IME whenever Swoop is installed (it already has every update the moment Swoop is updated: nothing to port), and its own floating panel keyboard `MiniNav` (fallback when Swoop is not installed; it types into the address bar or a web page field through `PageField`, with a small text model `MiniKeyboardModel` and the shared glide engine, no `InputController`). This note is about the second one.

## What arrives by the engine sync alone (`tools/sync-keyboard-core.sh`, the vendored copy of `core/.../input/**`)
* R19d the glide always answers (relaxed second decoder pass; `GlideResult.ready`).
* R20d the thumb-offset plane (`TouchField`) inside `GlideDecoder` / `GlideTyping`, and its `touch` export line.
* The word-before lookup that skips opening quotes and brackets (R21 part), pronoun helpers (`InputController.pronounI`).

## What the sync does NOT do and must be adapted by hand (found by reading the Browser code)
1. `Prefs.glideHabit` (Browser `app/.../data/Prefs.kt`) stores exactly two floats and its setter ignores any other length: the engine now hands over 2 + 22 numbers (habit + touch plane), so Kestrel would STOP SAVING the thumb habit. Fix: keep the two habit floats as they are and store the plane numbers in a second preference.
2. `MiniNav.importLearned` / export use only `parsed.habit`: the `touch` line must be carried too.
3. The text rules (capital after a full stop, lone I, no spaces inside quotes, auto-space and a deleted space that stays deleted, a capital for a glided word after a full stop) live in `InputController` (IME only). Kestrel's `MiniKeyboardModel` has none of them. They are added there behind `smartText` (default OFF: the address bar and every existing test are untouched), which `MiniNav` turns on only for a web page's text field that is not a password field. Developed in the Keyboard repo (the model is part of the vendored copy), tested there, then synced.
4. The strip after a glide: Kestrel shows the glide guesses only. It gets the same layout as Swoop: [other guess] [predicted next word] [other guess]; blank boxes take no touch.

## Decided NOT to port, and why
* Docked resize both ways (R19c): the Kestrel panel is already a floating panel with its own corner handles in both directions.
* The copied-text first box (R20a): on Android 12 and later a normal app (not the system keyboard) that reads another app's clipboard makes Android show "pasted from clipboard" every time. Kestrel types through Swoop (which has the box, and is exempt from the notice) whenever Swoop is installed. Kept as an owner decision; easy to add for copies made inside Kestrel itself.

## Rules for the Browser side
Own branch only (`claude/keyboard-caps-space-resize-glide-r72nmp` in tjshea90/Browser), based on the newest tip of another session's work; `git -C` and absolute paths, never `cd` into it; repo text ASCII only; no release while another session ships (a session was preparing alpha70 on branch ccr-587430ac-royssd at 06:05).

## Done (2026-11-18, Browser branch claude/keyboard-caps-space-resize-glide-r72nmp, based on the newest other-session tip 2e8c825; NOT released, NOT merged to the Browser main branch: that is the other session's / the owner's call)
* Engine synced (pin = Keyboard commit recorded in the Browser `KEYBOARD_PIN`): everything of R19d, R20d, R21, R23a (BackspaceRepeat, MiniKeyboardModel.deleteWord), R23c arrives with the copy; the copy's own tests (about 550) pass inside the Browser `:core:test`.
* `Prefs.glideHabit` stores 2 numbers or 2 + 22 (touch plane in its own preference), a 2-number set clears the plane (Forget); `MiniNav.importLearned` carries the `touch` line when the engine was not started.
* `MiniNav`: sentence habits (`smartText`) ON in the text fields of pages (not passwords, never the address bar); after a glide in a page field the strip is [other guess][predicted next word][other guess] with the accent on the middle box, blank boxes draw nothing and take no touch, a tap on the next word keeps the glided word (learned) and inserts the next one with its space (counts double); held Backspace uses `BackspaceRepeat` and, after the long hold, `MiniKeyboardModel.deleteWord`.
* Browser tests: `MiniNavTest` (+5: next-word box and tap, plain strip in the address bar, capital after a full stop, address never capitalised, held Backspace; the obsolete "no word fits" test now checks that the closest word is typed).
* NOT ported, on purpose: number pad (Kestrel's `PageField` does not tell the keyboard the web input type; Kestrel types through Swoop when it is installed, which has the pad), the copied-text first box (Android 12+ "pasted from clipboard" notice for an app that is not the keyboard), docked resize (the Kestrel panel already floats and resizes both ways), shorter minimum height (its own panel geometry).
* NOT run: Kestrel on the tablet or an emulator; no Kestrel release was made (the other session ships alpha70).
