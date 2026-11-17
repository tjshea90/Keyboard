# CHECKPOINT 296 — read me first, then TASKS.md

**Written:** 2026-11-17T15:57:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `037bbf6` (this checkpoint is the commit after it)

## Just done
P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl. ?123 ? a -> H?a, glide top, floating; real screenshots match the photo layout (light theme on the emulator); core MiniKeyboardModel M5 (symbols page returns to letters after one special character) + test, core 405 green; README updated; version 0.5.0-alpha1 code 6

## Do this next
dispatch emu-smoke.yml api 36 (Android 16) on this head and read it; bash ship.sh 'note' ; dispatch android.yml full_build=true note; wait tag+green; get_release_by_tag; bash tools/record-release.sh swoop-v0.5.0-alpha1 'same note'; then P3 Browser: sync engine (bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard after committing), port M9 glide-vs-tap into MiniNav inner KeysView (MIN_GLIDE_PITCHES 1.2 -> 0.45 + stayedOnKey, GLIDE_START 5dp), SwoopLink + Listener.navSwoop + Prefs.swoopKeyboard + Settings toggle; heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M ime/build.gradle.kts

## Last ten checkpoints
```
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
  f2a01e3 ckpt 291: RELEASED swoop-v0.4.0-alpha1 (run 44055649970 green, recorded); M10/M13 ticked
  7f64a43 ckpt 290: gated swoop-v0.4.0-alpha1 (ime code 5) and pushed it: handoff self-test, unit 
  8232797 ckpt 289: pre-ship: 0.4.0-alpha1: Gboard-style layout with ?123 bottom right, toolbar (s
  89345f9 ckpt 288: TASKS M1-M9,M11,M12,M14,M15 ticked with proofs; tools/core-input-hash.sh (shar
  fff8405 ckpt 287: M14 backup rotation + update-safety tests, version 0.4.0-alpha1 (code 5), READ
  b6d15cb ckpt 286: M8 squeeze/pinch/slide of docked keyboard + tests, toolbar/clipboard/emoji/dic
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
