# CHECKPOINT 304 — read me first, then TASKS.md

**Written:** 2026-11-17T17:21:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `0c7f39a` (this checkpoint is the commit after it)

## Just done
Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 44131311857 and FIXED in core InputController: the smoke test types into the keyboard's OWN Setup field (same process); Android 10 runs such editor calls inline, so the cursor report arrived INSIDE commitText before the edit was noted and the controller counted its edit twice (window 'H??'), so no space/capital after ?. Fix: Guarded editor counts calls in flight, early reports are matched to the edit when noted (expect), an editor that reports inline leaves no waiting entries (inlineEditor), Backspace at text start leaves none; tests: core InlineReportsTest (7), InputControllerFuzzTest inline mode on the same streams as the async runs, app KeyboardPresenterTest 2 more (rig reports inside the calls), all core + ime suites green. Also: Settings.theme default is now Dark (the photo is dark; a device in day mode got a light keyboard; test theKeyboardIsDarkUntilAnotherLookIsChosen), docs/LAYOUT.md updated. Stage 6b (full stop, space, letter -> '. B') passed on Android 10 already (run 44131311857, H?a. B). Android 16 run 44131324409 hit the known emulator flake 'no text field on the Setup screen' (not a code failure).

## Do this next
(1) sync the engine into Kestrel: bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard (needs a clean Keyboard tree: ckpt just committed), then in the Browser tree only via git -C and absolute paths: run /home/user/Browser/gradlew -p /home/user/Browser :core:test (and bash tools/check-keyboard-core.sh), commit + push on branch ccr-5531c47b-hl1gzo; (2) dispatch emu-smoke.yml api 29 and api 36 on this head, fetch artifacts (python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>): the smoke.log 'H? A' check and stage 6b must pass on both, and on 29 the keyboard trace must show beforeLen equal to the text length; (3) if green: tick Q-SMARTSPACE with claim.sh --done, bump ime/build.gradle.kts to 0.5.0-alpha3 (code 8), bash ship.sh, dispatch android.yml (full_build true + note), record-release.sh after green; (4) the final plain-words reply to the owner with both Release links and the amcheckpoint the hook asked for

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
  a45db76 ckpt 301: gated swoop-v0.5.0-alpha2 (ime code 7) and pushed it: handoff self-test, unit 
  0920da6 ckpt 300: smoke.sh: bring the docked keyboard back reliably before the floating stage (u
  442dba5 ckpt 299: Android 16 emulator run 44122461319: the inset fix WORKS (keys now above the 3
  c68504b ckpt 298: Android 16 fix: the emulator run on API 36 (44120198332) showed the system nav
  8568d04 ckpt 297: gated swoop-v0.5.0-alpha1 (ime code 6) and pushed it: handoff self-test, unit 
  59b102c ckpt 296: P1+P4(api29) done: emulator smoke run 44118913362 (Android 10) 0 failures incl
  2e51a71 ckpt 295: P1 verified further: ime lint ok (online), gear/translate icons drawn, cyan de
  4d33958 ckpt 294: P1 (photo layout) built and green: core 404 tests, ime 181 tests incl. KeysVie
```

(22 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
