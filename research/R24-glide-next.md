# R24 - Glide prediction on REAL thumbs (owner request R24b, 2026-11-18)

Owner, 2026-11-18, his words: "Do more research to improve the word glide prediction if possible, but it is pretty good right now".

`research/R20-glide-personalization.md` (26 sources, ranked techniques) already exists and is NOT repeated here. Its biggest stated gap was: every number came from a SIMULATED thumb (`GestureSim`),
"no real-thumb data of any kind". This session closes that gap for the first time, with a public corpus of real swipes, and re-tunes the decoder on it.

## 1. The data (test-side only, never committed)

FUTO Swipe (MIT licence, `huggingface.co/datasets/futo-org/swipe.futo.org`, 1.04 M swipes, split by donor session): one JSON object per line with `word`, `sentence`, `word_idx`, `canvas_width/height`,
`orientation` and `data` = points `{t, x, y}` (x, y normalised 0..1). We read byte ranges of `dev.jsonl` (HTTP Range request, a few MB to 40 MB) in the test harness
`core/src/test/kotlin/app/kestrel/core/input/glide/RealSwipe*.kt`; the file goes to `core/build/` (git-ignored), is never written into the repository and the repository has no copy of it.
Run: `REAL_SWIPES=1 REAL_SWIPES_FILE=build/real-swipes-40.jsonl REAL_SWIPES_MB=40 ./gradlew :core:cleanTest :core:test --tests '*RealSwipeEval*' -i`
(`cleanTest` is needed: Gradle does not see environment variables as inputs and would skip the run). Other variables: `REAL_SWIPES_OFFSET_MB` (a different byte range = different donors), `REAL_VARIANTS`
(comma list of names from `variants()` or `all`), `REAL_VARIANT_N`, `REAL_THREADS`, `REAL_DIAG=1`.

What the corpus is: phone-width portrait QWERTY, three letter rows and no space row, a web page, encyclopaedia-style sentences (so many names and rare words: 4.7 % of swipes are words the shipped word
list lacks, e.g. place and person names), mean 78 touch points and 1.56 s per swipe. It is NOT the owner's tablet and NOT texting; the "everyday words" slice (the 5 000 commonest words of the list) is the
closer proxy for texting.

### 1.1 The geometry is calibrated from the data
The dataset card does not say where its keys are. The mean start point of the swipes that begin with each letter (and the mean end point of those that end with it) puts the keys on ten equal columns and three
equal rows filling the canvas, the home row indented by half a key (q-row y 0.19, a-row 0.53, z-row 0.86; `a` x 0.105, `s` 0.205, ...). An axis-aligned least-squares fit of those letter centres onto this keyboard's key
centres leaves a residual of **0.09 key pitch** (slice A) and 0.07 (slice B): the mapping is trustworthy.

## 2. First real-thumb numbers

Shipped decoder (0.7.0-alpha3 values), real swipes, the word before taken from the sentence as context:

| slice | swipes in the list | first / top-3, no context | first / top-3, with context | everyday words (top 5000), with context |
| :- | :- | :- | :- | :- |
| A (offset 0, 40 MB) | 6 054 | 86.8 / 93.8 | 87.6 / 94.0 | 90.6 / 96.0 |
| B (offset 150 MB, other donors) | 6 652 | 88.0 / 95.0 | 89.0 / 95.0 | 92.4 / 97.2 |

For comparison the R20 yardstick on the SIMULATED thumb said 93 (normal) / 86 (sloppy) first. The real hand sits near the "sloppy" simulated one. (FUTO's own SHARK2-style baseline: 80.05 / 90.47 top-1 / top-3; their
neural decoder 92.94 / 97.46; different word list and no personal learning, so only the order of magnitude compares.)

### 2.1 What a real thumb does differently from the simulator

| measure | simulator | real (slice A / slice B) |
| :- | :- | :- |
| scatter of the first and last touch around that letter's key | 8 dp (normal), 11 dp (sloppy) | sd 0.44 x 0.29 pitch = about **10.8 dp** / 10.1 dp (wider along the row than across it) |
| drawn path length / ideal path length through the letters | 0.92 assumed (corners cut) | **1.09 / 1.08**, ln sd **0.13**, p10 0.94, p90 1.26 |

