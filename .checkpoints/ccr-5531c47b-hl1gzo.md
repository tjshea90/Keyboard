# CHECKPOINT 294 — read me first, then TASKS.md

**Written:** 2026-11-17T15:48:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `91ff997` (this checkpoint is the commit after it)

## Just done
P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysViewLookTest pixel colours measured from the photo (bg 0d1d1d, keys 253535, modifiers 35494a, ?123 a4cedc, Enter 00dde6) and a rendered side-by-side with the photo; ImeLayout = 4 rows ROW_UNITS 10, no number row by default (hints), ?123 left / Enter right, Shift+Backspace on row 3, symbols as 2 pages (Page.SYMBOLS2, =\< toggle), MiniKeyboardGeometry.slots (equal pitch rows), KeysView: gaps, round keys, drawn shift/backspace/enter/gear/translate icons, toolbar spread; defaults number row off + cyan accent; KeysService.onAppPrivateCommand FLOAT/DOCK for the Kestrel panel; docs/LAYOUT.md. lint running

## Do this next
run :ime:lintDebug (log scratchpad lint1.log); then bash tools/core-input-hash.sh; update docs/KESTREL-LINK.md; tick P1 (bash tools/claim.sh P1 --done 'ImeLayoutTest+KeysViewLookTest+render'); then P3: Browser repo (git -C /home/user/Browser): branch ccr-5531c47b-hl1gzo is at old 063b276, fast-forward it to origin/ccr-e89b9dfe-pl3lk5 (alpha64, already contains the vendored engine), run tools/sync-keyboard-core.sh with this repo, wire the nav panel button -> Swoop (sendAppPrivateCommand ACTION_FLOAT after showSoftInput); heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
```

(30 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
