# R20 - Making glide prediction personal and accurate: survey, measurements, ranked techniques (owner request R20d, 2026-11-18)

Owner, 2026-11-18, his words: "do more research and work on making the keyboard learning as good as possible. I want it to be very good at predicting my glide typing."

This file is research only. Nothing in the engine was changed for it. Everything measured here was measured on the repository's own SIMULATED thumb (`GestureSim`); no real glide, no tablet and no emulator took part (see section 9).

## How to read the tags

| Tag | Meaning |
| :- | :- |
| [V] | Verified: in this session I read the text of the paper, page, patent or code (PDF converted to text, or the page itself). |
| [Vabs] | Verified at abstract / summary level only (full text not readable here). |
| [S] | Secondary: a search-result summary or a third-party page; not checked against the primary. |
| [R] | Repository fact: I read the code or the document in this repository. |
| [M] | My measurement in this session on the simulator (reproducible, section 8). Simulated thumb only. |
| [I] | My inference. No source says it. |

Sources are numbered S1... and listed with URLs in section 10.

---

## 0. The verdict on one screen

What the numbers say about where the remaining errors are (all [M], simulated thumb, 380 dp wide keyboard, 140k-word list, first guess):

* The right word is among the words that reach the careful pass 99.1 to 99.9 % of the time for calm to sloppy fingers (98.6 % for a "bad" one). Candidate recall is NOT the problem; ranking is.
* 82 to 99 % of the first-guess misses are glides where the word the keyboard chose is MORE COMMON than the word meant. Misses are mostly "meant the less common word".
* For a normal finger, 26 % of misses are words whose ideal shape lies within 0.4 key of the chosen word's, 41 % between 0.4 and 0.8 key, 31 % further. For a calm finger 45 % / 41 % / 14 %. So most misses are look-alike words, not wild paths.
* After the keyboard has learned a person who repeats phrases, words seen 6 or more times are right 99 to 100 % of the time, but words seen only once or twice stay at 76 % (a hand with a 10 dp habit per word) to 90 % (no habit). The long tail of rare use is what personal learning does not yet reach.

So the ranking below favours techniques that help words the person has NOT used many times (a position-dependent touch model, stroke-shape transfer, second-word evidence), techniques that break ties between look-alikes with a second signal (timing, the next word), and the measurement machinery that tells real thumbs from the simulator.

| Rank | Technique | Honest expected gain (first guess) | Cost on the 2 GB tablet | Confidence |
| :- | :- | :- | :- | :- |
| 1 | Position-dependent touch-offset field (6-number affine model, optionally per-zone), learned from the ends of accepted glides and from tap touches, one per orientation | 0 if the thumb's offset is uniform (the global habit already handles it); +2 to +5 points in my simulator when the offset varies by about 0.3 to 0.4 key across the board [M]; real size unknown | 12 floats and one 3x3 matrix per orientation; under 0.05 ms per decode; O(1) per tap, no allocation | Medium for the mechanism (tap evidence is real), low for the size on glides |
| 2 | Measure on real thumbs: on-device shadow decoder (A against B, counts only) plus an opt-in replay file the owner attaches, plus a one-off replay of the MIT-licensed FUTO swipe corpus on a CI runner | none by itself; it turns every other gain from a guess into a number and keeps bad ideas out | one extra decode per glide only while an experiment is on, on the preview thread | High that it is needed |
| 3 | Conservative next-word post-correction (revise the previous glided word when the word after it makes an alternative clearly better; undoable) | +0.3 to +1.5 points of words right in running text [M]; Google published gesture word error 11.51 % to 9.20 % [V] | one rescoring of 5 saved candidates per glide; negligible | Medium; the risk is a surprising edit |
| 4 | Speed / dwell / loop cue with per-user likelihood ratios (timestamps are not captured today) | at most +0.5 point from doubled letters (to/too, of/off, good/god: ceiling measured in the word list), up to about +1 point with slow-point cues for collinear look-alikes (four/for, our/or) [M ceilings] | 4 bytes per touch point; O(n) speed profile; a few multiplications per candidate | Medium for the idea, low for the size |
| 5 | Stroke-direction arc model plus letter-pair templates: a few numbers that bend every straight segment the way this thumb bends it, so a habit learned on "for" also helps "four", "form", "fork" and never-seen words | unknown, 0 to +2 points; aims at the 76 % of words seen once or twice | 16 to about 100 floats | Low (no published result; cheap to test) |
| 6 | Shape memory 2.0: per-word spread, per-letter offset template, forgetting by calendar time | +0.5 to +1.5 on habitual words that are still wrong (the "6+ times wrong" rows) | about 2 m floats per word instead of 32 | Medium |
| 7 | Correction learning as a clipped pairwise re-ranker (learn feature weights from picks over the first guess) | 0 to +1; my sweep shows the decoder's weights are already flat | 8 floats | Low |
| 8 | Personal language model: check the mixing against the published recipe (0.8 background, 0.2 personal cache), add app / field-class affinity | 0 to +1 point; recency and topic memory measured at +0.0 to +0.5 [M] | small tables | Medium that tuning is cheap, low that it matters |
| 9 | Cold-start kit: import the browser file, "Teach from text" priming, shrinkage priors, optional 12-word calibration | shortens the first 300 glides (86 -> 93 % in the simulator) [M] | none at run time | Medium |
| 10 | Per-user adaptive language weight / length term | at most +0.6 point [M]; not recommended now | none | High that it is small |

Read section 4 for the seven required fields of each technique (what, evidence, what the repository already does, expected gain, cost, sketch in this codebase, how to measure).

---

## 1. What the engine does today and what it does not [R]

Read: `GlideDecoder.kt`, `PersonalModel.kt`, `PatternMemory.kt` (with `Fingerprint`), `GlideSupport.kt` (`GlideTyping`), `GlideSession.kt`, `TypingLearner.kt`, `Suggester.kt`, `KeysView.kt`, and the yardsticks.

Decode pipeline (`GlideDecoder.decode` -> `decodePass`):
1. The touch path is cut into 48 equal-length steps in key pitches (`resample`), moved by the global thumb habit (`biasX`, `biasY`).
2. Quick pass: only words whose first and last letters are within 3.2 pitch^2 of where the finger went down and lifted; score = mean of each letter's capped squared distance to the path + 0.7 x end distance x 0.5 - 0.10 x ln p(word); the best 400 survive, plus up to 48 remembered words (`injectRemembered`).
3. Careful pass (`dp`): monotone assignment of the word's letters to path steps, cost = per-letter distance (capped at 3.0) + 2.0 x how far the path strays from the straight line between neighbouring letters + 3.0 x first/last letter; a letter that repeats counts ONCE (`loadLetters`).
4. Second look (`refine`) for the best 40: the whole path is moved by 0.8 of its mean offset from the word's letters (cost 3.0 per pitch^2).
5. Language: score = -cost + lm x prior - rare-word price; `lm` = 1.5 x best cost per letter, clamped 0.12 to 1.2; prior = ln(0.6 x unigram + 0.4 x bigram) where the unigram includes the person's use (`PersonalModel.unigramMass`, 2e7 occurrences per use up to 200 uses) and recent use (`recentMass`, 1.5e7 per use with a fade of 0.992 per word), and the pair / triple blends the person's own pairs and triples (`pairWeight`, `tripleWeight`).
6. `applyPatterns` lifts a word by min(4.5, 2.5 x ln(1 + evidence)) when remembered shapes look like this glide (only if the path fit is within 2.2 per letter of the best); `applyCorrections` lifts "meant B" by min(1.6, 0.9 x ln(1 + n)) when "guessed A" is first again and B fits within 0.9 per letter.
7. R19d: when the strict pass finds nothing, a relaxed pass answers with the closest word.

Learning (`GlideSession`, `TypingLearner`, `GlideTyping`):
* Kept glide: word +1, pair, triple, shape (`PatternMemory.add`, then `contradict` 0.7), thumb habit moves 12 % of the remaining offset (`learnFrom`, clamp +-0.6 pitch, saved by `GlideHost.saveHabit`).
* Strip pick: counts 3, remembers "guessed -> meant", contradicts look-alike shapes by 2.
* Backspace after a glide, then a retyped or re-glided word: replacement counts 3, the deleted glide's shape is donated to the replacement if the word's straight-line shape is within 1.15 key (`FIT_D`); a retry that gives the deleted word first again is demoted (`RETRY_MS` 6 s).
* Taps: `TypingLearner` learns words, pairs, triples, erase-and-retype, picks, undone fixes; sentence starters; case forms; the share of automatic fixes the person takes back moves the autocorrect margin (`autoMarginShift`, -0.3 to +1.5 nats).
* Shape memory: 16 points in key units, at most 4 shapes per word, 1200 words, 3000 shapes, merge at 0.3 key, `SIGMA` 0.35 key, confirmation count capped at 12.

