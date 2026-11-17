# CHECKPOINT 369 — read me first, then TASKS.md

**Written:** 2026-11-17T20:01:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `2fa3f46` (this checkpoint is the commit after it)

## Just done
R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS on Android 10 + real Gecko: tap typing exact in text/textarea/editable page fields, 10/10 glided words (also with a thumb 0.2 key off), autocorrect (teh/dont/recieve/im) works in Gecko and one Backspace restores the typed word, address box shows Swoop browser layout (startInput kind=OMNIBOX omnibox=true), example.com loads 1.0 s, en.wikipedia.org 3.3 s (paint 970 ms, done 1522 ms), private tab noLearn=true, Swoop 40.3 MB PSS. Root cause of the earlier all-FAIL runs: raw touch injection needs adb root (workflow + driver now do it, with a loud fallback). FOUND by the sim: autocorrect never fixes misspellings that are in the word list as rare words (helo -15.5, wierd -15.9, untill -15.2, wether -14.4 are 'known' above DEEP_TAIL -16) and mangles texting abbreviations that are not in the list (thx->th, brb->barb, tbh->th, idk->id, omw->mow, pls->plus, hehe->here, ofc->of). KB-406 yardstick + casual pairs: Tatoeba pairs (1.5M human sentences, 11.3M tokens, CC BY 2.0 FR, counted on a runner by count-pairs.yml, held-out 1/40) merged with mergePairs w=0.55 k=30 min-count 6 = klx 3.64 -> 4.01 MB: next word in 3 24.2 -> 28.8 %, strip after 2 letters 63.4 -> 75.9 %, keystrokes saved 46.2 -> 51.3 % (held-out Tatoeba 43.7 -> 47.4), typos put right 90.2 -> 92.7 %, simulated glide in running text 95.7 -> 96.9 % (sloppy, mine) and 91.7 -> 92.8 % (held-out); SODA adds nothing on human text so it is NOT used. Suggester.CONTEXT_MIX 0.6 -> higher gives a little more (0.9: KSR 52.0/48.1). Runs: daily-sim suite=all quick=false dispatched.

## Do this next
1) DECIDE + APPLY: ship Tatoeba mc6 pairs: gradlew :tools:glideLexicon --args='--base ime/src/main/assets/glide/en.klx --merge-pairs <scratch>/lm/T_mc6.txt --merge-max-weight 0.55 --merge-k 30 --out ime/src/main/assets/glide/en.klx' (T_mc6.txt = pairs_tatoeba.txt min-count 6, the file is in the scratchpad pairs3 + lm; if lost: re-dispatch count-pairs.yml and prune with count>=6), set Suggester.CONTEXT_MIX 0.75, run :core:test + PredictionEval before/after, credits in ime/src/main/assets/glide/LICENSE.txt (Tatoeba CC BY 2.0 FR) + docs/DICTIONARY.md 'Casual pairs'; 2) texting abbreviations + misspellings: add tools/glide/texting-words.txt via a new tools --add-words (min counts like --extra) so pls plz thx omw idk btw lmk tbh brb ttyl np ty rn irl nvm ikr ofc jk wtf lmao hehe... are KNOWN (never autocorrected), lower Suggester.DEEP_TAIL (-16 -> about -14.5) so rare list-misspellings (helo wierd untill wether publically) are fixed, cheaper MISSING for a doubled letter (adress realy tomorow), measure with a new misspelling section in PredictionEval; 3) read the daily-sim suite=all run (pages + video at full speed) and file Browser-side findings in docs/ for the other session; 4) releases Swoop 0.5.0-alpha4 (ime/build.gradle.kts code 9) + Kestrel alpha68.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
