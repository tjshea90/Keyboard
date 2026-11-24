# R32 - punctuation, digit long-press, e-mail suggestions, strip corrections, rare words

Owner request 2026-11-24 (TASKS.md R32a-e). Written the moment the findings existed; updated as each part lands.

## 1. How Gboard-family keyboards treat spaces and punctuation (R32a)

Gboard is closed. The keyboard it descends from, AOSP LatinIME, and its maintained fork HeliBoard are open, so the logic below is read from their
`InputLogic` source (fetched 2026-11-24 from the HeliBoard `main` branch and the LineageOS LatinIME `lineage-21.0` branch). **What was read and what was not:**
the first ~40k characters of each `InputLogic.java` came back verbatim, the rest as a summary; the resource strings that define the exact punctuation character
sets (`symbols_preceded_by_space`, `symbols_followed_by_space`, `weak_space_*`) and the digit popup tables could NOT be fetched. The character sets used in Swoop
are therefore our own judgement (safe subsets, see the table), and the digit popups follow Gboard's well-known fractions and superscripts, not a copied table.

### The model: a space has a STATE
| state | set when | what it means for the next key |
|---|---|---|
| WEAK | a space the keyboard added (after a picked suggestion) **and also a space the person typed** | a closing mark typed now takes the space's place |
| PHANTOM | after a picked suggestion, a glide word, or sentence punctuation | the space is not typed yet; it is added when the next LETTER arrives |
| SWAP_PUNCTUATION | a swap just happened | Backspace reverts the swap |
| DOUBLE | double space turned into ". " | Backspace reverts it |

Rules read from the code:
1. **Closing punctuation on a weak space swaps** ("word" + space + "," -> "word, "): if the mark is not "usually preceded by a space" and is "usually followed by a space" the space and the mark swap; otherwise (neither) the space is just removed. A typed space counts as weak too, so this is not only for glide/suggestion spaces.
2. **Opening marks** (brackets, quotes) are "usually preceded by a space": the space stays in front of them.
3. **After . , ! ? : ;** a space is owed before the next letter (phantom), unless the text before looks like a URL.
4. **Double space -> ". "** when the character before the space is a letter, digit or closing mark, within a short time; Backspace undoes it.
5. **Backspace right after** a swap, a double-space period or a suggestion-with-space reverts that step (the typed word comes back).
6. Capitals follow sentence punctuation through the editor's cursor-caps mode.

## 2. What Swoop did before, and what changed (R32a)

Before: only a space put in by **our own glide** was swallowed by `, . ! ? : ; / .com ) ' - _ @ %` (`ownTrail`). A word taken from the strip, a next-word pick, an
automatic fix followed by space, or a space the owner's thumb typed all left "word ,". If the keyboard lost track of its own glide (late or odd selection reports
from an app), even the glide case failed - the likely source of the "sometimes" in the owner's report.

Now (`InputController.type`, tests `PunctuationSwapTest`, 17):
* **Any single space after a word, number or closing bracket is a weak space for `,` `.` `!` `?`**, whoever typed it, in plain text and search boxes, when the "space after punctuation" switch (default on) is on. Two spaces, a space after punctuation, a selection, a word being tapped, and address / e-mail / password / number boxes are left alone.
* **Smileys keep their space**: `:` `;` `)` swap only on a space the keyboard added (as before), so "hello :)" typed by hand stays.
* A space the keyboard added (glide, strip word, next-word pick) is also swallowed by the same marks, as before, now also after a strip word.
* **Backspace right after a swap puts the space back** ("hello," -> "hello "), like AOSP's revert; the swap mark is forgotten by any other key, cursor move, glide, pick or paste.
* The space AFTER the mark is added lazily by the next letter (the existing auto-space), and a full stop gives the next word its capital (existing sentence-start rule); Enter / double space / sentence capitals are unchanged.

NOT done on purpose: a space before `:` `;` is not removed when typed by hand (smileys), and no space is added after `)` / `]` (code, "f(x)").
NOT run on a tablet or phone. Whether Gboard itself swaps on a typed space is from the AOSP source (a typed space is WEAK), not from the closed app.

## 3. Strip taps: replace, never add (R32d)

Gboard shows corrections for the word just typed or swiped and next-word guesses only after the word is finished; a tap on a correction replaces the word. Swoop's strip after a glide is the owner's own design
(R20b: `[other guess] [NEXT word] [other guess]`), which mixes boxes that REPLACE the glided word with one that ADDS a word. Reproduced cause of "it keeps ave and adds and behind it":

* the decoder gives five guesses but only the first three were shown as replacements; the 4th and 5th guess ("and", the commonest word in English) was also the commonest predicted NEXT word, so it appeared in the middle box and a tap added it ("ave and ");
* a side box without another guess was filled with a next word ("filler"), and a tap on it also added;
* when the keyboard lost track of the glide (a late or foreign selection report), the strip changed to next-word guesses and a tap on the intended word added it behind the wrong one.