What is NOT there (each is a gap this survey targets):
* No timestamps. `KeysView.addPoint(x, y)` stores positions only (history points are included, `getHistoricalX`, but not `getHistoricalEventTime`); no pressure or touch size.
* Tap coordinates are thrown away: `KeysView.pressUp` calls `emit(key)`; `KeysView.Listener.onKey(key)` has no x, y. The commonest input has no touch model at all (`Suggester.near()` uses key adjacency, not coordinates).
* One global thumb offset (two numbers) for the whole board; `refine` re-centres per word but treats every letter alike.
* Repeated letters are collapsed in `dp`, so "to" and "too", "of" and "off", "good" and "god", "feel" and "fell" cost exactly the same on any path; only the language separates them.
* Shape memory is per exact word: a habit learned on "for" does not touch "four" or "form" (stated in `research/KEYBOARD-APP-PLAN.md` 8.3.4).
* Corrections are remembered as exact pairs (guessed A, meant B); there is no learning of decoder weights.
* No automatic revision of the previous word from the next word (R18a gives a manual strip for a tapped word).
* The language model has no app, field or topic context (`FieldInfo` carries the field kind only, not the app).
* No on-device A/B measurement and no real-thumb data of any kind (`research/KEYBOARD-APP-PLAN.md` G7).
* The simulator: `GestureSim` has per-letter aim scatter, one global per-glide shift, an optional per-person global habit (`userBiasDp`), per-word habit shifts (`GlideHabitEval.Hand`), corner rounding, jitter, wobble, and touch steps spaced by distance. It has NO position-dependent offset, NO left / right thumb, NO time (no speed, dwell or loop), NO stroke arc, and repeated letters are drawn as one touch, so doubled-letter twins have identical paths.

---

## 2. What was measured in this session [M]

All runs: shipped `en.klx` (140 494 words), `GlideParams()` defaults, keyboard 380 dp wide (key pitch 38.3 dp), words drawn by usage frequency, no sentence context unless stated, simulated thumb. Seeds differ by about 1 point from one another at n = 2000 to 3000: a change under 1 point needs a paired run on identical paths (the harnesses below use identical paths for every variant).

### 2.1 Baseline with the existing yardstick (`GlideEval`)

| Finger | n | first / top-3 / recall of the 400 | seed |
| :- | :- | :- | :- |
| normal (aim 8 dp) | 2000 | 93.1 / 98.4 / 99.5 % | 1 |
| sloppy (aim 11 dp) | 2000 | 85.6 / 95.6 / 99.1 % | 1 |
| calm / normal / sloppy / bad | 3000 each | 97.0 / 94.2 / 87.0 / 76.9 first; top-3 99.5 / 99.0 / 96.8 / 90.7 | 3 |

Decode 4.1 to 4.9 ms on average on this desktop (warm JIT). The Cortex-A53 estimate of 5 to 8 times slower in `research/KEYBOARD-APP-PLAN.md` is unmeasured.

Learning curves (existing tools, 70 % of what the simulated person writes comes from 40 own phrases):
* `GlideEval curve=1`, 6 stretches of 300 glides, first guess right: normal, no learning 86.0 87.7 87.3 87.0 84.3 88.0; with learning 93.3 96.0 98.0 97.7 99.0 99.0. Sloppy: 74.7 -> 85.3 91.0 94.7 94.7 93.0 97.0.
* `GlideHabitEval curve`, normal finger, every word with its own fixed hand habit of 10 dp per letter, 3000 glides: nothing learned 64 %; words / pairs / corrections 91 %; plus glide shapes 95 %. Words seen 1 to 2 times: 76.0 % (shapes on); 6 or more times: 99.2 %. With no hand habit: 89.5 % and 100 %.

### 2.2 Where the first-guess misses come from (`Miss.java`, scratch)

2500 glides per finger. "Shape distance" = `Fingerprint.distance` between the ideal shapes of the word meant and the word the keyboard chose, in key units.

| Finger | first right | misses | shape within 0.4 key | 0.4 to 0.8 | further than 0.8 | chosen word more common than meant | same keys but a doubled letter differs |
| :- | :- | :- | :- | :- | :- | :- | :- |
| calm | 97.0 % | 76 | 45 % | 41 % | 14 % | 99 % | 11 |
| normal | 93.1 % | 172 | 26 % | 41 % | 31 % | 90 % | 11 |
| sloppy | 86.1 % | 347 | 14 % | 46 % | 38 % | 82 % | 9 |

Reading: touch noise explains the "further than 0.8" group (a touch model helps there, and in the middle group); look-alike words explain the rest (the personal prior, a second cue, the next word help there). Doubled-letter twins alone are about 0.4 % of glides.

### 2.3 Headroom inside the word list (`twins.py`, `collide.py`, scratch)

* 22.3 % of the 140 494 words contain a doubled letter; in the 20 000 commonest words they carry 9.6 % of the usage.
* 2 061 groups of words differ only in doubled letters (the / thee, to / too, of / off, in / inn, good / god, feel / fell, been / ben, soon / son, as / ass, all / al, but / butt). Taking the commonest member every time would be wrong for 0.48 % of usage in the top 20 000 words. That is the ceiling for a doubled-letter cue on a cold, context-free decoder.
* Words whose ideal shape lies within 0.25 / 0.4 / 0.6 key of a MORE COMMON word with the same first and last letter: 1.9 % / 2.9 % / 6.4 % of the usage in the top 20 000. Examples at 0.4: four > for (0.00, the u lies on the o-r line), our > or (0.00), too > to, off > of, god > good, three > the (0.13), whole > while (0.13), live > love (0.26), lost > list (0.20), sorry > story (0.24), past > part (0.16), bank > back (0.30), soon > son. These are the words for which a path alone cannot decide and a personal prior, a pause at the right letter, or the next word must.

### 2.4 Position-dependent touch offset (`PosBias.java`, scratch; details in section 8)

3 seeds x 2400 glides per cell; the "301+" cell is 6300 glides (one standard error about 0.4 point; variants share identical paths). Hand = where this simulated thumb lands relative to the key it aims at, as a function of position, up to 0.4 key (15 dp) ASSUMED shapes, not measured data. "Today" = the engine's global habit (`learnFrom`).

First guess right after the learning period (glide 301 and on), %:

| Hand | Finger | no learning | today (global habit) | + 3x3 zones | + affine (ends) | + affine (ends + inner letters) | oracle field |
| :- | :- | :- | :- | :- | :- | :- | :- |
| none | normal | 93.6 | 93.5 | 93.7 | 93.8 | 93.6 | 93.6 |
| none | sloppy | 86.4 | 86.2 | 86.5 | 86.6 | 86.4 | 86.4 |
| uniform shift (0.15, 0.25 key) | normal | 90.4 | 93.5 | 93.6 | 93.9 | 93.6 | 93.6 |
| uniform shift | sloppy | 82.4 | 86.2 | 86.5 | 86.6 | 86.5 | 86.4 |
| fan (left keys land right, right keys land left, up to 0.4 key) | normal | 86.5 | 88.0 | 92.2 | 92.7 | 92.8 | 93.0 |
| fan | sloppy | 77.9 | 78.8 | 83.4 | 84.4 | 84.6 | 85.0 |
| 3x3 irregular zones (sd 0.3 key) + shift | normal | 80.4 | 83.2 | 86.7 | 85.7 | 85.6 | 83.9 (*) |
| irregular zones | sloppy | 74.0 | 76.2 | 77.9 | 78.4 | 78.0 | 74.9 (*) |

(*) My "oracle" applies the true field at the touched position, but the simulator interpolates between SHIFTED letter aims, so for a piecewise field the oracle is not an upper bound.

Learning speed (fan, normal finger, glides 1 to 100 including the learning): today 88.0, zones 90.7, affine 93.0 to 93.7, oracle 94.0. Affine ends reached the oracle within the first 100 glides. On a hand with no structure a small early dip appeared for zones / affine with a sloppy finger (84.0 against 86.3 in the first 100 glides, 300 glides: about +-2 points, so not significant) which is the reason for the shrinkage in 4.1.

Reading: if the thumb's offset is the same everywhere, nothing is gained and nothing is lost (the global habit is enough). If it varies by a third of a key across the board, a 6-number model recovers almost all of what the oracle recovers, within about 100 glides: +4.7 points (normal), +5.6 (sloppy) on the fan hand. These sizes are as large as I made the assumed field; how large the owner's real field is, is exactly what nobody knows.

### 2.5 Next-word post-correction (`PostCorr.java`, scratch)

168 everyday sentences (`EvalCorpus`), each glided 12 times with different noise, cold start (no personal model), the context of every word is what is on screen, the previous word's top-5 candidates are re-scored with ln P(next word on screen | candidate) x the decoder's own language weight, and replaced only when better by `margin` nats. 14 076 words.

