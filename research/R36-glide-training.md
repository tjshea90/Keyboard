# R36 - Glide training (owner 2026-11-26) and what teaching a keyboard "how I glide" can do

Owner, his words: "I did the glide practice and it is attached. Can you make a much longer glide training that i can glide full paragraphs and problematic words several times and the keyboard learns how i glide? Would this work,?"

Everything below was measured in this session; nothing was run on the owner's tablet or on an emulator (the sandbox has no KVM).

## 1. His practice file (alpha15, 40 words, one tablet)

`PracticeEval` on the file (it stays outside the repo; the lab reads `PRACTICE_FILE=<path>`):

| | result |
| :- | :- |
| view / keys | 592 x 281 px at density 1.0625, key pitch 55.5 dp (key 50.8 x 37.9 px) |
| the keyboard's own answers when the glides were made | first word right 39 / 40 (97.5 %), right word in the first three 40 / 40 |
| shipped decoder alone | 38 / 40 (95.0 %), first three 40 |
| decoder + shape network | 39 / 40 (97.5 %), first three 40 |
| the misses | "our" typed as "or" (decoder alone also "there" typed as "the") |
| sweep of the big-key values (shapeW, visitCap, lmMin) | the shipped values sit at the top of the plateau (97.5 %) |
| thumb habit learned from the 40 glides (`PracticeEval.replay`) | x 0.03, y 0.03 pitch = 1.9 / 1.7 dp (his thumb lands almost exactly on the key centres); touch plane up to 9.3 dp at a corner |
| taught each glide after decoding it vs a keyboard that learns nothing | 38 / 40 vs 38 / 40 (a file of 40 DIFFERENT words can only show the thumb lesson, and his thumb is on target) |

So on isolated common words the engine already reads his tablet well. His real-use misses (ave for and, fur for for, there/the, access/and) come from somewhere a clean 40-word drill does not reach: running text (the words before the word), speed, rarer words, or look-alike twins whose paths are almost the same (o-r and o-u-r are the same straight line through u).

## 2. What a glide of a KNOWN word can teach (code audit)

`GlideSupport.kt`, `GlideSession.confirmPrevious`, `PersonalModel`, `PatternMemory`, `GlideDecoder.learnFrom`:

| lesson | what is stored | used by | for a kept glide in daily use | in the training |
| :- | :- | :- | :- | :- |
| thumb | single shift (`biasX/Y`, 12 % of the leftover offset per glide, clamp 0.6 pitch) + touch plane (first and last letter, ridge fit) | shifts every path before judging | after `LearnGuard`, only when the word is not rare (`keepTrust`) | every glide, the word is certain |
| shape | `PatternMemory`: up to 4 shapes of 16 points per word, weight 1 kept / 3 picked, `contradict` 0.7 / 2 | lifts a word up to +4.5 cost units when the shape looks like one it drew before, only when its path fit is within `patMaxGap` of the best (reach shrinks with key size, R28) | yes | phone-sized keys only (below) |
| confusion | `PersonalModel.addConfusion(guessed, meant)` | lifts the meant word up to 1.6 nats when the guessed word is first again (`confGain`, `confMaxGap`) | yes, from corrections | never (below) |
| word counts / pairs / triples | `PersonalModel.add` | language prior, strip | yes | NEVER: a training paragraph is not what he writes |

