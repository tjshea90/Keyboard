#!/usr/bin/env bash
# net_probe.sh - run by .github/workflows/emu-smoke.yml (mode net-probe) INSIDE a booted Android emulator. Starts the keyboard's Diagnostics screen of the DEBUG build with
# probe=true (the debug build only: a release build ignores it), which runs ConnectionProbe: permission, clock, name lookup, connect, secure handshake and a real
# answer for the fixed word "hello" from MyMemory and Google, on Android's own network stack. The steps arrive in logcat (tag SwoopProbe: stage names and short safe
# reasons, never typed text). Exit 0 = every step worked; 1 = a step failed or no result came. The log is printed in full and kept in $OUT.
set -uo pipefail
PKG="${SWOOP_PKG:-app.swoop.keyboard.debug}"
APK="${SWOOP_APK:-ime/build/outputs/apk/debug/ime-debug.apk}"
OUT="${OUT:-emu-out}"; mkdir -p "$OUT"
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done'
adb shell settings put global hide_error_dialogs 1
echo "Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r')), $(adb shell getprop ro.product.model | tr -d '\r')"
adb install -r -t "$APK" >/dev/null 2>&1 && echo "installed $PKG" || { echo "::error::install failed"; exit 1; }
adb logcat -c
# (the exported Setup screen; adb cannot start the non-exported Diagnostics one: "not exported from uid")
adb shell am start -n "$PKG/app.swoop.keyboard.SetupActivity" --ez probe true >"$OUT/am-start.txt" 2>&1 || true
cat "$OUT/am-start.txt"
done=0
for i in $(seq 1 90); do          # up to 3 minutes: the probe has per-step time limits
  adb logcat -d -s SwoopProbe:D > "$OUT/probe.log" 2>&1
  if grep -q 'done:' "$OUT/probe.log"; then done=1; break; fi
  sleep 2
done
adb logcat -d > "$OUT/logcat-full.txt" 2>&1 || true
echo "---- probe log"; cat "$OUT/probe.log"; echo "----"
if [ "$done" != 1 ]; then echo "::error::the probe gave no result (see logcat-full.txt in the artifact)"; grep -iE 'FATAL|AndroidRuntime|swoop' "$OUT/logcat-full.txt" | head -20; exit 1; fi
PHASE_A_BAD=0
if grep -q ' FAIL ' "$OUT/probe.log" || grep -q 'D SwoopProbe: *FAIL' "$OUT/probe.log"; then echo "::error::a network step failed on this Android (see the log above)"; PHASE_A_BAD=1; else echo "phase A (plain): every step worked on this Android"; fi

# ---- R25 (2026-11-18): the owner's tablet says "Android refuses the keyboard a network socket" (EPERM). Hypothesis: Data Saver (on by default on Android Go) + a METERED Wi-Fi (a phone's
# hotspot is one) + a BACKGROUND app (a keyboard process) = the sockets are refused unless the app is on the "Unrestricted data" list. Reproduce it here, informational (never fails the job):
#   phase B: Data Saver on, Wi-Fi metered, the probe runs while the app is in the background (HOME pressed 2 s after the start; the probe waits 8 s)
#   phase C: the same with the app on the Unrestricted data list (cmd netpolicy add restrict-background-whitelist <uid>)
APP_UID="$(adb shell dumpsys package "$PKG" | grep -m1 -oE 'userId=[0-9]+' | cut -d= -f2 | tr -d '\r')"
echo "---- R25: app uid $APP_UID"
adb shell cmd netpolicy list wifi-networks > "$OUT/netpolicy-wifi.txt" 2>&1; cat "$OUT/netpolicy-wifi.txt" | head -8
WIFI_ID="$(grep -oE 'AndroidWifi' "$OUT/netpolicy-wifi.txt" | head -1)"; WIFI_ID="${WIFI_ID:-AndroidWifi}"
background_probe(){   # $1 = label
  adb logcat -c
  adb shell am start -n "$PKG/app.swoop.keyboard.SetupActivity" --ez probe true --ei probe_delay 8 >/dev/null 2>&1 || true
  sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 1
  for i in $(seq 1 60); do adb logcat -d -s SwoopProbe:D > "$OUT/probe-$1.log" 2>&1; if grep -q 'done:' "$OUT/probe-$1.log"; then break; fi; sleep 2; done
  echo "---- probe $1"; grep -E 'Android limits|connect IPv4|handshake|answer|done:|FAIL' "$OUT/probe-$1.log" | cut -c1-230
}
adb shell cmd netpolicy set restrict-background true 2>&1 | head -2
adb shell cmd netpolicy set metered-network "$WIFI_ID" true 2>&1 | head -2
adb shell cmd netpolicy get restrict-background 2>&1 | head -2
adb shell dumpsys netpolicy 2>/dev/null | grep -iE 'restrict|metered' | head -6
background_probe B
if grep -qE 'EPERM|EACCES' "$OUT/probe-B.log"; then echo "R25 REPRODUCED: with Data Saver on, a metered Wi-Fi and the app in the background Android refuses the sockets (EPERM/EACCES)"; else echo "R25 not reproduced in phase B (see probe-B.log)"; fi
adb shell cmd netpolicy add restrict-background-whitelist "$APP_UID" 2>&1 | head -2
adb shell cmd netpolicy get restrict-background 2>&1 | head -2
background_probe C
if grep -qE 'EPERM|EACCES' "$OUT/probe-C.log"; then echo "R25: still refused with the app on the Unrestricted data list (phase C)"; else echo "R25: with the app on the Unrestricted data list the sockets work (phase C)"; fi
adb shell cmd netpolicy remove restrict-background-whitelist "$APP_UID" >/dev/null 2>&1 || true
adb shell cmd netpolicy set metered-network "$WIFI_ID" undefined >/dev/null 2>&1 || true
adb shell cmd netpolicy set restrict-background false >/dev/null 2>&1 || true
[ "$PHASE_A_BAD" = 0 ] && exit 0 || exit 1
