# CHECKPOINT 546 — read me first, then TASKS.md

**Written:** 2026-11-18T04:19:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `ca73de5` (this checkpoint is the commit after it)

## Just done
R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide strip [alt][next][alt], 2 new settings, docs/STRIP.md); ime suite 246 green. Research agent B (glide personalisation) still running

## Do this next
R20d: wait for the agent B notification, read research/R20-glide-personalization.md, pick the top 3-4 measurable techniques (first: position-dependent touch offset instead of one global bias; add a left/right-thumb noise model to GestureSim), implement in GlideDecoder/PersonalModel with before/after numbers from GlideEval/GlideHabitEval; then full :core:test :ime:testDebugUnitTest :ime:lintDebug, bump ime/build.gradle.kts to 0.7.0-alpha1 code 12, bash ship.sh, dispatch android.yml ONCE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2657834 ckpt 528: R20b built: after a glide the strip is [other guess][NEXT word][other guess] (
  bbb8240 ckpt 524: R20c done+tested (pronoun I everywhere: InputController.capitalizeI/fixLoneI/w
  c72bbee ckpt 514: wrote owner's second 2026-11-18 batch R20a-d into TASKS.md (his words); 0.6.0-
  b4f6a50 ckpt 511: swoop-v0.6.0-alpha2 RELEASED AND RECORDED: run 44232237543 green (check, test-
  cffd8a8 ckpt 506: DISPATCHED android.yml ONCE for swoop-v0.6.0-alpha2 (code 11) on ref claude/ke
  ae7bda6 ckpt 505: gated swoop-v0.6.0-alpha2 (ime code 11) and pushed it: handoff self-test, unit
  01a8630 ckpt 504: pre-ship: 0.6.0-alpha2: a glide always types the closest word (never 'no word 
  2ddf8c6 ckpt 490: R19c done+tested (docked two-axis resize via bar arrows, window-tall during dr
  60344cb ckpt 480: R19a/b/d core done+tested: always-answer relaxed decode pass (GlideDecoder.alw
  02723ef ckpt 469: wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)
```

(17 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
