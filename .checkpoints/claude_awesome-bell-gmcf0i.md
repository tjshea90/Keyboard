# CHECKPOINT 1085 — read me first, then TASKS.md

**Written:** 2026-11-26T20:30:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `e06329b` (this checkpoint is the commit after it)

## Just done
R34b R34c DONE (e-mail boxes recognised by hint/label, learning in every ordinary box + at box change, domains after @, My e-mail addresses screen); full :core:test + :ime tests + lint green; version bumped to 0.7.0-alpha13 (code 24)

## Do this next
Next: bash ship.sh "<note>" (runs both suites again), dispatch android.yml (mcp__github__actions_run_trigger run_workflow ref=claude/awesome-bell-gmcf0i inputs full_build=true + note), wait, record-release.sh swoop-v0.7.0-alpha13, give the owner the link. Meanwhile R34a LM experiment: scratchpad lm/pairs_a/b/c.txt (C4 pairs, 4 shards) -> build klx candidates with ./gradlew :tools:glideLexicon, eval with GLIDE_KLX=... REAL_SWIPES=1 ShapeNetEvalTest

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M ime/build.gradle.kts

## Last ten checkpoints
```
  002111e ckpt 1069: R34d DONE in code+tests: KeysView no longer types the first letter when a sec
  d5ce1d3 ckpt 1063: R34e DONE in code+tests: translation switches itself off on a new input sessi
  1bc8768 ckpt 1057: R34 written into TASKS.md in the owner's words (5 items: glide engine, e-mail
  4c2c91a ckpt 1054: R33 DONE: swoop-v0.7.0-alpha12 released (https://github.com/tjshea90/Keyboard
  3a86baf ckpt 1048: alpha12 (ime code 23, swoop-v0.7.0-alpha12) gated and pushed; android.yml DIS
  441d4b0 ckpt 1047: gated swoop-v0.7.0-alpha12 (ime code 23) and pushed it: handoff self-test, un
  f9936c8 ckpt 1046: pre-ship: R33: a word on the strip that is spelled like the glided word and f
  a20ef6e ckpt 1036: R33 code: look-alike-fix rule (GlideSession.looksLikeFix + SpellingNear + eng
  00d6ec0 ckpt 1022: R33 written into TASKS.md (owner: ave still wins over and, tapping and adds i
  aed8fb7 ckpt 1019: R32 DONE: swoop-v0.7.0-alpha11 released (https://github.com/tjshea90/Keyboard
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
