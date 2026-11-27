# CHECKPOINT 1231 — read me first, then TASKS.md

**Written:** 2026-11-27T04:03:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-turing-tyvm1e` · **builds on:** `5c96957` (this checkpoint is the commit after it)

## Just done
R38 code written, not yet verified: KeysView (erase key bigger via widenErase, keyAt by nearest edge, erase pressed during the other thumb's glide, fresh unmoved finger kept after the first thumb lifts), KeyboardRoot (system back-swipe exclusion strips, touches beside a squeezed keyboard handed to the keys), InputStats 3 counters, tests in KeysViewTest/KeyboardRootTest/DiagnosticsTest, PracticeEval text-only sweep; research/R38-glide-edges-erase.md written (section 5 sweep line not filled)

## Do this next
run :ime:testDebugUnitTest --tests KeysViewTest --tests KeyboardRootTest --tests DiagnosticsTest via retry loop (scratchpad retry.sh; the robolectric jar nativeruntime-dist-compat-1.0.16 hits HTTP 429, just retry), fix reds, then full :core:test + :ime:testDebugUnitTest + :ime:lintDebug, re-run PracticeEval on his file for the text-only sweep and put the numbers into R38 section 2, then R38-1..5 ticks, bump ime versionCode 28 / 0.7.0-alpha17, ship.sh, dispatch android.yml, record-release, plain-words answer

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b1d3f23 ckpt 1216: R38 (owner 2026-11-27: training file alpha16, glide edges/one-letter, bigger 
  e66717b ckpt 1205: R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
