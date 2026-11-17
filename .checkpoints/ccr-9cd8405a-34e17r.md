# CHECKPOINT 287 — read me first, then TASKS.md

**Written:** 2026-11-17T08:53:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9cd8405a-34e17r` · **builds on:** `b12af71` (this checkpoint is the commit after it)

## Just done
M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), README, smoke geometry for new layout; core+ime tests+lint green. WAITING for lexicon helper ae9303e340af4888b

## Do this next
when helper reports: run :core:test :ime:testDebugUnitTest :ime:lintDebug, check APK size budget, tick TASKS M1-M15 with proofs, bash ship.sh note, dispatch android.yml ref ccr-9cd8405a-34e17r full_build=true, wait, record-release; THEN M16: vendor Keyboard core/input into /home/user/Browser (my branch ccr-9cd8405a-34e17r only) with tools/sync-keyboard-core.sh + pin; write docs/KESTREL-LINK.md (note: no IPC data sharing because the dev signing key is public)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
  ec5e72e ckpt 282: owner's reversal done: the 123 key and symbols page are back (space bar 4.9 un
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
