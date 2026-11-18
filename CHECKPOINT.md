# CHECKPOINT 511 — read me first, then TASKS.md

**Written:** 2026-11-18T03:50:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `c5f9d32` (this checkpoint is the commit after it)

## Just done
swoop-v0.6.0-alpha2 RELEASED AND RECORDED: run 44232237543 green (check, test-jvm incl lint, build+verify+publish); Release has swoop-0.6.0-alpha2.apk (SHA-256 5b440dee...). R19a-d done. NOT run on tablet/phone/emulator

## Do this next
Nothing running. Owner installs https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.6.0-alpha2 on tablet+phone and tests: glide after a full stop (capital), deleting the space after a full stop then typing com, docked arrow drag up/down/sideways + Full size, gibberish glide types a word. Then fix what they report (next release 0.6.0-alpha3 code 12 via bash ship.sh). Optional: emu-smoke.yml (api 29+36) for the docked height drag window; Kestrel pinned-copy sync when a Browser checkout exists

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cffd8a8 ckpt 506: DISPATCHED android.yml ONCE for swoop-v0.6.0-alpha2 (code 11) on ref claude/ke
  ae7bda6 ckpt 505: gated swoop-v0.6.0-alpha2 (ime code 11) and pushed it: handoff self-test, unit
  01a8630 ckpt 504: pre-ship: 0.6.0-alpha2: a glide always types the closest word (never 'no word 
  2ddf8c6 ckpt 490: R19c done+tested (docked two-axis resize via bar arrows, window-tall during dr
  60344cb ckpt 480: R19a/b/d core done+tested: always-answer relaxed decode pass (GlideDecoder.alw
  02723ef ckpt 469: wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)
  3839c16 ckpt 466: handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings bl
  c028463 ckpt 464: 0.6.0-alpha1 RELEASED AND RECORDED: run 44220063374 green (check, test-jvm inc
  37209dd ckpt 462: 0.6.0-alpha1 shipped to GitHub: ship.sh gates green, android.yml DISPATCHED ON
  6e52915 ckpt 461: gated swoop-v0.6.0-alpha1 (ime code 10) and pushed it: handoff self-test, unit
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
