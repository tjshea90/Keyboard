# CHECKPOINT 7 — read me first, then TASKS.md

**Written:** 2026-11-16T23:35:00Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-c57dc9d3-149psl` · **builds on:** `ab58b19` (this checkpoint is the commit after it)

## Just done
KB-000 mostly done: save-ladder kit ported from Browser (read-only) and renamed Swoop; privacy guards added (secretscan blocks learning exports/replay/APK/screens, inbox refuses exports and >20KB); CLAUDE/BRIEF/HANDOFF/TASKS written; plan + kestrel-ref research copied; hooks installed in session root; self-test 61/63 green (only android.yml checks red: workflow not written yet)

## Do this next
KB-001: write .github/workflows/android.yml (tags+workflow_dispatch only, inputs full_build+note, contents: write, gh release create --target) so test_handoff goes fully green; then Gradle skeleton from /home/user/Browser configs (read with absolute paths, NEVER cd there): settings.gradle.kts (:core :ime :tools :eval), gradle/libs.versions.toml, wrapper, core/build.gradle.kts (-Xjdk-release=8), ime/build.gradle.kts (minSdk 29 target 36, app.swoop.keyboard, versionCode 1 / 0.1.0-alpha1). Then KB-002 port core/input + 122 tests.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
```
