# Architecture (the working summary of plan section 6; the plan has the reasoning and the alternatives that were rejected)

## Decision: build a new small Kotlin IME on the Kestrel browser's pure-Kotlin glide engine
Not a fork of HeliBoard (GPL-3.0, native engine, glide needs a closed library), FlorisBoard (Compose), FUTO (source-available licence) or AOSP LatinIME; those are behaviour references only. Not a keyboard inside the browser APK. Details: plan 6.1.

## Modules
| Module | Kind | Holds | Rules |
| :- | :- | :- | :- |
| `:core` | pure Kotlin/JVM, Java 8 API surface (`-Xjdk-release=8`) | key model and geometry, glide decoder, lexicon (KGL1), personal model, pattern memory, learned-data export; later the input state machine, tap decoder, language features, translate logic | no Android imports; allocation-free on the tap path; one thread owns the personal model |
| `:ime` | Android application `app.swoop.keyboard`, minSdk 29, targetSdk 36 | `InputMethodService`, `KeysView` (one Canvas-drawn View per page), setup/diagnostics/settings activities, clipboard, emoji, the `translate` package | framework Views only, no AndroidX in the shipped dex; `translate` is the ONLY package that may touch the network |
| `:tools` | JVM CLI | `GlideLexiconCompiler` (word list compiler), later n-gram / emoji / embedding builders | build-time only |
| `:eval` | JVM lab | runs `GlideEval` / `GlideHabitEval` today (simulators live with the core tests), the replay runner and sweeps from KB-400 | never shipped |
Package roots stay `app.kestrel.core.*` until one mechanical rename commit after the inherited tests are green (file-format magic strings `KESTREL-KEYBOARD-LEARNING` and `KGL1` never change).

## Editor model (KB-101, plan 6.4)
The app's editor is the source of truth; the keyboard keeps a short window (about 64 characters before and 16 after the cursor, selection, composing region, caps mode) and a few flags (`composing`, `lastGlide`, `lastAutocorrect`, shift state, page). Every edit records the selection the keyboard expects; an `onUpdateSelection` that does not match means the user or the app moved things: clear all keyboard-owned state and re-read. Replacements (autocorrect, strip swap, undo) are atomic and verified: `beginBatchEdit`, check the expected tail with `getTextBeforeCursor`, `deleteSurroundingText`, `commitText`, `endBatchEdit`; refuse if the tail is not what was expected. Enter uses `performEditorAction` when the field defines an action, else a key event. Backspace sends `KEYCODE_DEL` for single characters. The `EditorBridge` interface lives in `:core` with a `FakeEditor` for tests; the `InputConnection` implementation lives in `:ime`.

## Threads
UI thread: drawing, touch, editor calls, O(1) work only. `glide` worker: final decode, learning, saving (the only thread that touches the personal model). `preview` worker: live guesses, its own decoder instance, at most one in flight, 110 ms cadence. IO executor: export/import, journal compaction. Translation (phase 6) runs on one background thread of the same process, only on the owner's request. No second process, no WorkManager, no alarms, no boot receiver: while idle the keyboard does nothing.

## Persistence (private files dir; plan 6.9)
`lex/en.klx` (static list copied from assets), `personal/<lang>.journal` + `.snap` (snapshot + append-only journal, atomic temp+rename), `touch/<layout>.txt`, `own_words.txt`, `diag/*.txt` (counters only), `replay/*.bin` (opt-in local ring buffer, never exported automatically). Exports go to Downloads only on the owner's action. `allowBackup=false` plus data-extraction rules that exclude everything (Android 12+).

## Rendering
One custom View per page drawn with `Canvas`; key rectangles and text baselines computed on size change; the SAME `MiniKeyboardGeometry.boxes` feed the drawing and the decoder, so the model and the pixels cannot disagree; dirty-rect invalidation; no animators (they are off on the tablet), no overdraw.

