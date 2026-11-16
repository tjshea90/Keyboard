# TASKS - the job list (box ticked only when written, tested, committed; name the proof). KB-ids = research/KEYBOARD-APP-PLAN.md section 12.
# `[~] ID ... {branch time}` = claimed by a session (tools/claim.sh). `[x] ID ... - proof` = done. New owner requests are appended at the BOTTOM under 'Owner requests' in their own words.

## Owner requests (newest last, in the owner's words)
- NOTE OR1 (standing instruction, 2026-11-16, the owner): "The attached is a project for the keyboard repo. Treat the browser repo as read only". Build the Swoop keyboard per research/KEYBOARD-APP-PLAN.md in THIS repo (tjshea90/Keyboard); tjshea90/Browser is READ-ONLY (CLAUDE.md rule; tools/test_handoff.sh has a check). The work is the KB-ids below.


## Phase 0 - Bootstrap and safety net (about 1-2 sessions)
- [x] KB-000 Ladder kit, docs at birth, this plan copied to `research/` (section 11) - done when: `bash tools/test_handoff.sh` passes; 3 edits produce `auto-checkpoint:` commits and pushes; `CHECKPOINT.md` has a concrete "next" - bash tools/test_handoff.sh 65/65 green; autosave hooks fired (auto-checkpoint commits pushed, branch in sync with origin); ckpt 7 written with a concrete next
- [ ] KB-001 Gradle skeleton: `:core` (JVM, Java-8 API surface, flags as in Kestrel), `:ime` (Android app, `minSdk 29`, `targetSdk 36`, namespace), `:tools`, `:eval`; version catalog like Kestrel's (agp 8.13.2, kotlin 2.3.10); dispatch-only CI - done when: one green CI run (`full_build=false`)
- [ ] KB-002 Port `core/input` (all files in 3.1) with the 122 tests and `GestureSim`/`GlideEval`; copy `en.klx`, `LICENSE.txt`, `extra-words.txt`, `docs/DICTIONARY.md`; keep package names; compiler task `glideLexicon` works - done when: `./gradlew :core:test` green with the same test counts; list is byte-identical (blob `63e399fbc6`)
- [ ] KB-003 Private signing key + secrets + CI verification (11.5) - done when: CI builds a release APK whose certificate equals `signing/CERT_SHA256`; owner action done
- [ ] KB-004 Write `docs/ARCHITECTURE.md` from section 6, `docs/IF-THE-KEYBOARD-BREAKS.md`, `BRIEF.md` - done when: files exist, linked from `HANDOFF.md`

## Phase 1 - Walking skeleton: type, tap, glide in any app (3-4 sessions)
- [ ] KB-100 Manifest, `method.xml` (subtype `en_US`), `KeysService`, Setup activity (enable / select / try-it field), empty `KeysView` - done when: on the emulator `ime list -a` shows it, it can be enabled and selected, a letter types into a test field
- [ ] KB-101 `EditorBridge` (interface in core, `InputConnection` implementation in `:ime`, `FakeEditor` in tests) and the input state machine (6.4): composing, own-edit tracking, batch replace, password/no-learning flags - done when: >= 40 unit tests incl. a randomised event fuzz asserting the editor text equals the model after every event; selection-mismatch tests
- [ ] KB-102 Port `KeysView` (draw, taps, key bubble, haptics, Backspace repeat, shift / caps lock, symbols page, space slide, hold-for-digit); full-width geometry; bottom-row variants (default, URL, email, number/phone) - done when: Robolectric pixel/touch tests ported from `MiniNavTest`/`MiniNavLookTest` adapted; emulator screenshot looks right
- [ ] KB-103 Wire the glide engine: `GlideTyping` on worker threads, warm-up, live preview, strip, one-Backspace-removes-word, swap from strip, learning events, save/load, Export/Import of v1 (`KeyboardFlow` port) - done when: emulator lab: glide "hello" -> text `hello `; learning file written; export/import round trip
- [ ] KB-104 Enter / action keys per `imeOptions`, caps mode from `EditorInfo`, no-learning in private fields - done when: unit + emulator tests on text, password, URL, email, number fields
- [ ] KB-105 Diagnostics screen with counters from 8.5 layer 1, memory, versions, timings, Copy button - done when: text matches the browser's `GlideStats.line()` plus new counters
- [ ] KB-106 **Safety:** crash-loop guard -> minimal fallback layout without decoders; globe/next-keyboard key always visible; Setup screen button "go back to my previous keyboard"; plain-words doc - done when: killing the service 3 times in a minute produces the fallback; test on emulator
- [ ] KB-107 First Release `0.1.0` via CI; owner installs on tablet and phone; check the keyboard window on Android 16 (gesture and 3-button navigation, section 6.3); first tablet numbers: decode ms, "word shown after lift", PSS, cold show - done when: numbers pasted by the owner into chat and recorded in `research/TABLET_NUMBERS.md`. **GATE G1**

