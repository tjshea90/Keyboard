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

## 4. Digit long-press (R32b), 5. E-mail suggestions (R32c), 6. Rare words (R32e)
(filled in below as each lands)