## Privacy and security (plan 6.8, 9.2)
Permissions are exactly `tools/expected-permissions.txt` (empty today; `INTERNET` only with KB-601 and its guard rails). Password fields and `IME_FLAG_NO_PERSONALIZED_LEARNING`: learn nothing, replay-log nothing, plain strip. No logging of typed text. Only the IME service, the launcher activity and (later) the translate-menu activity are exported. Release signing key is private (KB-003); `tools/verify-apk.sh` checks the certificate, package, version, minSdk, permission list, cleartext/backup flags, GMS and size on the built APK.

## Build and release
GitHub Actions only (`.github/workflows/android.yml`: tags + manual dispatch, never `push:`). Gates (`check`: tooling self-test + versionCode) in about a minute; `full_build` runs the JVM suites, builds, verifies and, once a signing key exists, publishes the Release. Local Gradle is for tests and lint (`./gradlew :core:test`, `:ime:lintDebug`; lint may need one online dependency fetch).

## Where the reasoning lives
Plan sections: 4 (how the engine works and its 12 gaps G1-G12), 7 (speed budgets and techniques), 8 (learning engine v2), 9 (translation), 10 (Gboard-like features), 13 (tests), 14 (risks). Measured glide numbers: `research/kestrel-ref/GLIDE_TYPING.md`.

## Capitals and the automatic space after punctuation (R19a, R19b - owner 2026-11-18)
* **The capital after a sentence end, whichever way the next word arrives** (`InputController`): a tapped letter after `.`/`!`/`?` brings its own space (Settings "Space after punctuation") and the capital; a **glide or a strip word** right after the full stop used to bring the space but not the capital (Shift was only armed once a space existed). `shiftWithLead` now counts the space the word brings along (`nextWordStartsSentence` is the same question for the glide's learning). In a field that asked for capitals the editor's own `getCursorCapsMode` is no longer trusted alone (web views and custom editors answer "none"): the keyboard's rule (`startsSentence`) also turns Shift on in plain text boxes. The pronoun "I." ends a sentence (every other single letter is an initial).
* **A space deleted on purpose stays deleted** (`keepTight`): Backspace over a space that follows `. , ! ? ; :` remembers the cursor offset; no space is put in there again for a tapped letter, a glide or a strip word (and no forced capital), so `example.com` can be typed after the keyboard's space is taken away. Forgotten when the cursor is moved by anyone else, on Enter, paste, a cursor slide and in the next field; it is tied to that one place, so the next sentence gets its space as usual. Undoing a whole glide with Backspace is not a deliberate space deletion.
* Tests: `SentenceStartTest` (16), `KeyboardPresenterTest` (glide after a full stop, deleted space), `AutoCapsTest`, `TypingOptionsTest`. NOT run on the tablet.

## Learning where this thumb lands, everywhere on the keyboard (R20d - owner 2026-11-18)
Research: `research/R20-glide-personalization.md` (ranked techniques, measured with the simulated hand; no real thumb data exists yet - every number is simulated).
* **Before:** one shift of the whole path (`GlideDecoder.biasX/biasY`, the "thumb habit"), learned from every kept glide.
* **Now, on top of it: `TouchField`** - an affine plane (6 numbers: how far the thumb lands from the key it aims at = w0 + w1 (u - 0.5) + w2 (v - 0.5) for x and for y, u/v across/down the letter keys) fitted by ridge regression on the FIRST and LAST letter of every kept glide (the points a person aims at; letters in between are biased by corner cutting - fitting them with an 18-number grid made a hand with no pattern worse, with the plane it does not), faded (`KEEP` 0.998), trusted in proportion to its evidence (`n / (n + 30)`: nothing happens for the first few glides), clamped to +-0.6 key, subtracted from every path before it is judged (`GlideDecoder.decodePass`). 22 numbers saved with the habit (`glide_habit.txt`), exported as a `touch` line, forgotten with the rest. `GlideParams.touchField = false` switches it off.
* Measured (6 simulated hands, 1200 glides each, first guess over the last 400): see the table in the research note, section 2.4 and `TouchFieldTest`. A thumb that lands differently on the left and right: clearly better; a hand with no such pattern: no loss (the test fails if there is one).
* Not built (the note ranks them lower or says they are small): next-word post-correction of the previous glided word, speed/dwell cues, tap touches as evidence, shadow A/B decoding on real thumbs. They are the next candidates if the owner's thumb shows the plane is not enough.
