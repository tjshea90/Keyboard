# CHECKPOINT 165 — read me first, then TASKS.md

**Written:** 2026-11-17T02:03:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `b14c4a9` (this checkpoint is the commit after it)

## Just done
N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect decision), TypingLearner (typed words/pairs/triples, picks, erase-and-retype, autocorrect undo), controller pickWord/typeCorrected/revert, PersonalModel unlearn/completions/nextWords, engine suggest/learnWord/unlearnWord; core 259 tests green

## Do this next
wire into :ime: (1) KeyboardPresenter: TypingLearner + suggestion refresh after every key (engine.suggest -> strip, drop stale), onCandidate -> learner.pick when no glide on screen, autocorrect at Space/punct when pref on; (2) KeysView strip visible when suggestions on even if glide off (setSuggestionsEnabled, no glide hint); (3) GlideHost passes saveOwn; Prefs SUGGEST/LEARN/AUTOCORRECT; (4) ime tests; then N7 options + N8 settings redesign, release 0.2.0-alpha1, N3 floating.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
  df9bd25 ckpt 134: emu-smoke: quiet job log (gradle cache off for that job) and smoke.log artifac
  4b66884 ckpt 132: emulator smoke result (run 43991821449, Android 10 x86_64 emulator on a GitHub
  26a28ff ckpt 130: gated swoop-v0.1.0-alpha1 (ime code 1) and pushed it: handoff self-test, unit 
  a7df414 ckpt 129: owner decision: sign like the other repos - PUBLIC dev key signing/dev/swoop-d
  7e85792 ckpt 122: README.md written for the now-public repo; emulator smoke run 43991821449 (API
  67b4887 ckpt 120: emulator smoke test written (tools/emu/smoke.sh + .github/workflows/emu-smoke.
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
