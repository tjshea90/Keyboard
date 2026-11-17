# CHECKPOINT 184 — read me first, then TASKS.md

**Written:** 2026-11-17T02:14:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `0795d00` (this checkpoint is the commit after it)

## Just done
N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before the next letter, not in addresses/abbreviations/numbers), doubleSpace() full stop, autoCapsOn + autoCaps after non-letters and Backspace; TypingOptionsTest (9); core all green

## Do this next
N7/N8 ime part: (1) ime/Settings.kt typed prefs object (Prefs keys to add: TRAIL AUTOCAPS AUTOSPACE DOUBLESPACE CAPI NUMBERROW KEYHEIGHT PREVIEW HAPTIC SOUND THEME ACCENT SPACESLIDE) with defaults; (2) KeysView.applyOptions(Settings) + themeMode (system/dark/light) + sound + spaceSlide flag; (3) KeyboardPresenter.startInput applies Settings, sets controller.autoSpace/autoCapsOn, double-space (clock 600 ms) in onKey for Key.Ch(' '), 'i'->'I' fix; (4) new SetupActivity UI in SettingsUi.kt: hero status card, live preview KeysView, sections Typing / Suggestions and learning / Layout and look / Feel / Your data, accent chips; ime tests (Robolectric) ; then release 0.2.0-alpha1 (versionCode 2) and N3 floating last.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
