# CHECKPOINT 528 — read me first, then TASKS.md

**Written:** 2026-11-18T04:11:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `ea373fc` (this checkpoint is the commit after it)

## Just done
R20b built: after a glide the strip is [other guess][NEXT word][other guess] (KeyboardPresenter.showCandidates/renderGlideStrip/pickNextAfterGlide, KeysView.showCandidates(accent), blank boxes), 4 presenter tests; NOT yet ticked (waiting for research/R20-clipboard-and-strip.md to confirm layout vs Gboard/SwiftKey)

## Do this next
R20a: read research/R20-clipboard-and-strip.md when the agent finishes (ls research/R20-*), then add clipboard chip (ClipboardHost last-copy + freshness, KeysView clip icon + Listener.onClipChip, presenter leftmost chip at start of word, Settings 'Suggest copied text'); then R20d from research/R20-glide-personalization.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bbb8240 ckpt 524: R20c done+tested (pronoun I everywhere: InputController.capitalizeI/fixLoneI/w
  c72bbee ckpt 514: wrote owner's second 2026-11-18 batch R20a-d into TASKS.md (his words); 0.6.0-
  b4f6a50 ckpt 511: swoop-v0.6.0-alpha2 RELEASED AND RECORDED: run 44232237543 green (check, test-
  cffd8a8 ckpt 506: DISPATCHED android.yml ONCE for swoop-v0.6.0-alpha2 (code 11) on ref claude/ke
  ae7bda6 ckpt 505: gated swoop-v0.6.0-alpha2 (ime code 11) and pushed it: handoff self-test, unit
  01a8630 ckpt 504: pre-ship: 0.6.0-alpha2: a glide always types the closest word (never 'no word 
  2ddf8c6 ckpt 490: R19c done+tested (docked two-axis resize via bar arrows, window-tall during dr
  60344cb ckpt 480: R19a/b/d core done+tested: always-answer relaxed decode pass (GlideDecoder.alw
  02723ef ckpt 469: wrote owner's four 2026-11-18 requests R19a-d into TASKS.md (his words)
  3839c16 ckpt 466: handoff saved (.handoff/handoff.md + archive; CLAUDE.md/AGENTS.md learnings bl
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
