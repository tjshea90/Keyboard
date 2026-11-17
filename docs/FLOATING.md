# Floating keyboard (N3) - design

Owner, 2026-11-17: "make the keyboard able to detach from the bottom of the screen and float and be resizeable like the browser keyboard. maybe have a neon bar at the top if i long press it the keyboard detaches."

## What the person sees (the browser's mini keyboard, research/kestrel-ref and the Browser repo's research/MINI_NAV.md AV1)
* **The neon bar (the grip)**: a short glowing bar in the middle of the top edge of the keyboard, always there while the Floating setting is on.
* **Docked** (default): the keyboard sits at the bottom as today. LONG PRESS the bar (about 350 ms, a short haptic tick) and the keyboard lifts off: it keeps following the finger, so one gesture detaches and places it. Dragging the bar UP by more than 28 dp does the same without waiting.
* **Floating**: drag the bar and the keyboard follows anywhere on the screen (kept fully on screen). Four L-shaped corner marks hug the corners: drag a corner to make the keyboard wider and taller (the key labels grow and shrink with the keys). The bar lights up while floating. Let go of the bar with the keyboard's bottom within 24 dp of the screen bottom, or double-tap the bar: it docks again (the size is kept). The app behind the keyboard is never pushed or resized while it floats: it simply sits behind it.
* Position and size are remembered (as fractions of the screen and a width in dp), so rotating the phone keeps it on screen. Settings > Layout and look > "Floating keyboard" switches the whole feature (bar included) off; "Reset settings" docks it and forgets the size.

## How (Android's rules for a keyboard window)
* The input method window is made FULL SCREEN and transparent while floating (`Window.setLayout(MATCH_PARENT, MATCH_PARENT)`); the keyboard panel is a child positioned by margins inside it (`KeyboardRoot` / `FloatingLayout`). Docked, the window is `MATCH_PARENT x WRAP_CONTENT` as before.
* `InputMethodService.onComputeInsets` while floating: `contentTopInsets = visibleTopInsets = window height` (the app believes the keyboard covers nothing: no resize, no panning) and `touchableInsets = TOUCHABLE_INSETS_REGION` with the region = the panel's rectangle plus the four corner grab squares. Every touch outside that region goes straight to the app behind. Docked, the default insets are untouched.
* Safe by construction: floating is entered only by the person's gesture; `CrashGuard` safe mode forces docked; the globe key stays on the bottom row; a floating panel cannot be dragged off screen; the dock gesture (double tap on the bar) always works.
* The decoder needs the real key positions after a resize: `KeysView` still computes them from one function (`MiniKeyboardGeometry.boxes`), so a resize only changes the view's size and `prepareGlide()` is called once when the drag ends.

## Geometry
`floatX`, `floatY` in 0..1000 (position of the panel's top-left within the free space), `floatW` in dp (232..620 and at most the screen width), `floatH` x100 (key row height multiplier 70..220). The first time: width = min(screen width, 360 dp) (a phone) or 460 dp (a tablet), near the bottom, centred.

## Tests
Robolectric: bar long press detaches and a drag moves it; clamp to the screen; corner drags resize within limits and the keys still type and glide; dock by double tap and by dropping at the bottom; settings persistence; `onComputeInsets` values in both modes; the touch region; safe mode docks. Not run: the real tablet and phone (touch feel of the 350 ms and the 26 dp corner squares, how each Android version treats a full-screen transparent IME window) - the emulator smoke run on GitHub checks the window really is full-screen and touches pass through.
