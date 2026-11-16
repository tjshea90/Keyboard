#!/usr/bin/env bash
# secretscan.sh — refuse to commit an obvious credential.
#
# WHY THIS EXISTS
# ---------------
# autosave.sh commits with `git add -A` and pushes to GitHub without a human
# looking at the diff first. That is the whole point of it — and it is also
# exactly how a pasted API key ends up permanently in history. Once pushed,
# rotating the key is the only real remedy; deleting the commit is not,
# because GitHub keeps unreachable objects reachable by SHA.
#
# So: high-confidence patterns ONLY. A false positive here silently stops all
# auto-checkpointing, which is a worse failure than the one being prevented,
# so nothing vague ("password=", "secret") is matched — only shapes that are
# unambiguously a live credential.
#
#   bash tools/secretscan.sh          # scans STAGED changes; 0 = clean
#
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0

# ---- PATH GUARD: unstage (never block) key material by NAME and files GitHub would refuse ----------------------------------
# Names: keystores, .env files, *.pem and friends must never be committed (Swoop has NO public dev key: the signing key is private, plan 11.5;
# the passphrase-encrypted keystore signing/swoop.jks.enc has a different extension on purpose).
# Size: GitHub rejects a push that contains a file over 100 MB, and that would wedge EVERY later autosave push, so anything over
# 90 MB is unstaged too. Both are reported on stderr; the rest of the commit goes ahead.
while IFS= read -r F; do
  [ -z "$F" ] && continue
  BADWHY=""
  if printf '%s\n' "$F" | grep -qE '(^|/)(keystore\.properties|\.env(\..*)?|id_rsa[^/]*|local\.properties)$|\.(pem|p12|key|jks|keystore)$'; then BADWHY="key material"; fi
  # Swoop's private data (plan 11.6): learned data, replay logs, APKs and pictures of the owner's screen must never reach the repo.
  if [ -z "$BADWHY" ] && printf '%s\n' "$F" | grep -qE '(^|/)(personal|replay|exports?-private|owner-data)/|\.(apk|aab|journal|snap|replay)$|(^|/)(glide_personal|own_words)\.txt$'; then BADWHY="learned data / replay log / APK"; fi
  if [ -z "$BADWHY" ] && printf '%s\n' "$F" | grep -qiE '\.(png|jpe?g|webp|heic|gif|mp4|mov|webm)$' \
     && ! printf '%s\n' "$F" | grep -qE '^(ime/src/main/res/|docs/img/)|/src/(test|androidTest)/resources/'; then BADWHY="picture/video outside ime/src/main/res, docs/img and test resources (the owner's screens stay out of the repo)"; fi
  if [ -z "$BADWHY" ] && [ -f "$F" ] && [ "$(stat -c %s "$F" 2>/dev/null || echo 0)" -gt 94371840 ]; then BADWHY="over 90 MB (GitHub refuses files over 100 MB)"; fi
  if [ -n "$BADWHY" ]; then
    git reset -q HEAD -- "$F" 2>/dev/null || git rm -q --cached -- "$F" 2>/dev/null
    echo "  secretscan: left $F out of the commit ($BADWHY)" >&2
  fi
done < <(git diff --cached --name-only 2>/dev/null)

# A pasted/exported LEARNING FILE (format v1 starts a line with the magic header) is the owner's private typing history: block it.
# Allowed: the documented example in docs/KEYBOARD-EXPORT.md and fixtures under any */src/test/* folder.
EXPORT_DIFF="$(git diff --cached -U0 -- . ':(exclude)docs/KEYBOARD-EXPORT.md' ':(exclude)*/src/test/*' 2>/dev/null || true)"
[ -z "$EXPORT_DIFF" ] && EXPORT_DIFF="$(git diff HEAD -U0 -- . ':(exclude)docs/KEYBOARD-EXPORT.md' ':(exclude)*/src/test/*' 2>/dev/null || true)"
if printf '%s\n' "$EXPORT_DIFF" | grep -qE '^\+KESTREL-KEYBOARD-LEARNING [0-9]'; then
  echo "SECRET SCAN TRIPPED - a keyboard LEARNING EXPORT (the owner's typing history) is in the tree - nothing was committed."
  echo "Remove it (exports are attached to a chat, never committed), then checkpoint again."
  exit 1
fi

# Added lines only. An existing line that already looked like this was already
# pushed long ago; re-flagging it would wedge every future checkpoint.
#
# Staged changes are what autosave.sh is about to commit, so they are the real
# subject. But a bare `bash tools/secretscan.sh` typed by hand — including the
# one autosave.sh tells you to run after it blocks, by which point it has
# already unstaged everything — must not report "clean" on an unstaged secret.
# So fall back to the whole working tree when the index is empty.
DIFF="$(git diff --cached -U0 2>/dev/null || true)"
[ -z "$DIFF" ] && DIFF="$(git diff HEAD -U0 2>/dev/null || true)"
ADDED="$(printf '%s\n' "$DIFF" | grep '^+' | grep -v '^+++' || true)"
[ -z "$ADDED" ] && exit 0

# Test fixtures are not credentials. If a test ever needs to assert that a key
# gets redacted from output, it necessarily CONTAINS a key-shaped string.
# Matching it would wedge every checkpoint the moment anyone edits near that
# line — the exact false-positive failure this file's header warns about. Two
# independent guards:
#   1. length — a real key carries dozens of chars after the prefix, fixtures
#      are usually short, so the patterns below already lean on that
#   2. these markers, for fixtures in any other shape
ADDED="$(printf '%s\n' "$ADDED" | grep -viE 'DO-?NOT-?(EXPORT|COMMIT|USE)|EXAMPLE|PLACEHOLDER|REDACTED|DUMMY|FAKE|YOUR_?KEY|XXXXX' || true)"
[ -z "$ADDED" ] && exit 0

HITS="$(printf '%s\n' "$ADDED" | grep -nE \
  -e 'sk-ant-[A-Za-z0-9_-]{40,}' \
  -e 'sk-[A-Za-z0-9_-]{32,}' \
  -e 'ghp_[A-Za-z0-9]{30,}' \
  -e 'gho_[A-Za-z0-9]{30,}' \
  -e 'github_pat_[A-Za-z0-9_]{30,}' \
  -e 'AIza[0-9A-Za-z_-]{30,}' \
  -e 'AKIA[0-9A-Z]{16}' \
  -e 'xox[baprs]-[A-Za-z0-9-]{10,}' \
  -e '-----BEGIN [A-Z ]*PRIVATE KEY-----' \
  || true)"

[ -z "$HITS" ] && exit 0

echo "SECRET SCAN TRIPPED — nothing was committed."
printf '%s\n' "$HITS" | head -5 | cut -c1-120 | sed 's/^/    /'
echo "Remove the credential, then checkpoint again. If this is a false"
echo "positive, widen the exclusion in tools/secretscan.sh — do NOT bypass it."
exit 1
