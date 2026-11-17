# Word prediction results (KB-406): what the strip does while the person TAPS

Yardstick: `core/src/test/kotlin/app/kestrel/core/input/glide/PredictionEval.kt` (run by `PredictionYardstickTest`; by hand in about a minute from the repository root after `./gradlew :core:compileTestKotlin`:
`java -cp core/build/classes/kotlin/main:core/build/classes/kotlin/test:<kotlin-stdlib jar> app.kestrel.core.input.glide.PredictionEval [verbose] [klx=<file>] [corpus=<file> [limit=N]] [glide [glides=N]] [notypos] [probe=word,word@prev]`).
Text: `EvalCorpus.kt`, 168 original everyday sentences (1 250 words: texts to family and friends, plans, shopping, work, tech trouble), 40 web searches, 109 common misspellings, 88 pieces of texting shorthand and 54 rare real
words, all written for this project (no licence needed). It is a ruler, not training data: do not tune on single lines; compare the same numbers before and after a change. A second, HUMAN-written ruler was
added because the first one is written by the same kind of author as the SODA dialogues it was compared with: 37 920 held-out Tatoeba sentences (one in 40, never counted into the pairs; kept on the CI runner's
artifact, not in the repository: `heldout_tatoeba.txt`, CC BY 2.0 FR).
It types like a person: letter by letter, looking at the strip, taps the suggestion at the first moment it shows. The strip rule is the same as `KeyboardPresenter.applySuggestions` (the typed text takes one of the three
places while it is not a word). No personal model: this is the day the keyboard is installed.

## Run 1 (2026-11-17, list `en.klx` as in 0.5.0-alpha3: 140 436 words, 241 520 pairs = book English)

| What | Result |
| :- | :- |
| Next word, nothing typed yet: right word first / among the three | 12.7 % / 24.2 % |
| After 1 letter, as the strip shows it (typed text takes a place) | in 3: 48.6 % |
| After 2 letters / 3 letters, as the strip shows it | 63.4 % / 83.5 % |
| Keystrokes saved when the person taps the strip at the first moment the word shows | 46.2 % |
| Web searches: next word in 3 / keystrokes saved | 20.8 % / 35.2 % |
| Typos (neighbouring key, letter left out, doubled, swapped) put right by the space | 90.2 % (a WRONG word 60 of 2 304) |
| A word typed RIGHT that a space would change | 0 of 916 |
| 109 common misspellings put right | 80 % (3 wrong, 19 left) |
| Shorthand changed by a space (88 words) | 25: pls -> plus, thx -> th, brb -> barb, idk -> id, omw -> mow, tbh -> th, hehe -> here, ofc -> of ... |
| Rare real words changed by a space (54) | 1 (pastern -> pattern) |
| Simulated glide of running text, sloppy thumb, first guess | 95.7 % (my sentences), 91.7 % (held-out Tatoeba) |

Reading: typo repair was strong and safe. The weak spots were prediction that needs context (the next word 24 %, the first two letters 63 %) and everyday shorthand. The cause was data, not code: the only pair
counts were 242 000 Google-Books pairs ("on my" -> own, life, products; "later this" -> is, page, site).

## Run 2 (2026-11-17, list with casual pairs and shorthand, `Suggester.CONTEXT_MIX` 0.75, doubled-letter slips cheaper)

Pairs: Tatoeba English sentences counted on a CI runner (`.github/workflows/count-pairs.yml`, `tools/glide/count_pairs.py`), mixed into the book pairs (`LexiconFile.mergePairs`, weight 0.55, k 30, pairs seen at least 6
times: 143 362), plus 58 shorthand words (`tools/glide/texting-words.txt`). File 3.64 MB -> 4.01 MB (303 532 pairs).

| What | Before (run 1) | After | Held-out Tatoeba before -> after |
| :- | :- | :- | :- |
| Next word among the three | 24.2 % | 28.7 % | 22.4 % -> 26.7 % |
| After 1 letter, on the strip | 48.6 % | 55.9 % | 45.2 % -> 50.9 % |
| After 2 letters, on the strip | 63.4 % | 77.0 % | 59.3 % -> 67.6 % |
| After 3 letters, on the strip | 83.5 % | 91.8 % | - -> 79.7 % |
| Keystrokes saved by tapping the strip | 46.2 % | 51.7 % | 43.7 % -> 47.9 % |
| Web searches: keystrokes saved | 35.2 % | 37.5 % | - |
| Typos put right by the space | 90.2 % (60 wrong) | 93.4 % (44 wrong) | - |
| Right word changed by a space | 0 of 916 | 0 of 916 | - |
| Misspellings put right (of 109) | 80 % | 83 % (3 wrong) | - |
| Shorthand changed by a space (of 88) | 25 | 0 | - |
| Rare real words changed (of 54) | 1 | 1 | - |
| Glide of running text, sloppy thumb, first guess | 95.7 % | 96.9 % | 91.7 % -> 92.9 % |

What was tried and not used: SODA dialogues (CC BY 4.0, 250 000 dialogues, 31 million words): +0.3 point of keystrokes saved on my sentences, -0.2 on human sentences, +0.6 MB: not worth it. Lowering
`Suggester.DEEP_TAIL` from -16 to -14.5 fixes 4 more of the 109 misspellings (untill, wierd, publically, millenium ...) but "corrects" bloat, trowel, farrier, tarn (rare real words): a wrong word put in by a space is
worse than a typo left, so it stays. `CONTEXT_MIX` 0.9 gains 0.3 point more but trusts a pair table that has no back-off. Pairs seen at least 3 times instead of 6: +0.2 point, +0.6 MB.

## Findings that are not code changes (open)

* The strip shows the typed text in quotes as one of its three places while it is not a word (it is also how a person teaches a new word: tap it). That costs about 8 points of hit rate after one letter
  (engine top three 64.3 % vs 55.9 % on the strip). A setting "show the typed word only when a fix is pending" would give the places back: the owner's call.
* After a comma the keyboard has no word before it (`InputController.twoBefore`), so the strip offers sentence starters (I, The, It ...) where "and", "but", "so" fit better. Telling a comma from a full stop
  (a flag from the controller) would help; not done.
* Pairs only know ONE word before; trigrams ("on my _") need a new file format (KB-201).
