# CHECKPOINT 401 — read me first, then TASKS.md

**Written:** 2026-11-17T22:30:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `9a16e59` (this checkpoint is the commit after it)

## Just done
FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are PUBLISHED and RECORDED (links in CHECKPOINT 397 / BUILDLOG.md of both repos). The typing run on the released commit (44187463954) = no regression: tapped phrases exact, 10 of 10 glided words in text / textarea / contenteditable, autocorrect 4 of 6 (helo and wierd stay on purpose), address box OMNIBOX, private tab noLearn; the one 'missing question mark' was the LAB SERVER taking events out of order (page counter n=153 had the '?'): labserver.py now ignores late events (tested). New: dailysim.py taps Block on a permission dialog (Google asked for the location and hid the address box), pypi.org and the 1080p film removed (they killed the emulator's graphics), film scene presses play. Committed but NOT released: Suggester.VULGAR (no vulgar word as an unprompted NEXT-WORD guess, 0 of 5000 contexts instead of 6 in alpha4 / 15 in alpha3; PredictionYardstickTest + SuggesterTest). Nothing is running except the optional hotspot page run 44187266683.

## Do this next
1) OPTIONAL: read the hotspot (5 Mbit/s imitation) page run 44187266683 (artifact daily-sim-pages) and add its table to Browser docs/handoff/SIM_FINDINGS_2026-11-17.md (my branch, new file) and TASKS R17c-prog; 2) NEXT SWOOP RELEASE (only when the owner asks or other work joins it): 0.5.0-alpha5 = the VULGAR next-word filter (already committed), bump ime/build.gradle.kts code 10, bash ship.sh, dispatch android.yml ONCE, record-release; 3) things the owner must test on the real tablet and phone (NOT proven anywhere): the navigation panel opening Swoop floating, the keyboard picker, the hand-over button, real-thumb accuracy, speed and memory on the MT8168B; 4) the owner's call: show the typed word in quotes on the strip only when a fix is pending (gives back about 8 points of hit rate), a comma/full-stop flag for sentence starters, trigram support (KB-201).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
