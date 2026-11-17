> First copied from the Kestrel browser repository (commit 063b2761) for the Swoop keyboard (KB-002); rebuilt 2026-11-17 from four sources (this file). KB-201 moves the list to the KGL2 format.

# The glide-typing word list: sources, licence, how it is made (AT1)

Glide typing (owner message 34) needs a list of words with how common each one is, and which words follow which. It ships as ONE binary file, `ime/src/main/assets/glide/en.klx`
(about 3.6 MB: 140 436 words, 241 520 word pairs; format KGL1, unchanged, read by `LexiconFile.read`). The notices that have to travel with it are in `ime/src/main/assets/glide/LICENSE.txt` (inside the APK).

Owner request, 2026-11-17: "do everything possible to improve the glide word prediction accuracy ... as many english words as possible ... ways to filter out rare words (for example it predicted the word tui)".
So the list now has (1) far more words, (2) counts that blend casual and written English, (3) a clear rule for which words are "rare" (see "Rare words" below), which the decoder (`GlideDecoder`, `Rarity`) and the
tap suggester (`Suggester`) use.

## What is in it

| Part | Source (version, where from) | Licence |
| :- | :- | :- |
| Which words are valid English (about 125 000 words; inflections, plurals, tenses) | SCOWL 2020.12.07 (Kevin Atkinson), the shared and the American-spelling lists, sizes up to 95, lowercase only. Debian source archive `scowl_2020.12.07.orig.tar.gz` | permissive notice (public domain parts, ENABLE, UKACD, Ispell, WordNet); the whole `Copyright` file of SCOWL is reproduced in `LICENSE.txt` |
| How common a word is, casual and web English | wordfreq 3.1.1 `large_en` (321 180 words; blend of Wikipedia, subtitles, news, books, web text, Twitter and Reddit counts), a wheel from PyPI | code Apache-2.0; data CC BY-SA 4.0 |
| How common a word is, spoken / subtitle English | hermitdave FrequencyWords, `content/2018/en/en_full.txt` (OpenSubtitles 2018; 1 656 996 words, 734 777 659 tokens), GitHub | code MIT; content CC BY-SA 4.0 |
| How common a word is, written English (Google Books), and the word pairs | SymSpell `frequency_dictionary_en_82_765.txt` and `frequency_bigramdictionary_en_243_342.txt` (symspellpy 6.10.2 wheel from PyPI) | MIT (the Google Books Ngram data asks for credit) |
| about 300 hand-written extra words and the apostrophe words (`tools/glide/extra-words.txt`) | written for this project | none needed |

Because wordfreq and FrequencyWords are CC BY-SA 4.0, the compiled `en.klx` is shared under CC BY-SA 4.0 too (stated in `LICENSE.txt`). The repository is public; keep that notice with the file.
What the notices must say (credit lines for the sources wordfreq itself is built from, SUBTLEX, OpenSubtitles, Google Books) is all in `LICENSE.txt`.

Not used, on purpose (research/PREDICTION-SOURCES.md section C): Norvig count files, raw SUBTLEX-US, raw Twitter/Reddit dumps, anything GPL or non-commercial.

### Checksums of the files as downloaded (2026-11-17)

| File | sha256 |
| :- | :- |
| `wordfreq-3.1.1-py3-none-any.whl` | `4b1c6ecffc6198be3396d5cf871c4423ca71c907c231348d352dd54d62b97473` |
| `wordfreq/data/large_en.msgpack.gz` (inside it) | `dffae8066b78dce0a6667cf5f58e567054f902674667090a7ac8a8a44628b05c` |
| `symspellpy-6.10.2-py3-none-any.whl` | `5a7cee286ef16e9ee25913ea5b9fb42ebc51ced60ce2b85c4d4be30ac3e81a51` |
| `frequency_dictionary_en_82_765.txt` | `68e9dc81c7e73bd7310b57e516ecaea0d8b6387ff71344a57c04174650a407a7` |
| `frequency_bigramdictionary_en_243_342.txt` | `fd892a160184101dd7ae807ac5a302d01fcea1c47304181a8ed7ed9c94545bcd` |
| `en_full.txt` (FrequencyWords 2018) | `7fea67ab954e2c01df6c608c9826e594cf36f8823b3243554f88245fb75dc506` |
| `scowl_2020.12.07.orig.tar.gz` | `5587667caa20c4891390c2d42dbb4d5c4c3f41bee77af1457ece3ba23fb859cc` |
| `scowl-2020.12.07/Copyright` (inside it) | `283326a422e29c510e2ba6805518c418ce3c35ed1fadc3eec83d2da4f6c5a055` |
| `ime/src/main/assets/glide/en.klx` (the result) | see `git log`; recompute with `sha256sum` |

