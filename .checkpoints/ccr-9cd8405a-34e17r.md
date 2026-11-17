# CHECKPOINT 285 — read me first, then TASKS.md

**Written:** 2026-11-17T08:44:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9cd8405a-34e17r` · **builds on:** `1367803` (this checkpoint is the commit after it)

## Just done
M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryActivity, strip long-press confirm), M11 emoji chips, M7 translate wired (helper's engine; Enter translates/Enter sends; INTERNET permission), ime tests+lint green

## Do this next
M8 squeeze docked keyboard: KeyboardRoot docked width = Settings.dockFrac/dockX, GripBar pinch + end arrows + horizontal drag, onComputeInsets touchable region; then M14 persistence test + M10/M13 when helper ae9303e340af4888b reports; then add ime tests for toolbar/clipboard/hideLetters/emoji/glide-short-swipe; version 0.4.0; ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
  ec5e72e ckpt 282: owner's reversal done: the 123 key and symbols page are back (space bar 4.9 un
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
