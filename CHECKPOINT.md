# CHECKPOINT 314 — read me first, then TASKS.md

**Written:** 2026-11-17T18:04:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `2251818` (this checkpoint is the commit after it)

## Just done
OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R17b one linked keyboard: Kestrel always types through Swoop, Swoop learns everything, Kestrel extras via privateImeOptions; R17c daily-use simulation on an Android 10 KVM emulator with both apps + improve both; R17d do not disturb the active Browser session ccr-e89b9dfe-pl3lk5 (it merges my checkpoints, last commit minutes ago)). No code changed yet.

## Do this next
R17b1: find out how Kestrel types today - read-only: Browser repo files app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt (in-app mini keyboard + navSwoop), geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt (navSwoop, address bar, where the system keyboard is suppressed: grep showSoftInputOnFocus / SOFT_INPUT_STATE / PageFieldInput), SwoopLink.kt; delegate the broad read to an Explore agent (conclusions only) to save context; then design R17b2/R17b3 into docs/KESTREL-LINK.md BEFORE code. In parallel read Browser .github/workflows/android.yml concurrency (lock per app) so no dispatch disturbs the other session, and plan R17c1 as a NEW workflow in this repo.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