| Finger | first guess right | margin 0.5 | 1.0 | 2.0 | 3.0 |
| :- | :- | :- | :- | :- | :- |
| normal | 97.5 % | +0.54 pt (78 mended, 2 spoiled) | +0.28 (39, 0) | +0.08 (11, 0) | +0.01 (1, 0) |
| sloppy | 94.0 % | +1.53 pt (223 mended, 7 spoiled) | +1.09 (155, 2) | +0.45 (63, 0) | +0.19 (27, 0) |

Relative error reduction at margin 0.5: 22 % (normal) and 25 % (sloppy), the same order as Google's published 20 % [V S9].

### 2.6 Things I measured and do NOT recommend building now

* Adaptive language weight per finger noise: sweeping `lmA` 0.75 / 1.5 / 3.0 at calm / normal / sloppy / bad gives first guess 97.1 / 97.0 / 96.2, 94.7 / 94.2 / 91.8, 87.6 / 87.0 / 85.1, 76.0 / 76.9 / 76.9. The existing per-glide adaptive weight is already flat across fingers; a per-person `lmA` is worth at most about +0.6 and below the seed noise for three of four fingers.
* Recency memory on a person who works in topic bursts (`GlideEval curve=1 burst=80 rec=1` against `rec=0`, 2400 glides): +0.0 to +0.5 point. Consistent with Fowler et al. [V S5]: an exponentially decaying cache did not beat a uniform cache.

---

## 3. Evidence survey: what the sources actually say

### 3.1 Decoding and shape models
* SHARK2 [V S1]: shape channel and location channel, each turned into a Gaussian probability, candidates beyond 2 sigma pruned, channels multiplied; location weights lowest in the middle of the gesture and rising linearly to both ends; a one-key-wide "tunnel" costs nothing; start/end pruning first; smoothed bigrams as the language model. SPEED: if the gesture took less time than a Fitts-law tracing time `tn(w)` for that word's shape, the location sigma is multiplied by 1 + gamma x log2(tn / ta) with gamma = 2 (expected range 1 to 10), because fast gestures are recalled shapes, not visually traced paths. The paper reports speeds, no accuracy figure.
* Word-gesture keyboard overview [V S2]: "sloppy" gestures tend to be faster and more error-prone; the authors name the lack of large-scale logging of real use as a gap.
* Gboard's decoder [V S9]: a finite-state "bi-key" decoder; repeated keys can be OPTIONAL in the lexicon (the second o of Google); gesture word error rate 11.90 % (baseline), 11.51 % (FST), 9.20 % (FST with post-correction); the authors warn false post-corrections are "especially frustrating" and suggest a small time window and high confidence. Gesture input "typically does not allow OOV recognition".
* Gboard's spatial model (taps and gestures) was replaced in 2017 by one LSTM trained with CTC, trained partly on user-interaction signals (reverted autocorrections as negatives, suggestion picks as positives); reported offline: about 15 % fewer bad autocorrects, 10 % fewer wrongly decoded gestures, 6x faster and 10x smaller [V S12]. The post describes no per-user adaptation.
* Google patent, incremental feature-based gesture decoding [V S13]: alignment points are chosen from curvature, direction, segment length and SPEED ("slower local speed or a pause makes a point more likely to be an alignment point"; fast gestures are treated as less precise); the physical cost and the lexical cost are weighted separately (more lexical weight for fast or long gestures, more physical weight for short, spatially similar ones); "weights may also adapt to the user over time". The text I read does not treat repeated letters.
* Grammarly's neural swipe decoder [V S15]: 33 features per touch point (position, 26 key-proximity values, approach angle, time since start, time since previous point, velocity, acceleration); "a neural network can learn to translate more time spent on certain letters to help decide between words such as to/too and of/off"; 7.5 M parameters; target about 17 ms; picks candidates with the tap-typing autocorrect (edit distance plus language model).
* FUTO Swipe [V S16]: 1 043 789 swipes, 12 278 donor sessions, MIT corpus with per-point t, x, y, word, sentence, canvas size and orientation; a SHARK2-style baseline reaches 80.05 / 90.47 / 94.54 % top-1 / 3 / 10 on QWERTY, the neural decoder 92.94 / 97.46 / 98.60; scoring = length-normalised CTC cost + log word frequency + a length term, parameters tuned with Optuna; the optional context model is "not evaluated". No personalisation in the text I read.
* Quinn and Zhai [Vabs S20]: a minimum-jerk production model with a few statistical via-points reproduces the shape AND dynamics of gesture typing; shows that gestures are not chains of aiming movements.
* Open source: HeliBoard says glide typing works "only with closed source library" [V S18]; AnySoftKeyboard's decoder matched corners only and its author lists "add small loop to ideal gestures on duplicate letters" as to-do (pool/poll) and reports pruning recall of 84.9 / 93.9 / 96.9 % for the 1 / 2 / 3 nearest start-end keys on one typist's text [V S17]; FlorisBoard's `StatisticalGlideTypingClassifier` credits that write-up [S S26].

### 3.2 Touch-offset (spatial) models
* Gboard personalisation [V S3] (Sivek and Riley, MobileHCI 2022): a per-user Gaussian spatial model whose key centres are moved by the mean offset of the last N taps (250 to 800; production 800 split in 4 buckets, unordered inside a bucket for privacy), grouped into rectangular key clusters found by a greedy split (7 clusters; offline 7 to 10 was best; 4 to 9 made no user-visible difference); training uses only words committed by TAP TYPING that match the final committed text; "small-but-significant" gains in speed and decoder accuracy: with personalisation on, against an unchanged control, WPM +0.2 to +0.6 % and modified-word ratio -0.05 to -1.9 % (one language +0.22 %) across 11 locale groups; retuning the Gaussian sigma ALONE (personalisation off) already captured part of it (for example WPM +0.12 / +0.57 and modified words -1.25 / -1.40 % in de-DE / en-GB), and the best personalised arm beat the best non-personalised arm in every language except English in India; more data helped with quickly diminishing returns; full covariance gave nothing; decaying weights made no difference over uniform; "Gboard users do not generally type by pressing directly on physical centers of keys". The paper is explicit that it covers tap typing only and that for gestures: "gesture typing is likely to have error dependent on the location of previous and next keys ... it will likely require a materially different form of adaptation"; per-posture and per-orientation statistics are named as the most important future work.
* Yin et al. [V S4]: a hierarchical backoff (base -> key -> posture+key -> user+key) with a minimum number of points before a finer model is used (about 55 for user+key on one key); posture classification from touch timing and distance between taps (83.6 % on individual touches, 86.4 % for the online classifier); for right-handed one-finger / one-thumb users touches on the left side are biased RIGHTWARD, for two-thumb users LEFTWARD; adaptive keyboard reduced character error by 13.2 % over a non-adaptive baseline in their tap study.
* Weir et al. [V S6]: Gaussian-process offsets and variances over the screen, trained on calibration touches ("users must provide calibration touches before using the system"); character error down by up to 7 % against a baseline and by up to 1.3 % against a leading commercial keyboard; the wider Gaussian where offsets are more variable gives the language model more latitude.
* Buschek et al. [V S7]: user-specific offset models can be trained from about 60 touches, can be transferred across devices and users with a transfer function, adapted models were over 8 % more accurate.
* Henze et al. [S S8]: 47.8 million keystrokes from 72 945 installations; a device-wide shift function cut errors by 9.1 % and improved speed by 2.2 % over the standard Android keyboard (the Gboard paper quotes 7.7 % and 2.6 % for a related comparison; I did not resolve the difference).
* Touchtype (SwiftKey) patent [V S14]: per-key 2-D Gaussian updated by MAP estimation with a Normal-inverse-Wishart prior; hyperparameters are capped to forget old data and blended back toward the prior; updated when the user picks a prediction or the top one is auto-accepted. SwiftKey's own FAQ says it "adjusts to where your keystrokes land" and that nothing it learns leaves the device by default [V S23]; it does not say this covers swipe input.
* Bi and Zhai (Bayesian Touch, FFitts) [S S21] and Azenkot and Zhai (touch behaviour by posture) [S S22]: touch-point distributions are Gaussian and depend on target size and posture; I could read only abstract-level summaries.
* No published study moves tap offsets to glides, and none shows a per-user glide decoder with measured gains. I searched specifically [S]; the only per-user glide statement is the sentence in Sivek and Riley above. Treat every glide-personalisation gain below as a hypothesis.

