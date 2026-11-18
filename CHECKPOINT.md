# CHECKPOINT 610 — read me first, then TASKS.md

**Written:** 2026-11-18T05:41:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `8557a6e` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha2 RELEASED AND RECORDED: run 44244608905 green (check, test-jvm incl lint, build+verify+publish); Release has swoop-0.7.0-alpha2.apk (SHA-256 b7f6840f...). R19a-d, R20a-d, R21 all done. NOT run on tablet/phone/emulator

## Do this next
Nothing running. Owner installs https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.7.0-alpha2 on tablet+phone and tests: copied text as first box, [guess][next word][guess] after a glide, capital I / i'm (taps, glides, no-suggestion boxes), quotes with glides, docked resize both ways, glide never says no word fits, capitals after a full stop. Then fix what they report (next release 0.7.0-alpha3 code 14 via bash ship.sh). Optional: emu-smoke.yml (api 29+36); Kestrel pinned-copy sync when a Browser checkout exists; lower-ranked research items in research/R20-glide-personalization.md (next-word post-correction, tap-evidence for the touch plane, shadow A/B decoding)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f0361bc ckpt 604: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/ke
  74fd792 ckpt 603: gated swoop-v0.7.0-alpha2 (ime code 13) and pushed it: handoff self-test, unit
  ed6b0a7 ckpt 601: 0.7.0-alpha1 RELEASED AND RECORDED (run 44242169394 green; Release swoop-v0.7.
  4b94676 ckpt 593: wrote owner's quotation-marks request R21 into TASKS.md. 0.7.0-alpha1 build ru
  b6f75e4 ckpt 588: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha1 (code 12) on ref claude/ke
  db7f5d7 ckpt 587: gated swoop-v0.7.0-alpha1 (ime code 12) and pushed it: handoff self-test, unit
  9ff3807 ckpt 586: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  1acd5e8 ckpt 581: pre-ship: 0.7.0-alpha1: what you copied is the first box of the suggestion str
  495b39a ckpt 579: R20d rank 1 done+tested (TouchField plane, persistence, export, docs). All fou
  c1cdf29 ckpt 546: R20a+R20b done+tested (clipboard chip with masking/dismissal/TTL 5 min, glide 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
