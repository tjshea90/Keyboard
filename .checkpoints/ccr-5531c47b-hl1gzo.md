# CHECKPOINT 338 — read me first, then TASKS.md

**Written:** 2026-11-17T19:09:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `91f3f27` (this checkpoint is the commit after it)

## Just done
KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences + 40 searches; PredictionEval: next word, completions after 1-3 letters as the strip shows them, keystrokes saved, 4 kinds of typo, right-word-changed; PredictionYardstickTest) and its FIRST NUMBERS recorded in research/LEARNING_RESULTS.md: next word in 3 = 24%, strip after 1 letter = 49%, KSR 46%, typos put right 90%, 0 right words spoiled. Weak spot = context (data: only 242k written-English pairs). Daily-sim run 44150728400 still in progress at 19:10Z (24 min in).

## Do this next
READ daily-sim run 44150728400 as soon as it ends (mcp__github__actions_list list_workflow_run_artifacts resource_id 44150728400 repo Keyboard; download_workflow_run_artifact; python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <empty scratchpad dir>; read console.txt report.md lab-server.jsonl). THEN: fix what it shows; dispatch daily-sim suite=all net=full quick=false; then bigram upgrade (casual pairs from Tatoeba/SODA built on a GitHub runner, never raw data in the repo) judged by PredictionEval; then releases Swoop alpha4 (code 9) and Kestrel alpha68 (code 169, check no android.yml run queued in Browser first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
