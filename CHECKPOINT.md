# CHECKPOINT 319 — read me first, then TASKS.md

**Written:** 2026-11-17T18:24:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `9f6a9c5` (this checkpoint is the commit after it)

## Just done
R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkpointed there; Browser app + geckoapp unit suites green incl. new SwoopInstalledTest): SwoopLink.isInstalled / offerChoice / OMNIBOX_OPTION, manifest <queries> for Swoop (release + debug), navSwoop gate = installed (not chosen -> keyboard picker / settings, not installed -> mini keyboard fallback), address box + Home box privateImeOptions app.kestrel.omnibox=1, KestrelGeckoView.onCreateInputConnection adds IME_FLAG_NO_PERSONALIZED_LEARNING in private tabs, Settings row 'Always use the Swoop keyboard (recommended)'. Keyboard repo: docs/KESTREL-LINK.md rewritten (the whole interface between the apps, why no live channel), dailysim.py gets address-box + private-tab scenes, Swoop debug 'startInput kind= noLearn=' line. daily-sim run 44146747558 (baseline = Kestrel BEFORE these edits, suite typing, net full) is running.

## Do this next
(1) read run 44146747558 report.md (artifact daily-sim-typing, fetch with python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>); (2) dispatch daily-sim again (suite typing, net full) so it includes the Kestrel edits + address/private scenes; (3) fix what the report shows (Swoop in Gecko fields); (4) R17b4 hand-over (Kestrel side: custom framework ContentProvider for one cache file + explicit intent to app.swoop.keyboard[.debug]/.ReceiveLearnedActivity + a Settings row; Swoop side is DONE); (5) re-sync the engine into Kestrel (core/input changed: FieldKind.OMNIBOX): bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard then Browser :core:test; (6) ONE deliberate Browser checkpoint only when everything is tested (the other session merges deliberate checkpoints of my branch)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
