# CHECKPOINT 1109 — read me first, then TASKS.md

**Written:** 2026-11-26T21:06:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `d8f517d` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.7.0-alpha14 (ime code 25) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/awesome-bell-gmcf0i, inputs {"full_build":"true","note":"R34a2: new Settings > Help > Glide practice: glide 40 words that are shown to you, the keyboard keeps only the paths of those glides and the words it offered (nothing else you type, nothing is learned from them), and one file can be saved and attached to the chat so the keyboard can be tuned on this tablet's real glides"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.7.0-alpha14), then:  bash tools/record-release.sh swoop-v0.7.0-alpha14 "R34a2: new Settings > Help > Glide practice: glide 40 words that are shown to you, the keyboard keeps only the paths of those glides and the words it offered (nothing else you type, nothing is learned from them), and one file can be saved and attached to the chat so the keyboard can be tuned on this tablet's real glides"
NEVER ship swoop-v0.7.0-alpha14 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  237e3ca ckpt 1106: R34a2 offline lab PracticeEval + PracticeEvalTest (simulated tablet glides ro
  dcae6cc ckpt 1103: alpha13 RELEASED and recorded (run 45892015689 green); R34a2 glide practice c
  23bde72 ckpt 1092: R34a2 Glide practice written into TASKS (after the measured ceiling R34a-meas
  fc5913f ckpt 1091: R34a measured (docs only so far): word-pair model, weight grid and miss class
  7c25fd6 ckpt 1087: alpha13 (ime code 24, swoop-v0.7.0-alpha13) gated, pushed and android.yml DIS
  59b1eb9 ckpt 1086: gated swoop-v0.7.0-alpha13 (ime code 24) and pushed it: handoff self-test, un
  e2fabad ckpt 1085: R34b R34c DONE (e-mail boxes recognised by hint/label, learning in every ordi
  002111e ckpt 1069: R34d DONE in code+tests: KeysView no longer types the first letter when a sec
  d5ce1d3 ckpt 1063: R34e DONE in code+tests: translation switches itself off on a new input sessi
  1bc8768 ckpt 1057: R34 written into TASKS.md in the owner's words (5 items: glide engine, e-mail
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
