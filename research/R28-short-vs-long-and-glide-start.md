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
RESULTS-PLACEHOLDER
