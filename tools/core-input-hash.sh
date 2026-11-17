#!/usr/bin/env bash
# Prints the fingerprint of the SHARED keyboard engine: every file under core/src/main/kotlin/app/kestrel/core/input (and translate/) of the checkout given as $1
# (default: this repo), as one sha256 of "<path> <sha256>" lines. The Kestrel browser vendors exactly these files (tools/sync-keyboard-core.sh there) and its
# check recomputes this value over its own copy: a different value means the two engines have DIVERGED (owner, 2026-11-17: "they should not diverge").
set -euo pipefail
root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
base="$root/core/src/main/kotlin/app/kestrel/core"
( cd "$base" && find input translate -type f -name '*.kt' 2>/dev/null | LC_ALL=C sort | while read -r f; do printf '%s %s\n' "$f" "$(sha256sum "$f" | cut -d' ' -f1)"; done ) | sha256sum | cut -d' ' -f1
