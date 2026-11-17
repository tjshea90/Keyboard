# CHECKPOINT 205 — read me first, then TASKS.md

**Written:** 2026-11-17T02:26:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `cb00f41` (this checkpoint is the commit after it)

## Just done
release swoop-v0.2.0-alpha1 (code 2) gated, pushed and android.yml dispatched on this branch (full_build=true); N2 N7 N8 N9 ticked

## Do this next
WAIT for the run: background loop git ls-remote --tags origin swoop-v0.2.0-alpha1, then mcp__github__actions_list list_workflow_runs (android.yml) to see it green; get_release_by_tag swoop-v0.2.0-alpha1 shows the APK; then bash tools/record-release.sh swoop-v0.2.0-alpha1 with the IDENTICAL note from ship.sh output (see BUILDLOG/ship note); give the owner the Release link on its own line. Meanwhile start N3 floating keyboard design in docs/FLOATING.md (full-height transparent IME window, onComputeInsets TOUCHABLE_INSETS_REGION, content insets so apps do not resize, long-press neon bar to detach, drag, resize handles, remember position/size).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cb00f41 ckpt 204: gated swoop-v0.2.0-alpha1 (ime code 2) and pushed it: handoff self-test, unit 
  be5d9d8 ckpt 203: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  c1f8d3a ckpt 202: pre-ship: 0.2.0-alpha1: number row, instant glide start and faster trail, quic
  16bc192 ckpt 184: N7 core done: InputController autoSpace (lazy space after , . ! ? ; : before t
  6108878 ckpt 177: N4 tap suggestions and N6 typing-learning wired into the keyboard: strip works
  2104156 ckpt 165: N4/N6 core done: Suggester (completions, typo fixes, next words, autocorrect d
  e2aee4a ckpt 152: N1 number row always on top (thin row, letters+symbols pages), N5 glide starts
  558b1e8 ckpt 143: owner's 9 requests (N1-N9) written into TASKS.md in his words; he likes the ke
  eddf918 ckpt 139: FIRST RELEASE swoop-v0.1.0-alpha1 published by GitHub Actions (run 43993744160
  604ad35 ckpt 135: release run 43992969251: build+verify (pinned dev-key certificate) PASSED on G
```