### 3.3 Language-model personalisation
* Fowler et al. [V S5]: closed-loop simulation with 90 Enron users (45 test): background LM only 5.7 % word error; adding a personal unigram cache 4.6 % (19.3 % relative); best mixing P = 0.2 x cache + 0.8 x background (lambda 0.8 on the background), window = ALL history; an exponentially decaying cache (half weight after about 2300 words) did not beat the uniform cache; the cache was primed with the first 30 days of mail (mean 1429 words); 42 of 45 users improved; a cache alone is not enough because sparse.
* AOSP user history [V S19]: a forgetting curve with levels 0 to 15, visible from level 2, +1 per use, one level lost every 15 days (32 steps), a decay check every 2 hours, a level-0 entry dropped after about 30 steps (about 14 days). A fixed calendar-time rule; ours decays only when the total weight passes 15 000.
* Gboard next-word model [V S10]: a CIFG language model with 670 units, 10 000-word vocabulary, 1.4 M parameters, 1.4 MB shipped, trained by federated averaging; top-1 recall 16.4 % against 13.0 % for the n-gram baseline on server logs, 5.82 % against 5.24 % in live impressions (top-3 13.75 % against 11.05 %). Gboard also learns new words with a character-level model under federated learning [Vabs S11]. Not applicable here (no network permission for learning, 2 GB, no ML library) except as the proof that a small personal-or-population model beats a plain n-gram at a few MB.

---

## 4. The ranked techniques

Each entry: **What**, **Evidence**, **Today**, **Gain**, **Cost**, **Sketch**, **Measure**.

### 4.1 Rank 1 - Position-dependent touch-offset field

