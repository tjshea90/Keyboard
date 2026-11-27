# CHECKPOINT 1205 — read me first, then TASKS.md

**Written:** 2026-11-27T00:14:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/great-fermi-3coauw` · **builds on:** `cbab854` (this checkpoint is the commit after it)

## Just done
R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only by HTTP 429 from repo.maven.apache.org on the Robolectric jar nativeruntime-dist-compat-1.0.16, attempt 5 passed): :core:test 694 tests 0 failures (15 lab classes skipped by design), :ime:testDebugUnitTest 361 tests 0 failures, :ime:lintDebug 0 errors 57 warnings, debug APK 7.7 MB, :tools and :eval assemble OK; nothing blocked (environment network unrestricted); results written into research/R37-claude-setup-audit.md section 4; CLAUDE.md Skills section now points at that audit. FOUND: tools/resume.sh briefing was 12 KB on a fresh session branch (10 merged old branches) so the harness cut it to a 2 KB preview, the 'Do this next' part is silently lost; tools/test_handoff.sh only checks a small synthetic repo in text mode

## Do this next
extra fix for that (develop in the scratchpad first, then copy in): new tools/fit-brief.py (collapse old 'sync: MERGED' lines to the 3 newest, then drop RULES block, tasks 18->10, shorter INBOX/OUTBOX tails, shorter checkpoint list, hard cut; JSON length under 9800), call it from the JSON path of tools/resume.sh only (text mode unchanged, python missing = old behaviour), add 2 checks to tools/test_handoff.sh near line 209 (real 12 KB sample shape + untouched small input), run bash tools/test_handoff.sh (73 passed before). Then: tick R37-1..R37-5 in TASKS.md (bash tools/claim.sh R37 --done ...), final plain-words answer to the owner (branch claude/great-fermi-3coauw; plugin switch-off list = section 3 of research/R37-claude-setup-audit.md)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
  6345562 ckpt 1187: R37 (owner: tidy Claude Code setup + tj-android-kit) written into TASKS.md an
  bfc03fa ckpt 1181: R36 DONE: swoop-v0.7.0-alpha16 released and recorded (https://github.com/tjsh
  75950b6 ckpt 1178: lab: PracticeEval.trainingReport (format-2 file decoded WITH the recorded wor
  781f3b8 ckpt 1174: alpha16 (ime code 27, swoop-v0.7.0-alpha16) gated and pushed; android.yml DIS
  3b001d5 ckpt 1173: gated swoop-v0.7.0-alpha16 (ime code 27) and pushed it: handoff self-test, un
  047ef15 ckpt 1172: pre-ship: Glide training (Settings > Help): glide 8 everyday paragraphs and l
  369bf17 ckpt 1167: R36 lab result: TrainingLabTest (made-up hand, real script, hold-out of unask
  1ce6bc8 ckpt 1163: R36b+c: GlideTraining (core script: 8 paragraphs, 3 look-alike drills x4, own
  8d6e357 ckpt 1148: R36a: GlideTeaching (core, pure: thumb + shape + confusion lessons from a KNO
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
