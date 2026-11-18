# CHECKPOINT 663 — read me first, then TASKS.md

**Written:** 2026-11-18T07:03:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/keyboard-caps-space-resize-glide-r72nmp` · **builds on:** `c62eb54` (this checkpoint is the commit after it)

## Just done
R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resize-glide-r72nmp (commit e0543fb pushed; held Backspace + MiniKeyboardModel.deleteWord resync NOT yet committed in Browser, full Browser suites running); Swoop 0.7.0-alpha3 build run 44254953577 in_progress (dispatched once)

## Do this next
1) read Browser test log scratchpad/browser4.log (EXIT line) then git -C /home/user/browser add -A, commit, push -u origin claude/keyboard-caps-space-resize-glide-r72nmp; 2) when run 44254953577 is success: mcp__github__get_release_by_tag swoop-v0.7.0-alpha3 shows APK, then bash tools/record-release.sh swoop-v0.7.0-alpha3 with the identical note (from CHECKPOINT/ship output); 3) tick R22 in TASKS.md via tools/claim.sh R22 --done; 4) tell owner honestly: nothing run on tablet/phone/emulator

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
  06f1fac ckpt 657: gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit
  9995f5d ckpt 656: pre-ship: 0.7.0-alpha3: hold Backspace to delete faster then whole words, clip
  74e5d4d ckpt 649: R23c gesture-length term evaluated (research/R23c-gesture-length.md, GlideLeng
  0db3f9a ckpt 639: R23a-e core+views done: held Backspace speeds up then deletes words (KeysView+
  649cee3 ckpt 623: wrote owner's third batch R23a-e into TASKS.md (backspace hold accelerates to 
  4d9bb2b ckpt 615: R22 plan written: docs/KESTREL-PORT-R22.md. Browser repo cloned shallow at /ho
  ca9737d ckpt 613: wrote owner's R22 'put all the updates into kestrel keyboard' into TASKS.md. s
  9844fd5 ckpt 610: swoop-v0.7.0-alpha2 RELEASED AND RECORDED: run 44244608905 green (check, test-
  f0361bc ckpt 604: DISPATCHED android.yml ONCE for swoop-v0.7.0-alpha2 (code 13) on ref claude/ke
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