## Phase 2 - Typing fundamentals: taps, autocorrect, suggestions, personal model v2 (6-8 sessions)
- [ ] KB-200 `TapDecoder` (8.3.5): prefix-range beam, Gaussian per key, allocation-free; tap simulator in `:eval` - done when: tests: neighbour-key typos corrected, completions ranked by context; microbenchmark within budget
- [ ] KB-201 KGL2 format + compiler: UTF-8, language tag, display forms, trigrams, `<s>`, pruning, size report; rebuild English from added corpora with licences recorded - done when: file size within budget; `LexiconFileTest` for KGL1 and KGL2; accuracy not below the KGL1 baseline on `GlideEval`
- [ ] KB-202 Suggestion strip, composing text, autocorrect policy, undo-on-Backspace with unlearn - done when: policy tests (never correct URL/email/password/numbers; revert rate counter)
- [ ] KB-203 Auto-capitalisation, auto-space, double-space period, smart punctuation, apostrophe words; incognito toggle - done when: field-type table tests
- [ ] KB-204 Personal model v2 (8.3.2): learn from taps, two-use rule, calendar decay, per-id arrays, case forms, journal persistence, migration of the v1 file - done when: `PersonalModelTest` extended; speed test vs old model
- [ ] KB-205 Import of the browser export v1 and "Teach from text" - done when: synthetic v1 file from the docs example imports; owner imports the real one
- [ ] KB-206 Dictionary manager (list / delete / add / shortcuts) and "Forget everything" covering all new tables - done when: UI test; forget leaves no learned file
- [ ] KB-207 **Daily-driver week** on the tablet (owner switches default keyboard); collect Diagnostics; fix the top bugs - done when: owner's report + counters. **GATE G2**

## Phase 3 - Gboard-like everyday features (5-7 sessions)
- [ ] KB-300 Landscape layout; one-handed / dock / resize; key-height setting - done when: emulator screenshots both orientations; thumb reach checked by the owner
- [ ] KB-301 Long-press popups (alternates, digits), number row option, full symbols pages - done when: tests + owner check
- [ ] KB-302 Emoji panel with search and recents (10.1) - done when: glyph filter tested with a fake `hasGlyph`
- [ ] KB-303 Clipboard panel (10.2) - done when: sensitive clips skipped; 60-minute expiry test
- [ ] KB-304 Text-editing toolbar and Backspace swipe (10.3) - done when: fake-editor tests; emulator check
- [ ] KB-305 Themes, haptics/sound settings, accessibility pass (10.6) - done when: TalkBack labels present; contrast check
- [ ] KB-306 Next-keyboard (globe) key, hardware keyboard handling, IME switcher - done when: works on tablet and phone. **GATE G3** = every P0/P1 row of the parity matrix (section 5.1) marked done or consciously deferred

## Phase 4 - Learning engine v2 (5-8 sessions) - the core of the request, see section 8
- [ ] KB-400 Shadow decoder, extended counters, opt-in replay log, `:eval` replay runner, simulator calibration against the first real file - done when: owner can switch an experiment on and read "A vs B" counts; replay runner reproduces the live results
- [ ] KB-401 Speed: bitmask pre-filter, banded DP / early abandon, per-id arrays (7.2 items 3-5) - done when: identical top-3 on the eval seeds; tablet timing recorded in `research/PERF.md`
- [ ] KB-402 Per-key touch model from taps and glide ends (8.3.4 items 1-3) - done when: `:eval` shows no regression and the lab's gain; shadow data later
- [ ] KB-403 Layout-aware shapes, export v2 (`s2`, `touch`, `case`), collision sets in the compiler - done when: v2 example read by a unit test; v1 still imports
- [ ] KB-404 Topic vector (embedding builder in `:tools`, runtime feature) and language-tag features (8.3.3) - done when: measured on held-out text; ships OFF unless the gate is met
- [ ] KB-405 Segment templates experiment (8.3.4 item 4) - done when: lab result recorded; ship ON only if it beats KB-402 alone
- [ ] KB-406 Next-word prediction evaluation (top-3 hit rate, keystroke savings) with n-gram + personal + topic - done when: numbers in `research/LEARNING_RESULTS.md`
- [ ] KB-407 Online re-ranker, only if the lab shows headroom (8.3.7) - done when: otherwise write "not built, because ..."
- [ ] KB-408 Export v2 with optional passphrase encryption; opt-in weekly snapshot (8.9) - done when: encrypted round trip test; wrong passphrase refused
- [ ] KB-409 Report: `research/LEARNING_RESULTS.md` with every number vs the targets in 8.1. **GATE G4** - done when: owner reads the plain-words summary

## Phase 5 - REMOVED (owner decision 2026-11-16: no Spanish typing). Do not build a Spanish layout, dictionary, accent handling or bilingual decoding.

