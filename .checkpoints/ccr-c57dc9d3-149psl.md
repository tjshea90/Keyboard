# CHECKPOINT 152 — read me first, then TASKS.md

**Written:** 2026-11-17T01:49:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `af4466b` (this checkpoint is the commit after it)

## Just done
N1 number row always on top (thin row, letters+symbols pages), N5 glide starts after 5 dp, N9/N2 overlay layer + unbuffered dispatch + bubble clears on lift; ime 82 + core tests green, lint ok

## Do this next
N4/N6: (1) core GlideTyping.suggest(prefix, prev, prev2, limit) on the engine thread (lexicon prefix range + PersonalModel + pair counts); (2) GlideSession/InputController: learn tapped words at word end (space/punct/enter), erase-and-retype correction learning; (3) KeyboardPresenter shows completions in the strip while typing with taps, tap a suggestion replaces the composing word; (4) tests in core GlideSessionTest + ime KeyboardPresenterTest. Then Prefs/options (N7), settings redesign (N8), release 0.2.0-alpha1 (versionCode 2), N3 floating last.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
  6f13916 ckpt 118: owner made the repo PUBLIC and asked for the first release now (OR2 in TASKS):
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
