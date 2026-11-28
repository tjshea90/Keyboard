#!/usr/bin/env bash
# sync-browser.sh - keep the Kestrel browser's built-in keyboard IDENTICAL to Swoop, with one command.
# Owner, 2026-11-28: "make the browser app always use the latest version of swoop with everything it learned so that the built in keyboard in the browser is identical at all times to swoop and everything it
# learned and all version updates" (owner requests 16 and 17 before it; the design is docs/KESTREL-LINK.md).
#
#   bash tools/sync-browser.sh --status   one line: is the Browser's engine (newest Browser branch) the same as this checkout's? resume.sh prints it at every session start
#   bash tools/sync-browser.sh            sync: bring the Browser's newest tips into MY Browser branch, copy the engine (core/.../input), the word list and the shape network, check the pin,
#                                         run the Browser's fast checks, then commit and push THAT branch (the Browser's own tools/ckpt.sh does the commit)
#   bash tools/sync-browser.sh --test     the same, plus the Browser's JVM suites (:core:test :app:testDebugUnitTest, about 25 minutes) BEFORE the commit
#
# WHEN: after EVERY Swoop change under core/src/main/kotlin/app/kestrel/core/input or the word list / shape network, once it is committed (ship.sh writes it into the next-step card, resume.sh warns while it is behind).
# RULES it keeps (CLAUDE.md): only through git -C and absolute paths, never a cd into the Browser (its Claude hooks would run), only MY branch (this session's, the same name in both repos), never main, never
# another session's branch, never a release from here (the Browser's release is its own workflow and lock; another session may be shipping).
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
MODE=sync; TEST=0
for a in "$@"; do case "$a" in --status) MODE=status ;; --test) TEST=1 ;; esac; done
B="$(realpath -m "$D/../Browser")"
if [ ! -d "$B/.git" ] && [ ! -f "$B/.git" ]; then echo "  Browser link: no checkout of tjshea90/Browser next to this one ($B): add it (add_repo) and clone it there"; exit 2; fi
OURS="$(bash tools/core-input-hash.sh)"
timeout "$([ "$MODE" = status ] && echo 15 || echo 60)" git -C "$B" fetch -q origin '+refs/heads/*:refs/remotes/origin/*' >/dev/null 2>&1 || echo "  Browser link: could not reach GitHub, using what was fetched before"
# the line the other Browser sessions ship from = the branch with the newest commit
NEWEST="$(git -C "$B" for-each-ref --sort=-committerdate --format='%(refname:short)' refs/remotes/origin 2>/dev/null | grep -v -e '^origin$' -e '^origin/HEAD$' | head -1)"
pin() { git -C "$B" show "$1:KEYBOARD_PIN" 2>/dev/null | sed -n "s/^$2=//p"; }
THEIRS="$(pin "$NEWEST" engine_sha256)"
if [ "$MODE" = status ]; then
  if [ -z "$THEIRS" ]; then echo "  Browser link: the newest Browser branch ($NEWEST) has no KEYBOARD_PIN"; exit 1; fi
  if [ "$THEIRS" = "$OURS" ]; then echo "  OK    Browser link: the Kestrel browser's keyboard engine ($NEWEST, Swoop commit $(pin "$NEWEST" keyboard_commit | cut -c1-7)) is identical to this one"; exit 0; fi
  echo "  !!    Browser link: the Kestrel browser's keyboard engine is BEHIND Swoop's (newest Browser branch $NEWEST is pinned to Swoop commit $(pin "$NEWEST" keyboard_commit | cut -c1-7)): after your core changes are committed run  bash tools/sync-browser.sh"
  exit 1
fi

# ---- sync
MY="$(git branch --show-current 2>/dev/null)"
[ -n "$MY" ] || { echo "  no branch here"; exit 1; }
if [ -n "$(git status --porcelain core ime/src/main/assets/glide 2>/dev/null)" ]; then echo "  FAIL  uncommitted changes in core/ or the assets: commit them first (the Browser pin names a commit)"; exit 1; fi
BB="$(git -C "$B" branch --show-current 2>/dev/null)"
if [ "$BB" != "$MY" ]; then echo "  FAIL  the Browser checkout is on '$BB', this session's branch is '$MY': switch it with  git -C $B switch $MY  (create it from the newest Browser tip first: git -C $B switch -c $MY $NEWEST)"; exit 1; fi
if [ -n "$(git -C "$B" status --porcelain 2>/dev/null)" ]; then echo "  FAIL  the Browser checkout has uncommitted work: commit it there first (bash $B/tools/ckpt.sh \"did\" \"next\")"; exit 1; fi
echo "  == 1/5 bring the Browser's other branches into $MY (the Browser's own tools/sync.sh)"
bash "$B/tools/sync.sh" --quiet 2>&1 | tail -5
echo "  == 2/5 copy the engine, the word list and the shape network"
bash "$B/tools/sync-keyboard-core.sh" "$D" 2>&1 | tail -7 || exit 1
echo "  == 3/5 check the pin"
bash "$B/tools/check-keyboard-core.sh" || exit 1
if [ "$TEST" = 1 ]; then
  echo "  == 4/5 the Browser's JVM suites (long)"
  "$B/gradlew" -p "$B" :core:test :app:testDebugUnitTest --console=plain -q || { echo "  FAIL  the Browser's tests are red with this engine: fix before the commit (the other Browser sessions merge this branch)"; exit 1; }
else
  echo "  == 4/5 (the Browser's JVM suites were NOT run: use --test, or run  $B/gradlew -p $B :core:test :app:testDebugUnitTest  yourself, before anyone ships from the Browser)"
fi
echo "  == 5/5 commit and push the Browser branch $MY"
VER="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' ime/build.gradle.kts | head -1)"
bash "$B/tools/ckpt.sh" "keyboard link: the Kestrel keyboard engine is Swoop $VER now (pin $(git rev-parse --short HEAD)): engine, word list and shape network copied by tools/sync-keyboard-core.sh" "next: build the Browser (ship.sh gecko), never while another session ships; the owner installs the new Kestrel to get this keyboard" 2>&1 | tail -4
