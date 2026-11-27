# CHECKPOINT 1257 — read me first, then TASKS.md

**Written:** 2026-11-27T05:03:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/great-fermi-3coauw` · **builds on:** `19d0544` (this checkpoint is the commit after it)

## Just done
R37 DONE (all 5 steps, evidence in TASKS.md): 23 tj-android-kit skills present; build+tests green in a cold container (core 694 tests, ime 361, lint 0 errors 57 warnings, APK 7.7 MB, no domain blocked, only 429 rate limits that R39 then fixed); CLAUDE.md tidied (-4.3 KB duplicate learnings block, stale facts fixed, Skills section: repo rules win); extra: tools/fit-brief.py + resume.sh guard so the SessionStart briefing stays under the 10,000-character hook cap (it was 12 KB and silently cut), self-test 78 passed 0 failed on the merged tree; plugin audit in research/R37-claude-setup-audit.md; branch claude/great-fermi-3coauw

## Do this next
nothing running for R37: the plain-words answer to the owner is the last thing to send. Waiting on the owner (his call, nothing done): switch off the plugins in section 3 of research/R37-claude-setup-audit.md on claude.ai; this account's environment setup script is still the OLD one (paste block for the Maven mirror is in research/R39-maven-429.md 'Other accounts'; add build-tools;35.0.0 to its SDK package list); the default branch ccr-c57dc9d3-149psl has no tools/prewarm.sh so the setup script only runs gradlew help. Another session (claude/peaceful-turing-tyvm1e) owns R38/alpha17: do not touch its release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d672e9d ckpt 1251: R39b answered: other accounts need one paste each (block in research/R39-mave
  7e40362 ckpt 1249: alpha17 build dispatched: android.yml run 45942092495 (run_number 31) on clau
  55b0b1e ckpt 1248: gated swoop-v0.7.0-alpha17 (ime code 28) and pushed it: handoff self-test, un
  ec2b546 ckpt 1247: R38-1..4 ticked; version bumped to code 28 / 0.7.0-alpha17; full suites green
  a540b09 ckpt 1245: R39 DONE: repo settings put Google's Maven Central copy before mavenCentral (
  e97ab52 ckpt 1236: R39 (owner mid-session: Maven 429 keeps happening, thought a Google mirror in
  b04be3e ckpt 1231: R38 code written, not yet verified: KeysView (erase key bigger via widenErase
  b1d3f23 ckpt 1216: R38 (owner 2026-11-27: training file alpha16, glide edges/one-letter, bigger 
  e66717b ckpt 1205: R37-2 DONE with evidence: cold container, 5 Gradle attempts (1-4 stopped only
  f526160 ckpt 1202: R37-3 done in CLAUDE.md: removed the Session Learnings block that was byte-id
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
