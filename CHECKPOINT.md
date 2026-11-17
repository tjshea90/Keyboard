# CHECKPOINT 295 — read me first, then TASKS.md

**Written:** 2026-11-17T15:50:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `472a947` (this checkpoint is the commit after it)

## Just done
P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan default accent, smoke.sh geometry updated to the photo layout (4 rows, slots, ?123 left, +new check ?123 ? a -> '?a'); version still 0.4.0-alpha1

## Do this next
dispatch .github/workflows/emu-smoke.yml (api 29, then 36) on ref ccr-5531c47b-hl1gzo, read the result; if green bump ime/build.gradle.kts to 0.5.0-alpha1 code 6, bash ship.sh, dispatch android.yml full_build=true, record-release; then P3 Browser (git -C /home/user/Browser; branch ccr-5531c47b-hl1gzo already fast-forwarded to alpha64 lane tip 20e28803); heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
  50e0018 ckpt 285: M12/M15 dictionary (core UserDictionary, engine block/add/lookup, DictionaryAc
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
