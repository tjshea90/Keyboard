# CHECKPOINT 315 — read me first, then TASKS.md

**Written:** 2026-11-17T18:08:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `fa01150` (this checkpoint is the commit after it)

## Just done
R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's address-and-search box marks itself with privateImeOptions app.kestrel.omnibox=1 (FieldInfos.hasOmniboxOption); it gets the URL keys (slash, .com, Go) but composing text, glide, guesses and learning like a search box, and NO automatic capitals / spaces / typo fixes / capital I / emoji / translate; private tabs keep using the standard IME_FLAG_NO_PERSONALIZED_LEARNING. Tests: FieldInfosTest, ImeLayoutTest, TypingOptionsTest (omnibox keeps an address an address), core+ime targeted suites green. Not released yet; the engine copy in Kestrel must be re-synced (core/input changed) when the Kestrel side lands.

## Do this next
wait for the Explore agent a3c124f5bdec8f1c2 (it maps Kestrel's text input: address bar, page fields, mini keyboard call sites, learned-data files, emulator tools); then write the design into docs/KESTREL-LINK.md and implement the Kestrel side on branch ccr-5531c47b-hl1gzo through git -C /home/user/Browser and absolute paths only (never cd there): address bar privateImeOptions app.kestrel.omnibox=1 + IME_FLAG_NO_PERSONALIZED_LEARNING in private tabs, ALWAYS the system keyboard (Swoop) when Swoop is installed, enable/choose guide, mini keyboard only as fallback; then re-sync the engine (bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard) and R17c1 (new workflow daily-sim.yml + tools/sim/ in THIS repo)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
