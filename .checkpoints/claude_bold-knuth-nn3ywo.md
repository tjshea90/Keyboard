# CHECKPOINT 1285 — read me first, then TASKS.md

**Written:** 2026-11-28T08:49:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `2a57dcd` (this checkpoint is the commit after it)

## Just done
R40-3 root cause found and fixed in core InputController: onSelectionUpdate now believes the editor's composing region (composingEndedByApp, new InputStats.composingLost) and pickWord replaces the WHOLE word when only its tail was composing; red-first tests: core ComposingDroppedTest (4), InputControllerTest (old 'composing report ignored' test replaced by 2 tests), presenter KeyboardPresenterTest +2, seeded StripPickFuzzTest (mutation-checked: without the fix seed 98 gives 'IwoIso'); core editor suites + presenter suite green. Scratch probe ReviseProbeTest in core tests (to delete). R40-1/4 analysis: GlideSession.revisePrevious (R26a) is the only retroactive rewrite of a glided word and has NO common-word/frequency guard; shipped data alone never flips to it -> yo, so the flip needs a learned pair/twin-key path (not yet reproduced)

## Do this next
next: R40-1/4 in core/src/main/kotlin/app/kestrel/core/input/glide/GlideSession.kt revisePrevious: (1) red test in core PostCorrectionTest with hand-set scores + a lexicon where yo has a learned/sparse pair after to (to 5.0 vs yo 4.9, engine.learnWord('yo','it')) proving the swap happens today; (2) add a pure CorrectionGuard (core glide package) = frequency weights: never swap a common word for one >2 nats rarer in the unigram prior, never to a word below Rarity.FLOOR unless the person uses it; (3) learn revised words lightly (keepTrust) not trusted; (4) lab ReviseAuditLabTest over EvalCorpus with GestureSim sloppy noise, variants current vs guarded, numbers into research/R40-correction-policies.md; (5) delete core/src/test/.../ReviseProbeTest.kt. Then R40-2 research doc (WebSearch + web-fetch agent on AOSP Suggest.java / HeliBoard), then ship alpha18, then R40-6 Browser sync LAST

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
  55b0b1e ckpt 1248: gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, un
  ec2b546 ckpt 1247: R38-1..4 ticked; version bumped to code 28 / 0.7.0-alpha17; full suites green
  a540b09 ckpt 1245: R39 DONE: repo settings put Google's Maven Central copy before mavenCentral (
  e97ab52 ckpt 1236: R39 (owner mid-session: Maven 429 keeps happening, thought a Google mirror in
  b04be3e ckpt 1231: R38 code written, not yet verified: KeysView (erase key bigger via widenErase
```

(22 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
