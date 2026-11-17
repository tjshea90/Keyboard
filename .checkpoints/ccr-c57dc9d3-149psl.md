# CHECKPOINT 212 — read me first, then TASKS.md

**Written:** 2026-11-17T02:31:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `ad8ecf6` (this checkpoint is the commit after it)

## Just done
RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate verified, APK 1.39 MB) and recorded in BUILDLOG; owner told with the link. N3 floating keyboard in progress: docs/FLOATING.md, core FloatingMath (+tests), ime GripBar + KeyboardRoot (floating panel, corner resize, touchRegion), Settings float prefs, KeysView.floatMul/heightFor - compiles, NOT wired into the service yet

## Do this next
N3 wiring: (1) KeysService.onCreateInputView uses root.host (modeChanged -> window.setLayout MATCH_PARENT x MATCH_PARENT when floating else WRAP_CONTENT, root.minimumHeight) and calls root.applySettings(Settings, safeMode) in onStartInputView; (2) KeysService.onComputeInsets: when floating contentTopInsets=visibleTopInsets=window height, touchableInsets=TOUCHABLE_INSETS_REGION, touchableRegion=root.touchRegion(); (3) host.geometryChanged -> presenter.prepareGlide(); (4) Settings screen: 'Floating keyboard' switch (Prefs.FLOAT_ON) in Layout and look + 'Dock the keyboard' note; Reset docks; (5) tests: KeyboardRootTest (long press detaches, drag moves/clamps, corners resize, double tap + drop-at-bottom dock, touchRegion, insets values via KeysServiceTest), update KeysServiceTest for new root; (6) emulator smoke: float via long press and check the IME window is full screen and touches pass through; then release 0.3.0-alpha1 (versionCode 3).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
