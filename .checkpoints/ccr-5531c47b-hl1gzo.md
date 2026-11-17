# CHECKPOINT 328 — read me first, then TASKS.md

**Written:** 2026-11-17T18:48:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `0dd62e0` (this checkpoint is the commit after it)

## Just done
Keyboard test suites green again after today's work (3 policy / ordering fixes: KeysServiceTest exported list now includes the explicit-intent ReceiveLearnedActivity, TranslateGuardTest allows only the android.net.Uri type (a file address, not network code), ReceiveLearnedActivityTest resets the process-wide Engine host (Engine.resetForTest) and waits for the engine's own save); README documents the always-Swoop design. Quick daily-sim run 44150728400 (typing, net full, the fixed driver) still running.

## Do this next
read run 44150728400's console.txt / report.md / lab-server.jsonl (python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>); fix what it shows; then suite=all net=hotspot; then release: Swoop 0.5.0-alpha4 (ime/build.gradle.kts code 9) via ship.sh + android.yml; Kestrel alpha68 code 169 in the Browser repo (absolute paths only; first check no android.yml run is queued/running there)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