The length ratio matters for R23c ("the" -> "there"): fingers draw MORE than the straight lines, not less, and the spread is tighter than assumed.

### 2.2 Where the real misses are (slice A, 6 054 swipes; `REAL_DIAG=1`)
* 750 first-choice misses (12.4 %). 385 of them have the right word second or third (this is what the strip rescues); 365 are not in the top three; only 132 (2.2 % of swipes) never reach the careful pass.
  So candidate recall is not the problem; ranking is (same verdict as R20 on the simulator).
* In 428 of the 750 the chosen word is MORE common than the meant one, in 321 it is LESS common (43 %): the path evidence outvotes the language too often for rare words.
* 309 of the 750 share first and last letter with the meant word (look-alike words: sled/skilled, last/least, there/the, then/them, among/along, replied/relied).
* First-choice miss rate by distinct letters: 2: 9 %, 3: 9 %, 4: 14 %, 5: 13 %, 6: 18 %, 7: 13 %, 8+: 14 %.
* `there` -> `the` is among the examples, i.e. the owner's slip exists on real thumbs.

## 3. Tuning on real swipes (see section 4 for what was adopted)

All numbers: the first 3 000 swipes of slice A (the word before as context; second column: 3 000 everyday-word swipes). One standard error of a paired difference is about 0.4 point, so differences below
that are noise; only changes that move all four numbers the same way and survive slice B count.

| variant | all, first / top-3 | everyday, first / top-3 |
| :- | :- | :- |
| shipped | 86.6 / 92.8 | 90.5 / 95.4 |
| **shapeW 2.0 -> 1.0** (cost of the path straying from the straight line between letters) | **87.7 / 94.0** | **91.8 / 96.6** |
| shapeW 4.0 | 84.0 / 90.2 | 87.8 / 92.7 |
| visitCap 3 -> 5 | 87.2 / 93.3 | 91.0 / 95.6 |
| endW 1.5 / 6 | 86.4 / 93.2 ; 85.8 / 91.7 | 90.3 / 95.8 ; 89.4 / 93.9 |
| lmA 0.75 / 3.0 | 86.1 / 92.7 ; 86.5 / 92.8 | 89.2 / 94.9 ; 90.8 / 95.6 |
| length term off | 86.5 / 93.3 | 90.4 / 95.8 |
| length centred on the real 1.09 (sigma .13 / .20) | 87.0 / 93.2 | 90.7 / 95.7 |
| path-offset correction (biasGain) off | 84.5 / 91.3 | 88.6 / 93.6 |
| rare-word price off | 87.2 / 93.2 | 90.3 / 95.2 |

(`lmW` had no effect because `lmA` > 0 makes the language weight adaptive.)

### 3.1 Finer sweep and the held-out check (what survived)

Slice A, first 3 000 swipes, word before as context (first / top-3):

| variant | all words | everyday words |
| :- | :- | :- |
| shipped (shapeW 2.0) | 86.6 / 92.8 | 90.5 / 95.4 |
| shapeW 0 (no straight-line price) | 77.6 / 87.0 | 86.7 / 94.1 |
| shapeW .5 / .75 / 1.0 / 1.25 / 1.5 | 87.2 / 93.8 ; 87.7 / 94.0 ; 87.7 / 94.0 ; 87.7 / 93.8 ; 87.2 / 93.4 | 91.8 / 96.6 ; 91.9 / 96.6 ; 91.8 / 96.6 ; 91.6 / 96.5 ; 91.1 / 96.1 |
| shapeW 1 + visitCap 4 / 5 / 6 | 87.8 / 94.2 ; 87.9 / 94.3 ; 87.9 / 94.3 | 91.8 / 96.7 ; 91.8 / 96.7 ; 91.8 / 96.6 |
| shapeW 1 + endW 2 / 4 | 87.5 / 94.1 ; 87.5 / 93.9 | 91.9 / 96.9 ; 91.5 / 96.4 |
| shapeW 1 + biasW 1.5 / 6 | 88.1 / 94.1 ; 86.8 / 93.2 | 92.0 / 96.7 ; 90.9 / 95.8 |
| shapeW 1 + length centred on 1.09 | 87.5 / 94.0 | 91.5 / 96.6 |
| shapeW 1 + visitCap 5 + length 1.09 + endW 2 | 87.8 / 94.6 | 91.9 / 97.1 |

