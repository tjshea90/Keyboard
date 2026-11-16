# HANDOFF - the single entry point for a session with NO context

You are probably a fresh Claude session. The owner builds Android apps from a phone/tablet and a chat, runs out of usage often, and wants any new session to continue exactly where the last one stopped. Everything below is on disk and pushed; nothing is in anybody's head.

## 1. Where things are (read in this order, 10 minutes)
1. `tools/resume.sh` already ran at session start (or `bash bootstrap.sh`). It printed `CHECKPOINT.md` ("Do this next" is authoritative), the open tasks, other sessions' lanes, and the INBOX/OUTBOX tails.
2. `CLAUDE.md` - the working agreement (auto-loaded): the save ladder, rules, commands.
3. `TASKS.md` - the job list. KB-ids are the steps of plan section 12; a `[~]` line is claimed by a running session.
4. `BRIEF.md` - decisions that do not change (device, five decisions, owner answers, privacy).
5. `research/KEYBOARD-APP-PLAN.md` - the full plan. Read sections 1, 2, 3, 4, 12 first; section 6 (architecture), 8 (learning engine), 9 (translation), 10 (Gboard features), 13 (tests), 14 (risks) when a task points at them. Evidence tags: `[R]` read in a repo, `[W]` web snippet, `[K]` general knowledge (verify before relying), `[I]` inference, `[T]` only the tablet can settle.
6. `docs/` (HARDWARE, DICTIONARY, KEYBOARD-EXPORT, and later ARCHITECTURE and IF-THE-KEYBOARD-BREAKS) and `research/kestrel-ref/` (the browser's glide research: measured accuracy tables, ideas, decisions; read GLIDE_TYPING.md sections 2, 8, 9, 11 before phase 4).

## 2. How to work (short)
- After every completed step: `bash tools/ckpt.sh "what I did" "what comes next (file, function, command)"`. Hooks also auto-save after every tool call, but only YOU know the intent. Work counts only once pushed.
- A new owner request goes into `TASKS.md` in their words first. Tick a box only with proof (`bash tools/claim.sh <ID> --done "test or run id"`).
- Claim long tasks (`bash tools/claim.sh <ID>`) so a parallel session does not duplicate them.
- Say honestly what was NOT run on the real tablet. Answer every owner message in plain words and re-list the open items.

## 3. Repository map
```
core/    pure Kotlin/JVM (package app.kestrel.core.* at first, renamed once in one mechanical commit after the inherited tests are green). Glide decoder, lexicon, personal model, input model. No Android imports.
ime/     the Android app (module :ime, package app.swoop.keyboard): InputMethodService, KeysView, setup/diagnostics/settings, clipboard, emoji; later the `translate` package (the ONLY network code).
tools/   shell tooling (the save ladder, release scripts) AND, from KB-001, the Gradle JVM CLI that compiles word lists (task :tools:glideLexicon). Shell tooling lives in tools/*.sh.
eval/    JVM lab: GestureSim, GlideEval, replay runner, parameter sweeps (from KB-002).
docs/ research/ TASKS.md CHECKPOINT.md BUILDLOG.md INBOX.md OUTBOX.md .checkpoints/ .claude/ .github/workflows/
```
(Modules appear as their KB task is done; `TASKS.md` and `CHECKPOINT.md` say which exist.)

## 4. Build, test, ship
- `./gradlew --offline :core:test` (JVM tests: the main safety net) | `./gradlew :ime:testDebugUnitTest` | `./gradlew :ime:lintDebug` | `bash tools/test_handoff.sh` (self-test of the save/release tooling, also run by every `ckpt`).
- Release (GitHub builds and signs; local builds are tests only): bump `versionCode`/`versionName` in `ime/build.gradle.kts` -> `bash ship.sh "note"` -> dispatch `.github/workflows/android.yml` via `mcp__github__actions_run_trigger` (`run_workflow`, `ref` = the session branch, inputs `full_build=true`, `note`) -> wait in a background loop -> the Release shows the APK -> `bash tools/record-release.sh swoop-v<version> "same note"` -> give the owner the Release link as plain tappable text on its own line. Never ship a tag twice. A tag PUSH from the sandbox is refused (the workflow creates it).
- **No release APK exists until KB-003** (private signing key). Until then `signing/CERT_SHA256` is `UNSET` and `verify-apk.sh` refuses every release APK; CI may build debug/unsigned artifacts for tests only.

## 5. Things that bite
- **The Browser repo is READ-ONLY.** In this sandbox it is checked out next to this repo. Never `cd` into it (its own hooks, if loaded, would commit and push there); use `git -C /home/user/Browser ...` or absolute paths. Copy files, never link them. Same for LinkMeter.
- The sandbox shell blocks network-looking commands (curl/wget), recursive-delete text, and writing absolute home-directory paths into files. Use GitHub MCP tools, git, Gradle, the Edit tool.
- Hooks: in the cloud sandbox with several repos under one session root the hooks are merged into the ROOT settings by `bash tools/install-hooks.sh` (it never touches other projects' hooks); with this repo as the only project the committed `.claude/settings.json` is read directly.
- The repo is PRIVATE but treat it as if it could leak: no exports, replay logs, keystores, APKs or screenshots of the owner's screen, ever.
- `INBOX.md`/`OUTBOX.md` contain the chat history of the project; they merge as unions.

## 6. Status
See `CHECKPOINT.md` (latest card) and `TASKS.md` (done boxes with proof). `BUILDLOG.md` lists published releases (none yet).
