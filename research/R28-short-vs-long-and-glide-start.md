# R28 - short words read as long ones, and glides that do not start (owner 2026-11-20)

Owner, 2026-11-20, his words: "The keyboard frequently incorrectly predicts large words for short glide words. For example, a lot of times when i try to glide type simple words like the or and, it predicts there or access. Also, sometimes the glide doesn't properly start. Make the glide sensitivity higher, so that as soon as i start moving my finger it recognizes that I'm glide typing."

Follows R23c (length term), R27a (shadow decode, thumb reset) - read research/R27-small-vs-large-words.md first. Nothing here was run on the tablet.

## 1. Glide start (R28b): four real bugs, all found by reading the touch code and all reproduced by a failing test first

| # | What happened | Where | Fix |
| :- | :- | :- | :- |
| 1 | A glide counted as started only after the finger had moved 5 dp. The trail, the live guess and "this is a glide" (which protects it from a second touch) all waited for that. | `KeysView.GLIDE_START_DP` | 2.5 dp. A long press that already shows its digit keeps 8 dp (`GLIDE_START_HELD_DP`), so a rolling thumb does not lose the digit. A press that only wobbles still types its letter once (`stayedOnKey` decides at the lift, unchanged); the live guess waits for 0.45 key widths of path, so a wobble cannot clear the strip's guesses. |
| 2 | **The 5 dp of padding between the strip and the top row of keys belonged to the strip** (`y < strip + pad`). A thumb that lands on the top edge of a top-row key (t of "the", e, o, p, i of "it" ...) hit "the strip": with nothing to press there the glide never started, nothing was typed, only the key-press buzz. With guesses on the strip (always, right after a glide) it was a guess button. | `KeysView.pressDown / toolAt / chipAt` | The strip ends where the keys begin (`inStrip(y) = y < stripPx()`); the padding snaps to the top row like every other gap already did. |
| 3 | **A second thumb that landed while the first was still gliding was thrown away.** Two-thumb typing on a tablet: the right thumb is still finishing "the" when the left thumb lands for "and". The second touch was ignored for its whole life, so the next word's glide silently did not start. | `KeysView.onTouchEvent` | The second finger is recorded from the moment it lands (`waitId`, `wtx/wty`); when the first finger lifts and the second has moved, its recorded path becomes the glide in progress (`takeOverWaiting`), without a second key-click. A finger that only rests (a palm) still types nothing. |
| 4 | (Checked, not a bug) the first touch point is never lost: the point of the DOWN is stored, every historical point of every MOVE is added, the UP point is added. The decoder pins the first letter to the first resampled point, so a path that did not start where the finger went down would break the word; it does start there. | | |

Tests (`KeysViewTest`): a 1 dp tremor is a press, 3 dp is a glide, a finger that comes back types its letter once; a glide that starts 3 dp above the top row glides (red before the fix); a second thumb's glide is not lost (red before); a resting second finger types nothing; the view reports numbers about strokes and hand-overs.

Diagnostics (new line, numbers only): `touch: N glide strokes, avg P touch points in D ms (a point every X ms), first move after avg F ms (worst W ms); second thumb took over T times, a resting second finger was ignored R times; S strokes began on the padding above the top row`. If glides still fail to start after this release, that line says how the tablet's screen delivers touches.

## 2. Short word read as a long one (R28a)

