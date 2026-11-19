# CHECKPOINT 840 — read me first, then TASKS.md

**Written:** 2026-11-19T01:24:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `8d33f7d` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha7 (code 18) DISPATCHED ONCE: android.yml run 44436640730 on sha 8d33f7d (2026-11-19T01:24Z). Do NOT dispatch again. R26a/R27a/R27b ticked; R26b (Kestrel) in progress on Browser branch ccr-2630e41a-palpyu

## Do this next
1) wait for run 44436640730 green (~12 min), confirm Release swoop-v0.7.0-alpha7 has the APK (get_release_by_tag), bash tools/record-release.sh swoop-v0.7.0-alpha7 with the IDENTICAL note (see CHECKPOINT ckpt 839); 2) R26b: bash /home/user/browser/tools/sync-keyboard-core.sh /home/user/Keyboard (pin = release commit), then git -C /home/user/browser add -A + commit + git -C /home/user/browser -c push.negotiate=true push -u origin ccr-2630e41a-palpyu (own branch only), run /home/user/browser/gradlew -p /home/user/browser :core:test in background (log /tmp/claude-0/sweeps/BROWSER-CORE.log), write the R26 note in the Browser docs/KESTREL-PORT-R22.md, claim.sh R26b --done; 3) tell the owner: alpha7 link, what shipped, what did not (dwell, grammar classes), Kestrel branch name, the Diagnostics lines to paste back

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8d33f7d ckpt 839: gated swoop-v0.7.0-alpha7 (ime code 18) and pushed it: handoff self-test, unit
  bc6e202 ckpt 838: pre-ship: 0.7.0-alpha7: the next glided word can fix the word before it when t
  ff86187 ckpt 802: R26a measured on slice A: post-correction +0.82 first-choice at margin 1 (43 m
  0a3f557 ckpt 800: wrote owner's R27a (short words read as long words: the->there, and->access) a
  1440ffd ckpt 796: wrote owner's R26a (optional research: next-word post-correction + dwell/speed
  2b63d15 ckpt 792: R25 family CONFIRMED FIXED by the owner (2026-11-19): restarting the tablet ma
  b8af559 ckpt 784: swoop-v0.7.0-alpha6 RELEASED AND RECORDED (run 44426423457 green; Release swoo
  7d2c909 ckpt 780: swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on 
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
```