Fix: a word the decoder guessed is never offered as "the next word"; it goes to a side box (first when the language also expects it next); side boxes never hold fillers; and within 20 s of a glide a tap on one of its own five guesses replaces the glided word even if the bookkeeping was lost (verified against the editor first). Tests: `KeyboardPresenterTest` (3 new, the rescue one proven red with the fix switched off).

## 4. Digit long-press (R32b)

Gboard: hold a digit key and a row of special characters opens above it; slide to one and let go. Swoop now does this (`KeysView.openDigitPopup`, `ImeLayout.digitHolds`, 7 view tests):

| digit | row after the digit itself |
|---|---|
| 1 | ¹ ½ ⅓ ¼ ⅛ |
| 2 | ² ⅔ |
| 3 | ³ ¾ ⅜ |
| 4 | ⁴ |
| 5 | ⁵ ⅝ |
| 6 | ⁶ |
| 7 | ⁷ ⅞ |
| 8 | ⁸ |
| 9 | ⁹ |
| 0 | ⁰ ⁿ ∅ ° |

* Where: the number row (Settings > Layout > Number row), the digits of the symbols page, and a held top-row letter when the number row is off (the digit bubble stays; the row opens with it).
* The digit itself is the first cell (lit); lifting the finger without choosing, or anywhere outside the row, types the digit, exactly as a hold did before. Sliding onto a cell lights it; lifting types it.
* Not on a number pad (phone / number / date fields): a digit there stays a digit.
* The table is Gboard as it is commonly seen, not copied from a file: the exact tables of AOSP LatinIME / HeliBoard could not be fetched (see section 1).

## 5. E-mail addresses (R32c)

* `EmailBook` (:core, 11 tests): up to 40 addresses, ranked by use then newest, kept lower case, only plain addresses (`isAddress`), saved as `email_addresses.txt` in the app's private folder with the same atomic write + `.bak` as the other learned files. **Not part of the export file** and never logged.
* What is learned (`EmailLearner`, 10 tests): only an address TYPED key by key (`InputController.typedEmail`: the whole token is the end of what was typed; a paste, an autofill, a strip pick, a glide or a cursor move breaks it). In a text box it counts when a space, comma, semicolon or Enter ends it; in an e-mail box when the box is left or its action key is pressed. A strip pick adds one use. Nothing in password boxes or where the app asked for no personalised learning; Settings has a switch (default on) and "Forget my e-mail addresses"; "Forget what it learned" also clears them.
* What is shown: in an e-mail box (`TYPE_TEXT_VARIATION_EMAIL_ADDRESS` / `WEB_EMAIL_ADDRESS`) the strip lists the best three addresses the moment the box gets the focus, narrowing as letters are typed (`jo` -> `john@gmail.com`); after an "@" it offers the typed part with the domains this person uses. A tap replaces what was typed of the address (nothing is added behind it: the box takes no space). A saved address is also offered in a box that asked for no learning (nothing is learned there).
* NOT run on a phone or tablet; Android's own autofill, if the app offers one, is separate.

## 6. Rare words beating common ones (R32e)

**Measured first, on real swipes** (FUTO dev slice A, 6 054 usable swipes; `RealSwipeEval` with `REAL_RARE=1`): on phone-sized real thumbs the owner's words are read right: **and 96.0 %, for 96.4 %, the 99.3 %**; the wrong answers are anna, flute, threw... never "ave" or "fur". The same finding as R27a / R28a / R29: whatever makes "and" -> "ave" and "for" -> "fur" on his tablet is not in the shape of ordinary swipes, so it cannot be cured by tuning shape alone. The ln priors in the shipped list: and -3.7, for -4.5, the -3.0, **ave -11.2, fur -11.3**, access -8.9, fir -12.7: "ave" and "fur" are about 7 nats (1 000 times) rarer than the words meant.

**What was built:** `CommonGuard`, the last step of the glide pipeline (after the decoder and the trained shape network): when the first guess is at least **5 nats** (about 150 times) less probable than another of the five guesses and that guess trails by **at most 3 score units**, the common word goes first. "Rare" is judged on the contextual prior the decoder already computed, which includes what this person typed and glided (a rare word they really use is not rare); the rare word stays in the strip. Diagnostics shows "a much commoner word put before a rare first guess N times".

**Why these numbers** (the whole shipped pipeline, 6 053 swipes, slice A): first choice 92.57 % (all words) / 94.54 % (everyday words, the 5 000 commonest). Flips / mended / spoiled for the grid:

| gap G, margin M | all words: flips +mended -spoiled (net) | everyday words |
|---|---|---|
| 5, 2 | 10 +8 -2 (**+6**) | 10 +8 -1 (+7) |
| **5, 3 (chosen)** | 21 +13 -5 (**+8**) | 17 +13 -2 (**+11**) |
| 5, 5 | 41 +14 -19 (-5) | 25 +14 -7 (+7) |
| 6, 3 | 11 +7 -3 (+4) | 8 +7 -1 (+6) |
| 4, 3 | 36 +15 -10 (+5) | 26 +15 -5 (+10) |