### 2.1 What was measured again
* Real phone swipes, slice A (6 054 swipes, baseline reproduced this session): short words (4 letters or fewer) 3 090, missed 257 (8.3 %); read as a word 2+ letters LONGER: **25 = 0.8 %**; long read as 2+ shorter: 143 of 2 963 (4.8 %). "the": 456 swipes, 1 % missed; "and": 151, 4 % (real examples: and -> anna/ana). So on real thumbs the decoder errs much more towards the SHORT word, the opposite of the owner's report.
* The R27a tablet study drew the tablet keyboard with a height of 0.55 x the width (rows 1.4 key pitches apart). The keyboard caps a row at 56 dp (`ROW_H_MAX_DP`), so on the owner's tablet (600x973 at 170 dpi = 564x916 dp) a row is 1.2 pitches apart in portrait and **0.75 in landscape** (a phone: 1.4). `FlatKeysStudyTest` repeats the study with the real row heights: the 24 commonest short words, first choice right **99.6 to 100 %**, short-for-long **0.0 %** at every finger noise (aim 8 and 12 dp), portrait, landscape and the old geometry alike; frequency-weighted words 94.6 to 96.7 % first (a flat keyboard costs about one point). Geometry is not the cause.

### 2.2 What is left: state the keyboard learned
`LearnedShortLongLabTest` (REAL_TABLET=1; LAB_VARIANTS / LAB_GEOS pick the runs): a simulated person (own habit per word, finger scatter 10 dp absolute) glides 3 000 everyday words on the tablet geometry with the real row heights; the keyboard learns as in the AX1 lab (words, pairs, corrections, glide shapes, thumb habit) and EVERY glide is also decoded by a decoder that learned nothing ("plain"). First-choice rate learned / plain, all words and the short ones (4 letters or fewer), and how often a short word came out 2+ letters longer:

| geometry, finger habit | variant | all | short words | short->long |
| :- | :- | :- | :- | :- |
| portrait 564x272, habit 0 | all learning on (what ships) | 95.7 / 96.9 | 96.3 / 98.1 | 2 / 0 |
| | no glide-shape memory | **97.3** / 96.9 | 98.0 / 98.1 | 2 / 0 |
| | no personal words/pairs/corrections | 97.0 / 96.9 | 98.2 / 98.1 | 0 / 0 |
| | no thumb habit / no touch plane / no correction pairs | 95.6 / 95.5 / 95.7 | 96.2 / 96.0 / 96.3 | 2 / 2 / 2 |
| landscape 916x272, habit 0 | all on | 94.7 / 96.6 | 95.8 / 97.9 | 2 / 1 |
| | no glide-shape memory | **96.8** / 96.6 | 97.7 / 97.9 | 5 / 1 |
| portrait, habit 10 dp | all on | 94.4 / 95.3 | 95.1 / 96.7 | 5 / 2 |
| | no glide-shape memory | 94.7 / 95.3 | 95.2 / 96.7 | **27** / 2 |
| landscape, habit 10 dp | all on | 94.6 / 95.8 | 95.4 / 97.9 | 4 / 2 |
| | no glide-shape memory | 96.0 / 95.8 | 97.0 / 97.9 | **18** / 2 |
| phone 292x161, habit 0 / 10 | all on (first run, per stretch of 1 000 glides) | 88.8-90.2 vs 87.4-89.3 / 78-81 vs 76-78 | | 1-9 / 1-11 |

Reading:
* On tablet-sized keys the keyboard's own learning makes the FIRST choice 1 to 2 points worse than a keyboard that learned nothing, for a clean finger and (a little less) for one with habits; on a phone-sized keyboard it helps when the hand has habits (+2 to +4 points) and is neutral otherwise. Short words lose more than the rest (-1.8 / -2.1 points in the clean case).
* The part that hurts on big keys is the glide-shape memory (`patGain` 2.5, up to +4.5 cost units, for any word whose path fit is within `patMaxGap` 2.2 pitch squared per letter of the best). Words / pairs / corrections alone are neutral to slightly positive; shapes alone are neutral; the two together over-count.
* Why it grows with key size: the shape lift and the "path fit may be this much worse" gaps are fixed numbers in key-pitch units tuned on a phone, where finger scatter is 0.3 to 0.4 pitch and the margins between look-alike words are small. On a 55 to 90 dp pitch the same finger scatters 0.1 to 0.2 pitch, the right word fits almost perfectly and a wrong look-alike is clearly worse; the same fixed lift can then overrule a clean path (the -> there, and -> access).
* The glide-shape memory also keeps the personal word counts from running away: without it the short words that came out longer rose from 5 to 27 (portrait) and 4 to 18 (landscape) of about 1 000 short glides, with it they stay near the plain decoder. So switching it off is not the answer; its reach has to follow the key size.

