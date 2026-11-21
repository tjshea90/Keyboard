# R29 - "make glide predictions smart ... figure out how Gboard prediction is so good" (owner 2026-11-21)

The owner's words (TASKS R29): "it still types the word access a lot when i glide the word and, and it often types fur when i glide the word for, or the name Maggie when I try to type the word make, our the words our
or out when i try to type the word or. Make the glide typing predictions better by determining the most common words typed, grammatical knowledge for predictions, make the glide typing begin even faster ...
for glide predictions make it smart (for example, see if there is a way to train the typing engine on English sentences like a mini llm, or make it understand sentence context and grammar, etc). Figure out how gboard prediction is so good".

Nothing here ran on the tablet or the phone. Everything is a simulated finger (`GestureSim`, `SimNoise`) or the FUTO real-swipe corpus (phone-sized keys), through the real decoder (`core`, JVM). Sources for Gboard: `R20-glide-personalization.md`
section 3 and the list at its end (S3, S9, S10, S12, S13, S16), `PREDICTION-SOURCES.md`, `R27-context-grammar.md`.

## 1. Why Gboard is good (from the published work, not from its code)
1. **Data and a trained shape model, not a trick.** Its spatial model was replaced in 2017 by one LSTM trained with CTC on huge amounts of real swipes and taps, with the user's own reactions as labels (a reverted autocorrection is a negative,
   a picked suggestion a positive): about 15 % fewer bad autocorrects and 10 % fewer wrongly decoded gestures [S12]. Swoop's shape model is a hand-built dynamic-programming fit with 10-odd tuned numbers (`GlideParams`).
2. **A real language model inside the search.** The decoder is a finite-state machine over keys and the lexicon with the word model composed into it [S9]; the next-word box uses a 1.4 MB neural model (CIFG LSTM, 670 units, 10 000 words)
   trained by federated learning, better than the 5-gram baseline it replaced (top-1 16.4 % against 13.0 %) [S10]. Swoop has word, pair and three-word counts from the shipped list and from the owner (`GlideDecoder.prior`, `PersonalModel`).
3. **Per-user adaptation that is measured.** Gboard moves its key centres by the mean offset of the last 250-800 taps [S3]; Swoop has the touch field (R20d), shape memory (AX1), correction memory ("meant B, guessed A") and `revisePrevious`.
4. **Post-correction only when confident** [S9]. Swoop re-judges the word before only with margin 1.0 and never right after an undo (R26a).
What Swoop cannot copy offline: the training corpus and compute. A neural model of Gboard's size is about 300 lines to RUN in Kotlin, but needs a few hundred million words and a GPU to TRAIN; the only text in the sandbox is the shipped list and the
FUTO sentences (encyclopaedia text). So the honest path is better use of the n-gram knowledge plus the owner's own data, which is what R26-R29 did.

## 2. The four slips, one by one
**or -> our / out.** `OrOurDiagTest` (simulated finger, shipped decoder, no learning), first choice for the glide of "or":

| keys / finger | no word before | after "to" | after "yes" |
| :- | :- | :- | :- |
| tablet portrait, aim 8 dp | 99.8 % | **16.5 %** (our 500 of 600) | 100 % |
| tablet portrait, aim 12 dp | 98.2 % | | |
| phone, aim 8 dp | 94.7 % (of, out) | | |
| phone, aim 12 dp | 78.0 % (of 58, out 17, it 15, is 10) | | |
"our" lies ON the straight line o -> r (the u key is between them): the two words have the same shape, so only the language can decide, and after "to" the shipped pair list (correctly) says "to our" is far likelier than "to or".
That is grammar working, not a defect; the glide cannot tell them apart and nothing in the real swipes does either (a dwell / speed cue was measured: no signal, R24 7.2). What helps the owner: both words are in the strip, a pick teaches
"meant or, guessed our" (correction memory) and from alpha8 on a KEPT wrong guess is no longer learned as if it were right (R29a, `LearnGuard`).
**and -> access, for -> fur, make -> Maggie.** NOT reproduced by the plain decoder (and 100 %, for 98.9 %, make 99.3 % on phone keys at aim 8; 100 % on tablet keys) nor with learning switched on (below). "for" -> "fur" is a corner cut
(the finger rounds the turn at o and passes over u), which a simulated finger that does not cut corners cannot show; a real thumb does. The only way left to see it is the owner's own report lines (Diagnostics: "learning changed the first guess",
"touch plane up to"): alpha8 has them and the owner has not sent them yet.

