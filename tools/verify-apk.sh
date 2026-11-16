#!/usr/bin/env bash
# verify-apk.sh - is this APK the one the tablet/phone can install over what is already there, and does it look sane?
#
# Building an APK proves it compiles. It does NOT prove it is installable over the installed copy: that depends entirely on the signing
# certificate, and a wrong one gives a perfectly valid APK that Android refuses as an update (the owner would have to uninstall first and
# lose what the keyboard learned unless they exported it). For a KEYBOARD the certificate is also the only thing that stops a malicious APK
# from silently replacing it, so the certificate is checked on the ARTIFACT, not just on the keystore that went in.
# The pin is signing/CERT_SHA256 (64 hex digits once KB-003 has created the private key; the word UNSET before that, which refuses every APK).
#
#   bash tools/verify-apk.sh [path/to.apk]            full check (release APK, certificate pinned)
#   bash tools/verify-apk.sh --no-cert [path/to.apk]  CI "check" runs on debug-signed test builds: everything except the certificate
#
# Also checks (cheap, from the APK itself): package name, versionCode/versionName equal what Gradle says, not debuggable, minSdk 29,
# the permission list is EXACTLY tools/expected-permissions.txt (plan 6.8 / 9.2: today none; INTERNET arrives with KB-601),
# usesCleartextTraffic is off, no Google Mobile Services anywhere in the manifest (the tablet has none; never add GMS), size budget.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
. tools/appinfo.sh
CERT=1; [ "${1:-}" = "--no-cert" ] && { CERT=0; shift; }
APK="${1:-$(app_apk)}"
[ -f "$APK" ] || { echo "  FAIL  no APK at $APK"; exit 1; }
FAILS=0
bad(){ echo "  !!    $*"; FAILS=$((FAILS+1)); }
ok(){ echo "  OK    $*"; }

norm(){ tr -d ': \r' | tr 'A-Z' 'a-z'; }   # apksigner and keytool disagree on case and colons
BT="$(ls -d "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/* 2>/dev/null | sort -V | tail -1)"

if [ "$CERT" -eq 1 ]; then
  WANT="$(norm < signing/CERT_SHA256 2>/dev/null | head -c 64)"
  if ! printf '%s' "$WANT" | grep -Eq '^[0-9a-f]{64}$'; then
    bad "signing/CERT_SHA256 is '${WANT:-missing}': the private signing key does not exist yet (task KB-003). No release APK may be published or installed."
  else
    GOT=""
    if [ -n "$BT" ] && [ -x "$BT/apksigner" ]; then
      OUT="$("$BT/apksigner" verify --print-certs "$APK" 2>&1)" || bad "apksigner verify FAILED: $(echo "$OUT" | head -2 | tr '\n' ' ')"
      GOT="$(echo "$OUT" | grep -i 'certificate SHA-256 digest' | head -1 | sed 's/.*digest: *//' | norm)"
    fi
    if [ -z "$GOT" ] && command -v keytool >/dev/null 2>&1; then
      CERTF="$(unzip -l "$APK" 2>/dev/null | grep -oE 'META-INF/[^ ]*\.(RSA|EC|DSA)' | head -1)"
      [ -n "$CERTF" ] && GOT="$(unzip -p "$APK" "$CERTF" 2>/dev/null | keytool -printcert 2>/dev/null | sed -n 's/.*SHA256: *//p' | head -1 | norm)"
    fi
    if [ -z "$GOT" ]; then bad "could not read a signature from $APK - treat it as unsigned"
    elif [ "$GOT" = "$WANT" ]; then ok "certificate is the pinned one ($(echo "$GOT" | cut -c1-16)...): installs over the installed copy"
    else bad "WRONG CERTIFICATE: it will NOT update the installed keyboard in place. expected $WANT got $GOT"; fi
  fi
fi

if [ -n "$BT" ] && [ -x "$BT/aapt2" ]; then
  BADGE="$("$BT/aapt2" dump badging "$APK" 2>/dev/null)"
  PKG="$(echo "$BADGE" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
  VC="$(echo "$BADGE" | sed -n "s/^package:.* versionCode='\([^']*\)'.*/\1/p")"
  VN="$(echo "$BADGE" | sed -n "s/^package:.* versionName='\([^']*\)'.*/\1/p")"
  MINSDK="$(echo "$BADGE" | sed -n "s/^sdkVersion:'\([^']*\)'.*/\1/p")"
  [ "$PKG" = "app.swoop.keyboard" ] && ok "package $PKG" || bad "package is '$PKG', expected 'app.swoop.keyboard'"
  [ "$VC" = "$(app_vcode)" ] && [ "$VN" = "$(app_vname)" ] && ok "version $VN (code $VC) equals the Gradle file" || bad "APK says $VN/$VC, Gradle says $(app_vname)/$(app_vcode)"
  [ "$MINSDK" = "29" ] && ok "minSdk 29" || bad "minSdk is '$MINSDK', expected 29"
  if [ "$CERT" -eq 1 ]; then echo "$BADGE" | grep -q "application-debuggable" && bad "APK is debuggable" || ok "not debuggable"; fi

  # PERMISSIONS: exactly the fixture. A keyboard sees every key pressed, so a new permission is a decision for the owner, never an accident.
  GOTP="$("$BT/aapt2" dump permissions "$APK" 2>/dev/null | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort -u)"
  WANTP="$(grep -vE '^\s*(#|$)' tools/expected-permissions.txt 2>/dev/null | sort -u)"
  if [ "$GOTP" = "$WANTP" ]; then ok "permissions are exactly the expected set ($(printf '%s' "$WANTP" | grep -c . | tr -d ' ') listed)"
  else bad "PERMISSIONS differ from tools/expected-permissions.txt. in APK: [$(printf '%s' "$GOTP" | tr '\n' ' ')] expected: [$(printf '%s' "$WANTP" | tr '\n' ' ')]"; fi

  XML="$("$BT/aapt2" dump xmltree "$APK" --file AndroidManifest.xml 2>/dev/null)"
  if printf '%s' "$XML" | grep -q 'usesCleartextTraffic.*=(type 0x12)0xffffffff'; then bad "usesCleartextTraffic is true"; else ok "cleartext traffic is not enabled"; fi
  if printf '%s' "$XML" | grep -q 'allowBackup.*=(type 0x12)0xffffffff'; then bad "allowBackup is true (learned typing data must never be backed up automatically)"; else ok "allowBackup is not enabled"; fi
  # A GMS DEPENDENCY (service, receiver, provider, uses-library, meta-data...) is forbidden.
  GMS="$(printf '%s' "$XML" | awk '/^ *E: /{el=$2} /com\.google\.android\.gms/ && el!="package"{print el": "$0}' | head -3)"
  if [ -n "$GMS" ]; then bad "manifest depends on Google Mobile Services (the tablet has none; never add GMS): $GMS"; else ok "no Google Mobile Services dependency in the manifest"; fi
else
  echo "  note  no aapt2 here - package/version/permission/manifest checks skipped"
fi

KB=$(( $(stat -c %s "$APK") / 1024 ))
BUDGET="${SWOOP_APK_BUDGET_KB:-6000}"
[ "$KB" -le "$BUDGET" ] && ok "size ${KB} KB (budget ${BUDGET} KB)" || bad "size ${KB} KB exceeds the budget ${BUDGET} KB"

[ "$FAILS" -eq 0 ] && { echo "  OK    $(basename "$APK") passed all checks"; exit 0; }
echo "  $FAILS check(s) failed - do not publish or install $(basename "$APK")."
exit 1
