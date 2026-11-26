# CHECKPOINT 1091 — read me first, then TASKS.md

**Written:** 2026-11-26T20:47:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `3287276` (this checkpoint is the commit after it)

## Just done
R34a measured (docs only so far): word-pair model, weight grid and miss classes are all flat/at ceiling on three real-swipe sets; R34a2 Glide practice written into TASKS as the next step

## Do this next
Next: build R34a2 in this order: (1) core GlidePractice.kt (words list, record, score, serialize/parse magic SWOOP-GLIDE-PRACTICE 1) + GlidePracticeTest; (2) secretscan.sh block the magic; (3) ime Practice.kt hook object + KeyboardPresenter hook (privateImeOptions app.swoop.practice=1 -> startInput practiceOn; onGlideEnd copy path, after decode call Practice.hook with top3, keys, view size) + presenter test; (4) PracticeActivity (+manifest, strings, Setup row Help) + Robolectric test; (5) PracticeEval lab in core tests (env PRACTICE_FILE); (6) full suites + lint, bump alpha14 (code 25), ship.sh, dispatch. WAIT for alpha13 run 45892015689 first (record-release after green)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7c25fd6 ckpt 1087: alpha13 (ime code 24, swoop-v0.7.0-alpha13) gated, pushed and android.yml DIS
  59b1eb9 ckpt 1086: gated swoop-v0.7.0-alpha13 (ime code 24) and pushed it: handoff self-test, un
  e2fabad ckpt 1085: R34b R34c DONE (e-mail boxes recognised by hint/label, learning in every ordi
  002111e ckpt 1069: R34d DONE in code+tests: KeysView no longer types the first letter when a sec
  d5ce1d3 ckpt 1063: R34e DONE in code+tests: translation switches itself off on a new input sessi
  1bc8768 ckpt 1057: R34 written into TASKS.md in the owner's words (5 items: glide engine, e-mail
  4c2c91a ckpt 1054: R33 DONE: swoop-v0.7.0-alpha12 released (https://github.com/tjshea90/Keyboard
  3a86baf ckpt 1048: alpha12 (ime code 23, swoop-v0.7.0-alpha12) gated and pushed; android.yml DIS
  441d4b0 ckpt 1047: gated swoop-v0.7.0-alpha12 (ime code 23) and pushed it: handoff self-test, un
  f9936c8 ckpt 1046: pre-ship: R33: a word on the strip that is spelled like the glided word and f
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
