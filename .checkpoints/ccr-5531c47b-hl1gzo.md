# CHECKPOINT 356 — read me first, then TASKS.md

**Written:** 2026-11-17T19:24:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `fc71118` (this checkpoint is the commit after it)

## Just done
R17c: first emulator run READ (run 44150728400, typing quick, full speed, Android 10 tablet shape): Swoop types into Gecko page fields (field kinds text/textarea/editable show 33-34 mapped keys, 'see you at 5' arrived exactly), a PRIVATE tab gives Swoop noLearn=true and a normal tab noLearn=false (R17b3 proven on the emulator), Swoop 40 MB PSS (debug), Kestrel 244 MB PSS. All 34 FAILs were DRIVER bugs, fixed now: a digit keeps the symbols page up (design: space or ABC returns) so later letters/glides found no keys; Kestrel's address box is an EditText whose hint is hidden while it holds the URL; the very first raw tap batch of the run was lost (retry + counted). New: tools/sim/test_drive.py (fake keyboard, 12 checks), scene_autocorrect, report written after each row, 70 min step limit. Offline: KB-406 yardstick + casual-pairs pipeline (tools/glide/count_pairs.py, fetch_soda.py, LexiconFile.mergePairs/entriesOf/pairsOf, tools --base/--merge-pairs/--dump-vocab, LexiconMergeTest, workflow count-pairs.yml registered via a tmp bad push because GitHub lists a new non-default-branch workflow only after a first run). Runs dispatched 19:23Z: count-pairs (soda 250000) and daily-sim typing quick.

## Do this next
1) read daily-sim run (newest daily-sim.yml run on ccr-5531c47b-hl1gzo, started 19:23Z) artifact daily-sim-typing: report.md; 2) read count-pairs run artifact casual-pairs (pairs_tatoeba.txt pairs_soda.txt pairs_both.txt starters_*.txt stats.txt), then locally: gradlew :tools:glideLexicon --args='--base ime/src/main/assets/glide/en.klx --merge-pairs <file> --merge-max-weight 0.7 --merge-k 30 --out <scratch>/x.klx', copy to a scratch en.klx and run PredictionEval (core/build/classes) against it (GlideEval.findAsset picks ime/src/main/assets/glide/en.klx: temporarily copy, never commit a worse list); tune weight/k/top-k by the KB-406 numbers AND keep pair count growth small (budget: klx +1.5 MB, Swoop under ~42 MB PSS); run :core:test (glide accuracy tests) before committing the new en.klx; add credits to ime/src/main/assets/glide/LICENSE.txt and docs/DICTIONARY.md; 3) then daily-sim suite=all net=full quick=false; 4) releases Swoop 0.5.0-alpha4 (ime/build.gradle.kts code 9) and Kestrel alpha68 (Browser repo, check no android.yml run queued first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
