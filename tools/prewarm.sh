#!/bin/sh
# tools/prewarm.sh - fills the Gradle, dependency and Robolectric caches of a FRESH container, so the first test, lint or build of a
# session does not download them (and does not meet a download limit in the middle of the work). Run by the owner's cloud-environment
# setup script in every new container, or by hand: sh tools/prewarm.sh. Nothing depends on it: every part is separate and a failure
# only prints a line, so a session always starts. Safe to run again. It writes only below build/, .gradle/ and the user caches.
cd "$(dirname "$0")/.." || exit 1
if [ -z "${ANDROID_HOME:-}" ]; then
  for c in /opt/android-sdk "$HOME/android-sdk"; do
    [ -d "$c/platforms" ] && ANDROID_HOME="$c" && break
  done
fi
: "${ANDROID_HOME:=/opt/android-sdk}"
export ANDROID_HOME
bad=0
part() { # part <what it warms> <gradle arguments>. R39: settings.gradle.kts asks Google's copy of Maven Central first (no 429 there); the rare artifact it lacks still comes from Central, which can answer HTTP 429 (a rate limit): then wait and try again, slower and with fewer parallel downloads
  name=$1; shift
  echo "== $name"
  n=0; extra=""
  while :; do
    n=$((n + 1))
    # shellcheck disable=SC2086
    if timeout 1500 sh ./gradlew --no-daemon -q --console=plain $extra "$@" > "/tmp/prewarm-part.$$" 2>&1; then cat "/tmp/prewarm-part.$$"; rm -f "/tmp/prewarm-part.$$"; return 0; fi
    cat "/tmp/prewarm-part.$$"
    if grep -q "status code 429" "/tmp/prewarm-part.$$" && [ "$n" -lt 4 ]; then echo "HTTP 429 from Maven Central: attempt $n of 4 failed, waiting 40 s"; extra="--no-parallel --max-workers=2"; rm -f "/tmp/prewarm-part.$$"; sleep 40; continue; fi
    rm -f "/tmp/prewarm-part.$$"; echo "warm-up part failed: $name"; bad=1; return 1
  done
}
part "the pure Kotlin tests" :core:test
part "the Robolectric tests (Android 10 files)" :ime:testDebugUnitTest --tests 'app.swoop.keyboard.KeysServiceTest'
part "lint" :ime:lintDebug
echo "warm-up done, problems: $bad"
exit $bad