**What.** Replace "one thumb habit" by an offset FIELD f(x, y) that says how far this thumb lands from where it aims at every place on the keyboard, and take it off every touch before decoding. Parametrisation, cheapest first:
* affine: f_x(u, v) = w0 + w1 (u - 0.5) + w2 (v - 0.5), the same for f_y; 6 numbers (u, v in 0..1 across the Q-P row and the Q-Z rows);
* per cluster: 3 x 3 zones, or Gboard's 7 greedy clusters, shrunk toward the global mean with n / (n + m);
* both: the affine field plus per-cluster residuals only for clusters with at least about 50 residuals.
One field per orientation (portrait / landscape), later a latent thumb (left, right, one-finger) because the sign of the left-side offset flips with posture (Yin [V]).
Where the evidence comes from: (a) the first and last letter of every ACCEPTED glide (people aim the ends carefully; `GlideDecoder.dp` already assigns step 0 and step n-1 to them, and SHARK2 weights the ends most [V S1]); inner letters only when the residual is under 0.8 key (corner cutting biases them; in my simulation inner letters added nothing); (b) every touch of a TAPPED word that was committed unchanged (Gboard's rule [V S3]); never from picked, fixed, erased or password text.
Offsets should be stored in dp, not in key pitches: this keyboard is resizable (R18c) and a thumb's physical offset does not shrink with the key size [I].

**Evidence.** Section 3.2: Sivek and Riley (production, taps), Yin (backoff, min points, posture), Weir (GP, calibration), Buschek (60 touches, transfer), Henze (population shift), Touchtype (NIW prior, capped forgetting). My simulation (2.4). Against: for glides nothing is published; Gboard calls it future work. Covariance and decay add nothing in the one production study, so: means only, uniform window.

**Today.** `GlideDecoder.biasX/biasY` (global), `learnFrom` (12 % steps, clamp +-0.6 pitch), `refine` (per word, same for every letter), `GlideHost.saveHabit` (two numbers), export line `habit <x> <y>`; no tap coordinates.

**Gain.** [M] 0 to +0.4 when the offset is uniform; +4.7 (normal) and +5.6 (sloppy) first-guess points for an assumed fan hand with 0.4 key reach; +2.5 to +3.5 (normal) and +1.7 to +2.2 (sloppy) for an irregular-zone hand; no loss on an unstructured hand. Real: unknown. If the owner's thumb offsets differ by 0.2 key or more between the left edge and the right edge or between the top row and the bottom row I expect +1 to +3 points on glides and a measurable gain in tap decoding later (`Suggester` could use the coordinates); if they do not, about 0 and nothing lost. Tap evidence in the literature is small (Gboard: a few tenths of a percent of speed; Yin 13 % character error in the lab).

**Cost.** Per orientation: 6 doubles of weights, a 3 x 3 normal matrix and a 3 x 2 vector (about 120 bytes), a 3 x 3 solve every 8 residuals; per decode one subtraction on 48 resampled points (under 0.05 ms) or on the raw touch points; per tap O(1) arithmetic into fixed arrays (no allocation, the tap rule holds). The save file grows by one line.

**Sketch (this codebase).**
* New `core/.../glide/TouchField.kt` (pure Kotlin):
```kotlin
class TouchField(private val keep: Double = 0.998, private val lamConst: Double = 0.5, private val lamSlope: Double = 2.0) {
    private val a = DoubleArray(9); private val b = DoubleArray(6)       // normal matrix 3x3, right-hand side 3x2
    val w = FloatArray(6)                                                // x: w0 w1 w2, y: w3 w4 w5 (dp)
    var n = 0f; private set                                              // evidence, decays with [keep]
    fun add(u: Float, v: Float, rx: Float, ry: Float) { /* a = keep*a + phi phi^T, b = keep*b + phi r; n = keep*n + 1; solve (ridge) every 8 adds */ }
    fun dx(u: Float, v: Float): Float = trust() * (w[0] + w[1] * (u - .5f) + w[2] * (v - .5f))
    fun dy(u: Float, v: Float): Float = trust() * (w[3] + w[4] * (u - .5f) + w[5] * (v - .5f))
    private fun trust() = n / (n + 30f)                                  // nothing before data; Yin: >= 55 points before a finer model
}
```
* `GlideDecoder.decodePass`: where `qx[i] -= biasX` is done, subtract `field.dx/dy` at the point's own (u, v) instead (and keep `biasX/biasY` for what is left; `learnFrom` then measures only the remainder). `GlideDecoder.offsetFor` already has the DP alignment (`assign[]`); add `endResiduals(lex, id, out)` returning the first and last letter's (x, y) residual and key centre.
* `GlideTyping.accepted` (called from `GlideSession.pick` / `confirmPrevious`) hands the residuals to the field next to `learnFrom`. Persist in `PersonalModel.serialize` / `parse` as a new line kind (older readers skip unknown kinds; `docs/KEYBOARD-EXPORT.md` v2 / KB-403 already plans `touch` lines) and keep `habit` for compatibility; clamp each weight to +-0.6 pitch like `MAX_BIAS`.
* Taps: `KeysView.Listener.onKey(key)` -> add `onTap(key, x, y)` with a default empty body; `KeysView.pressUp` passes the up point; `KeyboardPresenter` forwards to `TypingLearner`, which keeps a fixed ring of the current word's touch points (preallocated floats) and, in `finishWord` when the word is committed unchanged (`realFix == null`, no erase, no pick), calls `engine.learnTouches(...)`. Separate glide and tap fields at first (tap-to-glide transfer is unstudied [I]); give the glide field the tap field as a prior with a small pseudo-count in a later step, judged by the shadow counters.
* Safety: learn only when `FieldInfo.learningAllowed`; reset in `GlideTyping.forgetAll`; the export clamps like `habit`.

**Measure.** `GlideEval` / `GlideHabitEval` with a position-dependent hand. No change to `GestureSim` is needed to prototype it: `GestureSim.path(word, noise, shift)` already takes a per-letter shift in dp, so a `Hand` that returns `f(key centre)` for each letter does it (that is how my scratch run was built). Proper additions: a `Hand` family next to `GlideHabitEval.Hand` (uniform, fan, tilt, 3x3 zones, thumb-pivot: offset grows with the reach from a bottom-corner pivot, one pivot per thumb with the thumb chosen by the start key's side), and a new column in `GlideHabitEval curve` for "+ touch field". Gates: no regression greater than 0.3 point on the no-offset and uniform hands for both normal and sloppy fingers; at least +2 points on fan and zones; within 1 point of the oracle after 100 glides; no early dip beyond noise (30 glides of shrinkage). Needs a new tap-coordinate simulator (touch = key centre + field + Gaussian of about 0.4 to 0.55 key sizes: Gboard's sigma was 0.55 and was retuned to 0.4 to 0.5 per language [V S3]) before the tap side can be measured; `PredictionEval` today models slips (neighbour key, missing, doubled, swapped), not coordinates.

### 4.2 Rank 2 - Measure on real thumbs (shadow decoder, replay file, FUTO replay)

**What.** Three layers (they already stand in `research/KEYBOARD-APP-PLAN.md` 8.5 / KB-400; this entry says why they rank this high):
1. On-device shadow decoding: while an experiment is on, the same glide is decoded a second time with variant B (a flag set: touch field, dwell cue, post-correction, other `GlideParams`) on the PREVIEW executor after the real result is delivered; when the final word is known (kept, picked, retyped) the keyboard counts which variant had it first. Diagnostics prints "B would have been first N times, A M times, K events". Nothing leaves the device.
2. An opt-in replay ring buffer (last 3000 glides: points WITH timestamps, candidate list, final word, label) that the owner exports and ATTACHES (never pastes) to a session; a session processes it outside the repository and commits only numbers.
3. A one-off job on a CI runner that replays the MIT-licensed FUTO corpus [V S16] through `GlideDecoder`, to calibrate `GestureSim` (aim scatter, corner cut, speed and dwell distributions) and to check that tuning on the simulator did not overfit. Caveats: the key geometry of that web canvas is not described on the dataset card [V], so the coordinates must be mapped onto our key boxes, and the typists were not on a tablet.

**Evidence.** Every accuracy figure in this repository is simulated (G7). My own run shows the simulator is fragile to what it assumes: a hand with a position-dependent offset changes the answer to "is a touch model worth it" from 0 to +5 points. Google needs 90 M-word arms to see 0.2 to 1 % effects [V S3]; a paired replay on one user needs about 2 350 glides to see a 1-point gain at 90 % when 3 % of glides differ (estimate in `research/kestrel-ref/GLIDE_RESEARCH.md`, [I]). Shadow decoding is paired, so it is the efficient design.

**Today.** `GlideStats` counts kept / picked / undone and timings; `GlideDecoder` has a second instance for previews (`previewDecoder`); no variant flag, no replay, no timestamps.

**Gain.** None directly. It decides which of ranks 1, 3, 4, 5 ship ON, and lets the owner's thumb be the judge. It also removes the biggest unknown (real versus simulated thumbs).

**Cost.** Shadow decode only while an experiment is switched on: one more decode per glide, off the UI thread, after the real answer (desktop 4 to 5 ms; the tablet is unmeasured and estimated 5 to 8 times slower). A replay ring of 3000 glides x about 60 points x (x, y, t) is about 2 MB if kept binary; off by default.

**Sketch.** `GlideTyping.decode` gets `variantB: GlideDecoder?` (own scratch); after `done(...)` run B on `previewExecutor`; `GlideSession.confirmPrevious` / `pick` call `engine.shadowOutcome(finalWord)`; `GlideStats.onShadow(aFirst, bFirst)` and a Diagnostics line; replay writer in `GlideHost` behind a Settings switch, export via the existing Downloads path used by `KeyboardFlow`. The replay runner is a new test-source `core/src/test/.../ReplayEval.kt` reading the attached file.

**Measure.** Shadow counters themselves (paired); `ReplayEval` reproduces the live top-1 of the shipped decoder first (that is its own gate); then compare variants offline and fit the `SimNoise` parameters.

### 4.3 Rank 3 - Conservative next-word post-correction

**What.** When glide N+1 is decoded and on screen, re-score the saved top-5 candidates of glide N with ln P(word N+1 | candidate) (and the trigram with the word before N), and replace word N only when the best alternative beats the current word by at least a margin (start at 1.0 nat plus the person's `autoMarginShift`), only for the immediately previous glided word, only while its text is still exactly what was put in, never in fields that must not be learned, always undoable with one Backspace (which the keyboard already counts as an undone fix and learns from).

**Evidence.** Ouyang et al. [V S9]: gesture word error 11.51 % -> 9.20 % (about 20 % relative) with post-correction, with the explicit warning about false post-corrections and the advice to restrict by time window and confidence. My simulation (2.5): +0.28 point (normal) and +1.09 (sloppy) at margin 1.0 nat with 0 and 2 spoiled words out of 39 and 155 mended; +0.54 / +1.53 at 0.5 nat with 2 and 7 spoiled.

**Today.** Not built; `research/kestrel-ref/GLIDE_RESEARCH.md` item 7 lists it as NOT built because of surprising edits. R18a (tap a word, see look-alike words) is the manual version and `InputController.replaceWordAround`, `TypingLearner.replaced`, `Alternatives.find` exist.

**Gain.** +0.3 to +1.5 points of words right in running text depending on the finger (cold start; with the personal trigram table in use the rescue set may be smaller, [I]).

**Cost.** One extra scoring pass of 5 candidates per glide; storing the previous glide's candidate list (5 strings, 5 floats). No allocation concern (once per glide).

**Sketch.** In `GlideSession.onDecoded` keep `prevCandidates` (text, score, lm) beside `pendingWord`; after `controller.commitGlide(best.text)` call `postCorrect(res)`: compute the same next-word term as in `PostCorr` (lm x ln(0.6 x unigram(next) + 0.4 x bigram(candidate -> next))), and if `bestS - curS > margin` use `controller.replaceWordAround` then `TypingLearner.replaced(old, new, ...)`; mark the edit like an automatic fix so that `revertedCorrection` / `learnFix(true)` stay the single source for the person's margin. Window: the previous glide must be within 10 s and untouched.

**Measure.** `PredictionEval.glideInContext` (it already glides running text with context): extend it to keep the previous glide's N-best and apply the rule; report mended, spoiled and net; gate: spoiled under 1 % of mended at the chosen margin and net at least +0.3 point for the sloppy finger on two corpora (the built-in sentences and the held-out Tatoeba lines named in `research/LEARNING_RESULTS.md`). Ship behind a Settings switch (default ON only after the shadow counters show a net gain on the owner's thumb).

### 4.4 Rank 4 - Speed, dwell and loop cues with per-user likelihood ratios

**What.** Capture a timestamp with every touch point and use time as a second channel:
* local speed along the path, from the resampled arc length and time; a point is a PAUSE when its speed falls below about 40 % of the glide's median speed (thresholds learned per person);
* "unexplained pause" cost: a word whose assigned letter positions are all far (over about 0.5 key) from a clear pause pays kappa x pause strength x distance to the nearest assigned letter; this is the patent's alignment-point idea [V S13] used as evidence about the word, and it separates collinear look-alikes (for / four on the o-r line, our / or) because the finger slows where it means a letter;
* doubled letters: a word with a doubled letter pays ln((1 - rho) / (1 - eps)) if there is no loop or dwell at that letter's position and earns ln(rho / eps) if there is; a word without a double pays the reverse when a cue sits on a letter; rho = P(cue | double) and eps = P(cue | no double) are learned PER PERSON from accepted glides (four counters), starting at rho 0.5, eps 0.1 [I];
* SHARK2's speed rule as a third use: a glide faster than the person's usual for that shape is more likely a recalled shape, so inflate the location cost, not the shape cost [V S1]; the rare-word bias warning of its author applies, so cap it.

**Evidence.** Section 3.1: SHARK2 (speed-dependent sigma, gamma = 2), Google's incremental decoder patent (speed / pause as alignment evidence; weights adapt to the user), Grammarly (time on a letter separates to/too, of/off), Ouyang (optional repeated keys), AnySoftKeyboard (loop for doubled letters, still to-do), FlorisBoard / CleverKeys loops [S]. Also `Quinn and Zhai` model dynamics, so speed profiles are learnable [Vabs]. Nothing published gives a gain for this cue on its own; the ceilings below are mine.

**Today.** No timestamps anywhere; `dp` collapses repeated letters; the simulator draws doubled letters as one touch.

**Gain.** Ceilings [M]: doubled-letter twins are 0.48 % of usage in the top 20 000 words (to/too, of/off, good/god, feel/fell, soon/son, been/ben) and 0.4 % of simulated normal glides; collinear or near-collinear look-alikes within 0.4 key are 2.9 % of usage, of which a pause cue can settle part. So +0.3 to +0.5 point from doubles, +0.3 to +0.6 from slow points, in the best case; the real size depends on how consistently this thumb loops or pauses (unknown).

**Cost.** `KeysView`: one `FloatArray`/`IntArray` of event times (`getHistoricalEventTime`) next to `gx`, `gy` (+4 bytes per point; the trail is capped by `MAX_TRAIL_POINTS`); in the decoder an O(n) speed profile and a short list of at most 4 pause points; per candidate O(pauses x letters) after the DP. Under 1 ms on the desktop.

**Sketch.** `KeysView.addPoint(x, y, t)`; `Listener.onGlideEnd(xs, ys, ts, count)` (default overload keeps old callers); `GlideTyping.decode(..., ts)`; `GlideDecoder.resample` also resamples `qt`; new `pauseCost(...)` and `doubleCost(...)` added after `careful`/`refine` and before the language term, each with its own `GlideParams` weight (0 = off, so all current tests keep passing); per-person counters in `PersonalModel` (`d` line: cues seen for doubles, doubles, cues seen for singles, singles). `lm`'s adaptive weight stays as is.

**Measure.** New simulator knobs (section 6): `SimNoise.timing` with a speed profile (minimum-jerk between aim points [Vabs S20]), a pause probability per letter, a loop-or-dwell probability at doubled letters, per-person speed scale. Report top-1 on the twin groups (191 groups among the top 20 000 words) separately from the overall number; gates: twin-group accuracy up by at least 10 points at rho = 0.6, overall no regression above 0.2 point when the cue is absent (rho = eps = 0.1), decode time up by under 10 %.

### 4.5 Rank 5 - Stroke-direction arc model and letter-pair templates

**What.** Let the keyboard learn how THIS thumb bends strokes, so that every straight segment between two letters in `dp` is replaced by a slightly bowed two-piece polyline. Two layers:
1. Direction layer (about 16 numbers): for stroke directions in 8 bins, the mean signed lateral deviation per unit length (a thumb pivoting at the bottom corner draws arcs; a fixed sign per direction, [I]) and the mean overshoot beyond the target letter.
2. Pair layer (only for pairs with enough evidence, shrunk by n / (n + 8)): the mean mid-point deviation of path steps between the DP-assigned steps of letters a and b, for the 30 to 60 most used pairs (th, he, in, er, an ...).
Both come from DP alignments of accepted glides (the letters' assigned steps, `assign[]`, are already computed).

**Evidence.** Sivek and Riley: gesture error "is likely to depend on the location of previous and next keys" and corner shapes vary [V S3]; the plan (8.3.4 item 4) proposes the pair layer; Quinn and Zhai: statistical via-points explain real gesture shape [Vabs S20]. I found no published test of per-user arc models. Everything about its size is [I].

**Today.** `dp` uses straight segments only; `PatternMemory` stores whole-word shapes (no transfer: "for" does not help "four"); `GlideHabitEval` shows that words seen 1 to 2 times are right 76 % of the time with a 10 dp per-word habit and words seen 6 or more times 99.2 % [M]; this layer is the only one in this list that is designed to help the first.

**Gain.** Unknown, 0 to +2 points (largest for a thumb with arcs, nothing for a thumb that draws straight lines). Gate it by the shadow counters.

**Cost.** 16 to about 100 floats; the `pre[]` prefix sums in `dp` need two pieces per segment instead of one (about 2x for that step, O(m x n)); the O(m x n^2) alignment loop is unchanged.

**Sketch.** `core/.../glide/StrokeModel.kt` with `bow(a, b)` returning a lateral offset in pitches (direction layer + shrunk pair layer); `GlideDecoder.dp` builds `pre[]` against the bowed two-piece polyline; learning hook beside `learnFrom` using `assign[]` of the accepted word.

**Measure.** A new `Hand` that bows strokes: lateral deviation = kappa(direction) x length (kappa a smooth function of direction) plus per-pair noise, drawn through `GestureSim`'s corner rounding (it already rounds corners, `SimNoise.cut`, so a bow is a small addition to the dense-curve builder). Metric: first guess on words seen 1 to 2 times in `GlideHabitEval curve` plus a "transfer" test: train on the 300 most used words, test on 2000 different words.

### 4.6 Rank 6 - Shape memory 2.0 (per-word and per-pair)

**What.** Improve `PatternMemory`:
* per-word spread: keep a running variance per point and use sigma_w in `similarity` (tight habits get sharper evidence, loose ones broader; clamp 0.2 to 0.6 key) instead of one `SIGMA` 0.35;
* a letter-space template: store the aligned per-letter offsets (m x 2 numbers, the `meanOffset` idea per letter) and, for remembered words, run `dp` with the key centres moved by them, so shape evidence enters the COST and works at any keyboard size and glide speed instead of only as a capped lift (`patCap` 4.5);
* forgetting by calendar time (AOSP: one level per 15 days [V S19]) in addition to the weight-based `decay()` at 15 000;
* "pair" memory in the sense of word pairs: nothing (shape rarely depends on the word before); in the sense of letter pairs: see 4.5.

**Evidence.** The repository's own `GlideHabitEval`: +4.8 points from shapes at a 10 dp habit, 100 % on words seen 6+ times, but 5 of the late wrong ones were not among the 400 candidates and 11 were ranked below the first even after 6+ sightings [M, reproduced: "not among the 400 candidates 5, ranked below the first 11"]. Touch-model literature supports per-key variance only weakly: full covariance gave no gain in the one production study [V S3].

**Today.** `PatternMemory` as described in section 1.

**Gain.** +0.5 to +1.5 points on habitual words that are still missed; none on new words.

**Cost.** m x 2 floats per remembered word instead of 32 (m is 3 to 8 letters): less than today for short words; the DP cost for up to 48 injected words is unchanged order.

**Sketch.** `PatternMemory.Pat` gets `var2: FloatArray?` and `offs: FloatArray?`; `GlideDecoder.injectRemembered` and `careful` use `offs` when present; `serialize` adds `s2` lines (v2, as the plan's 8.3.4 item 5 and KB-403).

**Measure.** `GlideHabitEval curve` rows "6+ times that were wrong" (now 5 + 11 of 3000) and the first-guess rate at 10 and 16 dp habits; gate: 6+ times at least 99.5 %, nothing else worse by more than 0.2.

### 4.7 Rank 7 - Learning from corrections as a clipped pairwise re-ranker

**What.** Keep the exact "guessed A, meant B" memory (it works) and add a small online model over the features of each candidate: path cost per letter, first / last letter cost, ln global probability, personal mass, recency, pattern evidence, length. On a strong label (strip pick over the first guess, retype after deletion, no undo) take one perceptron step w += eta x (phi(chosen) - phi(first)); weights stay within 0.5x to 2x of the hand-tuned defaults, drift per session capped, a reset in Settings.

**Evidence.** Gboard's global spatial model was trained from reverted autocorrections and suggestion picks [V S12]; pairwise preferences are learnable from implicit feedback and perceptron-trained structured decoders are standard [S, from `research/kestrel-ref/GLIDE_RESEARCH.md` items A15, A16, not re-read]. No keyboard study shows per-user weight learning with measured gains; and the `lmA` sweep (2.6) says the weights are flat, so the headroom is small.

**Today.** `confusions`, `contradict`, retry demotion, `autoMarginShift`.

**Gain.** 0 to +1 point; I would not build it before ranks 1 to 4 and the shadow counters.

**Cost.** 8 floats, one update per strong label.

**Sketch.** `GlideDecoder` exposes the per-candidate feature vector (`GlideCandidate` gets `features: FloatArray`); `GlideSession.pick` / `confirmPrevious(afterUndo)` call `engine.rerankLearn(chosen, first)`; the model lives in `PersonalModel` (`r2` line).

**Measure.** A simulated person whose finger is good on the left half and sloppy on the right, or whose vocabulary favours rarer words (`rareW` too high for them): compare first guess with and without the learner on `GlideEval curve`; gate: never below the fixed weights on a person who matches the defaults.

### 4.8 Rank 8 - Personal language model: check the mix, add context

**What.** (a) Compare the mixing with the published recipe (0.2 personal cache, 0.8 background, all history, no decay) by sweeping `PersonalModel.UNIGRAM_COUNT`, `MAX_USES`, `RECENT_COUNT`; note the personal mass is ADDITIVE, 2e7 / 5.4e11 = 3.7e-5 of probability per use, so the effective cache share grows with use (1.7 % after 500 typed words, about 19 % after 5 000) instead of being a fixed 0.2 from the start [I from the constants]. (b) Context buckets: a field-class affinity (`FieldKind`, chat / search / mail / notes) and optionally the app, `EditorInfo.packageName` (not in `FieldInfo` today), kept as small per-bucket counts with a low weight and only for words used at least twice. (c) Calendar forgetting as in 4.6.

**Evidence.** Fowler [V S5]: +19.3 % relative word error from a personal cache, 42 of 45 users better, decay no better than uniform, priming with 30 days (mean 1 429 words) of text. AOSP forgetting curve [V S19]. No source tested per-app or topic caches for keyboards (the earlier survey also found none, `research/kestrel-ref/GLIDE_RESEARCH.md` section 5); my recency ablation gave +0.0 to +0.5 (2.6).

**Today.** Everything in `PersonalModel` (words, pairs, triples, recency 0.992, starts, cases, confusions).

**Gain.** 0 to +1 point (cold start excluded). It matters most in the first weeks, which is section 5.

**Cost.** Small tables (a few hundred bytes per bucket).

**Sketch.** `UNIGRAM_COUNT` etc. become constructor parameters of `PersonalModel` for the sweep; `GlideDecoder.prior` / `Suggester.logP` ask `pm.unigramMass(text, total, bucket)`; `FieldInfo.app` filled in `FieldInfos` from `EditorInfo.packageName` as a small hashed id.

**Measure.** `GlideEval curve=1 burst=N rec=0|1` (exists); add a person with 3 apps whose favourites overlap 40 %; `learningCurve` first-stretch and last-stretch numbers; `PredictionEval` with a personal model for next-word hit rate.

### 4.9 Rank 9 - Cold-start kit

See section 5 for the full list. **What / Evidence / Today / Gain / Cost / Sketch / Measure** are given there.

### 4.10 Rank 10 - Per-user adaptive language weight and a length term

**What.** Estimate the person's finger noise from the trimmed mean cost per letter of accepted glides and set `lmA` / `lmMin` / `lmMax` from it; add FUTO's length term (its score is length-normalised cost + lambda x log frequency + beta x length [V S16]).
**Evidence.** FUTO tuned both with Optuna on real swipes [V S16]; SHARK2's author warns that speed-driven weighting can let a rare word beat a common one for short words [S in `research/kestrel-ref/GLIDE_RESEARCH.md`, A2].
**Today.** `lm` is already adaptive per glide (1.5 x best cost per letter, clamped).
**Gain.** At most +0.6 point and for three of four fingers below seed noise [M 2.6]. Not recommended before real data exist; re-test the length term with the replay file (rank 2).
**Cost.** None. **Sketch.** `GlideParams.lenW` x letters in the score; `GlideEval` already parses `lmA`, add `lenW`. **Measure.** `GlideEval noise=calm,normal,sloppy,bad lmA=... lenW=...` with paired seeds.

---

## 5. Cold start

The simulator says what a new owner will feel: first stretch of 300 glides 86.0 % without learning, 93.3 % with (normal finger); sloppy 74.7 % -> 85.3 % (2.1). Most of the gain arrives inside 300 to 600 glides. Evidence-backed ways to shorten that:

1. **Shrink toward a neutral prior instead of waiting.** Every new learner above uses n / (n + m): the touch field m about 30 (Yin: at least about 55 points before a user+key model beats the key model alone [V S4]; Buschek: about 60 touches [V S7]; Touchtype caps hyperparameters so the prior never vanishes [V S14]); shape evidence already counts confirmations; the post-correction margin starts at 1 nat plus `autoMarginShift`.
2. **Import what already exists.** The browser's learning file (words, pairs, triples, corrections, shapes, `habit`) via KB-205; `LearnedExport.read` parses it. Only the habit is spatial, so it seeds the field's constant term. [R]
3. **"Teach from text"** (KB-205): the owner attaches a text file of their own writing; the keyboard learns words, pairs and triples with a bounded weight and the usual noise filters. Fowler primed the personal cache with 30 days of mail (mean 1 429 words) and measured 19.3 % relative error reduction over the whole run [V S5]. Measure: `learningCurve` with the first N words of the simulated person's phrases pre-added; target: the first stretch as good as the third stretch without priming.
4. **Learn from taps from the first day.** The owner types mostly by tapping; a tap yields a touch point and (after the word is committed unchanged) its key, so 1 000 typed words give about 5 000 labelled touches a day, against about 2 per glide (ends only): this is the fast way to fit a touch field (Yin's 55 points x 7 clusters = about 400 taps; Gboard keeps 800 [V S3]). It needs the coordinate plumbing of 4.1.
5. **Optional 60-second calibration** (off by default): "glide these 12 words". Weir et al. needed calibration touches for their GP [V S6]; 12 words give 24 clean end residuals and 12 whole-path samples. Skip unless the shadow counters show the field matters for this owner; it is friction.
6. **Transfer between the owner's two devices** (tablet and phone). Buschek et al. show a model from one device can be adapted to another with a transfer function [V S7]; offsets are physical and keyboards differ, so only the SHAPE of the field (sign pattern) is a plausible prior and only with a small pseudo-count. [I]
7. **Do not pre-load personal words from other sources** (contacts and the like need permissions the keyboard does not hold).
8. **Learning guards stay** (two-use rule, noise filter, password and no-personalised-learning fields learn nothing) so that an early wrong habit cannot lock in; Fowler found 3 of 45 users made worse by a cache with too little data in earlier notes of this repository (`research/kestrel-ref/GLIDE_RESEARCH.md`) while their own run had 42 of 45 improved [V S5].

**Gain** (technique 9): about 5 to 10 points of first guess during the first few hundred glides in the simulator; zero afterwards. **Cost:** none at run time; the work is KB-205 and the coordinate plumbing. **Measure:** `GlideEval curve=1` first stretch with and without priming; `GlideHabitEval` first stretch with the habit imported.

---

## 6. How to measure each idea with the existing tools, and what the simulator needs

| Idea | Existing harness | New simulator piece needed |
| :- | :- | :- |
| 4.1 touch field | `GlideEval` / `GlideHabitEval.Hand` through `GestureSim.path(word, noise, shift)` (per-letter dp shifts) | A `Hand` family (uniform, fan, tilt, zones, thumb-pivot, left / right thumb chosen by the start key's side, portrait / landscape switch). No `GestureSim` change is strictly needed. For taps: a `TapSim` (touch = key centre + field + Gaussian 0.4 to 0.55 key sizes) and a tap decoder that uses coordinates. |
| 4.2 shadow / replay | counters in `GlideStats`; `ReplayEval` (new) | `SimNoise` calibrated from the replay and from the FUTO corpus |
| 4.3 post-correction | `PredictionEval.glideInContext` extended with the previous N-best (my `PostCorr` is the prototype) | none |
| 4.4 speed / dwell / loop | `GlideEval` with twin-group accuracy added | `SimNoise.timing`: timestamps on every point (`GestureSim.Path.ts`), minimum-jerk speed between aim points, pause probability per letter, loop-or-dwell probability at doubled letters (and the repeated letter drawn as one touch plus a cue), per-person speed scale; today doubled letters are one touch |
| 4.5 arcs | `GlideHabitEval curve` words seen 1-2 times; train-on-300, test-on-2000 transfer test | `SimNoise.arc`: direction-dependent bow plus per-pair noise in `GestureSim`'s dense-curve builder |
| 4.6 shape memory 2.0 | `GlideHabitEval curve` (6+ times wrong rows), `PatternMemoryTest` | none |
| 4.7 re-ranker | `GlideEval curve=1` | a person with unusual weights (sloppy half, rare-word vocabulary) |
| 4.8 personal LM | `GlideEval curve=1 burst= rec=`, `PredictionEval` with a `PersonalModel` | a simulated person with 3 app contexts |
| 4.9 cold start | `GlideEval curve=1` first stretch; `GlideHabitEval` | priming option in `learningCurve` |
| 4.10 weights | `GlideEval ... lmA= lenW=` | none |

Rules for every result (from the plan's gates and my seed experience): report calm / normal / sloppy / bad; use identical paths for the variants; at n = 3000 two seeds of the same setting differ by about 1 point, so a claim under 1 point needs paired paths or n of at least 10 000; keep the hand-less (no offset) and uniform-offset rows as no-regression gates; never tune on a single sentence set (`research/LEARNING_RESULTS.md`); the simulator is only a relative ruler (the earlier survey cites Conway and Sangaline's warning that an untrained simulator only supports relative comparisons: [S] via `research/kestrel-ref/GLIDE_RESEARCH.md`, source A12, not re-read).

---

## 7. Things the sources say NOT to bother with (for this engine)

* Full covariance per key: no gain in the production study [V S3]. Means only.
* Decayed weighting of touch history: "no difference" against uniform weights [V S3]; a plain window of the last 400 to 800 accepted touches is enough. For words the same: decaying cache no better than uniform [V S5] (my recency ablation agrees [M]).
* Federated learning and server training: not applicable (no network use for learning); the lesson is only that a 1.4 MB model beat an n-gram [V S10].
* A neural decoder in pure Kotlin: FUTO's encoder is 1.0 MB int8 with 1.5 to 7.6 ms on a Pixel 4 core pair [V S16, Appendix B read in the text I fetched] and reaches 92.9 % against a SHARK2 baseline at 80.1 % on ITS data; our decoder is already above that baseline on simulated paths, and the next measurable step is real data (rank 2), not a model.
* Pressure and touch size: the Weir et al. pressure model changed the typing interaction, not accuracy of ordinary use [V S6]; Android touch-size data on the owner's tablet is unknown; skip.

---

## 8. The scratch experiments (how to reproduce)

The harnesses are Java programs in the session's scratchpad (not in the repository); they call the compiled `:core` main and test classes (`GlideEval`, `GestureSim`, `GlideDecoder`, `Fingerprint`, `EvalCorpus`) and read `ime/src/main/assets/glide/en.klx`. To keep them: port each to a test-source Kotlin file under `core/src/test/kotlin/app/kestrel/core/input/glide/` (`PositionBiasEval.kt`, `MissAnatomy.kt`, `PostCorrectionEval.kt`, `LexiconHeadroom.kt`) and run like the other evals (`java -cp core/build/classes/kotlin/main:core/build/classes/kotlin/test:<kotlin-stdlib jar> app.kestrel.core.input.glide.<Name> ...`).

* `PosBias`: for each (hand, finger, seed) the same word sequence (`GlideEval.frequencyPick`, seed x 7919) and the same `GestureSim(keys, Random(seed))` are decoded by six variants: no learning; today's `learnFrom`; and corrected-touch variants that subtract an online field estimate from every touch point BEFORE `decode`: 3 x 3 zones (shrunk toward the global mean with m = 15), affine ridge regression (3 features 1, u - 0.5, v - 0.5; ridge 0.5 on the constant and 2.0 on slopes; forgetting 0.998 per residual; solved after each glide), the same fed with inner letters gated at 0.8 key, and an oracle that subtracts the true field. Learning happens only for glides whose true word is in the shown three (as `GlideEval.run learn=true`). Residuals come from my own monotone alignment of the word's distinct letters to a 48-step resampled path (first and last letters pinned); residual = path point - key centre. Hands: none; uniform (0.15, 0.25 key); fan (dx = 0.40 key x (1 - 2u), dy = 0.20 key); 3 x 3 zones with offsets N(0, 0.3 key) plus (0.10, 0.20 key). Keyboard 380 x 161 dp, 3 seeds x 2400 glides.
* `Miss`: 2500 glides per finger, decode without learning, classify each first-guess miss by `Fingerprint.distance(ofWord(meant), ofWord(guess))`.
* `PostCorr`: 168 sentences x 12 seeds, context = the words on screen, rescoring term = (the glide's `lastLm`) x ln(0.6 x p_unigram(next) + 0.4 x p_bigram(candidate, next)).
* `twins.py`, `collide.py`: read the KGL1 file directly (format in `Lexicon.kt`), collapse repeated letters, and compare 16-point ideal shapes (the `Fingerprint` rule, key units) inside each first-letter / last-letter bucket of the 20 000 commonest words.

Nothing in these runs touched learned data, the repository, or a device.

---

## 9. What I could not verify (do not rely on)

* Any real-thumb number. All gains above are for a simulated thumb whose touch field I assumed.
* Whether the owner's thumb offset varies across the board at all (this is the one fact that decides rank 1's size) and whether the tablet's double letters are looped, paused or ignored.
* Tap-to-glide transfer of offsets; per-user glide models in any paper (none found); the size of any arc effect (rank 5).
* Henze et al.'s exact figures (two versions circulate: 2.2 % / 9.1 % in the paper's own summary as I found it, 2.6 % / 7.7 % as quoted by Sivek and Riley), the Azenkot and Zhai offsets, Bi and Zhai's quantitative results, FlorisBoard's constants, CleverKeys' accuracy, Samsung's internals (a patent only), SwiftKey Flow's decoder (nothing technical found), the Touchtype patent's sample counts (the patent gives none).
* The FUTO corpus' key geometry and device types (the dataset card does not say), and how many of its swipes were made by thumbs on tablets.
* Tablet timing of anything above (decode 4 to 5 ms is this desktop's warm JIT).
* The arXiv identifiers dated 2026 (FUTO Swipe) were read on arxiv.org during this session; I did not check them against any other index.

---

## 10. Sources

Papers and pages marked [V] were read as text in this session; [Vabs] abstract or summary page only; [S] secondary.

* S1 [V] Kristensson and Zhai, "SHARK2: a large vocabulary shorthand writing system for pen-based computers", UIST 2004. https://pokristensson.com/pubs/KristenssonZhaiUIST2004.pdf
* S2 [V] Zhai and Kristensson, "The word-gesture keyboard: reimagining keyboard interaction", CACM 55(9), 2012. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/39190.pdf
* S3 [V] Sivek and Riley, "Spatial model personalization in Gboard", MobileHCI 2022 (arXiv 2209.11311). https://arxiv.org/abs/2209.11311
* S4 [V] Yin, Ouyang, Partridge, Zhai, "Making touchscreen keyboards adaptive to keys, hand postures, and individuals: a hierarchical spatial backoff model approach", CHI 2013. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/41930.pdf
* S5 [V] Fowler, Partridge, Chelba, Bi, Ouyang, Zhai, "Effects of language modeling and its personalization on touchscreen typing performance", CHI 2015. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/43272.pdf
* S6 [V] Weir, Pohl, Rogers, Vertanen, Kristensson, "Uncertain text entry on mobile devices", CHI 2014. https://www.keithv.com/pub/uncertain/WeirEtAlCHI2014.pdf
* S7 [V] Buschek, Rogers, Murray-Smith, "User-specific touch models in a cross-device context", MobileHCI 2013. https://www.dcs.gla.ac.uk/~rod/publications/BusRogMur13.pdf
* S8 [S] Henze, Rukzio, Boll, "Observational and experimental investigation of typing behaviour using virtual keyboards for mobile devices", CHI 2012. https://www.uni-ulm.de/fileadmin/website_uni_ulm/iui.inst.100/1-hci/hci-paper/2012/2012-henze_observational.pdf
* S9 [V] Ouyang, Rybach, Beaufays, Riley, "Mobile keyboard input decoding with finite-state transducers", 2017 (arXiv 1704.03987). https://arxiv.org/abs/1704.03987
* S10 [V] Hard et al., "Federated learning for mobile keyboard prediction", 2018 (arXiv 1811.03604). https://arxiv.org/abs/1811.03604
* S11 [Vabs] Chen, Mathews, Ouyang, Beaufays, "Federated learning of out-of-vocabulary words", 2019 (arXiv 1903.10635). https://arxiv.org/abs/1903.10635
* S12 [V] Google Research blog, "The machine intelligence behind Gboard", 2017. https://research.google/blog/the-machine-intelligence-behind-gboard/
* S13 [V] Google patent US8782549B2, incremental feature-based gesture-keyboard decoding (first 100 000 characters of the page read). https://patents.google.com/patent/US8782549B2/en
* S14 [V] Touchtype patent US9639266B2, user input prediction with a user-specific touch model. https://patents.google.com/patent/US9639266B2/en
* S15 [V] Grammarly engineering blog, "Deep learning for swipe typing". https://grammarly.com/blog/engineering/deep-learning-swipe-typing
* S16 [V] Miller and Kostarevas, "FUTO Swipe: layout-agnostic neural swipe decoding", arXiv 2606.25247 (read the first 100 000 characters of the HTML), and the dataset card https://huggingface.co/datasets/futo-org/swipe.futo.org . https://arxiv.org/abs/2606.25247
* S17 [V] AnySoftKeyboard pull request 1870, gesture-typing write-up. https://github.com/AnySoftKeyboard/AnySoftKeyboard/pull/1870
* S18 [V] HeliBoard README. https://github.com/Helium314/HeliBoard
* S19 [V] AOSP LatinIME `forgetting_curve_utils.cpp`. https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/native/jni/src/dictionary/utils/forgetting_curve_utils.cpp
* S20 [Vabs] Quinn and Zhai, "Modeling gesture-typing movements", Human-Computer Interaction 33(3), 2018. https://research.google/pubs/modeling-gesture-typing-movements/
* S21 [S] Bi and Zhai, "Bayesian Touch", UIST 2013. https://research.google/pubs/bayesian-touch-a-statistic-criterion-of-target-selection-with-finger-touch/
* S22 [S] Azenkot and Zhai, "Touch behavior with different postures on soft smartphone keyboards", MobileHCI 2012. https://research.google/pubs/touch-behavior-with-different-postures-on-soft-smartphone-keyboards/
* S23 [V] Microsoft SwiftKey, "Sharing your typing data" FAQ. https://support.microsoft.com/en-au/topic/microsoft-swiftkey-keyboard-sharing-your-typing-data-faq-d737059d-8810-448e-b376-9af56171a37d
* S24 [R] This repository: `research/kestrel-ref/GLIDE_RESEARCH.md` (the 2026-11-14 survey, 194 tool calls; its ranked list is respected here and not repeated), `research/PREDICTION-SOURCES.md`, `research/LEARNING_RESULTS.md`, `research/KEYBOARD-APP-PLAN.md` sections 4, 8, 12, `docs/DICTIONARY.md`, `docs/KEYBOARD-EXPORT.md`.
* S25 [S] CleverKeys wiki (double letters "swiped in a small loop or just passed through once") and Google patent US 7,250,938 (a sub-gesture signals a double letter), found as search snippets. https://tribixbite.github.io/CleverKeys/wiki/typing/swipe-typing.html , https://uspto.report/patent/grant/7,250,938
* S26 [S] FlorisBoard `StatisticalGlideTypingClassifier` (header credits the AnySoftKeyboard write-up; a search-result mirror of the 2021 commit). https://github.com/florisboard/florisboard
