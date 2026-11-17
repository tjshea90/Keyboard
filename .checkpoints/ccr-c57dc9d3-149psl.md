# CHECKPOINT 282 — read me first, then TASKS.md

**Written:** 2026-11-17T05:03:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `786e459` (this checkpoint is the commit after it)

## Just done
owner's reversal done: the 123 key and symbols page are back (space bar 4.9 units, hold . or , grid kept as a shortcut); N10 N12 N13 N14 + apostrophe restoration done; core 287, ime 128, lint ok; version already 0.3.0-alpha1 (code 4), README fixed; NOT shipped, NOT yet emulator-tested with the 123 layout

## Do this next
SHIP 0.3.0-alpha1: (1) bash ship.sh '0.3.0-alpha1: suggested-word tap works in Chrome/Brave address bars, wider space bar, no keyboard-switch key by default (hold space; Settings switch), hold full stop or comma for a symbols grid, 123 key stays, apostrophes restored (dont -> don't)' ; (2) mcp__github__actions_run_trigger run_workflow android.yml ref ccr-c57dc9d3-149psl inputs full_build=true + the same note; (3) optional first: dispatch emu-smoke.yml (api 29) - tools/emu/smoke.sh already expects the 123 bottom row [1.5,1,4.9,1,1.6]; (4) wait for tag swoop-v0.3.0-alpha1 + green, get_release_by_tag, bash tools/record-release.sh swoop-v0.3.0-alpha1 with the identical note; (5) tell the owner with the link. THEN ask the owner which research items to build (research/PREDICTION-SOURCES.md section B: 1 = harness with real swipes). Open owner questions: Q-HEARTBEAT, Q-LINKMETER.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M README.md
     M TASKS.md

## Last ten checkpoints
```
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
