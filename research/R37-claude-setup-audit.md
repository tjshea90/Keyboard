# R37 - Claude Code setup audit (2026-11-26/27, session branch claude/great-fermi-3coauw)

Owner request R37 (TASKS.md): tidy the Claude Code setup for Android work, check the new `tj-android-kit` plugin, build and test, list what to switch off on claude.ai. This file is the durable copy of the findings; the plain-words answer to the owner is in OUTBOX.md. Nothing was installed, no account or environment setting was changed.

## 1. tj-android-kit
23 skills, all in the session's skill list (the plugin folder is synced from the account). Which skill before which work: `CLAUDE.md` section "Skills". Followed literally, three would break the Rules: `testing-setup` installs Hilt/DI/mocking libraries, `edge-to-edge` requires Compose, `kotlin-concurrency-and-flow` assumes coroutines. `compose-*` (5), `material-3`, `adaptive`, `navigation-event` target Compose/Material; this app is framework Views, so they are "never load" or "ideas only". The `design:` plugin has same-named copies of `accessibility-review`, `design-critique` and `ux-copy`: use the `tj-android-kit:` ones.

## 2. Build and environment facts
- `ANDROID_HOME=/opt/android-sdk`; platforms android-29, android-34, android-36; build-tools 34.0.0 and 36.0.0. AGP 8.13.2 also wants build-tools 35.0.0: Gradle downloads it from Google on the first build (about a minute, every fresh container). `CLAUDE.md` and plan line 152 said "platforms 36/37": stale, CLAUDE.md fixed (plan left alone).
- The cloud environment ("Default") has network access `unrestricted`: no domain is blocked, nothing to add. Hosts the build config names, should the network ever be limited: `repo.maven.apache.org` (Maven Central), `dl.google.com` (Google Maven + SDK parts), `plugins.gradle.org`, `services.gradle.org` (wrapper).
- Cold container: Maven Central answered **HTTP 429** (rate limit) for single jars (`kotlin-stdlib`, then `kotlin-build-tools-impl`) while Gradle downloaded in parallel. Not a block. Each retry keeps what it fetched; `--no-parallel --max-workers=2` plus re-running got through (`:core:compileKotlin` green on the third run). Now in CLAUDE.md "Commands".
- Why the warm-up does not help yet: the environment's setup script runs a repo's `tools/prewarm.sh` if the checkout has one, but containers start from the default branch `ccr-c57dc9d3-149psl` (there is no `main`), which has no `tools/prewarm.sh` (it lives on session branches that `tools/sync.sh` merges in later, after the script ran). The script falls back to `gradlew help`. Options for the owner: (a) put `tools/prewarm.sh` on the default branch; (b) add `build-tools;35.0.0` to the setup script's package list. Not done: both change the default branch or the cloud environment.

## 3. What is loaded in a session (from the synced plugin folder: 74 plugins)
682 skills (names alone are about 23 KB of every session's start), about 100 connector entries (about 50 need a sign-in, 15 fail to start), 11 plugin agents with long descriptions, and these plugin HOOKS that run code in every session whatever the project:
- `security-guidance`: on every prompt, every edit and Bash call and at Stop (its AI diff review appears to be off here: it left a "no credentials" notice file; the scripts still run and keep a growing log).
- `stackhawk-hawkscan`: SessionStart text orders a web scan after every code change without asking; needs Docker and a key; this repo has no web app.
- `carta-investors` (7 hooks, 3 at SessionStart), `pixeltable` and `cockroachdb` (Python after every edit), `signoz` (Node at start and on WebFetch), `honeycomb`, `datadog` (only for their own tools).

Switch off on claude.ai (73 plugins; keep only `tj-android-kit`), grouped by why:
- A. Hooks above: security-guidance, stackhawk-hawkscan, carta-investors, pixeltable, cockroachdb, signoz, honeycomb, datadog.
- B. Telemetry, code review services or web search/scrape (the repo rule: no accounts, telemetry, nothing leaves the repo): stackhawk-api, product-tracking-skills (an agent that proposes analytics events after code changes), growthbook, langfuse, grafana-assistant, grafana-cloud-mcp, qodo, tavily, exa, tinyfish, brightdata-plugin, nimble.
- C. Duplicates or rivals of this repo's own system: design, engineering, productivity (task and memory skills vs TASKS.md / CHECKPOINT.md), product-management, desktop-commander, gitkraken, browser-use, prisma.
- D. Other platforms and vendors: dart-flutter (its connector fails every session), unity, qt-development-skills, figma, canva, adobe-for-creativity, miro, pdf-viewer, wix, sanity-plugin, base44, appwrite, valtown, fastly-agent-toolkit, buildkite, cloudinary, clickhouse, planetscale, qdrant-skills, datarobot-agent-skills, servicenow-sdk, auth0, zapier, twilio-developer-kit, zoom-plugin, synthflow, vanta-mcp-plugin, airwallex-agentos, finance, data, bigdata-com, daloopa, lseg, bio-research, carbone-skill, vibe-prospecting, grasp, b12-claude-plugin, ai-firstify, cowork-plugin-management, postiz, modern-web-guidance, frontend-design, consistent-ui, mobile-design-system.
- Account skills (18, claude.ai Skills page): docs, google-workspace, computer-use, chrome-browser, built-in-browser, deep-research, import-memory, morning, learn, doc-coauthoring, docx, pdf, pptx, xlsx, theme-factory, web-artifacts-builder, mcp-builder, skill-creator (keep `skill-creator` only to edit tj-android-kit).
- Connectors (claude.ai): Sentry and Supabase, both not connected and not loaded; Sentry is crash telemetry.
- Keep: tj-android-kit; the GitHub tools; Claude Code's own skills (code-review, simplify, security-review, loop, fewer-permission-prompts, init, run).

## 4. Build and tests
Measured in this cold container with `./gradlew --continue --no-parallel --max-workers=2 :core:test :ime:assembleDebug :ime:testDebugUnitTest :ime:lintDebug`, then `:tools:assemble :eval:assemble`:
- `:core:test`: 82 classes, 694 tests, 0 failures, 15 lab classes skipped by design (they need REAL_SWIPES-style flags).
- `:ime:testDebugUnitTest`: 23 classes, 361 tests, 0 failures, 1 skipped.
- `:ime:lintDebug`: 0 errors, 57 warnings (UnusedResources 22, ObsoleteSdkInt 12, PluralsCandidate 7, ViewConstructor 4, ClickableViewAccessibility 3, RtlHardcoded 2, rest 7).
- `:ime:assembleDebug`: debug APK 7.7 MB. `:tools` and `:eval` build; they have no tests of their own.
- It took 5 attempts: 1-4 stopped on HTTP 429 from `repo.maven.apache.org` for ONE Robolectric jar (`nativeruntime-dist-compat-1.0.16`; attempt 1 had run `:core:test` for 11 minutes first), attempt 5 passed in 1 min 28 s. Nothing was blocked.
- NOT run: anything on the tablet or an emulator (no KVM here), the GitHub Actions workflow, `tools/prewarm.sh`.

## 5. Changed / not changed
Changed: `CLAUDE.md` (see its git diff), `TASKS.md` (R37), this file. Not changed on purpose: `.claude/settings.json` (6 light hooks, in step with `tools/session-root-hooks.json`; no skills, commands or agents live in the repo), `AGENTS.md` (still holds the old Session Learnings block), `HANDOFF.md` section 4 (same commands as before, still valid once the cache is warm), plan line 152, the cloud environment's setup script, any plugin setting.
