# CHECKPOINT 332 — read me first, then TASKS.md

**Written:** 2026-11-17T18:54:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `aa21a9d` (this checkpoint is the commit after it)

## Just done
OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in TASKS.md; daily-sim.yml and dailysim.py now default to net=full (fast wifi is the normal case; the hotspot imitation stays an optional second pass); judge page-load / video / typing at FULL speed first (the tablet's CPU / GPU / RAM are the limit then, not the link). Kestrel side: panel bar no longer claims Swoop is up while the person is still choosing it (MiniNav.swoopNotUp, tested), Kestrel lint (:app:lintRelease) and tooling self-test green, SW2 note for the other Browser session written in the Browser TASKS.md. Quick daily-sim run 44150728400 (typing, net full) still running; a send_later wake (trig_01Fxk8gu37CvhccEPFTVxvTJ, 19:01Z) will bring me back to read it.

## Do this next
when the wake fires (or sooner): read run 44150728400 (artifacts -> python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>; console.txt, report.md, lab-server.jsonl); fix what it shows; then dispatch daily-sim suite=all net=full (default) quick=false for real pages + films at full speed; optional hotspot pass last; then releases: Swoop 0.5.0-alpha4 (ime/build.gradle.kts code 9, bash ship.sh) and Kestrel alpha68 code 169 (Browser repo, absolute paths only, no android.yml run queued there first); delete the send_later trigger if it is no longer needed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
