# CHECKPOINT 303 — read me first, then TASKS.md

**Written:** 2026-11-17T16:56:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `4207719` (this checkpoint is the commit after it)

## Just done
Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot night-keyboard.png shows the dark photo look (teal-black background, slate keys, pale-blue ?123 pill left, cyan Enter pill right, blank space bar, digit hints). Chasing Q-SMARTSPACE (Android 10 typed H?a, no space and no capital after ?): added debug-only trace lines (Diag, booleans and counts, never a character) to KeyboardPresenter.onKey/onUpdateSelection, a smoke.sh stage 6b (full stop, space, letter must end with '. B'), unit tests green; dispatched emu-smoke.yml api 29 and api 36 on this head.

## Do this next
read the two emu-smoke runs (mcp__github__actions_list list_workflow_runs emu-smoke.yml branch ccr-5531c47b-hl1gzo, newest two; artifacts emu-smoke-29 and emu-smoke-36, get the url with actions_get download_workflow_run_artifact, fetch with python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir in scratchpad>); in smoke.log the 'key before/after' and 'sel report' lines show which space-after-punctuation condition fails on Android 10 (autoSpace, composing, sel, lastPunct); fix it in core InputController / KeyboardPresenter WITH a test; THEN decide the default colours (Settings.theme default 0 = Automatic follows the phone; the photo is dark, so default 1 = Dark is the safer reading of 'exactly like the photo'; tests: SetupActivityTest, KeyboardPresenterTest line 296); ship swoop 0.5.0-alpha3 only if a real fix is in (bump ime/build.gradle.kts, bash ship.sh, dispatch android.yml, record-release); then the final plain-words reply to the owner (links: https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.5.0-alpha2 and https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha66) and the amcheckpoint the hook asked for

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
  25f4de5 ckpt 293: cold-resumed at the 3h timer: lane ccr-9cd8405a-34e17r (other account, dead fo
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
