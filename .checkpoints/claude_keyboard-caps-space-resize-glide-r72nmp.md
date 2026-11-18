# CHECKPOINT 469 — read me first, then TASKS.md

**Written:** 2026-11-18T03:14:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `4a97f92` (this checkpoint is the commit after it)

## Just done
wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)

## Do this next
start R19a: read InputController.startsSentence + KeyboardPresenter auto-space / caps flow, find the missed-capital paths; then R19b (backspace after auto-space), R19c (docked resize both axes in KeyboardRoot), R19d (GlideSession no-match fallback)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  3839c16 ckpt 466: handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings bl
  c028463 ckpt 464: 0.6.0-alpha1 RELEASED AND RECORDED: run 44220063374 green (check, test-jvm inc
  37209dd ckpt 462: 0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ON
  6e52915 ckpt 461: gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit
  2b21971 ckpt 446: R18a done: tap any earlier word -> strip offers words with close glide pattern
  f6c6db4 ckpt 432: R18b done: auto-capitals. InputController.startsSentence (empty TEXT box, newl
  0ff76f7 ckpt 415: R18c done: smooth resize (view-transform preview while dragging corner/arrow/p
  b17d0e8 ckpt 409: R18e done: hide-letters draws one solid blank slate (KeysView.blankSlate: no k
  2a3d366 ckpt 404: R18: wrote the owner's five 2026-11-18 requests (R18a-e) into TASKS.md in his 
  b7c6303 ckpt 401: FINAL STATE (2026-11-17 22:30Z): Swoop 0.5.0-alpha4 and Kestrel G alpha68 are 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
