# CHECKPOINT 290 — read me first, then TASKS.md

**Written:** 2026-11-17T09:26:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9cd8405a-34e17r` · **builds on:** `8232797` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-9cd8405a-34e17r, inputs {"full_build":"true","note":"0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (settings, clipboard, translate), clipboard history, translate to Mexican Spanish (Enter translates, Enter sends), my dictionary, emoji suggestions, hold a suggestion to remove it, hide letters, squeeze and move the docked keyboard, capital after sentence end, glide wins over tap, bigger rarer-word-aware word list"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.4.0-alpha1), then:  bash tools/record-release.sh swoop-v0.4.0-alpha1 "0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (settings, clipboard, translate), clipboard history, translate to Mexican Spanish (Enter translates, Enter sends), my dictionary, emoji suggestions, hold a suggestion to remove it, hide letters, squeeze and move the docked keyboard, capital after sentence end, glide wins over tap, bigger rarer-word-aware word list"
NEVER ship swoop-v0.4.0-alpha1 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
  ec5e72e ckpt 282: owner's reversal done: the 123 key and symbols page are back (space bar 4.9 un
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
```
