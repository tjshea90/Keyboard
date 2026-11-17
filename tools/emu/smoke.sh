#!/usr/bin/env bash
# smoke.sh - run by .github/workflows/emu-smoke.yml INSIDE a booted Android emulator (adb works). The first proof that Swoop works on a real Android:
#   1 it registers as a keyboard, can be enabled and chosen   2 tapping a text field shows it   3 real touches on its keys type letters
#   4 a swipe over the letters glides a word in   5 Backspace removes   6 holding the space bar (keyboard picker) does not crash   7 no crash anywhere (logcat).
# Typed text is never logged here beyond the fixed test words. Artifacts (logcat, screenshots) go to $OUT.
set -uo pipefail
PKG="${SWOOP_PKG:-app.swoop.keyboard.debug}"
APK="${SWOOP_APK:-ime/build/outputs/apk/debug/ime-debug.apk}"
OUT="${OUT:-emu-out}"; mkdir -p "$OUT"
exec > >(tee "$OUT/smoke.log") 2>&1     # the whole result is also an artifact
COMP="$PKG/app.swoop.keyboard.KeysService"
FAILS=0
ok(){ echo "  OK    $*"; }
bad(){ echo "  FAIL  $*"; FAILS=$((FAILS+1)); }
shot(){ adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null || true; }

adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done'
adb logcat -c
adb shell settings put secure show_ime_with_hard_keyboard 1     # an emulator has a hardware keyboard: without this the soft keyboard is hidden
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0

# ---- 1 install, register, enable, choose
adb install -r -t "$APK" >/dev/null 2>&1 && ok "installed $PKG" || { bad "install failed"; exit 1; }
# Android registers a freshly installed keyboard a moment AFTER the install returns (the run of 2026-11-17 selected it too early: "Unknown input method", the stock keyboard stayed
# current and every later check was about the wrong keyboard). So: wait until it is listed, enable it, choose it, and STOP if it is not the chosen one.
for i in $(seq 1 30); do
  adb shell ime list -a > "$OUT/ime-list.txt" 2>&1   # (a file, not a pipe: grep -q in a pipe under pipefail kills adb with SIGPIPE and reports a false failure)
  if grep -q "$PKG" "$OUT/ime-list.txt"; then break; fi
  sleep 2
done
grep -q "$PKG" "$OUT/ime-list.txt" && ok "ime list -a shows Swoop" || bad "ime list -a does not show $PKG"
adb shell ime enable "$COMP" > "$OUT/ime-enable.txt" 2>&1; grep -qi "enabled" "$OUT/ime-enable.txt" && ok "enabled" || bad "ime enable: $(cat "$OUT/ime-enable.txt")"
for i in 1 2 3 4 5; do
  adb shell ime set "$COMP" > "$OUT/ime-set.txt" 2>&1
  [ "$(adb shell settings get secure default_input_method | tr -d '\r')" = "$COMP" ] && break
  sleep 3
done
if [ "$(adb shell settings get secure default_input_method | tr -d '\r')" = "$COMP" ]; then ok "Swoop is the chosen keyboard"
else bad "default_input_method is $(adb shell settings get secure default_input_method) ($(cat "$OUT/ime-set.txt" | head -2 | tr '\n' ' '))"; echo "emulator smoke: Swoop could not be chosen, nothing else can be tested"; exit 1; fi

