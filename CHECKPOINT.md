# CHECKPOINT 514 — read me first, then TASKS.md

**Written:** 2026-11-18T04:03:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `c73f799` (this checkpoint is the commit after it)

## Just done
wrote owner's second 2026-11-18 batch R20a-d into TASKS.md (his words); 0.6.0-alpha2 is released and recorded

## Do this next
start: (1) spawn 2 research agents (clipboard chip + next-word strip layout in Gboard/SwiftKey/iOS; glide-decoder personalisation techniques) writing to research/R20-*.md, (2) meanwhile R20c: grep capitalizeI in KeyboardPresenter, write a failing presenter test for 'i' and 'i\'m' typed in the middle of text/after '3) ', find root cause, (3) read ime/.../ClipboardHost.kt + KeysView chips for R20a

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b4f6a50 ckpt 511: swoop-v0.6.0-alpha2 RELEASED AND RECORDED: run 44232237543 green (check, test-
  cffd8a8 ckpt 506: DISPATCHED android.yml ONCE for swoop-v0.6.0-alpha2 (code 11) on ref claude/ke
  ae7bda6 ckpt 505: gated swoop-v0.6.0-alpha2 (ime code 11) and pushed it: handoff self-test, unit
  01a8630 ckpt 504: pre-ship: 0.6.0-alpha2: a glide always types the closest word (never 'no word 
  2ddf8c6 ckpt 490: R19c done+tested (docked two-axis resize via bar arrows, window-tall during dr
  60344cb ckpt 480: R19a/b/d core done+tested: always-answer relaxed decode pass (GlideDecoder.alw
  02723ef ckpt 469: wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)
  3839c16 ckpt 466: handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings bl
  c028463 ckpt 464: 0.6.0-alpha1 RELEASED AND RECORDED: run 44220063374 green (check, test-jvm inc
  37209dd ckpt 462: 0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ON
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
