# CHECKPOINT 386 — read me first, then TASKS.md

**Written:** 2026-11-17T22:09:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `3e1362d` (this checkpoint is the commit after it)

## Just done
gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"full_build":"true","note":"0.5.0-alpha4: Swoop now guesses your next word and finishes your words much better, because it learned from 1.5 million everyday English sentences and not only from book English (in the lab the right word is on the suggestion strip after two letters 77 percent of the time instead of 63, and tapping suggestions saves about 52 percent of the keystrokes instead of 46). Texting shorthand such as pls, thx, omw, idk, brb and tbh is no longer turned into other words. A typo with a doubled letter (adress, realy) is fixed more often. The link with Kestrel: Kestrel's address bar now tells Swoop it is the browser address box (slash, full stop and Go keys, no automatic capital or space), private tabs in Kestrel make Swoop learn nothing, and Swoop has a receive screen that can take the words Kestrel's own small keyboard learned. NOT proven on the tablet or the phone: all of this was measured on a simulated Android 10 tablet and on lists of test sentences, so tell me what you see."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag swoop-v0.5.0-alpha4), then:  bash tools/record-release.sh swoop-v0.5.0-alpha4 "0.5.0-alpha4: Swoop now guesses your next word and finishes your words much better, because it learned from 1.5 million everyday English sentences and not only from book English (in the lab the right word is on the suggestion strip after two letters 77 percent of the time instead of 63, and tapping suggestions saves about 52 percent of the keystrokes instead of 46). Texting shorthand such as pls, thx, omw, idk, brb and tbh is no longer turned into other words. A typo with a doubled letter (adress, realy) is fixed more often. The link with Kestrel: Kestrel's address bar now tells Swoop it is the browser address box (slash, full stop and Go keys, no automatic capital or space), private tabs in Kestrel make Swoop learn nothing, and Swoop has a receive screen that can take the words Kestrel's own small keyboard learned. NOT proven on the tablet or the phone: all of this was measured on a simulated Android 10 tablet and on lists of test sentences, so tell me what you see."
NEVER ship swoop-v0.5.0-alpha4 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