## Phase 6 - Translation, LinkMeter's way, inside the one keyboard app (2-4 sessions; see section 9; can start right after G1, in parallel with phases 3-4)
- [ ] KB-600 Port LinkMeter's pure code to `:core/translate/` in Kotlin: `Translation`, `TranslateFlow`, `Json` (3.3), with its tests (`TranslationTest`, about 56,000 checks) and the 36-breakage checker - done when: same checks pass; same breakages caught; no Android imports; recorded answers only
- [ ] KB-601 The `app.swoop.keyboard.translate` package: `Translator` glue (MyMemory then Google, one thread, timeouts, no redirects, user agent `Swoop`), connection log (no words), Test the connection, the `PROCESS_TEXT` activity "Translate to English" with a read-only panel; **plus the guard rails of 9.2 in CI**: exact permission fixture, source and dex scans, two-host allowlist test, dependency fixture, `usesCleartextTraffic=false`, `TranslateGate` tests - done when: CI fails when any guard rail is broken (prove it by a deliberate break); emulator lab: the menu entry appears in a test app; the log never contains the words
- [ ] KB-602 Toolbar Translate toggle (OFF at install, lit when on, "ES" mark on Enter, first-tap explanation, auto-off timer), eligibility and exclusions (password/URL/e-mail/number/phone/search/private fields, "Never translate in these apps" pre-filled with `com.tj.linkmeter`), the Enter flow on `EditorBridge` (9.3), strip with Undo / Send my English, LinkMeter's wording, second-Enter rules - done when: `FakeEditor` tests for every row of 9.3; emulator lab with a pretend translator: Enter -> Spanish in the field, Enter -> the action; nothing created while off
- [ ] KB-603 Received messages (9.4): the selection-menu route, the one-tap clipboard chip (only while the toggle is on), translate-selection button; **count the presses on the tablet in the apps the owner names** - done when: table "app -> best route -> presses" in `research/TRANSLATE_RESULTS.md`; chip tests with a fake clipboard
- [ ] KB-604 Friendly-tu guard, 30-sentence casual list, formal-answer counter (9.6); docs: `docs/TRANSLATE.md` for Swoop and the keyboard's SAFETY wording - done when: list passes on recorded answers
- [ ] KB-605 Dispatch-only "service check" workflow (9.8) and the quota/fallback behaviours verified with recorded answers (429, quotaFinished, puzzle redirect, timeouts) - done when: workflow runs; tests green
- [ ] KB-606 Acceptance on the tablet and the phone with the owner's script (9.8) - done when: owner's verdict recorded in `research/TRANSLATE_RESULTS.md`

## Phase 7 - Hardening and release quality (3-4 sessions)
- [ ] KB-700 Baseline profile, R8 rules review, budgets enforced in CI (APK size, permissions, memory smoke) - done when: CI fails when a budget is broken
- [ ] KB-701 Android 16 phone full pass (edge-to-edge, insets, predictive back in Settings, 16 KB page size if native code exists) - done when: owner's phone report
- [ ] KB-702 Security review (exported components, file modes, logs, clipboard, replay log), run the `security-review` skill and a manual pass - done when: findings fixed or recorded
- [ ] KB-703 Emulator lab in CI (adapt `emu-lab.yml`, `glide_emu.py`): typing, glide, strip, Backspace, cursor, password field, screenshots as artifacts only - done when: lab green on dispatch
- [ ] KB-704 Plain-words user guide and About screen with all licences (SymSpell MIT notice, corpora, models) - done when: present in the APK
- [ ] KB-705 Sign-off: daily driver on tablet and phone for 2 weeks, counters reviewed. **GATE G5** - done when: owner says it replaced Gboard/the stock keyboard

## Phase 8 - Stretch (only when asked): offline voice typing (10.4), split keyboard, handwriting, more languages.

## Decisions the owner must make (nothing blocks until the task named)
- [ ] Q-KB003 Signing key: plan 11.5 says the owner pastes the keystore (base64) and 3 passwords into four GitHub secrets, but they would have to be shown in chat first. PROPOSAL (needs the owner's yes before KB-003): a one-time dispatch-only workflow makes the keystore in CI, encrypts it with a long passphrase the owner types into ONE GitHub secret (SWOOP_KEYSTORE_PASSPHRASE) and commits only signing/swoop.jks.enc + signing/CERT_SHA256. The private key never appears in chat or in clear in the repo.
- [ ] Q-HEARTBEAT May the session create ONE recurring wake (every 2 hours) that resumes the work after a usage cut-off? It spends a little usage each time (plan Appendix G).
- [ ] Q-LINKMETER KB-600 needs READ access to the public repo tjshea90/LinkMeter (mcp__claude-code-remote__add_repo, access read); Browser is already readable here. Ask only when KB-600 starts.
