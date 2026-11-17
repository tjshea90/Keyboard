# CHECKPOINT 324 — read me first, then TASKS.md

**Written:** 2026-11-17T18:42:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `a9b768e` (this checkpoint is the commit after it)

## Just done
daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 44147785336 showed: adb missing from PATH (fixed in the workflow); the key map logged once can be STALE because the keyboard window moves after its first layout and the log ring buffer drops old lines -> Swoop (debug) now re-logs its key map every 500 ms while attached, the driver waits for a settled map, caches the last one, enlarges the log buffer (-G 16M); letters read as capitals while Shift is on -> Keymap.char finds either case; field focus retries; human-speed taps in one shell call (inject_taps); sloppy-thumb glide pass; quick mode). Proven so far on the Android 10 tablet-shaped emulator with the real Gecko engine: Swoop shows for a web page field, tap-typing 'hello world' arrives exactly in the page (insertCompositionText per letter, insertText on space). Kestrel branch merged with the other session's alpha67 (code 168) cleanly: my release must be alpha68 / code 169. New quick run queued (typing, net full).

## Do this next
read the new daily-sim quick run report (python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>; console.txt + report.md + lab-server.jsonl); fix what it shows; then run suite=all net=hotspot; release plan: Swoop 0.5.0-alpha4 (ime/build.gradle.kts code 9), Kestrel alpha68 (code 169: bump geckoapp/build.gradle.kts, bash /home/user/Browser/tools/sync-ext-version.sh, bash /home/user/Browser/ship.sh gecko note; check no other android.yml run is queued in the Browser repo first)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
