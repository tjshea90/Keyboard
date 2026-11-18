# R23c - "the" must not become "there" (gesture length)

Owner 2026-11-18: "right now the keyboard isn't very good at predicting between small words, notably 'the' often becomes 'there'. 'The' uses a shorter gesture than there, so it shouldn't confuse them."

## What was wrong
`the` and `there` start and end on the same keys (t ... e). The decoder judged only how close the finger path came to each word's letters, so a glide of `the` that wobbled toward `r` fitted `there` almost as well, and a glide of `there` whose e-r-e detour was cut short fitted `the` as well. Nothing used the one thing that differs: how long the path is.

## What was added
`GlideDecoder.lengthCost()` (called by `careful()` and `refine()`): the log ratio of the drawn length to the ideal path through the word's letters, times `lengthExpect` (0.92: fingers cut corners); a Gaussian-like cost z^2/2 with a tighter sigma for too short (0.16) than too long (0.30), priced by `lengthW` 1.0 and capped at `lengthCap` 4.0. Words with fewer than 2 distinct letters are not charged.

## Measured (GlideLengthEvalTest, simulated finger over the real key layout, 21 look-alike pairs: the/there, he/her, we/were, to/top, an/and, in/into, on/one, is/its, for/form, of/off, or/ore, at/ate, us/use, so/some, me/men, no/now, be/bed, do/does, how/show, all/also)
| finger | short word first | long word first | yardstick top1 / top3 (700-1200 common words) |
|---|---|---|---|
| normal, term off | 97.0 | 71.9 | 92.4 / 98.8 |
| normal, term on (shipped) | 96.9 | 76.6 | 93.4 / 99.1 |
| sloppy, term off | 89.3 | 64.1 | 82.0 / 93.3 |
| sloppy, term on (shipped) | 89.4 | 67.4 | 84.1 / 94.8 |

Sweeps of lengthW 1-3, lengthExpect .85-1.0 and the two sigmas: stronger variants lift the long word a little more (up to 79-80 %) but cost the short word 1-5 points on a sloppy finger and the yardstick 1-2 points. The shipped values are the best all-round.

## Honest limits
* The simulated finger already finds `the` 98-100 % of the time (it does not wobble toward `r` the way a real hand does), so the pair numbers show the term helping the LONG word and costing the short word nothing; the owner's real `the` -> `there` slips cannot be reproduced here and the fix is not yet confirmed on the tablet.
* `there` itself is still often read as `the` by the simulated finger (e-r-e is a detour of about one key and gets cut): the term moves it from 0 % to 3-6 % first choice and keeps it in the strip. A longer-hold / loop detector would be the next step if the owner reports it.
* Next step if the tablet still shows the slip: record the glide length of each corrected `the`/`there` pair (counters only, never text) and tune lengthW per person, or learn lengthExpect from the owner's accepted glides like the touch plane does.