Held-out slice B (other donors, first 3 000 swipes):

| variant | all words | everyday words |
| :- | :- | :- |
| shipped | 90.1 / 95.6 | 92.7 / 97.0 |
| shapeW 1.0 | 90.2 / 96.0 | 93.3 / 97.5 |
| shapeW 1.5 | 90.2 / 95.7 | 92.9 / 97.2 |
| shapeW 1 + visitCap 5 | 90.4 / 96.2 | 93.5 / 97.7 |
| shapeW 1 + visitCap 5 + biasW 1.5 | 90.3 / 96.2 | 93.6 / 97.9 |
| shapeW 1 + endW 2 | 90.0 / 95.8 | 93.0 / 97.5 |
| shapeW 1 + visitCap 5 + length 1.09 + endW 2 | 89.7 / 95.8 | 92.8 / 97.5 |

Reading (honest): slice A suggested +1.1 points; the held-out donors give about +0.1 first / +0.4 top-3 (all words) and +0.6 / +0.5 (everyday words) for shapeW 1.0, and about +0.3 / +0.6 and +0.8 / +0.7
with visitCap 5 as well. Averaged over both slices the change is worth about +0.8 points first choice and +1 point top-3. Slice A alone overstated it (a different set of people, a different mix of rare words).
The length-centring (1.09), endW and biasW moves are NOT consistent across the slices and were NOT adopted; with the straight-line price lowered, centring the length term on the real 1.09 gave nothing (87.5 against 87.7 on
A, 89.7 against 90.2 on B), so the R23c term stays as it is.

## 4. What was changed in the app (R24b)
`GlideParams.forPitchDp(pitchDp)` (core, used by `GlideSupport` for both decoders): `shapeW` 1.0 and `visitCap` 5.0 for key pitches up to 45 dp (every phone, and the donors' keyboards were 32-45 dp), sliding linearly to the earlier
`shapeW` 2.0 and `visitCap` 3.0 at 70 dp and above (tablets). `GlideKeys.pitchDp` carries the pitch in dp (`KeysView.glideKeys` divides by the screen density; tools and tests lay keys out in dp already, so they get the old
defaults: `GlideParams()` is unchanged and every simulator yardstick keeps its numbers). Tests: `GlideParamsPitchTest`.

Why the slide: a thumb's scatter is about the same size in dp on any keyboard, so on big keys it is a smaller share of a key and the stricter straight-line price (tuned on the simulator, whose scatter in key units was
smaller) is the better guess. THE TABLET SIDE HAS NO REAL-SWIPE EVIDENCE. If the tablet turns out worse than before, set `SMALL_PITCH_DP` and `LARGE_PITCH_DP` to the same value to get one setting everywhere.

## 5. Not built, and why
* Next-word post-correction (R20 rank 3, +0.5 to +1.5 on the simulator): not built this time; it edits text the person already sees and needs its own care. The real-swipe harness can now measure it (the corpus has the whole sentence).
* Dwell / speed cue: the corpus has timestamps (`t`), so this is now measurable on real data too. Next step if wanted: per-point speed at each letter, tested on to/too, of/off. Not done.
* The 4.7 % of swipes whose word is not in the list are names and places (encyclopaedia text), not a texting problem; the learned-words path (own dictionary) already covers the owner's names.
* Replay file in the repository: NOT done on purpose (rule: no replay logs / no real swipes in the repository).

## 6. How to repeat it
`REAL_SWIPES=1 REAL_SWIPES_FILE=build/real-swipes-A.jsonl REAL_SWIPES_MB=40 REAL_VARIANTS=all ./gradlew --no-build-cache :core:cleanTest :core:test --tests '*RealSwipeEval*' -i` (add
`REAL_SWIPES_OFFSET_MB=150` and another file name for held-out donors; `REAL_DIAG=1` prints the miss analysis; `--no-build-cache` and `cleanTest` because Gradle ignores environment variables as inputs). A run of 3 000
swipes x 20 variants takes about 5 minutes on 4 cores.
