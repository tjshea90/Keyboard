# CHECKPOINT 312 — read me first, then TASKS.md

**Written:** 2026-11-17T17:49:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `5414890` (this checkpoint is the commit after it)

## Just done
SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Release swoop-v0.5.0-alpha3 has swoop-0.5.0-alpha3.apk, BUILDLOG line written, TASKS P6 and Q-SMARTSPACE ticked). Contains: Dark default; day-mode polish (docked panel, root and squeezed strips painted in the keyboard colour; settings screens' framework theme and title bar); Android 10 inline-report fix in core InputController (Kestrel engine copy re-synced to Keyboard 4644951, pin matches, Kestrel core tests green; no new Kestrel APK). Emulator proof: api 29 day mode + squeeze (runs 44138416431, 44139418243) and api 36 (44138541543) 0 failures. Nothing is proven on the real tablet or phone.

## Do this next
NOTHING IS RUNNING. Wait for the owner: install swoop-0.5.0-alpha3.apk (https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.5.0-alpha3) on the tablet and the phone and try the 16 items; open owner decisions: (1) a private signing key if a LIVE learned-data link between Swoop and Kestrel is wanted (today: Swoop as the chosen keyboard = one memory, plus Export/Import), (2) Key height Extra large on the tablet for the photo's taller keys. A new session starts with: bash tools/resume.sh, read TASKS open items (KB-6xx translate boxes are stale: the code exists, the live-service test is the owner's), KB-701 Android 16 full pass, KB-705 sign-off.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  f3a528d ckpt 309: Third day-mode visual fix: the strips beside a SQUEEZED docked keyboard now al
  b2a7229 ckpt 308: Fixed the two day-mode visual bugs of the Dark default: (1) KeyboardRoot.panel
  ef0242f ckpt 307: android.yml run 44137544253 (swoop-v0.5.0-alpha3) was CANCELLED ON PURPOSE bef
  1bac5df ckpt 306: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE: android.yml run 44137544253 
  d1e884f ckpt 305: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
  4644951 ckpt 304: Q-SMARTSPACE ROOT CAUSE FOUND with the debug trace of Android 10 run 441313118
  dd3f89d ckpt 303: Night-mode emulator run 44128638712 (API 36, alpha2 head) is GREEN: screenshot
  9a65bb7 ckpt 302: BOTH RELEASES OUT AND RECORDED: Swoop 0.5.0-alpha2 (the Android 16 fix; API 36
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
