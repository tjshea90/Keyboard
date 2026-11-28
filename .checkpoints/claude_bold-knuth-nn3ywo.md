# CHECKPOINT 1304 — read me first, then TASKS.md

**Written:** 2026-11-28T09:19:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/bold-knuth-nn3ywo` · **builds on:** `f69e67a` (this checkpoint is the commit after it)

## Just done
R40-1/R40-4 DONE in core: CorrectionGuard (two-tier weight rule: A allowance grows as the old word gets rarer 1.5/1.85/2.75/4.5 nats; B shipped-list pair strength gain >=5 nats, drop <=5.5) wired into GlideSession.revisePrevious, revised words now learned like kept ones (no trust), GlideTyping.pairLn, stats 'kept because the other reading was far rarer'; red-first CommonWordGuardTest (to it -> yo it reproduced; 3 tests red with the guard opened) + CorrectionGuardTest on the real list numbers; real-swipe replay (3011 judged words, RealSwipeEval REAL_POST guard sweep): unguarded 33 changed/22 mended/5 spoiled, rule A alone 25/15/5, A+B 33/22/5 = the guard costs nothing on real swipes. Full :core:test 716 tests 0 failed 15 skipped (14.5 min). R40-2 research written: research/R40-correction-policies.md sections 1-6 (Gboard paper read, HeliBoard Suggest.kt via helper, SwiftKey/iOS/FUTO/ASK public pages). Browser pin found: tjshea90/Browser newest tip claude/elegant-darwin-h7dlay (another session pre-shipping alpha116 TODAY) has KEYBOARD_PIN 4ed7b4c = Swoop 0.7.0-alpha7 (10 releases behind)

## Do this next
next: (1) ime tests + lint for the core changes (./gradlew :ime:testDebugUnitTest :ime:lintDebug, run in background, poll); (2) R40-5 audits in core tests: a tap-typing autocorrect audit lab over EvalCorpus (typos, neighbour slips, capitalised/all-caps/names, contractions: AOSP never auto-fixes capitals-only or mid-sentence capitalised words) and a glide common-for-rare severity metric; fix what it shows; fill research/R40-correction-policies.md section 6; (3) bump ime/build.gradle.kts to 0.7.0-alpha18 (code 29), bash ship.sh, dispatch android.yml, record-release; (4) R40-6 Browser: git -C /home/user/Browser merge origin/claude/elegant-darwin-h7dlay into my branch claude/bold-knuth-nn3ywo, bash /home/user/Browser/tools/sync-keyboard-core.sh /home/user/Keyboard (needs this repo committed+pushed), run Browser :core:test :app:testDebugUnitTest, automatic freshness design (see docs/KESTREL-LINK.md sec 4), push only claude/bold-knuth-nn3ywo in Browser, never release while alpha116 ships

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     D core/src/test/kotlin/app/kestrel/core/input/glide/ReviseProbeTest.kt

## Last ten checkpoints
```
  9d70391 ckpt 1285: R40-3 root cause found and fixed in core InputController: onSelectionUpdate n
  4ee8fc8 ckpt 1254: R38 + R39 + R39b DONE: swoop-v0.7.0-alpha17 released and recorded (https://gi
  d7a582d ckpt 1259: R37 final polish: CLAUDE.md Skills section tightened to 1.9 KB (all 23 skills
  8348d0d ckpt 1257: R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills presen
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
  55b0b1e ckpt 1248: gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, un
  ec2b546 ckpt 1247: R38-1..4 ticked; version bumped to code 28 / 0.7.0-alpha17; full suites green
  a540b09 ckpt 1245: R39 DONE: repo settings put Google's Maven Central copy before mavenCentral (
  e97ab52 ckpt 1236: R39 (owner mid-session: Maven 429 keeps happening, thought a Google mirror in
```

(18 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
