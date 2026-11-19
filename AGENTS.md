# AGENTS.md

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
