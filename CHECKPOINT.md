# CHECKPOINT 224 — read me first, then TASKS.md

**Written:** 2026-11-17T02:37:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `38c19d6` (this checkpoint is the commit after it)

## Just done
FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) made the keyboard window as tall as the screen when measured AT_MOST; KeyOverlay now measures like the keys; regression test KeyboardRootTest.theDockedKeyboardIsOnlyAsTallAsItsKeys. Floating keyboard N3 wired (GripBar, KeyboardRoot floating panel, corner resize, service window+insets, settings switch), KeyboardRootTest 17 green; core 276, ime 119, lint ok

## Do this next
(1) extend tools/emu/smoke.sh: assert the InputMethod window height is about the keyboard's (not full screen), long-press the neon bar (adb input swipe x y x y 700), dump window frame (full screen) + screenshot to artifact, tap-through check; dispatch emu-smoke.yml on this branch (api 29); (2) if green: version 0.2.1-alpha1 code 3, ship.sh, dispatch android.yml, record, tell the owner the 0.2.0 build had the tall-window bug (do not install it) and give the 0.2.1 link; (3) tick N3 with proof.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
