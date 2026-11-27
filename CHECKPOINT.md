# CHECKPOINT 1202 — read me first, then TASKS.md

**Written:** 2026-11-27T00:09:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/great-fermi-3coauw` · **builds on:** `d5166ef` (this checkpoint is the commit after it)

## Just done
R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-identical to AGENTS.md (-4.3 KB, left a 1-line pointer between the same markers), fixed stale text (SDK platforms 29/34/36 + build-tools, assigned-branch line: ccr-c57dc9d3-149psl is the repo's DEFAULT branch, 'encrypted key file' gone, 'empty until KB-601' gone, absolute /home paths of the Browser repo replaced by ../Browser, hook scripts named with tools/ prefix), added Commands lines (assembleDebug, HTTP 429 = rate limit not a block, REAL_SWIPES pointer) and the Skills section for tj-android-kit (repo rules win); tools/test_handoff.sh 73 passed 0 failed; research/R37-claude-setup-audit.md written (plugin audit: 74 plugins on disk, 8 register hooks incl. security-guidance and stackhawk-hawkscan). R37-2 measured: :core:test 694 tests 0 failures 15 lab tests skipped by design, :ime:assembleDebug OK (debug APK 7.85 MB); :ime:testDebugUnitTest and lint were blocked ONLY by HTTP 429 from repo.maven.apache.org on nativeruntime-dist-compat-1.0.16.jar (not a blocked domain: environment network is unrestricted)

## Do this next
wait for the retry script (scratchpad build_retry.sh, summary in full-summary.log, up to 8 attempts 40 s apart; if lost rerun ./gradlew --continue --no-parallel --max-workers=2 :ime:testDebugUnitTest :ime:lintDebug) and note the result in research/R37-claude-setup-audit.md section 4; then tick R37-1..R37-5 with bash tools/claim.sh R37 --done evidence and give the owner the final plain-words answer (branch claude/great-fermi-3coauw, list of plugins to switch off from section 3 of the research file)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
  feb6999 ckpt 1135: R36 (owner attached his glide practice file and asked for a much longer glide
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
