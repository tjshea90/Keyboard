# CHECKPOINT 1048 — read me first, then TASKS.md

**Written:** 2026-11-24T20:41:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-euler-w64u7s` · **builds on:** `441d4b0` (this checkpoint is the commit after it)

## Just done
alpha12 (ime code 23, swoop-v0.7.0-alpha12) gated and pushed; android.yml DISPATCHED ONCE on claude/bold-euler-w64u7s at 20:41Z; R33 ticked; full suites core 653 + ime 312 green, lint green

## Do this next
find the run id (mcp__github__actions_list list_workflow_runs android.yml); NEVER dispatch again for this tag; when green and the Release shows the APK: bash tools/record-release.sh swoop-v0.7.0-alpha12 with the SAME note as ship.sh; then give the owner the Release link and the R33f check; if red read get_job_logs, fix, bump to code 24

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  441d4b0 ckpt 1047: gated swoop-v0.7.0-alpha12 (ime code 23) and pushed it: handoff self-test, un
  f9936c8 ckpt 1046: pre-ship: R33: a word on the strip that is spelled like the glided word and f
  a20ef6e ckpt 1036: R33 code: look-alike-fix rule (GlideSession.looksLikeFix + SpellingNear + eng
  00d6ec0 ckpt 1022: R33 written into TASKS.md (owner: ave still wins over and, tapping and adds i
  aed8fb7 ckpt 1019: R32 DONE: swoop-v0.7.0-alpha11 released (https://github.com/tjshea90/Keyboard
  ac950c8 ckpt 1015: alpha11 (ime code 22, swoop-v0.7.0-alpha11) gated and pushed; android.yml DIS
  10618d9 ckpt 1014: gated swoop-v0.7.0-alpha11 (ime code 22) and pushed it: handoff self-test, un
  9a86224 ckpt 1013: pre-ship: R32: punctuation swap (space before , . ! ? taken back, Backspace r
  ab28919 ckpt 1000: R32e CommonGuard (gap 5 nats, margin 3, measured on real swipes: +8 all / +11
  5fd5f44 ckpt 992: R32b digit long-press row (ImeLayout.digitHolds, KeysView.openDigitPopup, 7 vi
```
