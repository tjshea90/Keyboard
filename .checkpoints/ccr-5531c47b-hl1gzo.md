# CHECKPOINT 293 — read me first, then TASKS.md

**Written:** 2026-11-17T15:32:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `1f35bb6` (this checkpoint is the commit after it)

## Just done
cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead for 6 h) had ALREADY built M1-M15 and released swoop-v0.4.0-alpha1; its M1 layout was modelled from words (the photo never reached it). Wrote my P1-P5 into TASKS.md, claimed P1, heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR (cron 31 */2, bound to this session)

## Do this next
P1: re-model the letters page on the photo (the photo is at the uploads path in the conversation; colours: bg 0d1d1d keys 253535 modifiers 35494a ?123 a4cedc enter 00dde6 text f1ffff hint 9babab): core ImeLayout.rows (ROW_UNITS 10, 4 rows, numberRow default off, Shift 1.5 / Backspace 1.5 on row 3, bottom ?123 1.5 , 1 space 5 . 1 Enter 1.5, symbols as 4 rows + SYMBOLS2), then KeysView.drawKey (gaps, corner radius, round ?123/Enter, drawn shift/backspace/enter icons, toolbar spread), update ImeLayoutTest/KeysViewTest/KeysViewLookTest, render PNGs with SWOOP_LOOK_DIR and compare with the photo; heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
  bba95ae ckpt 284: M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 n
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
