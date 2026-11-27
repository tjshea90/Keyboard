# R38 - his training file (alpha16), glides that do not start at the edges, one letter instead of a glide, and the erase key (owner 2026-11-27)

Owner, his words: "[attached: Swoop glide training 2026-11-26 2239] first see if the fixes were merged from earlier. Then optimize from the training file. It still often does not register that I'm glide typing, especially at the edges of the keyboard. It often types one letter when I'm trying to glide. Sometimes i have a hard time pressing the erase key because I'm moving fast. Try to make it bigger and easier to press. Without ruining anything else"

Nothing below was run on his tablet or an emulator (the sandbox has no KVM): the numbers are the file replayed through the shipped decoder, and Robolectric touch tests on API 29.

## 1. Were the earlier fixes merged? (R38-1)

* Every remote branch of this repo (15, from `ccr-259fcbb5` to `claude/great-fermi-3coauw`) is an ancestor of this session's branch (`git merge-base --is-ancestor`): `tools/sync.sh` merged them all, nothing is lost.
* The repo's default branch `ccr-c57dc9d3-149psl` is 0 commits ahead and ~1 160 behind: none of the work (R17 .. R37) is on it. That is the owner's call (CLAUDE.md, `tools/MIRROR_MAIN` is off); every APK he installs comes from a Release, not from a branch.
* The last release is `swoop-v0.7.0-alpha16` (R34 alpha13-15: e-mail fields, domains after @, glide start, translation switches itself off; R36 alpha16: Glide training). His file says `app 0.7.0-alpha16`: the fixes of R34d ("glide start instant") WERE installed when he made it, and the problems below are what is left after them.
* The R37 lane (`great-fermi`, setup tidy-up) is merged too but not finished: its checkpoint still lists `tools/fit-brief.py` and the ticks R37-1..5.
* The Kestrel browser's copy of `core/.../input` is NOT updated (no checkout of that repo in this container; one counter added to `InputStats` here only).

## 2. His file (R38-2)

447 glides, 13 pages (8 running text, 5 drills), key pitch 37.5 dp (keys 34.4 x 33.9 px: small, number row on), view 404 x 246 px at density 1.0625.

| | first word | in the first three |
| :- | :- | :- |
| what the keyboard offered when he made them | 87.7 % | 96.2 % |
| shipped decoder alone, replayed now | 85.9 % | 96.0 % |
| decoder + shape network, replayed now | 86.6 % | 96.2 % |
| running text only (299) | 89.0 % | 95.0 % |
| look-alike drills only (148) | 81.8 % | 98.6 % |
| replay with teaching after every glide | 86.6 % vs 85.9 % for a keyboard that learns nothing | |

* 47 of the 60 misses are "the right word is in the first five but not first" (order / language); only 13 are "the path does not say it". The commonest: our -> or (5), meet -> met (4), off -> of (4), too -> to (4), good -> god (3), thank -> than (2), out -> it (2), if -> of (2). They are the look-alike twins the drills ask for on purpose (o-r and o-u-r are the same straight line through u): no path-shape change separates them; only the words before and what he picks can.
* The sweep of `shapeW` / `visitCap` / `lmMin` on all 447 glides peaks 1.3 points above the shipped values (87.9 against 86.6 %, six glides) and sits on drills, which over-represent the longer look-alike (the sweep on running text alone is in section 5). Not adopted on that alone.
* What the file CANNOT show: every line is a glide that REGISTERED. A glide the keyboard read as one letter, or never saw, is not in it. So the three complaints that are about glides that do not start can only be found in the touch code and in how his touches are spread (section 3).

## 3. How his thumbs touch (from the 447 first points)

* Rests before it moves: median 106 ms, 90 % under 138 ms, longest 213 ms. A thumb that lands for the next word while the other is still finishing the last one has typically NOT moved yet when the first thumb lifts.
* Starts outside the key the word begins with: 156 of 447 (35 %); mean offset 8.9 px to the right and 3.8 px above the key centre, scatter 22 px across, 10 px up and down.
* Home and bottom row: he lands a median 7 px ABOVE the centre (p5 at the key's top edge).
* Edge keys: first letter `a`, `l`, `p`: median 17 px right of the centre (the key's right edge). Nothing starts left of x = 31.6 px (30 dp: the width of Android's back-swipe zone at the SCREEN edge); on the right side starts reach x = 403 of 404, and 'p' starts stop at 22 px beyond the centre where 'l' starts reach 30 px (a touch beyond the panel's edge lands in the dead strip of a squeezed keyboard).
* No registered glide came within 30 px of being read as a tap (nearest: "we", 29.9 px outside its key; the tap margin is 8.5 px): the tap-or-glide rule is not what loses his glides.

## 4. What loses a glide, found by reading the touch code (R38-3)

1. **The system's back swipe at a screen edge.** With gesture navigation, a touch that starts within ~30 dp of the left or right edge of the screen is the "back" swipe: Android takes the finger away from the keyboard (a cancel). A glide that starts on q, a, p, l or the erase key dies before it began. The keyboard never asked Android to leave its edge strips alone. Fix: `KeyboardRoot.keepEdgeSwipeOffTheKeys` (API 29 `setSystemGestureExclusionRects`, two 56 dp strips, the system keeps 200 dp of height per edge).
2. **A thumb that lands for the next word, then the first thumb lifts before it has moved.** `takeOverWaiting` dropped it whole (every later move ignored): the next word's glide silently did not start. With his 106 ms rest this is the normal two-thumb rhythm. Fix: a finger that landed less than `RESTING_MS` (300 ms, longer than any rest in his file) ago is kept; if it only lifts again it types nothing (a palm stays a palm), if it moves it is the glide.
3. **Beside a squeezed keyboard.** The window of a squeezed docked keyboard is as wide as the screen and the strips beside the keys belonged to nobody: a glide that began a few mm outside q or p did nothing. Fix: a touch that starts in the 36 dp beside the keys is handed to the keys view (nearest key).
4. (Checked, no change) tap or glide at lift: all of his glides leave their key by 30 px or more.

## 5. The erase key (R38-4)

* **Pressed with the other thumb while one thumb is gliding: thrown away.** Every second touch during a glide was recorded and dropped; erase is a key whose press is real. Now it erases at once (the first thumb's glide goes on), repeats while held, and stops at lift (`KeysView.startSideErase`).
* **Bigger:** no letter may move (the glide shapes, the thumb habit and his learned words are measured on the letters' places), so the key grows into the gaps around it: half a gap left, a little into the margin at the right, half a gap above and below (about 24 % taller, 12 % wider on his tablet); the neighbours keep their boxes.
* **Easier:** the key under a thumb that lands in a gap is now the one with the nearest EDGE, not the nearest centre: a wide key (erase, shift, enter, space) used to lose the gap beside it to a narrow letter.

## 6. Where the code is

`KeysView` (`keyAt`, `widenErase`, `startSideErase`, `takeOverWaiting` / `noTap`), `KeyboardRoot` (`keepEdgeSwipeOffTheKeys`, `inKeyMargin`), `InputStats.touchLine` (three new counters: erase during a glide, finger kept, margin touches), tests `KeysViewTest` (R38 section), `KeyboardRootTest` (R38 section), `DiagnosticsTest`; lab `PracticeEval.trainingReport` (running-text misses one by one and the big-key values on running text alone).
