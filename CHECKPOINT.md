# CHECKPOINT 466 — read me first, then TASKS.md

**Written:** 2026-11-18T02:13:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-typing-improvements-jetik4` · **builds on:** `454c6dc` (this checkpoint is the commit after it)

## Just done
handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings block refreshed) after swoop-v0.6.0-alpha1 release

## Do this next
Nothing running. Owner tests swoop-v0.6.0-alpha1 on tablet + phone; then fix what they report (next release 0.6.0-alpha2 code 11 via bash ship.sh). Optional: emu-smoke.yml for the new UI; sync Kestrel pinned copy when a Browser checkout exists

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  .handoff/handoff.json
    M  .handoff/handoff.md
    A  .handoff/handoffs/2026-11-18/handoff-2026-11-18T02-13-20-707Z.json
    A  .handoff/handoffs/2026-11-18/handoff-2026-11-18T02-13-20-707Z.md
    M  .handoff/handoffs/index.md
    M  AGENTS.md
    M  CLAUDE.md

## Last ten checkpoints
```
  c028463 ckpt 464: 0.6.0-alpha1 RELEASED AND RECORDED: run 44220063374 green (check, test-jvm inc
  37209dd ckpt 462: 0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ON
  6e52915 ckpt 461: gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit
  2b21971 ckpt 446: R18a done: tap any earlier word -> strip offers words with close glide pattern
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
  64e1f2b ckpt 397: BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/t
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
