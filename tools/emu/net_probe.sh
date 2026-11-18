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
if grep -q ' FAIL ' "$OUT/probe.log" || grep -q 'D SwoopProbe: *FAIL' "$OUT/probe.log"; then echo "::error::a network step failed on this Android (see the log above)"; exit 1; fi
echo "every step worked on this Android"
exit 0
