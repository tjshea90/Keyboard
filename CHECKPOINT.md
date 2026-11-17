# CHECKPOINT 318 — read me first, then TASKS.md

**Written:** 2026-11-17T18:19:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `d5b2b2e` (this checkpoint is the commit after it)

## Just done
R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_INPUT_MAP.md (key result: every Kestrel field and every page field already uses the SYSTEM keyboard, only the panel bar has the mini keyboard; one decision point MiniNav.openKeyboard -> navSwoop). Swoop side done: FieldKind.OMNIBOX, debug key map, KeyboardFlow.importUri + ReceiveLearnedActivity (explicit-intent hand-over of what another app learned; confirm dialog; tests green), daily-sim run 44145772330 (suite typing, net full) is running on the CURRENT Browser branch = the baseline.

## Do this next
(1) read the daily-sim run 44145772330 artifact (report.md) when done: baseline of Swoop in Kestrel page fields; (2) implement the Kestrel side in /home/user/Browser on branch ccr-5531c47b-hl1gzo (git -C + absolute paths ONLY, never cd): SwoopLink.isInstalled + MF <queries> (release + debug ids), navSwoop gate (installed -> Swoop, not chosen -> showInputMethodPicker), privateImeOptions on BottomDock.urlField + HomeView box, KestrelGeckoView.onCreateInputConnection private flag, Settings row retitle + hand-over row (FileProvider + explicit intent), tests; run Browser :app:testDebugUnitTest :geckoapp:testDebugUnitTest before any deliberate checkpoint; (3) re-sync the engine (bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard); (4) docs/KESTREL-LINK.md rewrite

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    ?? research/KESTREL_INPUT_MAP.md

## Last ten checkpoints
```
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
