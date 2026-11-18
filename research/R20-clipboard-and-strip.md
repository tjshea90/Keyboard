# R20: the clipboard chip and the 3-box suggestion strip - what other keyboards do, and what Swoop should do

Date of research: 2026-11-18. Owner requests (2026-11-18, in the owner's words):
1. "if I copied something, the first suggested word in the keyboard suggestion boxes should be what I copied. Use the same logic as other keyboards."
2. "make the keyboard also suggest future words by predicting the next words I'm going to type... maybe in the middle box of the three suggestion boxes, while the left and right boxes give possible words I meant based on the glide pattern."

## 0. How to read this note

Labels on every claim:
- **[V]** verified: the page body or the source code was fetched and read in this session (the fetch tool summarised it, so quotes are short and may be paraphrased; the URLs in section 6 let anyone re-check).
- **[S]** secondary: a news or blog article that was fetched and read, but it is not the vendor's own page and may be dated.
- **[U]** unverified: only a search-result snippet, a recollection, or nothing found. Do not build on a [U] without testing it.

Nothing here comes from running a keyboard on a device. All behaviour of closed-source keyboards (Gboard, SwiftKey, Samsung, iOS) is therefore as reported by articles, not observed. Where a vendor documents nothing, this note says "not found" rather than guessing.

## 1. Request 1: the clipboard chip

### 1.1 Android rules for what an input method may read (question f)

| Fact | Label | Source |
|---|---|---|
| Android 10 and later: "Unless your app is the default input method editor (IME) or is the app that currently has focus, your app cannot access clipboard data." | [V] | Android 10 privacy changes |
| Android 10 platform code (`ClipboardService.clipboardAccessAllowed`): after the app-op check, a read is allowed if the caller holds `READ_CLIPBOARD_IN_BACKGROUND`, or is the default IME (package of `Settings.Secure.DEFAULT_INPUT_METHOD`), or its UID has window focus, or is the content-capture / augmented-autofill service. The IME check does not need focus. | [V] | AOSP mirror, tag android-10.0.0_r1 |
| Reads return nothing while the device is locked (`isDeviceLocked`). | [V] | same file |
| Android 16 platform code (tag android-16.0.0_r1): the same rule is still there (`isDefaultIme` allows the read; focus check only if not the IME). Writes need no focus. This settles the plan's open `[T]` item "is the clipboard rule unchanged on Android 16": yes, for the default IME. | [V] | AOSP mirror, tag android-16.0.0_r1 |
| Android 12 and later: the system shows a toast "APP pasted from your clipboard" when an app calls `getPrimaryClip()`; no toast for the app's own clip, for repeated access, or for metadata-only `getPrimaryClipDescription()`. | [V] | Copy and paste guide |
| In the Android 16 code the toast (`showAccessNotificationLocked`) is skipped for the default IME ("Exclude special cases: IME, ContentCapture, Autofill"). `hasPrimaryClip` never shows it. So Swoop, while it is the selected keyboard, reads the clipboard without any toast on both Android 10 and 16. | [V] | AOSP mirror, android-16.0.0_r1 |
| `ClipDescription.EXTRA_IS_SENSITIVE` (string "android.content.extra.IS_SENSITIVE") exists since API 33; the docs say it is only "a rendering hint from the source application" that "does not change clipboard behavior or add additional security". Apps on older SDKs set the same string by hand, so Swoop can read it on API 29 with `getExtras()` (API 24). | [V] | ClipDescription reference; Secure clipboard handling |
| `ClipDescription.getTimestamp()` (API 26) returns when the clip was copied to the global clipboard, so the age of a clip is known without a listener. | [V] | ClipDescription reference |
| Android 13 and later: a system copy-preview appears and the clipboard is cleared automatically after about an hour (the Android developer guide says the clearing is automatic from API 33; the one-hour figure is from 9to5Google/Android Police, 3,600,000 ms). | [S] | Secure clipboard handling; 9to5Google |
| Some Android 10 devices (OEM builds, Go edition) could differ; not checked on the tablet. | [U] | - |

Consequence: Swoop may read the clipboard at any time it is the active keyboard, on both devices, with no toast and no permission. Peeking at the metadata first (`primaryClipDescription`: MIME type, sensitive flag, timestamp) before reading the text is free and keeps the text out of memory when it is not going to be shown.

### 1.2 Keyboard by keyboard (questions a, b, c, d, e, g)

| Keyboard | When it appears (a) | How long (b) | Where and how it looks (c) | When it goes (d) | Sensitive / password (e) | Shown again after use (g) |
|---|---|---|---|---|---|---|
| **Gboard** | Right after a copy or cut, when a text field is focused, if "Show recently copied text and images in the suggestions bar" is on. A 2020 test report says it appeared "immediately following a word or phrase being cut" and seemed to depend on relevance (a copied URL did not appear in YouTube). [S] | Clipboard-manager items: unpinned ones are deleted after 1 hour (Android Authority 13 Aug 2026, Google's own clipboard tray warning). [V] A beta teardown shows a 1/2/3/6/12/18/24-hour slider, not released. [S] **The chip's own time limit: not found.** [U] | Replaces the toolbar shortcut icons (GIF, sticker, settings) while a clip exists (9to5Google 2020). [S] XDA describes "a single clipboard suggestion in the middle" of the suggestion bar. [S] Android Police 2021: long clips are split into extra chips with a type icon (phone, link, address, date, time, number) next to the full text. [S] | "The only way to dismiss them has been to type something" (Android Authority, 18 Aug 2026); a close cross is being tested. [V] After a paste the chip is replaced by the normal icon row (2020 test). [S] | Passwords are shown as dots/ellipsis, not plain text. [S] `EXTRA_IS_SENSITIVE` makes the keyboard hide the preview. [V] | After a paste: no (one-time shortcut, 2020 test). [S] A new copy shows a new chip. [U] |
| **Microsoft SwiftKey** | The last copied clip is shown on the prediction bar as a quick-paste option by default; setting under Rich input > Clipboard. [V] (Microsoft's page describes it for the synced Cloud Clipboard; whether a plain local copy behaves the same is not stated.) | Clipboard history: 1 hour, pinned clips kept. [V] Cloud clip: 1 hour. [V] Chip time limit: not found. [U] | On the prediction bar; "only the last copied clip" appears there. [S] Truncation and label: not found. [U] | Not found. [U] | Not found. [U] | Not found. [U] |
| **Samsung Keyboard** | No suggestion-strip chip found; the clipboard is a toolbar button that opens a panel. [S] | not found | - | - | - | - |
| **iOS** | Not a keyboard feature until iOS 27: after copying, a system paste suggestion appears in the QuickType bar when the cursor enters a text field, showing a preview or "Paste from [App]". [S] (9to5Mac Sept 2026, Apple World Oct 2026.) | not stated | QuickType bar, top row above the keys. [S] | not stated | not stated | not stated |
| **FUTO Keyboard** | Release 0.1.25: "The suggestion bar will now let you quickly paste recently copied entries"; new clipboard settings for "duration, amount, and whether to keep sensitive clips". [V] | Configurable duration. [V] | Details of the chip: not found. [U] | not found | There is a "keep sensitive clips" option, i.e. sensitive clips are skippable. [V] | not found |
| **HeliBoard** (AOSP LatinIME descendant) | [V from source] Shown only when the strip is in its neutral state (no suggestions) and at the start of input; takes priority over the neutral suggestions. Setting "suggest clipboard content". | **3 minutes** (`RECENT_TIME_MILLIS = 3 * 60 * 1000`); older clips are never suggested. | A full-strip view with a close button; text cut to 200 characters for display (no ellipsis added); images supported; numeric fields only get a clip that is a valid number. | Tap (pastes, hides, and sets "don't show current clip"); the close button (same flag); a new clip resets the flag. It stays through typing pauses until the 3 minutes end. | Masked as asterisks of the same length when the clip is flagged sensitive (reads the extra) or the field is a password field. | No: not for the same clip. A new copy resets it. |
| **OpenBoard / AOSP LatinIME** | No clipboard chip found (HeliBoard added it). [U] | - | - | - | - | - |
| **AnySoftKeyboard** | Release note 1.10.1794 "Suggesting recent clipboard entry". [U: release-note snippet only] | not found | not found | not found | not found | not found |
| **Fleksy** | Nothing found. [U] | - | - | - | - | - |

What the evidence says in one paragraph: every keyboard that has the feature shows it only while the strip has nothing better to show (nothing typed yet), as a tappable chip with the copied text, never as a permanent fixture; it is one tap to paste and the tap uses the clip up; typing makes it go away (Gboard) or it lasts a short fixed time (HeliBoard 3 minutes, the only number in source code); a clip flagged sensitive or a password field gets the text masked, not hidden, because pasting a password you just copied from a password manager is the main legitimate use. The one-hour figure the owner may have heard belongs to the clipboard manager (the history list), not to the chip.

## 2. Request 2: the 3-box strip and next-word prediction

### 2.1 Slot layout (questions a, b, c)

| Keyboard | Layout rule | Label |
|---|---|---|
| **AOSP LatinIME / HeliBoard / OpenBoard** (same layout code) | `SuggestionStripLayoutHelper`: 3 slots; centre index = count / 2 = slot 1; centre gets 40 % of the width, each side 30 %. The most important word goes in the **centre** (the auto-correction when one will be applied, otherwise the typed word); the second most important goes **left** (`mTypedWordPositionWhenAutocorrect = centre - 1`); the rest alternate outward (centre-1, centre+1, centre-2, ...). For a typing list the typed word is shown unless the input style is typing / tail batch / update batch with a floating preview: then the typed word is omitted (position -1) and the best goes centre. No quotation marks are drawn; the typed word is marked by colour, bold and underline flags. Long words are squeezed to 70 % horizontal scale, then ellipsized in the middle. `SuggestedWords` keeps the typed word at index 0 and the auto-correction at index 1; `KIND_PREDICTION` marks "a suggestion made with no input" (next-word list). | [V] |
| **Gboard** | The Gboard paper (Hard et al. 2018): "The center position in the suggestion strip is reserved for the highest-probability candidate", the second and third candidates occupy the left and right. [V] Since 2021 the middle slot is bold-highlighted and is the one applied if you keep typing without choosing another. [S: 9to5Google 13 Aug 2021] The leftmost slot is "the underlined leftmost suggestion ... that has always followed along what you were typing" (search snippet of an Android Police 2019 piece; the page itself returned an error). [U] A typed word shown in quotation marks on the left: **not found in any source**. [U] | mixed |
| **SwiftKey** | Three options; "press the spacebar to insert the middle prediction"; to keep an unlearned word "tap the left-hand or middle prediction (depending on text field)" which holds the verbatim text. [V] | [V] |
| **iOS QuickType** | The current spelling of the word being typed is the left entry (tutorial); exact 3-slot arrangement and quotation marks: not verified. [U] | [U] |
| **Samsung, FUTO** | FUTO docs do not describe the slots. Samsung: nothing found. [U] | [U] |

### 2.2 After a glide / swipe (question a)

- **SwiftKey Flow**: since the 2017 beta, "When Flowing, we will show you three possible predictions instead of auto inserting the word"; the main one is the middle; if you keep flowing or tapping without choosing, the middle one is inserted automatically; tapping another replaces it. So after a SwiftKey glide the strip shows **alternatives for the swiped word**, best in the middle. [V: Android Police 2017]
- **AOSP / HeliBoard** (closed glide library): the batch input styles omit the typed word and put the best candidate in the centre with the other candidates fanned out. [V for the layout; the exact lifecycle (when the strip switches to next words) is from memory, not re-checked: U]
- **Gboard**: no source found that says whether, after a glide, the strip shows alternatives of the swiped word or next-word predictions, or both. Tap-to-replace behaviour: not documented in anything found. [U]
- **No keyboard found mixes the two jobs** (alternatives for the word just glided on the sides and next-word guesses in the centre). The owner's idea is therefore a design of our own, not "the same as other keyboards". Everything below treats it as such. It does not contradict any verified convention, but it changes what the centre slot means (see 4.2).

### 2.3 After a space with nothing typed, and while typing a prefix (questions b, c)

- Next-word list (no typed word): centre = highest-probability prediction, left = second, right = third. [V: Gboard paper. For AOSP this is my reading of the layout code (best to the centre, second to the left, the rest alternating outward), not a separately observed behaviour: treat as S/U]
- While typing a prefix: centre = best completion or the auto-correction (the one that Space/continued typing will apply); left = the literal typed word (or the second best); right = the next completion. [V for AOSP and SwiftKey (spacebar inserts the middle); S for Gboard]
- This "centre = what Space will accept" convention is the strongest agreement across Gboard, SwiftKey and AOSP.

### 2.4 How next words are ranked and how big the models are (questions d, e)

| Keyboard | Engine | Label |
|---|---|---|
| **Gboard** | Next-word prediction moved from a word n-gram FST (Katz-smoothed 5-gram, 1.25 M n-grams) to a CIFG LSTM: 10,000-word vocabulary, 1.4 M parameters, 1.4 MB shipped; trained federated on-device. Server-log recall top-1 / top-3: 16.4 % / 27.0 % (neural) vs 13.0 % / 22.1 % (n-gram). Live strip recall top-1 / top-3: 5.82 % / 13.75 % vs 5.24 % / 11.05 %; click-through 2.35 % vs 2.13 %. Later work keeps the same architecture (one 670-unit layer, 96-d embeddings) with differential privacy. | [V] |
| **Gboard "smart completion" / Smart Compose** | Whole-sentence completion shown "right next to whatever you're typing", accepted by a swipe right; English, US, a list of messaging apps; whether it runs on device: not found. | [S]/[U] |
| **SwiftKey** | n-gram engine that read the last two words (secondary article), replaced in 2016 by an on-device neural network; "double-word prediction" (two-word suggestions, only when it makes sense) added in SwiftKey 6.0 (2015). | [S] |
| **FUTO** | Optional "Transformer LM" predicts the next word and improves autocorrect (English only); optional on-device fine-tuning from what you type while the phone idles; a community Portuguese model is about 40 MB quantized (a ~42 M parameter model). FUTO Swipe ships a small "context LM" of about 1.5 M parameters / 6.25 MB that supplies a re-rank score and next-word prediction (English only). | [V] |
| **AOSP / HeliBoard** | Dictionary with bigram data plus user history; details of recency weighting not re-checked. | [U] |
| **iOS** | Context-sensitive and learns the person; internals not verified. | [S] |

Multi-word / sentence prediction on a small footprint: the shipped evidence is (1) SwiftKey's two-word chip, (2) Gboard's inline sentence completion (server-side or not unknown), (3) FUTO's 6-40 MB models. A 1.4 MB / 1.4 M-parameter LSTM is the smallest proven shipped neural next-word model, and it only moved top-1 recall by 3.4 points over a good n-gram. For Swoop (83 k words with word pairs, personal pairs and triples, a 2 GB tablet) a neural model buys little and costs size and startup time. Realistic expectation to set with the owner: even Gboard's best model has the right word in the centre about 1 time in 6 on logs (top-1 16.4 %) and in the strip's three slots about 1 time in 4 (27 %); live, with real users and zero-state events, it is lower (5.8 % / 13.8 %). A next-word slot will often be wrong; it must be cheap to ignore.

## 3. Findings that matter for Swoop

1. The clipboard chip belongs in the strip only while nothing has been typed; it is a one-shot (tap = paste and done), time-limited (HeliBoard's 3 minutes is the only number in code; Gboard's chip time is not documented) and removed by typing.
2. The 1-hour rule is for the clipboard history. Swoop already has that (`ClipboardStore`, one hour, 30 items, pins) and it matches Gboard's.
3. The IME exemption makes the read free on Android 10 and 16: no toast, no permission, no focus needed. Locked device returns nothing.
4. Sensitive clips and password fields are masked, not hidden, in Gboard and HeliBoard.
5. The centre slot means "best / what Space or the next tap will take" in Gboard, SwiftKey and AOSP. Left and right are the runner-up and the rest.
6. Nobody was found that puts next-word guesses beside glide alternatives; it is our own design.

## 4. Recommended design for Swoop's 3-box strip

Facts about our code that the design uses (read from the repo, not changed here): `KeyboardPresenter.refreshSuggestions()` decides what the strip shows; with nothing typed it asks the engine for next-word guesses (`startsWord`), after a glide the strip shows the glide's candidates (`controller.lastGlide != null`), `KeysView.showCandidates(words, accent)` draws up to three equal-width chips in list order with an optional outlined accent chip, and `ClipboardHost` already reads the clipboard (skipping sensitive clips for the history).

### 4.1 Clipboard chip (request 1)

State rule: **one extra chip, the first (leftmost) one, offered only in the neutral state** (nothing typed since the last space or the start of the box, no glide pending). That matches the owner's words ("the first suggested word") and every keyboard above (neutral strip only).

| Question | Rule for Swoop |
|---|---|
| Eligible clip | Text clip (MIME `text/plain` or `text/html` coerced to text), not blank, age at most **3 minutes** by `ClipDescription.getTimestamp()` (fall back to the time our clip listener saw it), at most 1000 characters (display is cut, the paste is the full text). One constant, easy to change. 3 minutes is HeliBoard's verified value; Gboard's is not documented. |
| Read cost | Check `primaryClipDescription` first (MIME, `android.content.extra.IS_SENSITIVE`, timestamp); read the text only when the chip will really be drawn; cache by timestamp so one clip is read once. Runs on the UI thread, microseconds; nothing allocated on the tap path. |
| Appearance | Leftmost chip, a small clipboard glyph (Canvas-drawn, no new asset) then the text with a single line and ellipsis in the middle (like AOSP's middle-ellipsize, but not squeezing below 70 % scale), accent outline so it reads as different from word guesses. The other two chips stay the usual next-word guesses (no gap). |
| Tap | Pastes the full clip (the existing `pasteText`), marks this clip used, strip returns to next-word guesses. |
| Goes away when | (1) tapped; (2) a long-press on the chip or its dismiss gesture (no extra buttons on a small strip: long-press to dismiss is enough); (3) the person types, glides or deletes anything in this input session (Gboard's "only typing dismisses it"); (4) 3 minutes pass; (5) a different text field opens after the clip is already used or dismissed (it was consumed). It is allowed to survive a field change when unused and younger than 3 minutes (the usual flow is copy in app A, open the reply box in app B). |
| Shown again | Not for a clip that was used or dismissed. A new copy (new timestamp, even the same text) offers a new chip. |
| Sensitive clip or password field | The text is shown as dots of the same length (HeliBoard) and is still pasteable (the main legitimate flow is pasting a password just copied from a password manager). Never logged, never added to the history (the existing rule: sensitive clips are skipped), never persisted by the chip. In a field with `IME_FLAG_NO_PERSONALIZED_LEARNING` the clip is not captured into history but the transient chip may still show masked. Note for the owner/brief: the "no clipboard capture in password fields" rule in CLAUDE.md is about history capture; the chip is in-memory and masked, which is consistent with it. Confirm if the owner wants no chip at all in password fields. |
| Off switch | Settings toggle "Offer the last copied text" (default on, because the owner asked); off also when `safeMode`. |
| Not in v1 | Splitting a long clip into phone/link/address chips (Gboard 2021); image clips; a second clip chip (only the newest; older ones are in the clipboard panel). |

### 4.2 Strip states

Slots are L (left), C (centre), R (right). "Alt" = another glide guess whose shape is close; "next" = next-word guess from bigram/trigram + personal pairs.

| State | L | C | R | Notes |
|---|---|---|---|---|
| Neutral, fresh unused clip | clipboard chip | next 1 | next 2 | request 1. Falls back to the next row when there is no clip. |
| Neutral, no clip (after a space, or start of box) | next 2 | next 1 (best) | next 3 | centre = best, as in Gboard / SwiftKey / AOSP. If the engine has fewer than 3, leave the sides empty rather than fill with noise. (Today the list is drawn best-first from the left; switching to centre-best is a one-line reorder in `applySuggestions`, plus the accent chip index.) |
| Typing a prefix, word unknown | fix or the literal in quotes | best completion (the one Space applies) | next completion | keep today's quotes on the literal; move the auto-fix to the centre so the centre always means "what Space will apply". Optional follow-up, not needed for the two requests. |
| **After a glide** (request 2) | alt 1 | **next 1** (next-word guess, given the glided word as context) | alt 2 | the glided word is already in the text, so it does not need a slot. L and R replace it when tapped (as today), C appends a new word plus a space and then the strip shows the next guesses after that word (type ahead by tapping the centre). Give C a different look (accent outline, which `KeysView` already supports) so it is not mistaken for a replacement. |
| After a glide, weak glide alternatives (shape distance above a cutoff, or only one alt) | alt 1 | next 1 | next 2 | never show a junk alternative just to fill a slot; use the free slot for the next guess. |
| After a glide, no next-word guess above a probability floor | alt 1 | alt 2 | alt 3 | today's behaviour. |
| Replace mode (tap in an existing word, R18a) | unchanged | unchanged | unchanged | not touched by this note. |

Reasons for choosing the owner's layout despite no keyboard doing it: it costs nothing on the verified conventions (centre = "most likely thing to tap next" still holds, because after a glide the glided word is no longer pending), it uses data the engine already computes (`glide.engine.suggest("", p1, p2, 3)` for the next word, the glide session for alternatives), and it speeds typing. The risk is a mistaken tap on C when the person meant to correct the word; hence the distinct look for C and a setting "After a glide: guess the next word in the middle" (default on) that falls back to the SwiftKey-style three-alternatives row.

### 4.3 What not to build now

- A neural next-word model or any sentence completion. Evidence: the smallest shipped model (1.4 MB LSTM) gains 3.4 points top-1 over an n-gram; FUTO's are 6-40 MB. Swoop's n-gram plus personal pairs and triples is the right size. If multi-word is wanted later, chain the top bigram/trigram greedily for two words and show it only when both steps are above a probability floor (SwiftKey's "two-word predictions, only when it makes sense").
- Chip text extraction (phone, link, address), clipboard images, sync.

### 4.4 Tests to write with the feature (JVM, no emulator)

1. A fake clipboard: clip age 2:59 shows, 3:01 does not; sensitive flag gives dots; a password field gives dots and does not enter the history.
2. Chip is consumed by tap, by long-press, by typing; a new timestamp brings it back; a field change keeps an unused young clip.
3. Strip state table above, row by row, through `KeyboardPresenter` with a fake engine (centre-best order, glide row, weak-alt row, floor row).
4. A view test that the chip ellipsizes to its width and the strip never grows taller (the 0.2.0-alpha1 IME height bug).
5. Honest limit to tell the owner: none of this was run on the tablet or an emulator; the read exemption is from platform source, and the Android 10 Go build on the tablet could differ.

## 5. Unverified items and what to test on the tablet

- Gboard's own chip time limit (not documented; the one-hour figure is for the history) and whether Gboard shows the chip again after typing.
- What Gboard's strip shows after a glide and whether tapping the swiped word reopens alternatives (not documented in anything found).
- Whether a typed word is shown in quotes in Gboard (our app does it; no source found for Gboard).
- Samsung Keyboard, Fleksy, AnySoftKeyboard chip behaviour (no usable sources).
- iOS 27's exact timing (articles do not say).
- Whether the tablet's Android 10 build lets the IME read a clip copied in another app while the keyboard window is hidden (platform code says yes; the listener already fires on the tablet's builds per the existing ClipboardHost comment "some Android versions refuse the read when no window has the focus": test with the diagnostics counter).

## 6. Sources

Android platform and documentation
- https://developer.android.com/about/versions/10/privacy/changes (Limited access to clipboard data)
- https://developer.android.com/develop/ui/views/touch-and-input/copy-paste (pasted-from toast, sensitive flag, Android 13 UI)
- https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling (EXTRA_IS_SENSITIVE, Android 12 toast, Android 13 auto-clear)
- https://developer.android.com/reference/android/content/ClipDescription (API levels: getExtras 24, getTimestamp 26, EXTRA_IS_SENSITIVE 33)
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-10.0.0_r1/services/core/java/com/android/server/clipboard/ClipboardService.java
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-16.0.0_r1/services/core/java/com/android/server/clipboard/ClipboardService.java
- https://9to5google.com/2022/08/16/android-13-clipboard/ (one-hour auto-clear, secondary)

Gboard
- https://www.androidheadlines.com/2020/04/google-gboard-clipboard-paste-suggestions.html (2020 test: top bar, cut/copy, passwords masked, one hour)
- https://9to5google.com/2020/05/20/gboard-android-clipboard-suggestions/ (replaces toolbar icons; dots in password fields)
- https://mobilesyrup.com/2020/04/23/gboard-testing-streamlined-copy-paste-feature/
- https://www.androidpolice.com/2021/07/26/gboards-clipboard-suggestions-are-getting-smarter-and-smarter/ (extracted phone/link/address chips)
- https://www.xda-developers.com/gboard-prepares-scrolling-paste-bar-clipboard/ (single suggestion in the middle)
- https://www.androidauthority.com/gboard-clipboard-duration-slider-apk-teardown-3698099/ (13 Aug 2026: one-hour default, 1-24 h slider)
- https://www.androidauthority.com/gboard-clipboard-suggestions-dismiss-button-apk-teardown-3699618/ (18 Aug 2026: only typing dismisses; cross icon in testing)
- https://9to5google.com/2021/08/13/gboard-suggestion-highlight/ (middle slot highlighted, applied if you keep typing)
- https://ar5iv.arxiv.org/html/1811.03604 (Hard et al., federated next-word model, centre slot rule, model size, recall)
- https://support.google.com/gboard/answer/7068415 and https://www.xda-developers.com/gboard-rolls-out-smart-completion-and-undo-autocorrect-on-backspace-tests-new-google-assistant-dictation (Smart Compose / smart completion)

SwiftKey
- https://support.microsoft.com/en-au/swiftkey-keyboard/how-to-use-microsoft-swiftkey-keyboard-to-copy-and-paste-text-between-swiftkey-and-windows (last clip on the prediction bar, 1 hour)
- https://www.androidcentral.com/swiftkey-now-lets-you-sync-your-clipboard-across-android-and-windows (history kept an hour, pinned excepted)
- https://support.microsoft.com/en-US/swiftkey-keyboard/how-does-the-microsoft-swiftkey-prediction-bar-work (three options, spacebar inserts the middle, verbatim slot)
- https://www.androidpolice.com/2017/03/16/swiftkey-beta-improves-flow-experience-3-possible-predictions/ (Flow shows three predictions, middle inserted automatically)
- https://www.phonearena.com/news/SwiftKey-6.0-adds-Double-Word-Prediction-and-more_id75013 (two-word prediction; search snippet only)

HeliBoard, AOSP LatinIME, FUTO, others
- https://raw.githubusercontent.com/Helium314/HeliBoard/main/app/src/main/java/helium314/keyboard/latin/ClipboardHistoryManager.kt (3-minute window, 200 characters, masking, dismissal)
- https://raw.githubusercontent.com/Helium314/HeliBoard/main/app/src/main/java/helium314/keyboard/latin/LatinIME.java (clipboard suggestion tried first when the strip is neutral)
- https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/master/java/src/com/android/inputmethod/latin/suggestions/SuggestionStripLayoutHelper.java (slot layout, 40/30/30 widths; fetched as base64 text)
- https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/master/java/src/com/android/inputmethod/latin/SuggestedWords.java (typed word index 0, KIND_PREDICTION)
- https://newreleases.io/project/github/futo-org/android-keyboard/release/0.1.25 and https://newreleases.io/project/github/futo-org/android-keyboard/release/0.1.25-rc1 (FUTO suggestion-bar quick paste, clipboard settings)
- https://docs.keyboard.futo.tech/print (FUTO Transformer LM, personalisation)
- https://huggingface.co/futo-org/swipe (FUTO Swipe context LM 1.5 M parameters, 6.25 MB, next-word and rerank)
- https://huggingface.co/barddoo/futo-keyboard-ptbr (community model size, search snippet)
- https://www.apkmirror.com/apk/anysoftkeyboard/anysoftkeyboard-2/anysoftkeyboard-2-1-10-1794-release/ (AnySoftKeyboard "Suggesting recent clipboard entry", snippet only)

iOS
- https://9to5mac.com/2026/09/22/ios-27-adds-convenient-new-iphone-feature-youll-use-all-the-time/ and https://appleworld.today/2026/10/how-to-use-the-new-copy-and-paste-feature-in-ios-27/ (iOS 27 paste suggestion)
- https://www.macrumors.com/2022/10/17/ios-16-1-paste-from-other-apps-settings/ (iOS 16 Allow Paste prompt applies to apps reading directly, not to the Paste menu)

Repo context read for this note: `research/PREDICTION-SOURCES.md`, `research/KEYBOARD-APP-PLAN.md` (sections 10.2, 9.4), `ime/src/main/kotlin/app/swoop/keyboard/ClipboardHost.kt`, `core/src/main/kotlin/app/kestrel/core/clipboard/ClipboardStore.kt`, `KeyboardPresenter.refreshSuggestions` / `applySuggestions`, `KeysView.showCandidates`.
