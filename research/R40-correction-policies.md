# R40 - how popular keyboards treat the correction of words, and what Swoop changed (owner 2026-11-28)

Owner, 2026-11-28, his words: "Sometimes the keyboard corrects a prior word to a rare word that makes no sense in context. There should be weights so that words like and make the are not replaced with words like
ave age access Maggie. Do further research on how popular keyboards treat correction of words. Sometimes i glide a word and the keyboard gets it wrong so i delete half the word and press the correct word which is
suggested, but it leaves the half typed word and simply adds the correct word to it. Just now when i typed "to it" the keyboard retroactively changed it to "yo it". That is ridiculous. Fix everything, improve the keyboard
as much as possible".

Written as the findings came in. Public sources only; **no Gboard / SwiftKey / Apple code or weights are used or copied** (closed, and this repository is public). Every fact below says where it came from and how sure it is.

## 1. What was read, and what was NOT readable

| Keyboard | Source actually read (2026-11-28) | How sure |
| :- | :- | :- |
| Gboard | Google Research blog "The Machine Intelligence Behind Gboard" (2017); Sivek and Riley, "Spatial model personalization in Gboard", Proc. ACM HCI 6 (2022), arXiv 2209.11311, pages 1-4 read by me; the 2017 FST paper (arXiv 1704.03987) was fetched but NOT read | published statements only; the autocorrection module itself is unpublished |
| AOSP LatinIME / HeliBoard | HeliBoard `Suggest.kt` (`latin/suggestions/Suggest.kt` on `main`, 590 lines) read by a helper and summarised to me; AOSP / FUTO / OmniROM commit history of the "safety net" found by search | code facts: good (but second-hand summary); the threshold values live in other files that were NOT read |
| Microsoft SwiftKey | Two Microsoft support pages (prediction bar, personalisation) | what the pages say; they are silent on internals |
| Apple iOS 17 | Press and review coverage (MacRumors, MacStories, 9to5Mac, CBS, The Register) of the WWDC 2023 announcement | secondary, launch-time only |
| FUTO Keyboard | keyboard.futo.tech landing page, docs landing page | marketing text only; its source is on gitlab.futo.org (not read) |
| AnySoftKeyboard | Wiki "Quick Start" and "FAQ" | settings names only |
| FlorisBoard, Fleksy | NOT researched this session | - |

Not found anywhere public: how Gboard decides to autocorrect (thresholds), and whether any keyboard rewrites the PREVIOUS word after the next one is typed.

## 2. What each one does (facts, each with its source)

**Gboard** (Sivek and Riley 2022, section 1-2; Google Research blog 2017)
* A typed word is decoded as a noisy channel: the best word is `argmax log p(touches | w) + log p(w | words before)`, the sum of a SPATIAL model score and a LANGUAGE model score, over a beam search of finite-state transducers. Only the word being typed is decoded, with the words before it as context.
* "Newer work since then includes modules that control auto-correction decisions through more than just raw language and spatial model scores, and a key correction model": so a separate module decides whether to replace, using more than the raw scores. Its details are not published.
* The 2017 blog: neural spatial models "showed about 15% reduction in bad autocorrects and 10% reduction in wrongly decoded gestures"; "reverted auto-corrections and suggestion picks" are the negative and positive training signals. So Gboard treats a Backspace that undoes a fix as proof the fix was wrong.
* Nothing published says Gboard rewrites the previous word once the next word is typed.

