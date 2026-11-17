# The layout (owner request M1: "model the keyboard layout exactly like the attached photo")

The photo is a Gboard screenshot (dark teal). It reached the session of branch `ccr-5531c47b-hl1gzo` on 2026-11-17 (the first release, 0.4.0-alpha1, was modelled from the words alone, with ?123 at the bottom right). Everything below was MEASURED from the photo (1280 px wide) and is tested (`ImeLayoutTest`, `KeysViewLookTest.theDarkLettersPageHasThePhotosColours`).

## Geometry (units: one letter-key pitch = 1)
| Row | Keys |
| :- | :- |
| toolbar | settings, clipboard, translate, spread evenly across the strip (the photo's grid, emoji and microphone buttons are NOT built: the owner listed which icons he wants) |
| 1 | q w e r t y u i o p, a small digit hint 1-0 in the top-right corner of each (a held key types its digit) |
| 2 | a s d f g h j k l, indented half a key at both ends |
| 3 | Shift 1.5, z x c v b n m, Backspace 1.5 (Z stands under S, M under K: all three rows share one pitch) |
| 4 | ?123 1.5 (round, pale blue, LEFT), comma 1, space 5 (blank), full stop 1, Enter 1.5 (round, cyan, RIGHT) |
| below row 4 | nothing: no bottom bar, no down arrow, no globe, no emoji key, no voice key |

Every row is exactly 10 units wide. A key is a slot minus a gap (`MiniKeyboardGeometry.slots`), the gap across a row is about 14 % of the pitch, the rows are 1.5 times as far apart as the keys are across, the keys are 1.35 pitches tall (44-56 dp), the corners are 22 % of a key's width, the round keys are full stadiums.

## Colours (dark by default; the light palette is a plain counterpart)
The keyboard is DARK until the person chooses otherwise (Settings > Colours: Dark / Automatic = follows the phone's day or night setting / Light), because the photo is the dark theme and a tablet or phone left in day mode would otherwise show a light keyboard (0.5.0-alpha3; before that the default was Automatic). `KeyboardPresenterTest.theKeyboardIsDarkUntilAnotherLookIsChosen`.
background `0d1d1d`, letter keys and space `253535`, Shift / Backspace / comma / full stop `35494a`, ?123 `a4cedc` with dark text, Enter and the lit translate icon = the accent (cyan `00dde6` by default; dark ink on bright accents, white on dark ones), text `f1ffff`, digit hints `9babab`. Icons (Shift: outline when off, solid when on, bar when locked; Backspace: outlined tag with a cross; Enter: return arrow) are DRAWN, not font glyphs (the tablet's fonts may lack them).

## Readings chosen where the owner's words were ambiguous (he is away; change any of them with one setting or one line)
1. **"the bottom right button brings up special characters"**: in the photo the bottom right key is Enter and ?123 is at the bottom LEFT. The keyboard follows the photo (?123 left, Enter right) AND gives the special characters from the bottom right area too: HOLD the full stop (or the comma) and slide to a symbol (a grid of 30). One special character typed from either way returns to the letters at once (digits stay).
2. **Number row**: the photo has none (digits are the hints), an earlier request (N1) asked for numbers on top at all times. The photo wins as the default; Settings > Layout > Number row puts the row back (then every page has it).
3. **Symbols pages**: four rows like the letters page (the keyboard never changes height). Page 1: digits, @#$%&-+()/, then =\< (to page 2) * " ' : ; ! ? and Backspace. Page 2: ~`|•√π÷×¶∆ / £¢€¥^°={}\ / ?123 (back to page 1) _<>[]©®. Every symbol of the earlier single page is still reachable.
4. **Hide the letters** (M4): the letters and the digit hints are not drawn; the key shapes stay, so the blank panel still shows where the keys are, and every key still types and glides.
5. **System bar and Android 15+ (edge to edge)**: the little down arrow, the pill and the globe under the keyboard in the photo are drawn by Android itself on the phone (gesture navigation), not by the keyboard; a keyboard app cannot remove them. With only one keyboard enabled Android normally leaves the globe out. Since Android 15 the system draws its navigation bar OVER the keyboard window: 0.5.0-alpha1 let it cover the bottom row on the Android 16 emulator, 0.5.0-alpha2 reads the bar's height (`WindowInsets` navigation bars, Android 15+ only) and stands the keys above it, with the keyboard's colour filling the strip behind it (`KeyboardRootInsetsTest`, emulator run on API 36).
