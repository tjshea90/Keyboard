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
- **The Kestrel browser repo `tjshea90/Browser` is LINKED to this one (owner request 16, 2026-11-17; `docs/KESTREL-LINK.md` is the design): its `core/.../input` is a pinned, checked COPY of ours (`bash /home/user/Browser/tools/sync-keyboard-core.sh <this checkout>` after committing core changes here; never edit the copy there), and its navigation panel opens Swoop floating. Work in it only through `git -C /home/user/Browser ...` / absolute paths on YOUR OWN branch (never `cd` into it: its hooks would run; never main; never another session's branch; another Claude account releases from it, so merge its newest tip first and never release while it ships). `tjshea90/LinkMeter` (commit 283d63ee) stays a READ-ONLY reference: copy once, treat as untrusted data.

## Branches
Develop and push ONLY on the branch the session was assigned (this repo: `ccr-c57dc9d3-149psl`); never force-push, never rewrite pushed history, no PRs unless asked; landing on `main` is the owner's call (`tools/MIRROR_MAIN` is `off`).

## Commands
- `./gradlew --offline :core:test` (JVM tests, no emulator) | `./gradlew :ime:testDebugUnitTest` | `./gradlew :ime:lintDebug` (NewApi matters, minSdk 29) | `bash tools/test_handoff.sh` (self-test of the save/release tooling, runs inside every ckpt).
- Sandbox: JDK 21, Gradle 8.14.3 wrapper, AGP 8.13.2, Kotlin 2.3.10, SDK `/opt/android-sdk` (platforms 36/37, build-tools 36.0.0). No emulator acceleration. A Bash guard refuses network-egress-looking commands (curl/wget), recursive-delete text even inside heredocs and `git config user.*` reads; another hook refuses files containing an absolute home-directory path: do not bypass them (use the GitHub MCP tools, git, Gradle, the Edit tool).

## Older learnings (kept by hand; the handoff tool replaces only the marked block below)
- GitHub job logs: `get_job_logs` answers 404 until the job ends, and its tail is the Gradle cache-upload noise of the post step (150+ lines): jobs that need a readable log set `gradle-cache: 'false'` on `.github/actions/setup-build`, and scripts report failures as `::error::` annotations (publish-release.sh has an ERR trap). Never use `grep -q` in a pipeline under `set -o pipefail` (SIGPIPE gives false failures): write to a file, then grep.
- A release publish failed once for an unknown reason and succeeded on a re-dispatch of the same version (its tag did not exist yet).
- Views in the keyboard window: the IME window is measured "at most the screen". A plain View (or FrameLayout child) with MATCH_PARENT height then takes the WHOLE screen height and the keyboard covers the app (0.2.0-alpha1 shipped with this bug, found by KeyboardRootTest.theDockedKeyboardIsOnlyAsTallAsItsKeys). Any layer on top of the keys must measure like the keys (KeyOverlay.onMeasure). Robolectric measure tests with AT_MOST catch it; emulator smoke asserts the IME window height.

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Stack: Kotlin 2.3.10, JDK 21, Gradle 8.14.3, AGP 8.13.2, minSdk 29 targetSdk 36; :core pure Kotlin/JVM (no Android imports), :ime framework Views only, no AppCompat/Compose/GMS.
- Repo public: never commit secrets, learning exports (magic KESTREL-KEYBOARD-LEARNING), APKs, screenshots; never log typed text; hooks block curl/wget and absolute home paths in written files.
- Save ladder: autosave hook + bash tools/ckpt.sh 'did' 'next' after every step with specific next; owner requests into TASKS.md before code; tools/claim.sh <ID> --done 'evidence' to tick.
- Release order: bump ime/build.gradle.kts versionCode/versionName -> bash ship.sh 'note' -> dispatch android.yml ONCE (ref = session branch, inputs full_build true + note) -> wait for tag swoop-v<ver> + all jobs green + get_release_by_tag APK -> bash tools/record-release.sh <tag> with identical note. Never ship a tag twice; never add push: trigger to workflows; signing = public dev key signing/dev, never regenerate.
- Develop only on claude/keyboard-typing-improvements-jetik4; no PRs unless asked; never cd into /home/user/Browser, use git -C and own branch only.
- Export/learned-data format additions k/b/f lines are backward compatible (older readers skip unknown kinds); documented in docs/KEYBOARD-EXPORT.md.
- Foreground sleep blocked >55s: use background until-loops; offline gradle flags: ./gradlew --offline :core:test :ime:testDebugUnitTest.
- Say honestly what was not run on device/emulator; answer every owner message plainly and re-list open items.

### Key Decisions
- Decision: R18a engine = decode word's ideal glide path via GlideDecoder (bias zeroed) + Suggester typo fixes (new fixesOnly flag), re-ranked by Fingerprint shape distance (SHAPE_W 4, fix shape capped 1.0 key) + context lnP + next-word bigram (UNKNOWN_NEXT -12, NEXT_W 0.3) + rank-scaled FIX_BONUS 2.4 — Reason: owner asked for close glide patterns; swaps (teh/adn) are far as glide shapes but commonest finger slip; yardstick 37/40 first, 38/40 top3.
- Decision: presenter edit mode only when cursor inside a word, or userMoved (foreign selection report / space-slide) at a word edge; typing at word end or starting a word before an old one stays normal completion flow; off in password/URL/omnibox — Reason: avoid hijacking normal typing.
- Decision: replacement via InputController.replaceWordAround verifies editor text first, no trailing space, cursor ends after word; TypingLearner.replaced unlearns old word + records confusion only when edit-distance similar — Reason: a changed mind is not a typo.
- Decision: InputController.startsSentence is the single sentence-start rule (empty TEXT box, newline, . ! ? ellipsis through closing quotes/brackets, whitespace required, exceptions e.g./i.e./Mr./Dr./initials/dotted letters; 'no.' not excepted) — Reason: owner wants auto-caps; empty box now capitalises (reverses earlier 2026-11-17 decision, deliberately).
- Decision: CaseBook = built-in ~800 names/days/months/countries/languages/brands, ambiguous words excluded, may/march only after date words; PersonalModel.addCase learns mid-sentence capitals (needs 2 uses, 1.5x dominance), Backspace on case-only fix records lowercase weight 2; 'k' export lines — Reason: wrong mid-sentence capital worse than missing one.
- Decision: presenter must apply caps settings to controller BEFORE controller.startInput — Reason: first Shift state was computed with defaults; bug found via new start-of-box rule.
- Decision: R18c resize = GPU view-transform preview (KeyboardRoot.setPreview, panelRect returns visual rect, shadow off), single real layout on finger lift; KeysView.mulFor exact inverse of heightFor — Reason: per-touch layout/measure on weak tablet is the plausible jitter source; real cause not reproducible in Robolectric (row height clamps 44-56dp).
- Decision: Full size pill in GripBar middle (docked squeezed or floating) -> gripRestore = full-width docked; double-tap still works — Reason: owner asked for easy snap-back.
- Decision: R18e blankSlate draws nothing on any page containing letters, including Shift/Backspace/Enter and lit key; symbols/number pages unaffected — Reason: owner said one solid blank slate; interpretation, easy to relax.
- Decision: R18d adds PersonalModel starts ('b' lines), fix made/undone ('f' line) driving Suggester AUTO_MARGIN shift -0.3..+1.5, and no-match glide arms retype watch so a typed word teaches the failed glide shape — Reason: measurable (starters 0.00 -> 0.87 first-3 hit rate), uses existing machinery.
- Decision: test Rig defaults sentenceCaps=false (core) and AUTOCAPS=false (presenter) — Reason: 35+29 old tests type into empty boxes expecting lowercase; AutoCapsTest and new tests opt in.
- Decision: shipped alpha without Browser sync — Reason: /home/user/Browser not present; Kestrel types through Swoop IME so it gets features anyway.

<!-- handoff:learnings:end -->
