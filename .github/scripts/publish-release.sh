#!/usr/bin/env bash
# publish-release.sh - run by .github/workflows/android.yml AFTER the APK was built and verified.
# Creates (or refreshes) the GitHub Release for the current version with the APK + its sha256 attached.
# GitHub creates the tag itself (`--target <sha>`): a tag PUSH from the Claude sandbox is refused (403), so nothing ever pushes one.
# Needs GH_TOKEN (the workflow's token, contents: write). Optional env: NOTE (what changed).
set -euo pipefail
# a failure names itself in the job log (an annotation), so the reason is visible without opening the whole step output
trap 'rc=$?; echo "::error title=publish-release.sh failed::line $LINENO: $BASH_COMMAND (exit $rc)"' ERR
gh --version | head -1
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
. tools/appinfo.sh

TAG="$(app_tag)"; ASSET="$(app_asset)"; VN="$(app_vname)"; VC="$(app_vcode)"; TITLE="$(app_title)"
cp "$(app_apk)" "$ASSET"
sha256sum "$ASSET" > "$ASSET.sha256"
SUM="$(cut -d' ' -f1 "$ASSET.sha256")"
CERT="$(tr -d ' \r\n' < signing/CERT_SHA256)"

{
  echo "${NOTE:-No change note was given.}"
  echo
  echo "**$TITLE $VN** (versionCode $VC): a light glide + tap keyboard for the tablet and the phone."
  echo
  echo "- Install: open this page on the device (the repository is public: no sign-in needed), tap \`$ASSET\`, and allow installing from this source if Android asks. It installs over the previous $TITLE because the signing certificate is the same."
  echo "- Then open the Swoop app: its Setup screen explains how to switch the keyboard on and select it. KEEP your old keyboard installed until you are sure Swoop works."
  echo "- Built and signed by GitHub Actions from commit \`${GITHUB_SHA:-unknown}\`; the certificate was verified on this exact APK (SHA-256 \`$CERT\`)."
  echo "- APK SHA-256: \`$SUM\`"
} > release-notes.md

FLAGS=()
[ "$(app_prerelease)" = true ] && FLAGS+=(--prerelease)

if gh release view "$TAG" >/dev/null 2>&1; then
  # a rebuild of an existing release: replace the files, and refresh the text only when a change note was given or the release has no text yet
  gh release upload "$TAG" "$ASSET" "$ASSET.sha256" --clobber
  BODY="$(gh release view "$TAG" --json body -q .body)"
  if [ -n "${NOTE:-}" ] || [ -z "$BODY" ]; then gh release edit "$TAG" --notes-file release-notes.md; fi
else
  gh release create "$TAG" "$ASSET" "$ASSET.sha256" --target "${GITHUB_SHA:?}" --title "$TITLE $VN" --notes-file release-notes.md "${FLAGS[@]}"
fi
URL="$(gh release view "$TAG" --json url -q .url)"
echo "Published $TAG -> $URL"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && echo "Published **$TAG** -> $URL" >> "$GITHUB_STEP_SUMMARY"
exit 0
