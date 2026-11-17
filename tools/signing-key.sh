#!/usr/bin/env bash
# signing-key.sh - the keyboard's PRIVATE release key (plan 11.5), kept as ONE passphrase-encrypted file in the repo.
#
# WHY NOT "four secrets with the keystore in base64": the owner would have to be SHOWN the keystore and its passwords in a chat (and the
# chat is copied into the repo by the INBOX/OUTBOX hooks). Here the key is created INSIDE a GitHub runner, so it never appears in any chat:
#   create   (run by .github/workflows/make-signing-key.yml, once, ever): makes a random keystore password and an RSA-4096 key, packs both
#            into signing/swoop.jks.enc encrypted with AES-256 (PBKDF2-SHA256, 10 million iterations: the repo is PUBLIC, so the file can be attacked offline and every guess must be slow) under the passphrase the OWNER typed into the repo
#            secret SWOOP_KEYSTORE_PASSPHRASE, and pins the public certificate in signing/CERT_SHA256. Refuses to overwrite an existing key
#            (a different key could never update an installed keyboard).
#   unlock   (run by android.yml before a release build): decrypts to a temp dir and writes keystore.properties (gitignored) for Gradle.
#   fingerprint  prints the pinned certificate of the file (needs the passphrase), to prove the pin.
# Needs env SWOOP_KEYSTORE_PASSPHRASE. Never prints it, never prints the keystore password. Overrides for tests: SWOOP_SIGNING_DIR, SWOOP_KEYSTORE_PROPS, SWOOP_TMP.
set -euo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SIGN="${SWOOP_SIGNING_DIR:-$D/signing}"
ENC="$SIGN/swoop.jks.enc"
ITER_FILE="$SIGN/KDF_ITER"    # the (public) PBKDF2 iteration count the file was made with
PROPS="${SWOOP_KEYSTORE_PROPS:-$D/keystore.properties}"
TMP="${SWOOP_TMP:-${RUNNER_TEMP:-$(mktemp -d)}}"
ALIAS=swoop
MODE="${1:-}"

need_pass() {
  [ -n "${SWOOP_KEYSTORE_PASSPHRASE:-}" ] || { echo "SWOOP_KEYSTORE_PASSPHRASE is not set (repo secret)." >&2; exit 1; }
  [ "${#SWOOP_KEYSTORE_PASSPHRASE}" -ge 20 ] || { echo "The passphrase must be at least 20 characters (use a long sentence)." >&2; exit 1; }
}
cert_of() { keytool -list -v -keystore "$1" -storetype PKCS12 -storepass "$2" -alias "$ALIAS" 2>/dev/null | sed -n 's/.*SHA256: *//p' | head -1 | tr -d ':' | tr 'A-F' 'a-f'; }
# decrypt into $1 (a private dir): p12 + password file
open_to() {
  openssl enc -d -aes-256-cbc -pbkdf2 -iter "$(tr -d ' \r\n' < "$ITER_FILE" 2>/dev/null || echo 600000)" -pass env:SWOOP_KEYSTORE_PASSPHRASE -in "$ENC" -out "$1/payload" 2>/dev/null \
    || { echo "Could not decrypt $ENC: wrong passphrase in SWOOP_KEYSTORE_PASSPHRASE?" >&2; exit 1; }
  head -n1 "$1/payload" > "$1/storepass"
  tail -n +2 "$1/payload" | base64 -d > "$1/swoop.p12"
  rm -f "$1/payload"
}

case "$MODE" in
  create)
    need_pass
    [ ! -e "$ENC" ] || { echo "REFUSED: $ENC already exists. A new key could never update the installed keyboard." >&2; exit 1; }
    W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT
    chmod 700 "$W"
    openssl rand -base64 36 | tr -d '\n=/+' | head -c 40 > "$W/storepass"
    keytool -genkeypair -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Swoop Keyboard" \
      -storetype PKCS12 -keystore "$W/swoop.p12" -storepass "$(cat "$W/storepass")" -keypass "$(cat "$W/storepass")" >/dev/null 2>&1
    FP="$(cert_of "$W/swoop.p12" "$(cat "$W/storepass")")"
    printf '%s' "$FP" | grep -Eq '^[0-9a-f]{64}$' || { echo "could not read the new certificate" >&2; exit 1; }
    { cat "$W/storepass"; echo; base64 -w0 "$W/swoop.p12"; echo; } > "$W/payload"
    mkdir -p "$SIGN"
    ITER="${SWOOP_KDF_ITER:-10000000}"      # (tests use a small number; a real key always gets the big one)
    openssl enc -aes-256-cbc -pbkdf2 -iter "$ITER" -salt -pass env:SWOOP_KEYSTORE_PASSPHRASE -in "$W/payload" -out "$ENC"
    printf '%s\n' "$ITER" > "$ITER_FILE"
    printf '%s\n' "$FP" > "$SIGN/CERT_SHA256"
    echo "Created $ENC (encrypted) and pinned the certificate: $FP"
    ;;
  unlock)
    need_pass
    [ -f "$ENC" ] || { echo "$ENC does not exist: the signing key has not been created (KB-003)." >&2; exit 1; }
    mkdir -p "$TMP"; chmod 700 "$TMP"
    open_to "$TMP"
    PW="$(cat "$TMP/storepass")"; rm -f "$TMP/storepass"
    FP="$(cert_of "$TMP/swoop.p12" "$PW")"; WANT="$(tr -d ' \r\n' < "$SIGN/CERT_SHA256")"
    if [ "$FP" != "$WANT" ]; then rm -f "$TMP/swoop.p12"; echo "The unlocked key's certificate ($FP) is not the pinned one ($WANT): refusing to build." >&2; exit 1; fi
    { echo "storeFile=$TMP/swoop.p12"; echo "storeType=pkcs12"; echo "storePassword=$PW"; echo "keyAlias=$ALIAS"; echo "keyPassword=$PW"; } > "$PROPS"
    chmod 600 "$PROPS"
    echo "Unlocked the signing key; certificate $FP matches the pin."
    ;;
  fingerprint)
    need_pass
    W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT; chmod 700 "$W"
    open_to "$W"; cert_of "$W/swoop.p12" "$(cat "$W/storepass")"
    ;;
  *) echo "usage: bash tools/signing-key.sh create|unlock|fingerprint   (env SWOOP_KEYSTORE_PASSPHRASE)" >&2; exit 2 ;;
esac
