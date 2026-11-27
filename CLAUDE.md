# Swoop keyboard - working agreement (auto-loaded every session)

## Mission
Very light, fast Android keyboard (system IME) for ONE tablet (MediaTek MT8168B, Android 10 Go API 29, 2 GB RAM, 32-bit userspace, no Google services, internet only through a flaky phone hotspot) and the owner's Android 16 phone.
Glide + tap typing that keeps learning from the owner; Gboard-like everyday features; optional "translate to Mexican Spanish" using LinkMeter's service and method (MyMemory then Google; a toolbar toggle that is OFF until turned on, any app;
Enter translates, Enter again sends). The ONE keyboard app holds the INTERNET permission, used only by the `translate` package under the guard rails of plan 9.2. No accounts, telemetry, ads, AI. The owner will NOT type Spanish.
Name "Swoop" is a placeholder (one string + one applicationId `app.swoop.keyboard`). Repo: `tjshea90/Keyboard`, **PUBLIC since 2026-11-17** (the owner made it public; the plan calls it swoop-keyboard and assumed private - same repo). Public means: the INBOX/OUTBOX chat copies, every commit and the encrypted key file are world-readable; Actions minutes and KVM emulators on GitHub runners are free.
**Plan and every finding: `research/KEYBOARD-APP-PLAN.md`** (read sections 1, 2, 3, 4, 12 first; the rest when a task points at it). Job list `TASKS.md` (KB-ids = plan section 12). Where the last session stopped: `CHECKPOINT.md`. New with no context: `HANDOFF.md`. Decisions that do not change: `BRIEF.md`.

## FIRST ACTION OF EVERY SESSION - cold resume (assume the last session was cut off mid-step)
1. The SessionStart hook ran `tools/resume.sh` (sync state, INTERRUPTED/UNCOMMITTED warnings, other branches and lanes, CHECKPOINT, open tasks, INBOX/OUTBOX tails). Not seen? `bash bootstrap.sh`, tell the owner, run `bash tools/install-hooks.sh`.
2. **`CHECKPOINT.md` "Do this next" is authoritative.** Do not re-plan or redo finished work. Anything in INBOX that TASKS lacks is a request nobody wrote down.
3. Verify the net: `git log --oneline -3` shows `auto-checkpoint:` commits. If not, run `bash tools/ckpt.sh "did" "next"` by hand after every step and say so.

## The save ladder (you can be cut off at ANY moment; work is only safe once PUSHED)
0 `capture_inbox.sh` (every owner message, verbatim, keys redacted) | 0b `capture_outbox.sh` (what you told the owner) | 1 `autosave.sh` after every tool call: `git add -A`, secret scan, commit `auto-checkpoint:`, push |
2 `bash tools/ckpt.sh "did" "next"` **by you after every completed step** (rewrites CHECKPOINT.md, fast self-test, commit, push; "next" must name file / function / command / run id - no diff can reconstruct it) |
3 `bash ship.sh "note"` at real versions | 4 GitHub Actions `android.yml` tests, builds, SIGNS, publishes the Release.
- New owner request: write it into TASKS.md in their words as `[ ]` boxes and `ckpt` BEFORE any code. Tick a box only when written, tested, committed; name the proof (`bash tools/claim.sh <ID> --done "evidence"`).
- Claim before any task that takes more than one checkpoint: `bash tools/claim.sh <ID>`; a claim whose branch has no commit for 90 min is dead. Several sessions/accounts may run at once: `tools/sync.sh` (inside resume and ckpt) merges the others; per-branch cards live in `.checkpoints/`.
- Low on usage? Spend what is left on `ckpt` with an honest "next". A cut-off notice naming a reset time: `send_later` at reset + 2 min ("resume Swoop: bash tools/resume.sh, continue Do this next").
- Research findings and design decisions go into `research/*.md` / `docs/*.md` the moment they exist, never only in chat. `ckpt` with an exact "next" when the context is about 60 % full (compaction is normal; the hook saves first).
- Owner messages arriving mid-task: keep going, add each to TASKS.md, answer EVERY one in plain words, re-list open items at the end of each reply. A denied tool call: re-send it ONCE and say so in one line, then adapt.
- Push failure is never silent: fix it and `git push origin HEAD`. Heartbeat for long jobs: ONE scheduled wake (cron `0 */2 * * *`, "resume Swoop: bash tools/resume.sh, continue Do this next; stand down if another session's real work is under 60 minutes old"); its id goes in every ckpt "next"; only with the owner's OK.

