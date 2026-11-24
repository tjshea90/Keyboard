# The suggestion strip: copied text first, next word after a glide (R20a, R20b - owner 2026-11-18)

Research: `research/R20-clipboard-and-strip.md` (what Gboard, SwiftKey, HeliBoard, AOSP LatinIME do; sources, and what is NOT verified).

## The three boxes
| State | Left | Middle | Right |
|---|---|---|---|
| Nothing typed since the last space, a fresh copy exists | **the copy** (small clipboard, text shortened in the middle) | next guess 1 | next guess 2 |
| Nothing typed, no copy | best next word (accent outline), then the others (as before) | | |
| Typing letters | completions / typo fix / the typed word in quotes (as before) | | |
| **After a glide** | other guess of the glide (replaces the glided word) | **the predicted NEXT word** (accent outline; adds a word) | other guess of the glide |
| After a glide, no next-word prediction | the plain glide guesses, best first (as before) | | |
| While the finger is still down | the live guesses, best first (as before) | | |

* Owner R20b: "maybe in the middle box ... while the left and right boxes ... give possible words I meant to type based on glide pattern". No other keyboard was found that mixes the two (research note section 2.2), so this is our own design; the glided word is already in the text, so it needs no box, and the centre stays "the most likely thing to tap next". With only one other guess, or none, the free side box holds the next-best next word (a tap on it ALSO adds that word, it does not replace the glided one). The middle is blank (nothing drawn, no touch) for the instant the engine takes to answer, so no box jumps. Settings > "Next word after a glide" turns it off (the boxes are then the old list of glide guesses).
* The next word comes from `GlideTyping.suggest("", glidedWord, wordBefore)`: the person's own pairs and triples first, then the shipped list's bigrams (`Suggester.nextWords`). Tapping it goes through `TypingLearner.pick`, like any strip word (counts double, learns the pair and the triple); the glide before it is confirmed.

## Replace or add? (R32d, owner 2026-11-24: "I press the word and in the recommendations to fix it, sometimes it keeps ave and just adds and behind it")
* **Only the middle box adds a word.** The side boxes always REPLACE the glided word; a side box with no other guess stays empty (it used to be filled with another next word, and a tap on it added the word).
* **A word the decoder itself guessed for this glide (any of its five) is never the "next word".** The 4th and 5th guesses (not shown before) go into the side boxes when there is room, first when the language also expects them next ("and" after "ave": the commonest next word AND a look-alike). Tapping such a word anywhere on the strip replaces.
* **Safety net:** if the keyboard lost track of its glide (a late or odd selection report from the app, so the strip changed to next-word guesses) a tap on one of that glide's own five guesses within 20 s still replaces the glided word, after checking the text before the cursor is still that word (`GlideSession.replaceLookalike`).
* Tests: `KeyboardPresenterTest` R32d block (the rescue test is proven red with the fix switched off). Research: `research/R32-punctuation-suggestions.md` section 3.

## R33: a look-alike is a correction (owner 2026-11-24, after alpha11)
* A tapped word replaces the glided word when the decoder listed it OR when it is spelled like it (`SpellingNear`) and at least 4 nats (about 55 times) likelier in context (`GlideSession.looksLikeFix`): "and" for "ave", "for" for "fur". It is shown in a side box, not as the next word, and a tap on it replaces from any box and after the keyboard lost the glide (within 20 s). "are" -> "and" is not a correction ("are" is common): the next word is added.
* Learning from a fix: the meant word counts three times, the wrong first guess loses 2 uses and its recent boost, and a kept glide of a rare word no longer teaches the keyboard (research note section 7).

## E-mail boxes (R32c)
In an e-mail box (not in any other box) the strip lists the saved e-mail addresses instead of words: the best three while the box is empty, narrowing as letters are typed; a tap replaces what was typed of the address. See `research/R32-punctuation-suggestions.md` section 5.

## The copied text (R20a)
* The first (left) box, only where a word starts (nothing typed since the last space or the start of the box) and no glide is pending. Gboard / SwiftKey / HeliBoard do the same (research note 1.2).
* Freshness `CLIP_CHIP_TTL_MS` = 5 minutes (HeliBoard, the one public rule, uses 3; Gboard's chip time is not documented). The copy's age is the clip's own timestamp (`ClipboardHost.copyTime`, tolerant of both clocks Android versions use) so a copy made while the keyboard was not on screen keeps its real age.
* One tap pastes the WHOLE copy (`pasteText`); the box shows one line (line breaks squeezed, shortened in the middle). A tap, a long press, any typed key, Backspace or a glide dismisses that copy (not offered again); a NEW copy, even of the same text, brings it back. Text typed around it never changes the copy.
* A copy marked sensitive (`android.content.extra.IS_SENSITIVE`, set by password managers) and every copy shown in a password box appear as dots of the same length and still paste (the usual way to fill a password); a sensitive copy is never written to the clipboard history. The copy is kept in memory only unless Clipboard history is on.
* Works with suggestions switched off (the strip then shows only the copy). Settings > "Suggest copied text" turns it off. Android lets the default keyboard read the clipboard (Android 10 and 16) without the "pasted from" notice.
* Tests: `KeyboardPresenterTest` (R20a / R20b blocks), `ClipboardHostTest`. NOT run on the tablet or the phone.
