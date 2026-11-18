# AGENTS.md

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
