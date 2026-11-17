# CHECKPOINT 286 — read me first, then TASKS.md

**Written:** 2026-11-17T08:48:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9cd8405a-34e17r` · **builds on:** `bb8368f` (this checkpoint is the commit after it)

## Just done
M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dictionary/short-swipe tests, ime 172 tests green

## Do this next
M14: .bak rotation in GlideHost.writeAtomically + fallback load + golden-format test; then wait for helper ae93 (lexicon) result, run full :core:test :ime:testDebugUnitTest :ime:lintDebug, bump ime/build.gradle.kts to 0.4.0 (code 5), README/docs, ship.sh + dispatch android.yml

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
  ec5e72e ckpt 282: owner's reversal done: the 123 key and symbols page are back (space bar 4.9 un
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
