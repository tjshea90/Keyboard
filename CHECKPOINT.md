# CHECKPOINT 302 — read me first, then TASKS.md

**Written:** 2026-11-17T16:41:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `e106dc9` (this checkpoint is the commit after it)

## Just done
BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36 emulator run 44127035979 green) and Kestrel G alpha66 (the Swoop link; run 44125112190 green); smoke.sh gets a night-mode screenshot stage (the photo is the dark theme); TASKS P1-P5 ticked; Q-SMARTSPACE logged

## Do this next
dispatch emu-smoke.yml api 36 and api 29 for the night-keyboard screenshot, fetch the artifact (python3 -I /home/user/Browser/tools/emu/fetch_artifact.py) and compare it with the photo; then final cleanup: delete the heartbeat trig_014NvZHSbSv1BNgvBVbYFsDR (mcp__claude-code-remote__delete_trigger), update CHECKPOINT/HANDOFF, final reply to the owner (release links: https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.5.0-alpha2 and https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha66)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
  ebc61e3 ckpt 292: M16 partly done: Browser branch pushed (vendored engine + pin + check + docs);
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