### 2.3 The fix, measured (what ships): the learned reach follows the key size
Second sweep, same lab, tablet geometry (first choice learned / plain, all words; short words; short->long): the fixed phone-tuned reach of learned lifts (`patMaxGap` 2.2, `confMaxGap` 0.9 pitch squared per letter) scaled by the finger's scatter in pitches:

| variant | portrait h0 | portrait h10 | landscape h0 | landscape h10 |
| :- | :- | :- | :- | :- |
| before (all on) | 95.7 / 96.9 | 94.4 / 95.3 | 94.7 / 96.6 | 94.6 / 95.8 |
| reach x (45/pitch)^2 | 95.8 | 94.5 | 96.1 | 95.5 |
| reach x 0.5 x (45/pitch)^2 | **96.5** | **95.1** | **96.9** | **96.3** |
| shape lift gain 1.25, cap 2.5 | 96.6 | 94.6 | 95.7 | 95.6 |
| shape lift cap 2.0 | 96.1 | 94.1 | 95.2 | 94.6 |
| **shipped** `GlideParams.learnedReach(pitch)`: 1 up to 45 dp, then (45/pitch)^2 x (1 - 0.5 x ramp over the next 10 dp) | 96.5 / 96.9 (short 97.6 / 98.1) | 95.1 / 95.3 (96.2 / 96.7) | 96.9 / 96.6 (98.2 / 97.9) | 96.3 / 95.8 (97.5 / 97.9) |

* The tablet in landscape goes from 2 points BELOW a keyboard that learned nothing to 0.3 to 0.5 above it; in portrait from 1.2 / 0.9 below to 0.4 / 0.2 below. Short words: -1.8 -> -0.5 (portrait, clean), -2.1 -> +0.3 (landscape, clean).
* Short words read as a word 2+ letters longer: 2 to 6 of about 1 000 short glides, the plain decoder 0 to 2; the counts are too small to separate (the simulated finger hardly ever makes the slip the owner reports). Whether this removes the owner's slips on his tablet is NOT shown here; it removes the one mechanism that could be demonstrated.
* Phone keyboards (pitch up to 45 dp) are bit-for-bit unchanged (`learnedReach` is 1), so the real-swipe numbers of R24b / R26a / R27a stand as they are.
* Tried and rejected: lowering the shape-lift cap (worse everywhere), switching the shape memory off (it keeps short-for-long from running away: 5 -> 27), switching the thumb habit / touch plane / correction memory off (no effect on this).

## 3. What is NOT known (honest limits)
* No real swipe from the owner's tablet exists. Everything about the tablet is a simulated finger on the tablet's real key geometry. The new `touch:` Diagnostics line and the existing `learning changed the first guess` / `touch plane up to` lines are what will say more.
* Two things were measured to NOT explain the slips: key geometry (flat keys, section 2.1) and the decoder on real swipes (0.8 %).
* If the slips continue after this release: paste Diagnostics (it now carries the touch line), and try Settings > Forget where my thumb lands; a glide whose first guess changes after that is a learned-state case.

## 4. Where the code is
`KeysView` (start threshold, strip boundary, second-finger hand-over, preview gate, touch notes), `KeyboardPresenter.onTouchNote`, `InputStats.touchLine`, `Diagnostics`, `GlideParams.learnedReach / forPitchDp`; tests `KeysViewTest` (glide start section), `DiagnosticsTest`, `GlideParamsPitchTest`; labs `FlatKeysStudyTest`, `LearnedShortLongLabTest` (both skipped unless REAL_TABLET=1).

