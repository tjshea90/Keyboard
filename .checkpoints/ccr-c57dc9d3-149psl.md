# CHECKPOINT 143 — read me first, then TASKS.md

**Written:** 2026-11-17T01:43:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `6ecbbdb` (this checkpoint is the commit after it)

## Just done
owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the keyboard so far

## Do this next
start N1/N5/N9/N2: (1) ImeLayout number row always on top (letters + symbols pages; MiniKeyboardGeometry.boxes gets optional row weights so the number row is thinner; decoder uses the same boxes), (2) KeysView split: static keys view + overlay layer (lit key, bubble, trail, strip) in a KeyboardRoot FrameLayout, requestUnbufferedDispatch, glide starts after ~6 dp of movement, (3) update KeysViewTest/LookTest. Then N4/N6: GlideTyping.suggest(prefix, prev, prev2) on the engine thread + learn tapped words at word end + erase-and-retype learning; presenter shows completions in the strip and a tap replaces the composing word. Then Prefs/options + settings redesign. Then release 0.2.0-alpha1 (versionCode 2). N3 floating last.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
  1bb33bd ckpt 112: docs: IF-THE-KEYBOARD-BREAKS no longer promises the Forget button before KB-20
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
