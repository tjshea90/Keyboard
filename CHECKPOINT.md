# CHECKPOINT 317 — read me first, then TASKS.md

**Written:** 2026-11-17T18:14:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `0dc0ad0` (this checkpoint is the commit after it)

## Just done
R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, make Swoop the keyboard, lab server + adb reverse, typing rounds in the web page fields of Kestrel via /f/<kind> pages: tap phrases + glide words scored from the page's own report), tools/sim/labserver.py (one page per field kind: text, search, textarea, contenteditable, url, email; /state /reset /log), tools/sim/swoopdrive.py, .github/workflows/daily-sim.yml (YAML colon fixed; tools/test_handoff.sh 69/69 green). Address-bar, pages and video scenes still to write (need the Explore agent's map of Kestrel). Also: debug-only keymap logging in KeysView/KeyboardRoot.

## Do this next
(1) check the workflow is dispatchable: mcp__github__actions_list list_workflows on tjshea90/Keyboard (a workflow file may need to be on the DEFAULT branch to be dispatched by itself: the Browser's emu-lab.yml header says so); if daily-sim.yml is not listed, find the repo default branch (git remote show origin) and decide with the owner-rule 'landing on main is the owner's call': alternative = trigger it through an existing dispatchable workflow or add it to the default branch ONLY if allowed; (2) read the Explore agent a3c124f5bdec8f1c2; (3) first run suite=typing net=full to prove the rig

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