`GlideTeaching` (core, pure) gives the three hand lessons; `GlideTyping.teach(word, guessed, serial)` runs it on the engine thread, only for the decode that was just made (a later glide invalidates the decoder's path), never for a word the person blocked or the list lacks, and counts it in Diagnostics ("Glide training taught N glides"). A shape is not kept when the glide is more than `FIT_D` (1.15 key units) from the straight line through the word's letters (a wild glide would teach the wrong shape).

A side finding fixed on the way: `GlideSession` moved the thumb habit even in boxes that ask for no personalised learning (`IME_FLAG_NO_PERSONALIZED_LEARNING`, passwords). It is now gated by `learnAllowed()` like everything else (CLAUDE.md rule), which also stops the practice and training boxes from teaching a wrong kept word.

## 3. The lab: would it work? (`TrainingLabTest`, REAL_TABLET=1)

A made-up hand glides the REAL training script (`GlideTraining`: 8 paragraphs with the words before each glide as the keyboard sees them, 3 look-alike drills x 4, the page of words that went wrong; about 430 glides) on the three geometries, a thumb offset of 6 dp and its own habit of 8 dp per word, aim 8 dp while training and 10 dp afterwards (everyday glides are sloppier); after every glide the keyboard is taught. Then the same hand glides 1 200 to 2 000 everyday words nobody asked for, decoded by the taught keyboard and by one that learned nothing. First choice, same seeds. This is the GENEROUS case for learning (a hand that repeats its habits exactly); the real number comes from the owner's own file.

| geometry | lessons | hold-out learned / plain | spoiled / fixed | practised words learned / plain |
| :- | :- | :- | :- | :- |
| phone 292x161 (2 seeds, 1 500) | thumb | 81.1 / 79.6 | 40 / 63 | 88.8 / 88.2 |
| | shape | 79.8 / 79.6 | 24 / 27 | 90.0 / 88.2 |
| | confusion | 79.4 / 79.6 | 9 / 6 | 87.8 / 88.2 |
| | all three | 80.8 / 79.6 | 61 / 79 | 89.2 / 88.2 |
| tablet portrait 564x272 (1 seed, 1 200) | thumb | 95.6 / 95.0 | 3 / 10 | 97.7 / 97.7 |
| | shape | 94.9 / 95.0 | 7 / 6 | 98.3 / 97.7 |
| | confusion | **92.9** / 95.0 | 29 / 4 | **93.0** / 97.7 |
| | all three | **93.1** / 95.0 | 34 / 11 | **93.2** / 97.7 |
| tablet landscape 916x272 (1 seed, 1 200) | thumb | 95.7 / 95.4 | 1 / 4 | 98.5 / 98.5 |
| | shape | 94.4 / 95.4 | 15 / 3 | 97.2 / 98.5 |
| | confusion | **90.3** / 95.4 | 67 / 6 | **87.0** / 98.5 |
| | all three | **90.6** / 95.4 | 71 / 13 | **87.6** / 98.5 |

Reading:
* **Thumb lesson: always positive** (+1.5 phone, +0.6 portrait, +0.3 landscape). Its size follows how far the thumb is off the key centres: 6 dp was assumed here; the owner's tablet file shows about 2 dp, so expect about nothing there.
* **Shape lesson**: level on a phone overall, +1.8 on the words that were practised; costs a little on big keys (the learned reach there is deliberately short, R28).
* **Confusion lesson is harmful on a tablet** (-2.1 portrait, -5.1 landscape, and it hurts the practised words most): the memory counts every miss of a 97 % keyboard as "A typed, B meant" and then lifts B over A for glides of A that were right. In daily use that memory fills from rare corrections; a training run feeds it by the hundred.
* The engine's learning state therefore needs the training to be SELECTIVE, which it is: `GlideTeaching.Parts.forPitchDp`: thumb always, shape only at key pitch <= 45 dp (`GlideParams.SMALL_PITCH_DP`), confusion never.

Shipped mix (3 seeds, 1 500): phone **80.5 vs 79.2** (practised words 90.5 vs 88.5), tablet portrait **95.4 vs 95.0**, landscape **95.3 vs 95.1**; spoiled 55 / 3 / 4, fixed 73 / 11 / 6. Never worse in any geometry.

Habit-repair example (`GlideTeachingTest`, phone keys): a hand whose "o" of "for" always lands 1.6 keys towards "i" gets "fur" 11 times of 12 from the plain keyboard; taught, "for" comes first from the 4th glide on. On the tablet's keys the same habit (>= 1.6 keys, about 90 dp) is NOT mended (reach 0.22 of the phone's): a miss of more than a key and a quarter is too far off for a remembered shape, by design.

## 4. The training screen (`TrainingActivity`, `GlideTraining`)

* Settings > Help > Glide training. Start screen: what it does, the switch "The keyboard learns from my glides here" (off = only measure) and a field for the person's own trouble words.
* Pages: paragraph, paragraph, drill, paragraph, paragraph, drill ... (8 paragraphs, 3 drills: the/there/they/then/and/any/access, for/from/or/our/out/you/your, it/is/in/on/to/too/of/off, every word 4 times in rounds, never the same word twice in a row), the optional page of his own words after the second paragraph, and at the end one page with the words that went wrong AND what was typed instead, 3 times each (a pair practised both ways: practising only "our" would bias the keyboard towards it).
* The box holds the words written so far (the last 8 tokens, punctuation included), so the engine decodes every paragraph glide with the real words before it; drills are decoded without context. One-letter words ("I", "a") are written for him, never prompted.
* After every page: its score and misses, "Next page", "Save the file now", "Stop and show the result". The result lists the score per page and the pairs that went wrong. Leaving without saving asks first (also with Android 16's predictive back); rotating the tablet keeps the training.
* The file is format 2 (`SWOOP-GLIDE-PRACTICE 2`, still blocked by `tools/secretscan.sh` and the inbox capture; written by `PracticeFile` into Downloads like the drill's): every glide with its path, the words the keyboard offered, the page and the two words before it, plus whether teaching was on. A damaged line is skipped, the others stay.
* Only prompted words are kept, only while the screen is open, only from the box with the private option.

## 5. What his training file will let me do (next session)

1. `PracticeEval` decodes it WITH the recorded context (`prev`, `prev2`) and prints first / first-three / rank of the right word per page kind (running text vs drill), by word length and by speed (glide milliseconds are in the file): is the miss a path problem (right word not in the five) or a language problem (in the five, not first)?
2. `PracticeEval.replay` replays the file in order with teaching and prints the thumb habit, plane and the first-choice rate by quarter for taught vs plain, now with repeated words in it (the shape lesson can show).
3. The pairs that went wrong in running text decide the next improvement (a better word-before model, R31, if the language side is the problem; the reach of remembered shapes on big keys if the shapes repeat).

## 6. Honest limits

* A simulated hand; real hands change from day to day. The only real data is the 40-word file and, soon, the training file.
* Careful training glides differ from everyday ones; the intro asks for everyday speed, and the lab already trains at aim 8 and tests at aim 10.
* The Kestrel browser's copy of `core/input` is not synced (the Browser repo is not attached to this session); `GlideTeaching` is additive and the browser has no training screen.
* Shape network speed on the real tablet is still unmeasured.