**AOSP LatinIME family (HeliBoard `Suggest.kt`; older AOSP via commit history)**
* A typed word that is NOT in the dictionary (and longer than one letter) may be replaced (`typedWordInfo?.sourceDict == null`). A typed word that IS valid is replaced only when the best suggestion beats it by a wide margin: the code requires a score gap of 20 (best suggestion plus a bonus of up to 30 against the typed word plus 20), and the scores come from the context-aware (next-word) lookup. So: valid words are protected by a margin, not absolutely.
* Never autocorrect: a word with digits; a mostly-capitals word; text that is not being composed (prediction, or an old word the person went back into: "resumed"); with no main dictionary; with correction switched off. A capitalised word (a probable name) is corrected only with a special setting.
* After an autocorrection the typed word stays on the strip as an option.
* The "safety net" (AOSP): when the suggestion is too far from the typed word, auto-correction is blocked and the typed word is kept; it is skipped in the aggressive mode and only runs when more than one suggestion exists. (From commit titles and summaries only; the exact edit-distance rule was not read.)
* The aggressiveness is a user setting (modest / aggressive / very aggressive in AOSP; "Auto-select suggestion aggressiveness" in AnySoftKeyboard).

**Microsoft SwiftKey** (support pages)
* With Autocorrect on, a space inserts the middle prediction; for a word it does not know that slot holds the typed text. It learns "as you type"; a learned word is removed with a long press on the strip. The page describes fixing an earlier word by hand only (tap the word, Backspace into it, pick the prediction): no automatic rewrite of the previous word.

**Apple iOS 17** (press coverage of the WWDC announcement)
* A transformer language model on the device; corrections can use the whole sentence; a corrected word is underlined for a moment and a tap shows the word as typed so it can be put back; the keyboard stopped "correcting" a common swear word. This is the one family that does change earlier text, and it shows the change on screen and makes it reversible.

**FUTO Keyboard, AnySoftKeyboard**: only feature names and settings were found; nothing on how a valid word is protected.

## 3. What they agree on (the principles Swoop is measured against)

