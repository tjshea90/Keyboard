# CHECKPOINT 316 — read me first, then TASKS.md

**Written:** 2026-11-17T18:10:57Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `36d0e19` (this checkpoint is the commit after it)

## Just done
R17c1 started (new files only, nothing in the Browser repo touched): .github/workflows/daily-sim.yml (dispatch only: builds Swoop debug + Kestrel G x86_64 debug from a READ-ONLY checkout of a Browser branch, tablet-shaped Android 10 emulator 600x1024@170 2 GB low-RAM, hotspot imitated by -netspeed 5000:5000 -netdelay 60:120, then python3 tools/sim/dailysim.py) and tools/sim/swoopdrive.py (adb helpers, raw multi-touch glide injection, key positions read from the keyboard's own debug logcat line 'SwoopDiag: keymap page=.. k71=x,y ..' which KeysView.keymapLine() / KeyboardRoot now log in DEBUG builds only, type_text / glide_word). tools/sim/dailysim.py (the scenario runner: install both apps, choose Swoop, focus Kestrel's address box, type + glide, lab pages, real sites, video, report.md) is NOT written yet: it needs the Explore agent's map of Kestrel (how to focus the address bar, what logs page loads).

## Do this next
read the Explore agent a3c124f5bdec8f1c2 result (task notification); then write tools/sim/dailysim.py using swoopdrive.py; bash tools/test_handoff.sh must stay green (no push trigger); dispatch daily-sim.yml suite=typing net=full first (cheap proof the rig works), then net=hotspot; read artifacts with python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
