# CHECKPOINT 253 — read me first, then TASKS.md

**Written:** 2026-11-17T04:47:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `f0abec1` (this checkpoint is the commit after it)

## Just done
owner's new requests (Brave strip tap does nothing, remove 123 and globe keys, wider space bar, research online predictive engines) written into TASKS.md as N10-N14 in his words

## Do this next
N10 first: reproduce with FakeEditor (inline autocomplete = selection after the glide + rewritten text): core InputController.swapGlide must work with a selection that starts at the glide end and onSelectionUpdate must keep lastGlide when before still ends with the glide tail; add controller.replaceRecent(old, word) verified fallback and use it in KeyboardPresenter.onCandidate when session.pick fails; then N11-N13 (ImeLayout bottom row, symbols grid on hold of '.', GLOBE setting, hold space = picker, KeysView), N14 research doc via a research helper; update tools/emu/smoke.sh bottom row geometry; release 0.3.0-alpha1.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
  0c99401 ckpt 236: first VALID emulator result (run 44006606996, Android 10 x86_64): Swoop chosen
  ee5d958 ckpt 232: emu smoke fixed: waits until Android lists Swoop, retries ime set, stops when 
  242c789 ckpt 228: emulator smoke extended: key geometry for the number row + neon bar, assertion
  79037e0 ckpt 224: FOUND AND FIXED a bug in 0.2.0-alpha1: the overlay layer (match_parent View) m
  61c5916 ckpt 212: RELEASE swoop-v0.2.0-alpha1 published (run 44002995655 green, certificate veri
  52a512f ckpt 205: release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
