# CHECKPOINT 288 — read me first, then TASKS.md

**Written:** 2026-11-17T08:54:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9cd8405a-34e17r` · **builds on:** `562b501` (this checkpoint is the commit after it)

## Just done
TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shared engine fingerprint) added

## Do this next
WAITING for lexicon helper ae9303e340af4888b (M10/M13: en.klx rebuild + rare-word prior). On its report: full :core:test :ime:testDebugUnitTest :ime:lintDebug, tick M10 M13, bash ship.sh, dispatch android.yml ref ccr-9cd8405a-34e17r, record-release; then M16 in /home/user/Browser own branch (vendor core/input from Keyboard via tools/sync-keyboard-core.sh + KEYBOARD_PIN)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
  ec5e72e ckpt 282: owner's reversal done: the 123 key and symbols page are back (space bar 4.9 un
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
