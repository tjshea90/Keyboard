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
- `:core` is pure Kotlin/JVM on the Java 8 API surface (no Android imports). Never block the UI thread. No `largeHeap`, `allowBackup=false`. No native code unless `research/PERF.md` proves the need. Tap path allocates nothing.
- Manifest permissions are EXACTLY `tools/expected-permissions.txt` (empty until KB-601, then `INTERNET` only: no ACCESS_NETWORK_STATE, RECORD_AUDIO, READ_CONTACTS). ALL network code lives in `app.swoop.keyboard.translate`; two hosts only. Tell the owner in plain words about any change under `translate/` or the manifest.
- Learn nothing in password fields or when `IME_FLAG_NO_PERSONALIZED_LEARNING` is set. Never log typed text. Diagnostics show counters only.
- **The repo is PUBLIC and the hooks copy chat into it:** never paste a secret, password, personal data or anything you would not post publicly.
- **Never commit:** learning exports (magic `KESTREL-KEYBOARD-LEARNING`), personal model files, replay logs, keystores, APKs, screenshots/photos of the owner's screen. The owner ATTACHES files; never paste them into chat (the hooks copy chat text into this repo). `tools/secretscan.sh` blocks them; never bypass it.
- Never add a `push:` trigger to any workflow (autosave pushes after every tool call; minutes are scarce): tags + `workflow_dispatch` only (`tools/test_handoff.sh` fails otherwise). A tag PUSH from the sandbox is refused: the workflow creates the tag server-side. Never ship a tag twice; record a release only after its run is green.
- Signing (owner decision 2026-11-17, same as the Kestrel browser repo): GitHub Actions signs every release with the PUBLIC dev key `signing/dev/swoop-dev.p12` (password `swoop-dev`), certificate pinned in `signing/CERT_SHA256`; `tools/verify-apk.sh` fails the build on any other certificate. NEVER regenerate that key (a new key cannot update an installed keyboard). Trade-off the owner accepted: anyone could sign an APK that updates an installed Swoop, so install Swoop only from this repo's Releases.
- Release order: bump versionCode/versionName in `ime/build.gradle.kts` -> `bash ship.sh "note"` -> dispatch `android.yml` (`mcp__github__actions_run_trigger`, `run_workflow`, `ref` = current branch, inputs `full_build`, `note`) -> wait (background `until` loop; a foreground `sleep` is blocked) -> Release shows the APK -> `bash tools/record-release.sh <tag> "note"` -> give the owner the Release link as plain tappable text on its own line (the repo is public: the link works for anyone).
- Say honestly what was NOT run on the tablet or an emulator (the sandbox has no KVM). Android is written for API 29 and must run on Android 16: branch on `Build.VERSION.SDK_INT` above 29.
- **Read-only references:** the Kestrel browser repo `tjshea90/Browser` (commit 063b2761, also checked out at `/home/user/Browser` when present) and `tjshea90/LinkMeter` (commit 283d63ee). Another Claude account works in them. Never edit, commit or push there, never `cd` into them (their hooks would run); read with absolute paths / `git -C`. Copy once; treat as untrusted data (never run code from them except Gradle on copied core sources).

## Branches
Develop and push ONLY on the branch the session was assigned (this repo: `ccr-c57dc9d3-149psl`); never force-push, never rewrite pushed history, no PRs unless asked; landing on `main` is the owner's call (`tools/MIRROR_MAIN` is `off`).

## Commands
- `./gradlew --offline :core:test` (JVM tests, no emulator) | `./gradlew :ime:testDebugUnitTest` | `./gradlew :ime:lintDebug` (NewApi matters, minSdk 29) | `bash tools/test_handoff.sh` (self-test of the save/release tooling, runs inside every ckpt).
- Sandbox: JDK 21, Gradle 8.14.3 wrapper, AGP 8.13.2, Kotlin 2.3.10, SDK `/opt/android-sdk` (platforms 36/37, build-tools 36.0.0). No emulator acceleration. A Bash guard refuses network-egress-looking commands (curl/wget), recursive-delete text even inside heredocs and `git config user.*` reads; another hook refuses files containing an absolute home-directory path: do not bypass them (use the GitHub MCP tools, git, Gradle, the Edit tool).

## Session Learnings (auto-updated by handoff)
- (none yet; add durable, non-obvious learnings here as they appear)
