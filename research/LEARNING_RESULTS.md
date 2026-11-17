# Word prediction results (KB-406): what the strip does while the person TAPS

Yardstick: `core/src/test/kotlin/app/kestrel/core/input/glide/PredictionEval.kt` (run by `PredictionYardstickTest`; by hand in 5 seconds:
`java -cp core/build/classes/kotlin/main:core/build/classes/kotlin/test:<kotlin-stdlib jar> app.kestrel.core.input.glide.PredictionEval [verbose]` from the repository root).
Text: `EvalCorpus.kt`, 168 original everyday sentences (1 250 words: texts to family and friends, plans, shopping, work, tech trouble) and 40 web searches, written for this project, no licence needed.
The corpus is a ruler, not training data: do not tune on single lines; compare the same numbers before and after a change.
It types like a person: letter by letter, looking at the strip, taps the suggestion at the first moment it shows. The strip rule is the same as `KeyboardPresenter.applySuggestions` (the typed text takes one of the three places while it is not a word).
No personal model: this is the day the keyboard is installed.

## Run 1 (2026-11-17, shipped list `en.klx` 140 436 words / 241 520 pairs, `Suggester` as in 0.5.0-alpha3)

| What | Result |
| :- | :- |
| Next word, nothing typed yet: right word first / among the three | 12.7 % / 24.2 % |
| After 1 letter, engine top three | first 37.9 %, in 3: 54.5 % |
| After 1 letter, as the strip shows it (typed text takes a place) | in 3: 48.6 % |
| After 2 letters: engine / strip | 66.3 % / 63.4 % |
| After 3 letters: engine / strip | 86.1 % / 83.5 % |
| Keystrokes saved when the person taps the strip at the first moment the word shows | 46.2 % (2 817 of 6 104) |
| Web searches: next word in 3 / on the strip after 2 letters / keystrokes saved | 20.8 % / 41.5 % / 35.2 % |
| Typo, neighbouring key: first / in 3 | 96.9 % / 100 % |
| Typo, a letter left out: first / in 3 | 80.9 % / 97.1 % |
| Typo, a letter doubled: first / in 3 | 98.4 % / 99.8 % |
| Typo, two letters swapped: first / in 3 | 97.5 % / 100 % |
| Typos put right by the space (not another real word) | 90.2 % (2 078 of 2 304); a WRONG word put in: 2.6 % (60); left alone: 7.2 % (166) |
| A word typed RIGHT that a space would change | 0 of 916 |

Reading: typo repair is strong and safe (no right word is ever spoiled). The weak spot is PREDICTION that needs context: the next word (24 %) and the first two letters (49 % to 63 %). The cause is data, not
code: the only pair counts are the 242 000 Google-Books pairs (written English); casual phrases ("on my way", "let me know", "I'm going to") have none. See `research/PREDICTION-SOURCES.md` item 2 (casual bigrams).
