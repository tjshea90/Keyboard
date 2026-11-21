# CHECKPOINT 953 — read me first, then TASKS.md

**Written:** 2026-11-21T23:19:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `efc71c9` (this checkpoint is the commit after it)

## Just done
R30 DONE: alpha10 released (https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha10), run #24 id 44960077318 green (check, test-jvm, build), BUILDLOG recorded, TASKS R30c-e ticked

## Do this next
nothing running. Waiting on the owner (R30f): alpha10 on tablet + phone, Diagnostics 'Glide typing:' line (shape model avg/worst ms, SWITCHED ITSELF OFF if too slow). Idea R31 neural next-word model not started. Final plain answer to the owner is the last thing to send

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  a40cab1 ckpt 951: R30 alpha10 shipped to GitHub: ship.sh green (handoff test, 290 ime + core tes
  e13f2a7 ckpt 950: gated swoop-v0.7.0-alpha10 (ime code 21) and pushed it: handoff self-test, uni
  cc1cf34 ckpt 947: R30 step 4b: ime tests 290 green (KeysViewTest timed-glide test, safe-mode eng
  7391048 ckpt 942: R30 step 4a: :core ShapeRescorer (hybrid score+0.7*logP, gate 3.5, letter-box 
  5bfe37a ckpt 932: R30 step 3c DONE: three disjoint real-swipe sets incl. FUTO TEST split: hybrid
  881fdb5 ckpt 930: R30 step 3 RESULT: hybrid (shipped score + 0.5*net log P over the top-40) = +4
  e9c055f ckpt 927: R30 step 3a: RealSwipe.read now keeps swipe times as ms since the first point 
  fa2d033 ckpt 925: R30 step 2 DONE: core/.../glide/ShapeNet.kt (pure Kotlin forward pass of the o
  f635ef6 ckpt 921: R30 step 1c: the open encoder is fully specified in research/R30-trained-shape
  e074535 ckpt 920: R30 step 1b: helper saved the open encoder's source (MIT, Copyright 2026 Parth
```