## How the words and their counts are chosen (`tools/glide/build_wordlist.py`)

1. **Vocabulary.** Every SCOWL word up to size 70 (spell-checker dictionaries: the 70 level is "common dictionary words with all inflections"); sizes 80 and 95 (huge and insane: ENABLE, UKACD, MWords) only when wordfreq or SymSpell has seen
   the word; words SCOWL does not have (slang, brands, web names: `lol`, `youtube`, `wifi`) only when they are clearly frequent (wordfreq >= 5 per million AND >= 300 subtitle occurrences, about 450 words); the extra words. Names, accents, digits,
   possessives (`dog's` would compete with `dogs` on the same keys; only real contractions such as `don't`, `it's`, `that's` stay) and single letters other than `a` and `i` are left out.
   Short words need real use: a 2-letter word needs wordfreq >= 15 per million, a 3-letter word >= 1 per million (a glide of two or three letters matches short junk by accident: this is also how `tui` and `xu` went).
2. **Counts.** For every word a weighted mean of the natural logs of its probability in the three frequency sources (wordfreq 0.4, subtitles 0.3, Google Books 0.3): a word has to be plausible in casual AND in written English to rank
   high. A word a source has never seen is given a floor (so a dictionary word nobody has counted is present with a tiny count and loses to every common word). The Google Books list cannot know words that are not in SCOWL
   (it was cut with SCOWL), so for those the other two decide. The counts are then scaled so that they add up to 541 808 760 578, the scale of the old list: `PersonalModel` weighs the person's own words in occurrences of THIS list
   (`UNIGRAM_COUNT` 2e7 per use), so the scale must not move.
3. **Pairs.** The 242 000 SymSpell pairs between words that are in the list (unchanged: they are the only pair counts with a licence we can use).

Result: 140 436 words (old list: 82 833), 3.64 MB (old: 2.71 MB). 4 400 words are common (probability above e^-9.5), about 17 000 are above the rarity floor, the rest is the long tail.

## Rare words (what the decoder and the suggester do with the counts)

A word is **rare** when its language prior (ln of its chance, the person's own use and the pair with the word before included) is below `Rarity.FLOOR` = -12.5 (about the 13 000th most common word; "weather" is -9.2,
"sneakers" -12.5, "jeopardize" -13.3, "quixotic" -15.3, the unused tail is -17 to -20).

* Glide decoder (`GlideParams.rareW`, `rareFloor`, `rareCap`, `rareMaxPerLetter`, `rareFitSpan`): a rare word pays 1.0 cost unit per nat below the floor (at most 7 nats) on top of the language weight, and is not offered at all unless the
  path fits it (cost per letter at most 3.4 for a word just under the floor, 1.1 for a word 2 nats under it). So a rare word wins only when its letters were plainly drawn; a glide that fits nothing gives no word (it used to give "tui").
* A word the person uses is not rare: `PersonalModel` adds its own mass (2e7 occurrences per use, 1.5e7 per recent use) to the prior before the test, so about eight uses lift a word past a common lookalike, one use past other rare words.
  A pair with the word before that the list knows (`hong kong`) also keeps a word out of the rare class.