1. **A word the person typed that is a real word is protected.** It is replaced only by a clearly better word, and the clearer the original is, the more evidence it takes (AOSP margin; Gboard's separate correction module).
2. **The language prior always counts at full strength.** A rare word does not win because one touch looked a little closer (Gboard: `+ log p(w | context)`, no discount).
3. **No automatic change to text the person already saw without a way back.** Backspace right after a fix puts the typed word back, and that undo is a LEARNING signal (Gboard) so the keyboard gets stricter.
4. **Some things are never corrected automatically:** digits, capitals-only words, names, old words the person went back to edit.
5. **The retroactive rewrite of an earlier word is rare** (only the iOS family, and there it is visible and reversible). Where it exists it needs more evidence than a first decision.

## 4. Swoop before R40, against those principles

| Principle | Swoop 0.7.0-alpha17 | Verdict |
| :- | :- | :- |
| 1 protect real words | `Suggester`: a typed word in the list (or used twice) is "known" and never fixed on a space; contraction rule needs 3 nats (20x) | good |
| 2 prior at full strength | decoder tempers the prior with the "lm" weight (0.45 and up); `CommonGuard` (R32e) moves a much commoner reading first only for a 6 nat gap | partly: fine for the first guess |
| 3 undo + learn | Backspace after a fix restores the word, unlearns the fix, the person's word counts twice, `autoMarginShift` makes the keyboard stricter | good |
| 4 never correct | digits excluded; capitals-only and names NOT excluded | gap (see section 6) |
| 5 retroactive rewrite | `GlideSession.revisePrevious` (R26a): after the NEXT glided word the earlier word is swapped for another of its 5 readings when the next word fits it better by 1.0 nat; **no frequency weight at all**; a swapped-in word was then learned at FULL trust | **the bug the owner saw** |

## 5. What was found and fixed (R40-1, R40-3, R40-4)

### 5.1 "to it" -> "yo it": the retroactive swap had no weights (R40-1 / R40-4)
* Reproduced end to end (CommonWordGuardTest): glide "to" (readings to / yo, the thumb between t and y), glide "it"; a rare look-alike with ONE known follower scores about 4 nats better "as the word before 'it'" than "to" does, because the list (or the person's own pairs) holds "yo it" and "to it" is a common but diluted pair. It beat a word thousands of times commoner. The shipped list alone does not do this for "to / yo" (no "yo it" pair, 0 flips in 300 simulated sessions); it needs a learned pair, which is how a loop starts: a wrong swap was learned at full trust, which made the next wrong swap more likely.
* The fix is the WEIGHT RULE `CorrectionGuard` (core): an automatic change old -> new is allowed when (A) new is not more than an allowance rarer than old, the allowance growing as old gets rarer (1.5 nats for an everyday word like to / and / the, 1.85 for make, 2.75 for age, 4.5 for a rare word), or (B) the shipped list of 243 000 word pairs proves it: the pair (new, next) is at least 5 nats stronger than (old, next) ("there are" -1.5 against "the are" -11.5) and new is at most 5.5 nats rarer. Only the shipped list counts for B: a pair the person made once or the class model guessed is never proof. The weights are the same ln-shares the glide decoder and the strip use (`GlideTyping.contextLn`, which includes what the person writes).
* Every one of the owner's pairs is refused: to -> yo, and -> ave, make -> Maggie, for -> fur, and -> access, and -> age (unit tests on the real list's numbers: `CorrectionGuardTest`).
* A word the next glide swapped in is now learned like a word that was only kept (lightly when it is rare), not at full trust: no loop.
* Measured on 3 011 real swipes (FUTO Swipe, the R26a replay `REAL_POST=1`, margin 1.0): unguarded 33 changes, 22 mended, 5 spoiled (+0.56 points); rule A alone 25 / 15 / 5 (+0.33); **rule A + B 33 / 22 / 5 (+0.56): the guard costs nothing on real swipes** while it blocks the sparse-pair class that real swipes cannot show (they carry no learned pairs). Rule B is what keeps "the -> there (before are / were)", "he -> grew (up)", "work -> serial (number)". Strength sweep of B (3, 5, 7 nats) and of rule A (1.0, 1.5, 2.5): flat from 3 to 5, B = 7 loses one mended word.
* The 5 spoiled words of the replay are all swaps toward a COMMONER but wrong word between rare words of an encyclopaedia text; none involves a rare replacement of a common word, so no weight rule can touch them.

### 5.2 "delete half the word, tap the suggestion: the half word stays" (R40-3)
* Root cause found by a seeded fuzz through the real presenter, keys view, glide engine and a real EditText (`StripPickFuzzTest`, mutation-checked: without the fix seed 98 gives "IwoIso", the half-typed "Iwo" glued in front of the picked "Iso"): **apps end the composing word on their own** (a page script, a React Native / Flutter / Compose field that re-sets its state) while the cursor stays. Android tells the keyboard in the next selection report (composing region -1..-1), but the keyboard ignored the composing region of a report with an unchanged selection ("usually only the composing region changed"). It went on believing the half word was still composing; replacing a composing word is `setComposingText`, which with NO composing region in the editor INSERTS beside the letters. Backspace (also `setComposingText`) and the automatic fix had the same hole.
* Fix (core `InputController`): a report that shows no composing word where one was believed, with the cursor where expected and no own edit outstanding, makes the word plain text (`composingEndedByApp`; the next edit closes whatever the editor may still hold, so a wrong report costs an underline, never text); `pickWord` replaces the WHOLE word before the cursor when only its tail was composing. Counter in Diagnostics ("word ended by the app N"). Tests: core `ComposingDroppedTest` (4), `InputControllerTest` (2), presenter tests (2), `StripPickFuzzTest`.

## 6. Still open / next (kept honest)
* NOT run on the tablet or the phone; Robolectric, a fake editor and the real word list only.
* Gap against principle 4: a capitals-only typed word and a capitalised word in the middle of a sentence (a probable name) can still be auto-fixed on a space. AOSP never does. To measure and close (R40-5).
* The typed "yo" (a real, rare word one neighbour key from "to") is "known" and never fixed, in any context. AOSP would replace it if the context made "to" far better. Left alone unless the simulation shows a net gain.
