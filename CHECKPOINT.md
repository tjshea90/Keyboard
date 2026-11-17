# CHECKPOINT 380 — read me first, then TASKS.md

**Written:** 2026-11-17T21:54:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `e7a49f2` (this checkpoint is the commit after it)

## Just done
R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20:48Z) = 81 OK, 2 FAIL, 3 WARN: all 6 field kinds + phrases + glides + autocorrect typed through Swoop into Gecko page fields, address box = OMNIBOX layout, private tab noLearn, 10 real sites loaded OK at full speed (first paint 195 ms - 2.8 s, finished 246 ms - 5.9 s; slowest apnews.com 5.9 s with 54 requests blocked, kotlinlang.org first paint 2.8 s), Swoop 41-44 MB PSS, Kestrel 244 MB. It stopped at site 11 of 14 (pypi.org) because the EMULATED ANDROID'S OWN system_server WATCHDOG fired at 20:47:51 (android.fg waiting for the ActivityManagerService lock) and adb shell timed out: so the films (video scene) did not run. 1 driver FAIL: after the Google results page the address box is not found in the UI dump. Word list with casual pairs + shorthand is now the shipped ime/src/main/assets/glide/en.klx (4.01 MB, 303 532 pairs, 58 new words) with Suggester.CONTEXT_MIX 0.75 and cheaper doubled-letter slips; :core:test and :ime:testDebugUnitTest green; docs/DICTIONARY.md, LICENSE.txt (Tatoeba CC BY 2.0 FR credit), research/LEARNING_RESULTS.md, TASKS (KB-406 ticked, R17c-prog). NOTHING released yet, nothing of this is proven on the real tablet or phone. Other sessions: Browser repo only touched on my own branch ccr-5531c47b-hl1gzo, no Browser workflow dispatched (its android.yml has no queued/running run; the other lane ccr-e89b9dfe-pl3lk5 is 3.5 h idle); Keyboard repo has no other active session.

## Do this next
1) re-run the rest of the simulation in smaller pieces so the emulator does not hang for 40 min: daily-sim.yml suite=pages minutes=10 then suite=video (add a per-scene adb health check: if 'adb shell true' takes > 20 s or system_server restarted, stop the scene and say so; consider -memory 2048 plus fewer apps open: close Kestrel tabs between sites), and look at why the address box is not found after the Google page (tools/sim/dailysim.py scene_address: find_edit after a page load; the bottom bar may be hidden while the page scrolls: scroll up / tap the page top first); 2) release Swoop 0.5.0-alpha4: ime/build.gradle.kts versionCode 9 / versionName 0.5.0-alpha4, bash ship.sh '<plain note>', dispatch android.yml ONCE (full_build true), verify tag + Release, bash tools/record-release.sh swoop-v0.5.0-alpha4 '<same note>'; 3) Browser (my branch only, NEVER the other session's, never main): after the Keyboard commit run bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard (refreshes the pinned core/input + en.klx copy for Kestrel's fallback keyboard), run its unit suites, THEN check mcp__github__actions_list list_workflow_runs Browser android.yml has no queued/in_progress run, bump geckoapp versionCode 169 / alpha68, bash tools/sync-ext-version.sh, bash ship.sh gecko '<note>', dispatch android.yml ONCE (app=gecko full_build=true note), verify, record-release; 4) write the Browser-side findings (GeckoSession.<init> 832 ms on the UI thread at cold start on the emulator, apnews.com 5.9 s, kotlinlang.org 2.8 s first paint, system_server watchdog after 40 min of use) into a new file docs/handoff/SIM_FINDINGS_2026-11-17.md in the Browser repo on my branch (a new file, nothing of the other session's is edited); 5) final plain-words report to the owner with the Release links.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