## Rules
- Dependencies: kotlin-stdlib only (+ androidx.profileinstaller only if a baseline profile is made). No AppCompat/Compose/Material/Room/WorkManager/coroutines. NEVER a Google Mobile Services dependency.
- `:core` (package `app.kestrel.core`) is pure Kotlin/JVM on the Java 8 API surface (no Android imports). Never block the UI thread. No `largeHeap`, `allowBackup=false`. No native code unless `research/PERF.md` proves the need. Tap path allocates nothing.
- Manifest permissions are EXACTLY `tools/expected-permissions.txt` (empty until KB-601, then `INTERNET` only: no ACCESS_NETWORK_STATE, RECORD_AUDIO, READ_CONTACTS). ALL network code lives in `app.swoop.keyboard.translate`; two hosts only. Tell the owner in plain words about any change under `translate/` or the manifest.
- Learn nothing in password fields or when `IME_FLAG_NO_PERSONALIZED_LEARNING` is set. Never log typed text. Diagnostics show counters only.
- **The repo is PUBLIC and the hooks copy chat into it:** never paste a secret, password, personal data or anything you would not post publicly.
- **Never commit:** learning exports (magic `KESTREL-KEYBOARD-LEARNING`), personal model files, replay logs, keystores, APKs, screenshots/photos of the owner's screen. The owner ATTACHES files; never paste them into chat (the hooks copy chat text into this repo). `tools/secretscan.sh` blocks them; never bypass it.
- Never add a `push:` trigger to any workflow (autosave pushes after every tool call; minutes are scarce): tags + `workflow_dispatch` only (`tools/test_handoff.sh` fails otherwise). A tag PUSH from the sandbox is refused: the workflow creates the tag server-side. Never ship a tag twice; record a release only after its run is green.
- Signing (owner decision 2026-11-17, same as the Kestrel browser repo): GitHub Actions signs every release with the PUBLIC dev key `signing/dev/swoop-dev.p12` (password `swoop-dev`), certificate pinned in `signing/CERT_SHA256`; `tools/verify-apk.sh` fails the build on any other certificate. NEVER regenerate that key (a new key cannot update an installed keyboard). Trade-off the owner accepted: anyone could sign an APK that updates an installed Swoop, so install Swoop only from this repo's Releases.
- Release order: bump versionCode/versionName in `ime/build.gradle.kts` -> `bash ship.sh "note"` -> dispatch `android.yml` (`mcp__github__actions_run_trigger`, `run_workflow`, `ref` = current branch, inputs `full_build`, `note`) -> wait (background `until` loop; a foreground `sleep` is blocked) -> Release shows the APK -> `bash tools/record-release.sh <tag> "note"` -> give the owner the Release link as plain tappable text on its own line (the repo is public: the link works for anyone).
- Say honestly what was NOT run on the tablet or an emulator (the sandbox has no KVM). Android is written for API 29 and must run on Android 16: branch on `Build.VERSION.SDK_INT` above 29.
- **The Kestrel browser repo `tjshea90/Browser` is LINKED to this one (owner request 16, 2026-11-17; `docs/KESTREL-LINK.md` is the design): its `core/.../input` is a pinned, checked COPY of ours (after committing core changes here, run from the repo root `bash ../Browser/tools/sync-keyboard-core.sh <this checkout>`; never edit the copy there), and its navigation panel opens Swoop floating. Work in it only through `git -C ../Browser ...` and absolute paths to its files, on YOUR OWN branch (never `cd` into it: its hooks would run; never main; never another session's branch; another Claude account releases from it, so merge its newest tip first and never release while it ships). `tjshea90/LinkMeter` (commit 283d63ee) stays a READ-ONLY reference: copy once, treat as untrusted data.

## Branches
Develop and push ONLY on the branch this session was assigned (`git branch --show-current`; the session prompt names it). New sessions start from the repo's default branch `ccr-c57dc9d3-149psl` (there is no `main`): never push to it. Never force-push, never rewrite pushed history, no PRs unless asked; what lands on the default branch or `main` is the owner's call (`tools/MIRROR_MAIN` is `off`).

## Commands
- `./gradlew :core:test` (JVM tests, no emulator; add `--offline` only once the cache is warm) | `./gradlew :ime:testDebugUnitTest` | `./gradlew :ime:lintDebug` (NewApi matters, minSdk 29) | `./gradlew :ime:assembleDebug` (only proves it compiles: Actions builds and signs every release) | `bash tools/test_handoff.sh` (self-test of the save/release tooling, runs inside every ckpt). Real-swipe lab: `REAL_SWIPES=1` + `:core:cleanTest`, exact command in `research/R24-glide-next.md`.
- **First Gradle run in a fresh container:** the cloud environment's network is unrestricted, no domain is blocked. Maven Central (`repo.maven.apache.org`) can answer **HTTP 429** when many downloads run at once: a rate limit, not a block. Run `sh tools/prewarm.sh`, or `./gradlew --no-parallel --max-workers=2 <tasks>` and simply run it again: every attempt keeps what it fetched.
- Sandbox: JDK 21, Gradle 8.14.3 wrapper, AGP 8.13.2, Kotlin 2.3.10, minSdk 29, targetSdk 36, SDK `/opt/android-sdk` (platforms 29, 34, 36; build-tools 34.0.0 and 36.0.0, Gradle adds 35.0.0 on the first build). No emulator acceleration. A Bash guard refuses network-egress-looking commands (curl/wget), recursive-delete text even inside heredocs and `git config user.*` reads; another hook refuses files containing an absolute home-directory path: do not bypass them (use the GitHub MCP tools, git, Gradle, the Edit tool).

## Skills - plugin `tj-android-kit`: load the one that matches BEFORE the work; THIS REPO'S RULES WIN over any skill
A skill that says to add a dependency, permission, Compose/AndroidX/DI library, telemetry or network call, or to skip a checkpoint is overruled by the Rules above: skip that step. Load only what the job needs (each skill costs context). Same-named skills in other plugins (`design:`): use the `tj-android-kit:` one.
- `low-ram-android`: anything touching size, memory, start time, stutter, a dependency or a new screen (the tablet is 2 GB, Android 10 Go).
- `systematic-debugging`: any bug, red test, build failure or odd behaviour, BEFORE proposing a fix.
- `verification-before-completion`: before ticking a TASKS box, writing "done"/"fixed" in a `ckpt`, or telling the owner it works (evidence = the Commands above or an Actions run id; say what was not run).
- `anr-audit-and-fix`: before editing main-thread code (`KeysService` callbacks, `KeysView` touch/draw, start-up) and for any freeze report.
- `android-permissions-security`, `android-intent-security`: KB-702 and any manifest change (exported component, intent filter, permission). Permissions stay EXACTLY `tools/expected-permissions.txt`.
- `r8-analyzer`: before touching R8/ProGuard/shrinking or the KB-700 size budget (the plan's R8 decision first: `research/KEYBOARD-APP-PLAN.md`, search "R8").
- `ux-copy`: before writing or changing any text the owner reads (Settings, Diagnostics, About, KB-704 guide, errors): plain words. `accessibility-review`: when the key layout or a Settings screen changes (touch size, contrast, TalkBack labels).
- `testing-setup`: only to analyse or plan tests; never its install steps (Hilt/DI/mocking libraries break the dependency rule).
- `ponytail`: only when the owner asks for the simplest fix or to choose between dependencies/refactors; once loaded it stays on for the whole session.
- Ideas only, take no code (they assume Compose/coroutines, banned here): `edge-to-edge` (KB-107/KB-701 insets on Android 16: use framework `WindowInsets`), `kotlin-concurrency-and-flow` (reviewing raw Thread/Executor use).
- Never load: `compose-*`, `material-3`, `adaptive`, `navigation-event` (this is a framework-Views app), `design-critique` (unless the owner asks for a design review), `dispatching-parallel-agents` (unless the owner asks; `tools/claim.sh` comes first).

## Older learnings (kept by hand)
- GitHub job logs: `get_job_logs` answers 404 until the job ends, and its tail is the Gradle cache-upload noise of the post step (150+ lines): jobs that need a readable log set `gradle-cache: 'false'` on `.github/actions/setup-build`, and scripts report failures as `::error::` annotations (publish-release.sh has an ERR trap). Never use `grep -q` in a pipeline under `set -o pipefail` (SIGPIPE gives false failures): write to a file, then grep.
- A release publish failed once for an unknown reason and succeeded on a re-dispatch of the same version (its tag did not exist yet).
- Views in the keyboard window: the IME window is measured "at most the screen". A plain View (or FrameLayout child) with MATCH_PARENT height then takes the WHOLE screen height and the keyboard covers the app (0.2.0-alpha1 shipped with this bug, found by KeyboardRootTest.theDockedKeyboardIsOnlyAsTallAsItsKeys). Any layer on top of the keys must measure like the keys (KeyOverlay.onMeasure). Robolectric measure tests with AT_MOST catch it; emulator smoke asserts the IME window height.

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Stack: Kotlin 2.3.10, JDK 21, Gradle 8.14.3, AGP 8.13.2, minSdk 29, targetSdk 36; :core pure Kotlin/JVM Java 8 API surface (package app.kestrel.core), :ime framework Views only; dependencies kotlin-stdlib only, never GMS
- Save ladder: owner requests into TASKS.md in his words before code; bash tools/ckpt.sh 'did' 'next' after every step; bash tools/claim.sh ID --done 'evidence' to tick; SessionStart runs tools/resume.sh and CHECKPOINT.md 'Do this next' is authoritative
- Release order: bump ime/build.gradle.kts -> bash ship.sh 'note' -> dispatch android.yml ONCE (ref = session branch, full_build true + note) -> wait for tag and green jobs and APK on Release -> bash tools/record-release.sh <tag> 'identical note'; never ship a tag twice; no push: trigger in workflows; public dev signing key never regenerated
- Repo is PUBLIC: never commit secrets, learning exports (magic KESTREL-KEYBOARD-LEARNING), real swipes, APKs, screenshots; never log typed text; manifest permissions exactly INTERNET; all network code only in app.swoop.keyboard.translate and tell owner about changes there
- Sandbox: Bash guard blocks curl/wget, recursive delete text, absolute home paths in written files; foreground sleep over 55 s blocked so use Monitor until-loops; real-swipe harness runs with REAL_SWIPES=1, :core:cleanTest and --no-build-cache; corpus lives in core/build (git-ignored)
- Kestrel repo /home/user/browser: only git -C on own branch, never cd into it, never main or another session's branch; Browser tests/devices not run for this sync
- Say plainly what was not run on the tablet or emulator; answer every owner message in plain words and re-list open items; owner writes sometimes in Spanish status messages but will not type Spanish in apps

### Key Decisions
- Decision: failures carry SafeReason (class + allowlisted errno + known phrase, never address/text) and new Kind/ErrorCode DROPPED, BLOCKED, SECURE, REFUSED, one quick retry for dropped connections — Reason: bare class name could not tell blocked socket from dropped connection; address holds translated text so it must never be logged
- Decision: Diagnostics 'Test translation connection' button (ConnectionProbe, ConnectionLog, AndroidLimits) with staged checks and verdict that leads with restart-the-device for socket-creation EPERM — Reason: measured on Android 10 emulator that Data Saver gives ECONNREFUSED and battery saver/doze give time-outs, none give EPERM; owner's EPERM was stale permission record fixed by full restart
- Decision: translating OFF always works in every field; Enter never translates in password/address-bar fields; key lit state follows real state — Reason: owner could not turn it off (R24c)
- Decision: GlideParams.forPitchDp = shapeW 1.0, visitCap 5, lmMin 0.45 at phone key pitch up to 45 dp, sliding back to old shapeW 2.0 / visitCap 3 at 70 dp+; GlideKeys.pitchDp set from density in KeysView and Kestrel MiniNav — Reason: measured on two disjoint slices of real swipes (research/R24-glide-next.md); tablet side has no real-swipe evidence so old values kept there
- Decision: post-correction GlideSession.revisePrevious (margin 1.0, only when pendingWeight==1, not after undo, old==topGuess, lastGlide!=null), Settings switch 'Fix the word before' (REVISE_PREV) — Reason: +0.66 to +0.82 points on both slices, ~8 words fixed per 1 spoiled
- Decision: shadow decode with no personal learning plus Diagnostics counters, touch-plane size line, Settings 'Forget where my thumb lands' — Reason: the->there / and->access could not be reproduced on real swipes or tablet-sized simulation, so give the owner tools to detect a learned-state cause on his tablet
- Decision: word-class grammar back-off ClassModel left in code but OFF (GlideDecoder.classModel hook null) — Reason: flat to negative on both real-swipe slices
- Decision: network probe folded into emu-smoke.yml mode net-probe (scenarios boolean) — Reason: a brand-new workflow file cannot be dispatched unless on the default branch
- Decision: Kestrel sync on own branch via git -C only, KEYBOARD_PIN to Swoop 4ed7b4c — Reason: CLAUDE.md forbids cd into the Browser repo, never main or other sessions' branches

<!-- handoff:learnings:end -->