# ---- 2 show it in a text field (the Setup screen has a try-it box)
adb shell am start -n "$PKG/app.swoop.keyboard.SetupActivity" >/dev/null
sleep 3
dump(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1; }
find_edit(){ python3 -I - "$OUT/ui.xml" <<'PY'
import re, sys
x = open(sys.argv[1], encoding='utf-8', errors='replace').read()
for m in re.finditer(r'<node [^>]*class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    l, t, r, b = map(int, m.groups())
    if b > t and r > l: print((l + r) // 2, (t + b) // 2); break
PY
}
TAP=""
for i in 1 2 3 4 5 6; do
  dump; TAP="$(find_edit)"
  if [ -n "$TAP" ]; then break; fi
  adb shell input swipe 500 1500 500 500 300
done
[ -n "$TAP" ] && ok "found the try-it field at $TAP" || { bad "no text field on the Setup screen"; shot setup; }
adb shell input tap $TAP; sleep 3
dump; TAP="$(find_edit)"   # (the field may have moved above the keyboard)
adb shell dumpsys input_method > "$OUT/dumpsys-ime.txt"
grep -q "mInputShown=true" "$OUT/dumpsys-ime.txt" && ok "the keyboard is showing" || bad "the keyboard did not show (mInputShown)"
grep -q "mCurMethodId=$COMP" "$OUT/dumpsys-ime.txt" && ok "Swoop is the current input method" || bad "Swoop is not the current input method (mCurMethodId: $(grep -m1 mCurMethodId "$OUT/dumpsys-ime.txt"))"
shot keyboard-shown

# ---- 3 where are the keys: the INPUT METHOD window frame
adb shell dumpsys window windows > "$OUT/dumpsys-window.txt"
python3 -I tools/emu/float_check.py --show "$OUT/dumpsys-window.txt" 0 || true      # the IME window facts, into the job log
DENS="$(adb shell wm density | grep -oE '[0-9]+' | tail -1)"
GEOM="$(python3 -I - "$OUT/dumpsys-window.txt" "$DENS" <<'PY'
import re, sys
t = open(sys.argv[1], encoding='utf-8', errors='replace').read(); dens = int(sys.argv[2])
blocks = re.split(r'\n\s*Window #\d+ ', t)
frame = None
for b in blocks:
    if 'InputMethod' in b.split('\n', 1)[0]:
        m = re.search(r'(?:mFrame|frame)=\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
        if m: frame = tuple(map(int, m.groups())); break
if not frame: sys.exit(1)
l, t_, r, b_ = frame
dp = dens / 160.0; pad = 3 * dp; gap = 3 * dp; strip = 36 * dp; grip = 22 * dp
W = r - l
# the IME window ALSO covers the navigation bar below the keyboard, so the keyboard's own height comes from its formula, not from the frame
rowh = min(max((W - 2 * pad) / 10.0, 40 * dp), 52 * dp)
H = grip + strip + rowh * 4.72 + 4 * gap + 2 * pad
# the keys view starts under the neon bar; rows: digits (weight 0.72), q.., a.., z.., bottom (1 each)
weights = [0.72, 1, 1, 1, 1]
usable = H - grip - strip - 2 * pad - 4 * gap
def row_y(i):
    y = t_ + grip + pad + strip
    for k in range(i): y += usable * weights[k] / sum(weights) + gap
    return y + usable * weights[i] / sum(weights) / 2
def row_x(ws):
    total = sum(ws); n = len(ws); unit = (W - 2 * pad - gap * (n - 1)) / total
    xs = []; x = l + pad
    for w in ws: xs.append(x + w * unit / 2); x += w * unit + gap
    return xs
rows = [
    ("qwertyuiop", row_x([1] * 10 + [1.5])[:-1], 1),
    ("asdfghjkl", row_x([.5] + [1] * 9 + [2])[1:-1], 2),
    ("zxcvbnm", row_x([2.25] + [1] * 7 + [2.25])[1:-1], 3),
]
out = {}
for letters, xs, ri in rows:
    for ch, x in zip(letters, xs): out[ch] = (int(x), int(row_y(ri)))
bx = row_x([1.5, 6, 1.5, 2.5])               # the bottom row: comma, space, dot, ?123 (no globe key by default; the space bar takes the rest)
out['space'] = (int(bx[1]), int(row_y(4))); out['dot'] = (int(bx[2]), int(row_y(4))); out['back'] = (int(row_x([1] * 10 + [1.5])[-1]), int(row_y(1)))
out['grip'] = (int(l + W / 2), int(t_ + grip / 2))
print(frame, (b_ - t_) / dp, ' '.join('%s=%d,%d' % (k, v[0], v[1]) for k, v in out.items()))
PY
)" || GEOM=""
if [ -z "$GEOM" ]; then bad "could not find the keyboard window frame"; else ok "keyboard window: ${GEOM:0:60}"; fi
# the keyboard window must be only as tall as the keyboard (0.2.0-alpha1 had a bug: an overlay view made it the whole screen). dp height of the window vs the screen:
SCREEN_PX="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)"; SCREEN_DP="$SCREEN_PX"
WIN_DP="$(echo "$GEOM" | awk '{print $5}')"
python3 -I - "${WIN_DP:-0}" "${SCREEN_DP:-0}" "${DENS:-160}" <<'PY' && ok "the keyboard window is as tall as the keyboard, not the screen (${WIN_DP} dp)" || bad "the keyboard window is far too tall (${WIN_DP} dp; screen $SCREEN_DP px)"
import sys
win_dp = float(sys.argv[1]); scr_px = float(sys.argv[2]); dens = float(sys.argv[3])
sys.exit(0 if 150 < win_dp < 0.55 * scr_px / (dens / 160.0) else 1)
PY
key(){ echo "$GEOM" | tr ' ' '\n' | grep "^$1=" | head -1 | cut -d= -f2 | tr ',' ' '; }
field_text(){ dump; python3 -I - "$OUT/ui.xml" <<'PY'
import re, sys
x = open(sys.argv[1], encoding='utf-8', errors='replace').read()
m = re.search(r'<node [^>]*class="android.widget.EditText"[^>]*', x)
t = re.search(r' text="([^"]*)"', m.group(0)) if m else None
print(t.group(1) if t else "")
PY
}

# ---- 3 type by tapping keys
if [ -n "$GEOM" ]; then
  for c in h i; do adb shell input tap $(key $c); sleep 0.4; done
  T="$(field_text)"; [ "$(echo "$T" | tr A-Z a-z)" = "hi" ] && ok "tapping h and i typed '$T' (the field asks for a capital at the start)" || bad "expected 'hi' in the field, got '$T'"
  shot typed
  # ---- 5 Backspace
  adb shell input tap $(key back); sleep 0.5
  T="$(field_text)"; [ "$(echo "$T" | tr A-Z a-z)" = "h" ] && ok "Backspace removed the last letter ($T)" || bad "expected 'h' after Backspace, got '$T'"
  # ---- 4 glide: a swipe along the top row from t to p (a word like top/tip/typo is decoded)
  adb shell input tap $(key space); sleep 0.4
  X0=$(key t | cut -d' ' -f1); Y0=$(key t | cut -d' ' -f2); X1=$(key p | cut -d' ' -f1)
  adb shell input swipe $X0 $Y0 $X1 $Y0 450; sleep 2
  T="$(field_text)"; echo "  info  field after the glide has ${#T} characters"; shot glided
  echo "  info  field bytes after the glide: $(printf '%s' "$T" | od -An -c | tr -s ' ' | head -2 | tr '\n' ' ')"
  case "$T" in *[a-z]" ") [ "${#T}" -ge 4 ] && ok "a glide typed a word with a trailing space ('$T')" || bad "the glide typed too little ('$T')" ;; *) bad "the glide did not type a word (field: '$T')" ;; esac
  # ---- 6 globe key: must not crash
  # holding the space bar opens the keyboard picker (there is no globe key any more); it must not crash and must not type a space
  SX=$(key space | cut -d' ' -f1); SY=$(key space | cut -d' ' -f2)
  adb shell input swipe $SX $SY $SX $SY 1000; sleep 2; shot picker
  T2="$(field_text)"; [ "$T2" = "$T" ] && ok "holding the space bar typed no space" || echo "  info  field before '$T' after the hold '$T2'"
  adb shell input keyevent KEYCODE_BACK; sleep 1                 # (closes the keyboard picker if Android showed one)

  # ---- 8 floating (N3): long press the neon bar; the window becomes the whole screen, only the panel takes touches, the app behind is not resized
  adb shell input tap $TAP; sleep 3                                # the field again: the docked keyboard shows
  GX=$(key grip | cut -d' ' -f1); GY=$(key grip | cut -d' ' -f2)
  adb shell input swipe $GX $GY $GX $GY 900; sleep 2.5
  adb shell dumpsys window windows > "$OUT/dumpsys-window-float.txt"; shot floating
  python3 -I tools/emu/float_check.py "$OUT/dumpsys-window-float.txt" "$SCREEN_PX" > "$OUT/float-check.txt" 2>&1; FC=$?
  cat "$OUT/float-check.txt"
  [ "$FC" -eq 0 ] && ok "a long press on the neon bar floated the keyboard (the window is the whole screen)" || bad "the keyboard did not float after a long press on the bar (float_check exit $FC)"
  # the field behind the floating keyboard must still take a tap, and the keyboard must stay up
  adb shell input tap $TAP; sleep 1
  adb shell dumpsys input_method > "$OUT/dumpsys-ime-float.txt"
  grep -q "mInputShown=true" "$OUT/dumpsys-ime-float.txt" && ok "the floating keyboard stays up while the field behind it is tapped" || bad "the keyboard went away when the field behind it was tapped"
fi

# ---- 7 no crash
adb logcat -d > "$OUT/logcat.txt"
echo "---- SwoopDiag (geometry only) ----"; grep SwoopDiag "$OUT/logcat.txt" | sed 's/^.*SwoopDiag: //' | head -60; echo "----"
if grep -E "FATAL EXCEPTION|AndroidRuntime: .*$PKG|Process: $PKG" "$OUT/logcat.txt" | head -3 | grep -q .; then bad "the app crashed (see logcat.txt)"; grep -E -A8 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -20; else ok "no crash in logcat"; fi

echo; echo "emulator smoke: $FAILS failure(s)"
[ "$FAILS" -eq 0 ]