* Tap suggestions (`Suggester`): the same floor, 0.7 nats lost per nat below it (completions, next words, typo fixes); a typed word below -16 (the unused tail) is not trusted as "known" on its own, so a far more likely one-slip fix may replace it.
* The quick pass of the decoder keeps 400 words (was 200): with 1.7 times the words the right one fell below the cut-off more often; this alone is worth about 1 point of top-1.

## How the file is made (reproducible, nothing runs the downloaded code)

All downloads go into NEW EMPTY folders OUTSIDE the repository; only data is read, with `python3 -I`.

1. Fetch (a pip download only fetches the wheel; nothing is installed or imported):
   `python3 -m pip download wordfreq==3.1.1 --no-deps -d <dir1>` and `python3 -m pip download symspellpy==6.10.2 --no-deps -d <dir2>`;
   `en_full.txt` from `https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_full.txt`;
   `https://deb.debian.org/debian/pool/main/s/scowl/scowl_2020.12.07.orig.tar.gz`. Compare the sha256 above.
2. Read the data files out of the wheels and the archive (they are zip / tar files): `wordfreq/data/large_en.msgpack.gz`, `symspellpy/frequency_dictionary_en_82_765.txt`, `symspellpy/frequency_bigramdictionary_en_243_342.txt`,
   `scowl-2020.12.07/final/` (Python's `zipfile` / `tarfile` with `extractall(..., filter='data')`).
3. From the repository root:
   `python3 -I tools/glide/build_wordlist.py --wordfreq <dir>/large_en.msgpack.gz --hermit <dir>/en_full.txt --symspell <dir>/frequency_dictionary_en_82_765.txt --scowl <dir>/final --extra tools/glide/extra-words.txt --out <work>/words.txt`
   then `./gradlew :tools:glideLexicon --args="--out ime/src/main/assets/glide/en.klx --words <work>/words.txt --pairs <dir>/frequency_bigramdictionary_en_243_342.txt"`
   (`--report <file>` of the python script writes one line per word with its origin and its three raw frequencies: read it to see why a word is in).
4. `./gradlew :core:test` (the accuracy tests read the file and fail when it is missing or too small).

The tool is `tools/src/main/kotlin/app/kestrel/tools/GlideLexiconCompiler.kt`; the file format and its reader are `core/.../input/glide/Lexicon.kt` (`LexiconFile`), tested together in `LexiconFileTest`.
A word in `extra-words.txt` gets AT LEAST its count; one that is not in the frequency list is added. Words are lowercase a-z (an apostrophe is kept for typing and dropped for the finger's path).

## How it was measured

Simulated glides (`GestureSim`, a made-up thumb: calm / normal / sloppy / bad scatter) over the real key layout, decoded against the shipped list: `GlideEval` (top-1 / top-3) and `RareWordEval`
(`java -cp ... app.kestrel.core.input.glide.RareWordEval old=<old en.klx> new=<new en.klx> noise=normal,sloppy rareW=1.0 pop=mid`: the same words, drawn by the OLD list's frequencies, through both lists; counts every time a rare word
came first although a common one was meant). Numbers and what they mean: `research/PREDICTION-SOURCES.md`, section "What was built (2026-11-17)". There is no recorded human glide data: real thumbs are sloppier or tidier
than the simulator in ways it cannot show, and nothing here was run on the tablet.

## What it cannot do (so nobody assumes it)

* English only. The owner also visits Spanish-language sites (pelisjuanita.com): Spanish words are NOT in the list, so a Spanish word will be glided into the closest English one. A second list (a Spanish
  frequency dictionary with accents shown but typed on the plain letters) is the way, and needs the owner's word (see TASKS AT1).
* Names, brands and slang that are not in the list cannot be glided: the keyboard remembers words that are typed or picked (AT1f) and the host names of visited sites, so they join the vocabulary after a first use.
  Capitalised names (people, places) are not in SCOWL's word lists that we use; only the frequent ones that appear in lowercase in the sources are.
* The pair list is still the 242 000 Google Books pairs of SymSpell (book English, no texting phrases): no licence-clean chat-register pair list was found (OpenSubtitles pairs: licence unclear).
