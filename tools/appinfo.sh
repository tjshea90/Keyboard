#!/usr/bin/env bash
# appinfo.sh - the ONE place that knows the app this repo builds (Swoop, module :ime, package app.swoop.keyboard). Source it
# (. tools/appinfo.sh) or call it:
#   bash tools/appinfo.sh <vname|vcode|gradle|module|dir|prefix|tag|asset|apk|title|prerelease|last_code>
#   bash tools/appinfo.sh of-tag swoop-v0.1.0     -> exit 0 when the tag is a Swoop tag, 1 otherwise
#
# ship.sh, record-release.sh, resume.sh, verify-apk.sh, test_handoff.sh and .github/workflows/android.yml all read the version
# through here, so the versionCode that is gated is always the one Gradle will really build.
#   tag swoop-v<versionName>     asset swoop-<versionName>.apk     BUILDLOG.md line: | tag | code N | time | note
APPINFO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

app_gradle() { echo ime/build.gradle.kts; }
app_dir()    { echo ime; }
app_module() { echo ":ime"; }
app_prefix() { echo swoop-v; }
app_title()  { echo "Swoop"; }
app_apk()    { echo "ime/build/outputs/apk/release/ime-release.apk"; }
app_vcode()  { grep -m1 -oE 'versionCode *= *[0-9]+' "$APPINFO_ROOT/$(app_gradle)" 2>/dev/null | grep -oE '[0-9]+'; }
app_vname()  { grep -m1 -oE 'versionName *= *"[^"]+"' "$APPINFO_ROOT/$(app_gradle)" 2>/dev/null | grep -oE '"[^"]+"' | tr -d '"'; }
app_tag()    { echo "$(app_prefix)$(app_vname)"; }
app_asset()  { echo "swoop-$(app_vname).apk"; }
app_prerelease() { case "$(app_vname)" in *-*) echo true ;; *) echo false ;; esac; }   # 0.1.0-alpha1 -> a pre-release
app_of_tag() { case "${1:-}" in swoop-v*) return 0 ;; *) return 1 ;; esac; }
# highest versionCode ever RECORDED in BUILDLOG.md (0 when none)
app_last_code() {
  local n; n="$(grep -E '^\| swoop-v[^| ]+ \| code [0-9]+ ' "$APPINFO_ROOT/BUILDLOG.md" 2>/dev/null | grep -oE 'code [0-9]+' | grep -oE '[0-9]+' | sort -n | tail -1)"
  echo "${n:-0}"
}

# called as a program (not sourced): print one field
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  if [ "${1:-}" = "of-tag" ]; then app_of_tag "${2:-}"; exit $?; fi
  case "${1:-}" in
    vname) app_vname ;; vcode) app_vcode ;; gradle) app_gradle ;; module) app_module ;; dir) app_dir ;;
    prefix) app_prefix ;; tag) app_tag ;; asset) app_asset ;; apk) app_apk ;; title) app_title ;;
    prerelease) app_prerelease ;; last_code) app_last_code ;;
    *) echo "usage: bash tools/appinfo.sh <vname|vcode|gradle|module|dir|prefix|tag|asset|apk|title|prerelease|last_code> | of-tag <tag>" >&2; exit 2 ;;
  esac
fi
