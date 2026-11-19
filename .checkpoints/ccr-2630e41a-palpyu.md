# CHECKPOINT 792 — read me first, then TASKS.md

**Written:** 2026-11-19T00:24:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `13ad8d6` (this checkpoint is the commit after it)

## Just done
R25 family CONFIRMED FIXED by the owner (2026-11-19): restarting the tablet made translation work (stale Android network permission record after the update that added INTERNET); R25/R25b/c/d/e ticked; docs/TRANSLATE.md + CLAUDE.md learnings written

## Do this next
nothing is running. Latest release swoop-v0.7.0-alpha6 (code 17). Open, in priority order: (1) Kestrel browser sync of core/input (/home/user/Browser absent in this container; owner's/other session's call; docs/KESTREL-LINK.md); (2) owner feedback on glide on the TABLET after the R24b re-tune (GlideParams.forPitchDp; if worse set SMALL_PITCH_DP = LARGE_PITCH_DP); (3) optional research: next-word post-correction and a dwell/speed cue, both measurable with the real-swipe harness (research/R24-glide-next.md section 5); (4) the owner's tests of alpha4 R24c (translating off in password/address boxes). Fix what the owner reports next

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M CLAUDE.md
     M OUTBOX.md
     M docs/TRANSLATE.md

## Last ten checkpoints
```
  b8af559 ckpt 784: swoop-v0.7.0-alpha6 RELEASED AND RECORDED (run 44426423457 green; Release swoo
  7d2c909 ckpt 780: swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on 
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
  21947b4 ckpt 772: R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPE
  c4b6a87 ckpt 768: R25c: owner's exact Test translation connection output received (group OK, bat
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
  7c4dd02 ckpt 757: pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refu
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
