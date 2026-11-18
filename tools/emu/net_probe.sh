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
# the emulator's DNS can still be coming up right after boot: wait until a name resolves (at most 30 s), else the first probe sees UnknownHostException for no app reason
for i in $(seq 1 15); do adb shell ping -c 1 -W 2 translate.googleapis.com >/dev/null 2>&1 && break; sleep 2; done
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
# (an IPv6 failure with IPv4 fine is not a failure here; a first probe right after boot can find the emulator's DNS not up yet: one retry below)
if grep -v 'connect IPv6' "$OUT/probe.log" | grep -q ' FAIL '; then echo "::error::a network step failed on this Android (see the log above)"; PHASE_A_BAD=1; else echo "phase A (plain): every step worked on this Android"; fi

# ---- R25 (2026-11-18): the owner's tablet says "Android refuses the keyboard a network socket" (EPERM / EACCES). Which Android limits do that to a BACKGROUND app (a keyboard process)?
# Each scenario starts a fresh process (am start -S), sends it to the background after 3 s (HOME), the probe waits 8 s and then runs, and the errno of the failures is reported. Informational: never fails the job.
APP_UID="$(adb shell dumpsys package "$PKG" | grep -m1 -oE 'userId=[0-9]+' | cut -d= -f2 | tr -d '\r')"
echo "---- R25: app uid $APP_UID"
adb shell cmd netpolicy list wifi-networks > "$OUT/netpolicy-wifi.txt" 2>&1
WIFI_ID="$(grep -oE 'AndroidWifi' "$OUT/netpolicy-wifi.txt" | head -1)"; WIFI_ID="${WIFI_ID:-AndroidWifi}"
background_probe(){   # $1 = label. Counts finished probes instead of trusting a log clear.
  local before; before=$(adb logcat -d -s SwoopProbe:D 2>/dev/null | grep -c 'done:')
  adb shell am start -S -n "$PKG/app.swoop.keyboard.SetupActivity" --ez probe true --ei probe_delay 8 >/dev/null 2>&1 || true
  sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 1
  for i in $(seq 1 60); do
    adb logcat -d -s SwoopProbe:D > "$OUT/probe-$1.full" 2>&1
    if [ "$(grep -c 'done:' "$OUT/probe-$1.full")" -gt "$before" ]; then break; fi
    sleep 2
  done
  local L=0; [ "$before" -gt 0 ] && L="$(grep -n 'done:' "$OUT/probe-$1.full" | sed -n "${before}p" | cut -d: -f1)"
  tail -n +$((L+1)) "$OUT/probe-$1.full" > "$OUT/probe-$1.log"
  echo "---- scenario $1 (probe lines of this run: $(grep -c 'SwoopProbe' "$OUT/probe-$1.log"))"; grep -E 'connect IPv4|answer|done:|FAIL' "$OUT/probe-$1.log" | grep -v 'connect IPv6' | cut -c1-200 | head -8
  local E; E="$(grep -oE 'E(PERM|ACCES|CONNREFUSED|CONNRESET|NETUNREACH|HOSTUNREACH)' "$OUT/probe-$1.log" | grep -v NETUNREACH | sort | uniq -c | tr '\n' ' ')"
  if grep -q 'translation can work' "$OUT/probe-$1.log"; then echo "SCENARIO $1: works" | tee -a "$OUT/scenarios.txt"; else echo "SCENARIO $1: BLOCKED, errno seen: ${E:-none (UnknownHost / timeout)}" | tee -a "$OUT/scenarios.txt"; fi
}
reset_all(){
  adb shell dumpsys battery reset >/dev/null 2>&1 || true
  adb shell settings put global low_power 0 >/dev/null 2>&1 || true
  adb shell dumpsys deviceidle unforce >/dev/null 2>&1 || true
  adb shell am set-inactive "$PKG" false >/dev/null 2>&1 || true
  adb shell am set-standby-bucket "$PKG" active >/dev/null 2>&1 || true
  adb shell cmd netpolicy remove restrict-background-whitelist "$APP_UID" >/dev/null 2>&1 || true
  adb shell cmd netpolicy set restrict-background false >/dev/null 2>&1 || true
  adb shell cmd netpolicy set metered-network "$WIFI_ID" undefined >/dev/null 2>&1 || true
  sleep 2
}
reset_all
# 1 Data Saver on + metered Wi-Fi
adb shell cmd netpolicy set restrict-background true 2>&1 | head -1
adb shell cmd netpolicy set metered-network "$WIFI_ID" true 2>&1 | head -1
background_probe datasaver
adb shell cmd netpolicy add restrict-background-whitelist "$APP_UID" 2>&1 | head -1
background_probe datasaver-unrestricted
reset_all
# 2 Battery saver on
adb shell dumpsys battery unplug >/dev/null 2>&1 || true
adb shell settings put global low_power 1 >/dev/null 2>&1 || true
sleep 4; adb shell dumpsys netpolicy 2>/dev/null | grep -iE 'Restrict power' | head -2
background_probe batterysaver
reset_all
# 3 Doze
adb shell dumpsys battery unplug >/dev/null 2>&1 || true
adb shell dumpsys deviceidle enable >/dev/null 2>&1 || true
adb shell dumpsys deviceidle force-idle 2>&1 | head -1
sleep 3; adb shell dumpsys netpolicy 2>/dev/null | grep -E "Restrict power|UID=$APP_UID|dozable" | head -4
background_probe doze
reset_all
# 4 App standby: the app is inactive in a rare bucket, the device unplugged
adb shell dumpsys battery unplug >/dev/null 2>&1 || true
adb shell am set-standby-bucket "$PKG" rare 2>&1 | head -1
adb shell am set-inactive "$PKG" true 2>&1 | head -1
adb shell am get-inactive "$PKG" 2>&1 | head -1
background_probe standby-rare
reset_all
adb shell am set-standby-bucket "$PKG" restricted 2>&1 | head -1
background_probe standby-restricted
reset_all
echo "---- R25 summary (which limit gives which errno for a background app)"; cat "$OUT/scenarios.txt" 2>/dev/null; true
[ "$PHASE_A_BAD" = 0 ] && exit 0 || exit 1
