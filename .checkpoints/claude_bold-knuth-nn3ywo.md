# CHECKPOINT 1356 — read me first, then TASKS.md

**Written:** 2026-11-28T16:48:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `23c8d3f` (this checkpoint is the commit after it)

## Just done
R40 ALL DONE and ticked (R40-1..7, R40-6 a-e, parent). Swoop alpha18 RELEASED (https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha18); Kestrel G alpha117 RELEASED + recorded (https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha117; Browser branch ckpt 3620 pushed, KBR40d ticked). Real Gecko evidence: daily-sim 46160968809 (tap 4/4, glide 10/10), 46164945440 (14 of 14 chip picks leave ONE word); run 46162924215 was vacuous (a Backspace after a glide takes the whole word) and is recorded as such; limit: the same-cursor 'composing gone' report never happened in Gecko (lost=0), proven by controller fuzz + unit tests only. R40-6(b) hard CI drift gate deliberately NOT built (pin may run ahead of the last Swoop release) - say so to the owner

## Do this next
next: (1) NOTHING left on R40: send the plain-words final report to the owner (Swoop alpha18 + Kestrel G alpha117 links, what was fixed, research summary, what is NOT proven on the tablet/phone, R41 live shared memory needs his decision, how to move Swoop's learned words into Kestrel's panel today: Swoop Export what the keyboard learned, Kestrel Settings Import what the keyboard learned); (2) after his answer on R41 build it (docs/KESTREL-LINK.md section 6) or drop it; (3) next Swoop release (alpha19) only with the next engine/behaviour change: it will carry GlideLearning + the debug-only chip positions + lost= diag; then bash tools/sync-browser.sh (Keyboard) and a Browser release; (4) older owner tests still waiting (R32f R33f R34f R36f R38f installs on the tablet and phone)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  70532b1 ckpt 1349: R40-7 run 1 (46162924215) was VACUOUS: one Backspace right after a glide take
  40dddef ckpt 1340: R40-7 daily-sim DISPATCHED on this repo: run 46162924215 (run_number 17, suit
  0cd4ae8 ckpt 1339: R40-6d DONE: Browser alpha117 released and recorded (https://github.com/tjshe
  084d839 ckpt 1329: STATE: Swoop alpha18 RELEASED AND RECORDED (https://github.com/tjshea90/Keybo
  f4f0a89 ckpt 1324: alpha19 prep (NOT released): core GlideLearning.confirm + KeptGlide = the sha
  2ff9b78 ckpt 1318: alpha18 DISPATCHED: android.yml run 46159028756 (run_number 32) on commit 93b
  93bb9c7 ckpt 1317: gated swoop-v0.7.0-alpha18 (ime code 29) and pushed it: handoff self-test, un
  10c0ce1 ckpt 1316: worker restarted mid-ship: state after ckpt 1304 is all committed+pushed: ime
  73c3ffc ckpt 1304: R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance 
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
