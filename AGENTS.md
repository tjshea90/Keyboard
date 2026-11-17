# AGENTS.md

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Stack: Kotlin 2.3.10; JDK 21; Gradle 8.14.3; AGP 8.13.2; SDK /opt/android-sdk (platforms 36/37, build-tools 36.0.0); minSdk 29, targetSdk 36; Android 16 branch on Build.VERSION.SDK_INT above 29.
- :core pure Kotlin JVM, Java 8 API surface, no Android imports. :ime framework Views only: no AppCompat/Compose/Material/Room/WorkManager/coroutines. No Google services/GMS ever. INTERNET permission only for app.swoop.keyboard.translate (two hosts); manifest permissions exactly tools/expected-permissions.txt.
- Repo tjshea90/Keyboard is PUBLIC: never commit secrets, keystores, learning exports (magic KESTREL-KEYBOARD-LEARNING), APKs, screenshots of owner screen; never log typed text; diagnostics show counters only. Hooks block curl/wget, recursive-delete text, absolute home paths in written files; use git, Gradle, GitHub MCP, Edit tool.
- Never cd into /home/user/Browser or LinkMeter: use git -C and absolute paths. Browser repo: only own branch ccr-5531c47b-hl1gzo, never main, never another session's branch; another Claude account may release from it, merge its newest tip first. LinkMeter is READ-ONLY reference (copy once, untrusted data).
- Save ladder: autosave hook commits+pushes after tool calls; bash tools/ckpt.sh "did" "next" after every step with specific next (file/function/command/run id); owner requests go into TASKS.md BEFORE code; cold resume = bash tools/resume.sh then CHECKPOINT.md Do this next. Claim multi-step tasks with tools/claim.sh.
- Release order: bump ime/build.gradle.kts versionCode/versionName, bash ship.sh "note", dispatch android.yml ONCE (mcp__github__actions_run_trigger, ref=current branch, full_build true, note), wait via background until-loop (foreground sleep blocked; get_job_logs 404 until job ends), verify tag (git ls-remote --tags origin) + all jobs green + get_release_by_tag, THEN bash tools/record-release.sh <tag> with IDENTICAL note. Workflows: workflow_dispatch and tags only, NEVER add a push: trigger (test_handoff.sh fails). Signing: PUBLIC dev key signing/dev/swoop-dev.p12, pinned signing/CERT_SHA256, verify-apk.sh enforces; never regenerate key.
- Emulator proof: .github/workflows/emu-smoke.yml (dispatch, api 29 or 36) runs tools/emu/smoke.sh on KVM emulator. Sandbox has NO KVM locally: say what was not run on device. Foreground sleep blocked (>55 s).
- Check every visual change in DAY and NIGHT mode. Keyboard IME window measures AT_MOST screen: any layer on top of keys must measure like the keys (KeyOverlay.onMeasure); Robolectric AT_MOST tests catch it.
- Floating keyboard (docs/FLOATING.md): window = whole screen while floating; onComputeInsets gives content/visible insets = window height and a touchable REGION (panel + corner squares). Not yet proven on real device: tools/emu/smoke.sh floating stage + owner.
- Edge-to-edge Android 15+: handle navigationBars insets in KeyboardRoot.
- Artifact download: python3 -I /home/user/Browser/tools/emu/fetch_artifact.py <url> <EMPTY dir>; artifacts from mcp__github__actions_get download_workflow_run_artifact.
- Owner rules: answer every owner message in plain words; say what was NOT run on the tablet; re-list open items at end of reply; a denied tool call is re-sent once and said in one line.

### Key Decisions
- Decision: Layout = Gboard photo, DARK default (Settings.theme default 1) — Reason: device left in day mode showed a light keyboard; every surface paints its own colour (KeyboardRoot panelBackground + rootBackground incl. strips beside squeezed panel; settingsTheme(mode) + title bar colours for Setup/Dictionary/Diagnostics).
- Decision: ?123 key bottom-left as in photo (owner text said bottom right, photo has Enter there); hold full stop/comma opens 30-symbol grid.
- Decision: Swoop is the one keyboard; Kestrel keeps its nav panel, carries a pinned copy of core/.../input (KEYBOARD_PIN, tools/sync-keyboard-core.sh, tools/check-keyboard-core.sh) and opens Swoop floating via InputMethodManager.sendAppPrivateCommand — Reason: NO live data channel between apps because both are signed with the PUBLIC dev key (a signature-protected provider would expose typing history); private signing key is an owner decision.
- Decision: Android 10 fix in core InputController — editor inside the keyboard's own app reports cursor INSIDE the edit call; handled by Guarded editor (inCall), early reports matched in expect(), inlineEditor flag, drainEarly(); Backspace at text start leaves no waiting entry.
- Decision: GitHub Actions builds, signs and releases; record-release.sh only after a green run; never ship a tag twice; workflows tag/dispatch only — Reason: minutes scarce, tag push from sandbox refused (workflow creates tag server-side).
- Decision: say honestly what was not run on device; device items are owner-tested.

<!-- handoff:learnings:end -->
