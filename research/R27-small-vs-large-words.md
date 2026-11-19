# R27a - small words read as large words (the -> there, and -> access)

Owner 2026-11-19, his words: "The keyboard has a hard time differentiating from small and large words. Sometimes when i try to type a small word like the or and, it types there and access."

## What the data says
Real swipes (FUTO corpus, phone keys, slice A, 6 054 glides, `REAL_DIAG=1`, `shortLong()`):
* Short words (4 letters or fewer): 3 090 swipes, **9.1 % first-choice misses**, about the same as long words. A short word read as a word 2 or more letters longer: **27 of 3 090 = 0.9 %**. "the": 456 swipes, **1 % missed**; "and": 151 swipes, 4 %.
* The commonest confusions are the same length (as > add 18, has > had 15) and long > short (there > the 8, three > the 7). Short > long is rare.
So the owner's complaint does NOT reproduce on real phone swipes.

Simulated finger on TABLET key sizes (`TabletStudyTest`, `REAL_TABLET=1`; key pitch 30 / 57 / 96 dp, finger aim 8 / 12 / 16 dp in absolute dp): at 57 and 96 dp pitch the 24 commonest short words are right **98 to 100 %** with **0 short-for-long slips** under every parameter set;
at the phone size and a very wobbly finger (aim 16 dp) the old parameters gave 6.0 % short-for-long and the pitch-aware ones of R24b 2.5 %. The bigger keys are not the cause either.

## Why the geometry cannot settle "the" against "there"
`the` and `there` end on neighbouring keys and the path of a "the" glide (h to e) passes right under `r`: the shape alone fits both. The language has to decide, and it did not have enough say on a clean glide: `lmMin` 0.12 (the weight is
`lmA` x the best fit's cost per letter, clamped) turned the 25-fold preference for "the" into about 0.4 cost units. Raised to 0.45 (R24b / section 7.3 of research/R24-glide-next.md): a small gain on both real slices.

## What is left, and what was built for it
Because neither real data nor the tablet-sized simulator reproduce it, the cause is most likely something LEARNED on the owner's tablet (a thumb habit or touch plane learned from words that were wrong, then applied to every glide). Built so his next paste shows it:
* Diagnostics: "learning changed the first guess N times (you kept the learned word M times, you picked the plain decoder's word K times)": a SHADOW decode with no personal learning runs after each glide (after the answer is out: no added delay) and is compared with the real answer; and "touch plane up to X dp".
* Settings > "Forget where my thumb lands": clears only the thumb habit and the touch plane and keeps every word, pair and correction. If the slips stop after it, that is the cause.
Tests: `ShadowAndThumbTest`.