## 3. What was measured and what it showed
* **A price for every letter of a word (`GlideParams.letterW`, default 0 = off).** Tried because extra on-path letters ("our") add free bends: it lifts "or" from 62-75 % to 93 % (phone, aim 8) and 80 % (aim 12), but costs accuracy everywhere
  else: all-words yardstick (`LetterWEvalTest`, 1 200 frequency-weighted words) phone normal 91.7 -> 87.3 first-choice at 0.5 and 84.7 at 1.0; tablet portrait 96.4 -> 94.2 and 90.1; real swipes (1 338, `RealSwipeEval`) everyday words
  91.0 (off), 91.3 at 0.25, 91.0 at 0.5, 89.8 at 1.0, all words 87.9 / 87.4 / 86.5 / 84.1. Not shipped; the knob stays for later experiments.
* **Does what the keyboard learns make things worse?** `KeptErrorsLabTest` with the reach the app really uses (R28a), 3 000 glides of everyday words, first choice, learned vs a keyboard that learned nothing:

  | | learned | plain | without shape memory | without personal counts |
  | :- | :- | :- | :- | :- |
  | tablet landscape, ideal user | 96.3 | 95.8 | 96.0 | 96.1 |
  | tablet portrait, ideal user | 95.1 | 95.3 | 94.7 | 94.9 |
  | phone, ideal user (earlier run) | 80.0 | 77.1 | 78.2 | 76.9 |
  | tablet landscape, keeps 40 % of its wrong words | 96.1 | 95.9 | | |
  | tablet portrait, keeps 40 % of its wrong words | 94.8 | 95.3 | | |
  Learning is level to slightly better on the tablet and clearly better on the phone, even when the person leaves 40 % of the wrong words in the text. (An earlier run that forgot to give the lab the shortened reach showed learning 1.2 points
  WORSE on the tablet: that was the lab, not the app.) The guard acted 0-3 times in 3 000 glides, so it is a safety net, not a change of accuracy.
* **Real swipes, phone keys (FUTO, 1 338 words in the list):** first / top-3: shipped decoder 87.9 / 94.7 (everyday words 91.0 / 96.8); without the word before it is about 2 points lower (84.8 on the older default). Short words read as a word 2+ letters longer: 1.6 % of
  all short words. Words missed are mostly names and rare words (sled -> skilled, rule -> type), not "short -> long".
* Earlier rounds that did NOT help and stay off: word-class grammar back-off (R27b), dwell / speed cue (R24 7.2).

## 4. What R29 changed in the app
`LearnGuard` (a kept wrong look-alike is learned lightly and teaches no shape or thumb habit when only learning put a rarer word in front of a 20 times commoner one), the shadow decode that counts when learning changed the first guess,
`GlideDecoder.staticLn`, extra Diagnostics lines, and the labs (`KeptErrorsLabTest`, `FocusWordsLabTest`, `LetterWEvalTest`, `OrOurDiagTest`: all skipped unless REAL_TABLET=1). Glide start (one letter typed when a glide begins) was
R28b (alpha8): 2.5 dp start threshold, the padding fix, the second-thumb hand-over; nothing more was changed here because nothing in the sandbox can show a thumb's first 30 ms.

## 5. What would move it next (needs the owner or data)
1. The owner's Diagnostics lines after a day of use ("learning changed the first guess", "touch plane up to"), and the exact words typed before a slip: one example of "word before + glided word + what came out" is worth more than any simulation.
2. A small neural next-word model: needs a corpus and training outside this sandbox; the app side (an LSTM forward pass, int8) would be a contained job. Not started, not promised.
3. A cue that separates words with the same shape ("or" / "our"): only the glide's timing could, and the corpus shows none.
