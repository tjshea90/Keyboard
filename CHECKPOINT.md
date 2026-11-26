# CHECKPOINT 1063 — read me first, then TASKS.md

**Written:** 2026-11-26T20:10:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/awesome-bell-gmcf0i` · **builds on:** `cdeae1b` (this checkpoint is the commit after it)

## Just done
R34e DONE in code+tests: translation switches itself off on a new input session, another box (app/field/hint/kind) or the keyboard going away (onFinishInputView/onWindowHidden); same-box restart keeps it; strip says why once (R.string.translate_auto_off)

## Do this next
Next: R34d in KeysView.kt (pressUp flick check, ACTION_POINTER_DOWN while armed: resting thumb >=300ms dropped, short overlap waits; promote/handOver/pendingTap) + 6 KeysViewTest tests; then R34b/c email (EmailLearner finishField at startInput restart, learn in any box, FieldInfos hint detection, EmailBook popular domains, EmailsActivity); then R34a

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1bc8768 ckpt 1057: R34 written into TASKS.md in the owner's words (5 items: glide engine, e-mail
  4c2c91a ckpt 1054: R33 DONE: swoop-v0.7.0-alpha12 released (https://github.com/tjshea90/Keyboard
  3a86baf ckpt 1048: alpha12 (ime code 23, swoop-v0.7.0-alpha12) gated and pushed; android.yml DIS
  441d4b0 ckpt 1047: gated swoop-v0.7.0-alpha12 (ime code 23) and pushed it: handoff self-test, un
  f9936c8 ckpt 1046: pre-ship: R33: a word on the strip that is spelled like the glided word and f
  a20ef6e ckpt 1036: R33 code: look-alike-fix rule (GlideSession.looksLikeFix + SpellingNear + eng
  00d6ec0 ckpt 1022: R33 written into TASKS.md (owner: ave still wins over and, tapping and adds i
  aed8fb7 ckpt 1019: R32 DONE: swoop-v0.7.0-alpha11 released (https://github.com/tjshea90/Keyboard
  ac950c8 ckpt 1015: alpha11 (ime code 22, swoop-v0.7.0-alpha11) gated and pushed; android.yml DIS
  10618d9 ckpt 1014: gated swoop-v0.7.0-alpha11 (ime code 22) and pushed it: handoff self-test, un
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