A bigger margin spoils words that really are rare (the corpus is encyclopaedia text full of names: Maggie, Anna), a smaller one does nothing. 5 / 3 is the point where both the all-words and the everyday-words net are positive and largest. "and": 96.7 -> 97.4 % first choice. **Held-out check** (slice B, other donors, 2 303 swipes, first choice 92.36 % / everyday 94.68 %, the setting chosen on slice A only): G=5, M=3 gives all words 4 flips +2 -1 (net +1), everyday words 3 flips +2 -0 (net +2); a margin of 5 or more turns negative on all words (-4), so 3 stays. Small, as expected on phone swipes that are already read well, and never harmful on everyday words.

**Honest limits:** the effect on phone swipes is small (+0.1 to +0.2 points) because the pipeline is already good there; the guard is insurance for the case the owner reports (a tablet where the shape fits a look-alike about as well as the real word). It cannot help when the shape alone puts the rare word clearly ahead (more than 3 units), and it needs his next Diagnostics line to be judged on the device. The tap path already prices rare words (`Suggester.RARE_W`, `DEEP_TAIL`); nothing changed there.

## 7. R33 - "still typing ave instead of and, and tapping and adds it after ave" (owner 2026-11-24, after alpha11)

Two separate causes, both found by reproducing, not by guessing.

**7.1 Why the alpha11 strip fix did not help.** It covered a word that the decoder itself listed among its five readings of the glide. A sloppy glide that reads "ave" often does NOT list "and" at all, and "and" is the commonest next word after anything, so it stands in the middle (next-word) box, where a tap adds it. Reproduced in `KeyboardPresenterTest` with a word list in the real list's proportions: after an "ave" glide the strip was `[ace, and, age]`, "and" in the middle, a tap gave "ave and ". Fix (`GlideSession.looksLikeFix`): a tapped word is a CORRECTION of the glided word when the decoder listed it, or when it is spelled like it (`SpellingNear`: 3+ letters, within one letter in length, within 2 edits, the same first or last letter) and is at least 4 nats (about 55 times) likelier in context (`GlideTyping.contextLn`, the strip's own score). Such a word is shown among the replacements (a side box), never as "the next word"; a tap on it replaces whichever box it stands in; the lost-glide rescue uses the same rule. "ave" -> "and" qualifies (2 letters, ln -11.2 vs -3.7), "are" -> "and" does not ("are" is common): the next word is still added. Diagnostics counts strip taps after a glide: replaced (how many because of this rule, how many after the keyboard lost the glide) and added.

**7.2 Why "ave" wins so often (the loop).** The personal model (`PersonalModel`) says: one use of a word is worth 20 million occurrences of the list, "about as likely as the 10 000th commonest word; ten uses the 1 500th". A glide that is merely KEPT counted as a full use. "ave" is 1 in 70 000 words (ln -11.2). Measured in `KeptRareLoopLabTest` on the real list, tablet geometry: a path 65 % of the way from a clean "and" to a clean "ave" gives "and" with nothing learned, and **one kept use of "ave" flips it to "ave"**. A correction that failed (and added behind ave) was one more kept "ave" each time, so the wrong word fed itself; the same loop fits access for and, fur for for, Maggie for make, our / out for or (R29). It is also why the phone-swipe tests (no personal model) never showed it. Also: `PersonalModel.add` gave the word a full "recent use" point whatever the lesson's weight, so even R29's light lessons boosted it.

Fix:
* a glide that was only KEPT (not picked, not retyped, not revised) teaches a rare list word only `GlideTyping.keepTrust` of a lesson: 0.1 at ln -11.5 or lower (ave, fur, maggie), rising to 1 at -8.5 and above; below 0.5 it also teaches no glide shape and no thumb habit;
* the recent-use point scales with the lesson's weight (`PersonalModel.add`);
* picking another word over a wrong first guess takes 2 uses of the wrong word back (and its recent boost); `unlearn` clears the recent boost;
* (tried and REMOVED: a ceiling of 3 uses' worth of boost for words the list finds very rare. It also held back rare words the person really TYPES many times, which three older tests, rightly, require to win; legacy damage is healed by corrections instead.)
* tests: `KeptRareWordTest` (the old lesson flips the halfway path, one kept glide now does not; ten old-style kept uses make it "ave" and 1 to 4 working corrections make it "and" again), `PersonalModelTest` (2), `KeyboardPresenterTest` R33 block (5).

**Not proven:** whether this is what happens on his tablet (no device here; the halfway path is a synthetic stand-in for his sloppy "and"); a rare word he truly means ten times in a row with no corrections is learned slowly on purpose; the damage already in his learned memory is not removed automatically: each correction now takes 2 uses of the wrong word back (about 5 corrections clear 10 old uses), and Settings > Forget what it learned removes everything at once.
