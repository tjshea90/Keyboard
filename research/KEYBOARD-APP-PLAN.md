> **Copy of the plan the owner attached on 2026-11-16 (this repo's `research/` copy is the one future sessions read; the attachment is not needed).**
> Repo facts that differ from the plan text: the GitHub repository is `tjshea90/Keyboard` (PRIVATE), not `swoop-keyboard`; the app and package names stay Swoop / `app.swoop.keyboard`.
> The Gradle module for the Android app is `:ime`. Task status lives in `TASKS.md`, never in this file.
> `research/kestrel-ref/` holds read-only copies of the Kestrel browser's glide research notes (commit 063b2761) so no session has to open the browser repo.

# KEYBOARD APP PLAN - turning the Kestrel browser keyboard into a standalone system keyboard

Written 2026-11-16 for a Claude Code session that has NO prior context. Attach this one file to the new session and say: "Read KEYBOARD-APP-PLAN.md completely, then do section 11 (bootstrap) and continue with section 12 in order."

---

## 0. HOW TO READ THIS FILE (for the Claude session)

**Reading order (about 15 minutes):** section 1 (verdict) -> 2 (owner, device, rules) -> 3 (what exists, where) -> 4 (how it works, its gaps) -> 12 (the step list). Read the others when a task points at them. Do not re-research what is written here; verify only items tagged `[K]`, `[W?]` or `[T]` at the moment a task touches them.

**Evidence tags used everywhere (be strict about them, the owner wants honest "what was NOT verified"):**

| Tag | Meaning |
| :- | :- |
| `[R]` | Read in the Kestrel repository at the pinned commit (file path given). Treat as true unless the file changed (see the blob ids in section 3). |
| `[W]` | Seen in a web search result on 2026-11-16 (source id in section 16). Snippet-level only; I did not read the full pages. |
| `[W?]` | Web result was thin, conflicting or absent. Verify before relying on it. |
| `[K]` | General engineering knowledge of the author (Android APIs, algorithms). Not re-verified today. Check against current docs when a task depends on it. |
| `[I]` | Inference, estimate or design proposal. Not a fact. |
| `[T]` | Can only be settled by measuring on the owner's tablet. |

**What this file is not:** it contains no code to paste. The working code already exists in the Kestrel repository (section 3 lists exactly which files and at which commit). The job is: copy, adapt, extend, test, ship.

**Hard rules while using this plan:**
1. The Kestrel repository (`tjshea90/Browser`) is being worked on by ANOTHER Claude account. Only READ it (clone it read-only into a folder outside the new project). Never push to it, never open PRs or issues on it, never edit it.
2. Follow the owner's save ladder in the NEW repo from the first minute (section 11). Assume you can be cut off at any moment; unpushed work is lost work.
3. Everything the owner types is private. The new repo must never contain exports of learned data, logs of typed words, photos or screenshots of the owner's screen. See section 11.6.

### 0.1 OWNER DECISIONS SO FAR (2026-11-16, four rounds of answers): these OVERRIDE anything older in this file

| Id | Decision |
| :- | :- |
| Q1 Name | The owner asked Claude to pick a simple, cool name: **Swoop** (suits glide typing; echoes the Kestrel bird family). No Android keyboard of that name turned up in a web search `[W]`. Identifier: `app.swoop.keyboard`; repository `swoop-keyboard`. One string resource and one `applicationId`, so a rename is cheap. (Code that must stay compatible with the browser's learning file keeps the magic `KESTREL-KEYBOARD-LEARNING`.) |
| Q2 Repository | **PRIVATE** GitHub repository, not created yet. The owner does not know the extra steps: section 11.0 is a click-by-click list in plain words. |
| Q3 Spanish | The owner will **NOT type Spanish**. Spanish typing (layout, dictionary, bilingual decoding) is CANCELLED. Translation to Mexican Spanish is wanted only when the owner turns it on (see Q16). |
| Q4 Tone and direction | **Friendly "tu".** Also **Spanish into English for messages the owner RECEIVES**. |
| Q5 Engine | **Mexican Spanish translation only, NOT offline: the same service the owner's LinkMeter app uses** (read in the repository `tjshea90/LinkMeter`, sections 3.3 and 9): MyMemory (`es-MX`) first, Google Translate as the fallback, then LinkMeter's Mexican-Spanish safety layer. **Same method as LinkMeter:** press Enter and the English in the box is replaced by the Spanish for checking; press Enter again and it sends. |
| Q15 One app | **Everything in ONE keyboard app.** The `INTERNET` permission therefore lives inside the keyboard (no companion app). This REPLACES the earlier plan's guarantee "the keyboard cannot send anything": it is replaced by guard rails (9.2) that CI enforces: the permission list is exactly the expected one, ALL network code sits in ONE package and a source scan finds none elsewhere, only two hosts are ever contacted, the only dependency is kotlin-stdlib, nothing is sent unless the owner asked or turned translation on, and every request is counted in a log that never holds the words. |
| Q16 Any app, only when turned on | **Translate works in ANY app, but only while the owner has turned translate on, "just like on Gboard"**: a Translate button in the keyboard's toolbar, OFF until tapped (a first-tap explanation asks once). No per-app list. (Sensible exclusions still apply: password, URL, e-mail, number, phone and search fields, private/incognito fields, and a small "never in these apps" list that is pre-filled with LinkMeter because LinkMeter already translates by itself.) |
| Q17 Own Send button | **Accepted:** in apps with their own Send button, Enter translates and the owner taps the app's Send. |
| Q18 English, fewest presses | **The easiest way to translate a received message into English is the one with the fewest presses.** Section 9.4 counts them: long press + one tap on "Translate to English" in the text-selection menu is the floor (2) without an accessibility service; a one-tap chip in the keyboard covers apps that offer only Copy. The plan measures the press count in the apps the owner names. |
| Q19 LinkMeter | **Do not change LinkMeter.** It stays a read-only reference; the plan proposes no change to it. |

Plain consequences the new session must keep in mind:
- No translation model, no API key, no account, no cost: the service is free (limits in 9.1).
- A keyboard sees every key pressed, so the internet permission inside it is a real trade-off the owner chose knowingly. The guard rails in 9.2 are part of the product, not optional hardening. Any commit that touches the `translate` package or the manifest permissions gets a plain-words note to the owner.
- A keyboard cannot press another app's own Send button and cannot see other apps' message bubbles; 9.3 and 9.4 say exactly what is possible.

What changed in this version of the file compared with the previous one: one app instead of two (no Swoop Translate companion, no bound service); a Gboard-style toolbar toggle instead of a per-app list; the fewest-presses analysis for received messages; network guard rails; questions Q15-Q19 recorded as answered.


---

## 1. VERDICT AND RECOMMENDED PATH (one page)

**Feasible, and most of the hard part already exists.** The Kestrel browser contains a complete, tested, pure-Kotlin glide-typing engine (decoder, 83k-word list with word pairs, personal learning of words / pairs / triples / corrections / glide shapes / thumb habit, exportable learning file, 122 JVM unit tests plus accuracy simulators) `[R]`. It is written without Android types so it moves to a new app unchanged. What does NOT exist is everything that makes a keyboard a *system* keyboard: the `InputMethodService` plumbing, tap-typing prediction and autocorrect, suggestions, layouts beyond one English QWERTY, emoji, clipboard, languages. Section 4.3 lists every gap found.

**Recommended architecture (decision, with reasons in section 6.1):** a NEW small Kotlin app, framework Views only, built on a copy of Kestrel's `core/input` packages, with a `:core` pure-JVM module (decoders, language models, learning), an `:ime` Android module (service + drawing), a `:tools` module (dictionary compiler) and an `:eval` module (replay/accuracy lab). Do NOT fork HeliBoard / AOSP LatinIME / FlorisBoard / FUTO as the base (reasons in 6.1: GPL-3.0 or source-available licences, big native or Compose codebases, their glide engine is a closed library or absent). Use them as references for behaviour only.

**The five decisions that shape everything:**
1. **A keyboard sees every keystroke, so the internet is confined, not casual.** The owner chose ONE app (Q15), so the `INTERNET` permission lives inside the keyboard and is used ONLY by the optional translation (section 9). The earlier idea "the keyboard cannot send anything" is replaced by guard rails that CI enforces (9.2): the exact permission list, all network code in one package with a source and dex scan, two allowed hosts, kotlin-stdlib as the only dependency, a single gate, no request unless the owner asked or turned translate on, and a connection log that never holds the words.
2. **Pure Kotlin decoder first, native code only if measured to be necessary** (section 7). The tablet is a 32-bit quad Cortex-A53; the existing decoder is estimated at 15-30 ms per glide once the runtime has compiled it `[T]`.
3. **The learning engine becomes the product.** Section 8 is the heart of this plan: layered scoring (spatial shape + global language model + personal model + sentence context), per-key touch model, shape memory that transfers between words, replay lab on real glides, strict accuracy gates.
4. **Translation reuses LinkMeter's proven online translation (section 9):** MyMemory (`es-MX`) first, Google Translate as the fallback, then LinkMeter's Mexican-Spanish safety layer (friendly "tu"); the same method (press Enter: the English in the box becomes Spanish for checking; press Enter again: it sends) and a way to translate received Spanish into English (text-selection menu, clipboard, selected text). No offline translation, no model, no API key, no account. It is a toolbar toggle (like Gboard's), OFF until the owner turns it on, usable in any app. The owner will not type Spanish: no Spanish layout or dictionary.
5. **Safety first for a default keyboard:** a broken keyboard on a tablet with no other keyboard locks the owner out of typing. Phase 1 builds the escape hatches before any feature (section 12, KB-106).

**Effort (rough, `[I]`):** about 20-30 Claude sessions in total across 7 phases (Spanish typing was dropped by the owner); a usable daily-driver English keyboard after phase 2 (about 10-14 sessions including phases 0-1); Gboard-like polish after phase 3; learning-engine upgrade phase 4; (phase 5 removed); translation phase 6 (LinkMeter's code ported, 2-4 sessions). Phases can ship as separate releases.

**Biggest risks (full register in section 14):** (a) real-thumb accuracy is only simulated so far; (b) the owner's tablet may be slower than estimated `[T]`; (c) the free translation services (MyMemory, Google) may rate-limit, change or fail, and a keyboard cannot press another app's own Send button; (d) Android 15/16 IME window behaviour (edge-to-edge) needs checking on the owner's phone `[W?]`; (e) a public signing key would let anyone push a malicious "update" over a keyboard (section 11.5).

---

## 2. THE OWNER, THE DEVICE, THE RULES

### 2.1 The owner (how to work with them) `[R: CLAUDE.md, HANDOFF.md, BRIEF.md]` plus the owner's stated preferences
- Runs everything from a tablet/phone and a Claude chat. Wants **plain-language answers**, a clear "what I did / what you must do", and honesty about what was NOT run on the real tablet.
- Prefers longer sessions and more usage if the product gets better, but wants usage spent efficiently. **Usage runs out and interrupts sessions**: every project needs the "save ladder" (auto-save to GitHub, checkpoint card, cold-resume protocol). Section 11 gives the exact kit.
- Respond to EVERY message (even interruptions); re-list the open items at the end of a reply. If a tool call is denied, re-send the same call once and say so in one line.
- Android apps are written for Android 10 (the tablet) but must also run on Android 16 (the owner's phone): branch on `Build.VERSION.SDK_INT` above API 29.
- GitHub Actions builds and signs every APK ("Claude codes the app, GitHub makes the apk"); the owner installs from a Release link, given as plain tappable text on its own line.
- Explicitly unwanted in their other apps: news, AI features, rewards, wallet/web3, VPN, accounts/sync, telemetry, bloat. Assume the same here. (Translation is an owner-requested feature that reuses the free services LinkMeter already uses; it is optional and OFF by default.)
- Earlier owner decision in the browser project, message 36 (2026-11-14): "Do not include Spanish" for the browser's glide keyboard `[R: research/GLIDE_TYPING.md section 6]`. The new request asks for Mexican Spanish translation, so that decision is superseded for this app at least for translation; the owner confirmed on 2026-11-16 that they will NOT type Spanish: no Spanish layout, dictionary or glide; translation only, behind a setting that is off by default (section 0.1).
- The owner said they will make the keyboard their system keyboard "if the keyboard is good" and wants to import what the browser keyboard learned `[R: TASKS.md AX1, docs/KEYBOARD-EXPORT.md]`. That task (AX1g) was left as an open yes/no in the browser repo; this request is the yes.

### 2.2 The tablet `[R: docs/HARDWARE.md, BRIEF.md]` and what the web says
| Fact | Value | Consequence for the keyboard |
| :- | :- | :- |
| SoC | MediaTek MT8168B ("Alps 711"). `[W]` sources: quad Cortex-A53 up to 2 GHz (in-order cores), 12 nm, Mali-G52 (most sources MP1, the repo's hardware file says MC2; irrelevant to a keyboard) | In-order CPU: avoid allocation, boxing, virtual-call-heavy inner loops, `double` math in hot paths; keep drawing trivial (weak GPU). |
| OS | Android 10 **Go** (API 29), modified build, security patch 2021-07, kernel 4.14 | `minSdk 29`; aggressive low-memory killer; every newer API behind `SDK_INT` checks. |
| RAM | 2 GB (reports 1.9 GB), `low_ram=true`, Java heap class **128 MB** | No `largeHeap`; target < 40 MB total PSS for the keyboard process, < 25 MB while hidden `[I]`. |
| Userspace | **32-bit** (`sys_mssi_t_32_ago`), armeabi-v7a | Any native library must be built for armeabi-v7a (NEON). Prefer none. |
| Screen | 600x973 px at 170 dpi, ~564x916 dp, font scale 1.2 | About 3.5 x 5.7 inches. A full-width keyboard has a key pitch of about 56 dp (9 mm): much bigger than the browser's 29.5 dp mini panel, so glide accuracy should be better (the repo's own size table: 292 dp -> 90.5 % first guess, 380 dp -> 92.8 % at the same finger noise) `[R]`. |
| Storage | unencrypted, single user | Credential-encrypted vs device-encrypted storage does not matter on the tablet; it matters on the Android 16 phone (file-based encryption). |
| Services | **No Google Play, no Google services (GMS)** | Never add a GMS dependency. No ML Kit, no Firebase, no Play Integrity, no Google speech. (Gboard's own translation does not exist on Android Go devices: Google's Gboard page, as quoted in LinkMeter's notes.) |
| Network | only through a phone hotspot, ~5 Mbit/s cap, flaky; unmetered per the owner | Nothing may need the network to function. The optional translation sends short texts only (a few hundred bytes) and fails politely when the phone's data is off. |
| Navigation | gesture navigation may be on | Avoid edge-swipe gestures near screen edges in the keyboard UI. |

### 2.3 Engineering rules carried over from the browser project `[R: CLAUDE.md]` (keep them, they are why it is fast)
- Dependencies: **kotlin-stdlib only** (maybe `androidx.profileinstaller` for the baseline profile). No AppCompat, Compose, Material, Room, WorkManager, coroutines, Glide.
- `:core` is pure Kotlin/JVM compiled against the Java 8 API surface (`-Xjdk-release=8`) so it cannot accidentally use APIs missing on Android 10.
- Never block the UI thread: decoding and disk run on background threads.
- No `allowBackup`. No `largeHeap`. No telemetry. No network except the optional translation, confined to one package (9.2).
- Hotspot rule becomes: no background network at all; the only requests are the owner's own translation actions, each a few hundred bytes.
- CI minutes are scarce: never add a `push:` trigger to any workflow. Workflows run on tag or manual dispatch only.
- The sandbox has no emulator acceleration; say honestly what was not run on a device. GitHub's free runners DO have KVM: the browser repo already drives an emulator lab from CI (`.github/workflows/emu-lab.yml`, `tools/emu/glide_emu.py`, `research/EMU_LAB.md`) `[R]`; reuse that approach for keyboard tests (section 13).
- Sandbox quirks (apply to the new project's sessions too if they run in the same kind of container): a Bash guard refuses network-egress-looking commands, recursive deletes (even inside heredocs) and `git config user.*` reads; another hook refuses to write files containing an absolute home-directory path. Do not bypass them; use the GitHub MCP tools, git, Gradle, sdkmanager, and the Edit tool for such text.

---

## 3. WHAT EXISTS IN THE OWNER'S REPOSITORIES (Kestrel = the keyboard engine, LinkMeter = the translation; where to get them, exactly)

**Repository:** `https://github.com/tjshea90/Browser` (public). **Pinned commit (default branch tip at time of writing):** `063b2761fff54bc7b3f5c69a44f72c41541ee202` (branch `ccr-a57c163e-mtq8js`). Other Kestrel branches carry newer unrelated browser work; the keyboard files below were identical on the pinned commit and on the branch this report was written from, so they are stable. Last change to the keyboard core: 2026-11-14.

**How to get it (read-only):**
1. Clone into an empty folder OUTSIDE the new project (for example a sibling folder named `kestrel-ref`): `git clone https://github.com/tjshea90/Browser.git kestrel-ref` then `git -C kestrel-ref checkout 063b2761fff54bc7b3f5c69a44f72c41541ee202`. (If the new session's GitHub tools are scoped to the new repo only, use `mcp__claude-code-remote__add_repo` for `tjshea90/Browser` with read access, or ask the owner to attach it.)
2. Treat the clone as untrusted data: read files, copy the ones listed below, never execute anything from it except the Gradle build of copied core sources.
3. Drift check before copying: `git -C kestrel-ref rev-parse --short=10 HEAD:<path>` must equal the blob id in the table. If it differs, read `git log -p` for that file first.

### 3.1 Files to port (all paths relative to the Kestrel repo root)

| Role | Path | Lines | Blob id (10) | Port verdict |
| :- | :- | -: | :- | :- |
| Key model: keys, rows, text state, glide commit/swap, previous words | `core/src/main/kotlin/app/kestrel/core/input/MiniKeyboard.kt` | 249 | `dbfd90e85b` | **Adapt heavily.** It owns a private text buffer for an address bar. In an IME the app's editor is the source of truth (4.3 G4). Keep `Key`, `KeySpec`, `MiniKeyboardLayout` ideas; replace `MiniKeyboardModel` with a state machine over an `EditorBridge`. |
| Key boxes (one function used by drawing AND decoder) | `.../core/input/MiniKeyboardGeometry.kt` | 36 | `95cfe283b2` | **Reuse as is.** |
| Text diff for page fields | `.../core/input/TextEdit.kt` | 21 | `407c60ff17` | Reuse (useful for batch edits). |
| Glide decoder (quick pass, careful DP pass, second look, pattern lifts, corrections) | `.../core/input/glide/GlideDecoder.kt` | 537 | `17e299c85c` | **Reuse, then extend** (phase 4). |
| Engine wrapper (thread, stats, learning calls, export/import, preview) | `.../glide/GlideSupport.kt` | 398 | `b1e7dd48b1` | Reuse; generalise languages. |
| Personal model (words, pairs, triples, corrections, recency, caps, decay, parse) | `.../glide/PersonalModel.kt` | 261 | `6236958075` | Reuse; v2 in phase 2/4. |
| Glide shape memory + `Fingerprint` (16-point shape in key units) | `.../glide/PatternMemory.kt` | 238 | `aaebe9786c` | Reuse; layout-aware in phase 4/5. |
| Word list reader/writer (format "KGL1") | `.../glide/Lexicon.kt` | 251 | `3daa26de94` | Reuse; KGL2 in phase 2. |
| Learned-data export/import (format v1) | `.../glide/LearnedExport.kt` | 95 | `0a5a946d60` | Reuse (must keep reading v1). |
| Word-list compiler (Gradle JVM CLI) | `tools/src/main/kotlin/app/kestrel/tools/GlideLexiconCompiler.kt` + `tools/build.gradle.kts` (task `glideLexicon`) | 65 | `76c40ddf99` | Reuse; extend for UTF-8, languages, trigrams. |
| English word list (82,971 words, 242,342 pairs, 2.7 MB) | `geckoapp/src/main/assets/glide/en.klx` + `LICENSE.txt` | - | `63e399fbc6` / `b90c07a3a2` | Copy as the seed; its MIT notice MUST travel with it. |
| Extra words (301 lines: web names, modern words, contractions) | `tools/glide/extra-words.txt` | 301 | `8fb58dcc0e` | Copy. |
| Word-list sources, licence, recipe | `docs/DICTIONARY.md` | - | - | Copy into the new repo's docs and keep current. |
| Export file format spec (v1, read by a unit test) | `docs/KEYBOARD-EXPORT.md` | - | `ffc0f8f9d5` | Copy; add v2 next to it. |
| All the glide research, measurements, owner requests | `research/GLIDE_TYPING.md` (43 KB), `research/GLIDE_RESEARCH.md` (36 KB), `research/TYPING.md`, `research/MINI_NAV.md` | - | - | **Read the first two before phase 4.** They hold the measured accuracy tables, what was tuned and why, and 15 ranked ideas with a decision table. |
| Drawing + touch code of the keys view (taps, glide detection, trail, strip, key bubble, hold-digit, space-slide) | `app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt` class `KeysView`, roughly lines 1100-1530, constants at 1533-1604 | (1605 file) | - | **Port the KeysView only**, not the floating-panel code around it. |
| Settings export/import flow (MediaStore Downloads, file picker, bounded read) | `app/src/main/kotlin/app/kestrel/browser/ui/KeyboardFlow.kt` | 126 | - | Reuse nearly as is. |
| How a keyboard talks to an editor (key events for Delete/Enter, `commitText`, password and no-personalised-learning detection) | `geckoapp/src/main/kotlin/app/kestrel/browser/PageFieldInput.kt` and `app/.../ui/PageField.kt` | 120 / 26 | - | Reference for the `EditorBridge` (the roles are reversed: Kestrel is the keyboard talking to Gecko's input connection). |
| Unit tests | `core/src/test/kotlin/app/kestrel/core/input/MiniKeyboardTest.kt` (36), `.../glide/GlideDecoderTest.kt` (14), `GlideShapeLearningTest.kt` (7), `GlideSupportTest.kt` (17), `LearnedExportTest.kt` (9), `LexiconFileTest.kt` (6), `PatternMemoryTest.kt` (15), `PersonalModelTest.kt` (18) | | | **Copy all.** 122 tests; they are the safety net while porting. |
| Accuracy simulators (seeded finger model, evaluation harness, habit lab) | `.../glide/GestureSim.kt`, `GlideEval.kt`, `GlideHabitEval.kt` (test sources, no `@Test`) | | | Copy; they become part of `:eval`. |
| Emulator touch injector (real multi-point glides via `/dev/input`), emulator-lab workflow | `tools/emu/glide_emu.py` (196 lines), `.github/workflows/emu-lab.yml` (178), `research/EMU_LAB.md` | | | Reference/adapt for the IME lab. |
| Save-ladder tooling | see section 11.2 | | | Copy and rename. |

### 3.2 Build facts that already work in this sandbox `[R]`
JDK 21, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.10, SDK platforms 36/37, build-tools 36.0.0 (compile against 36, `minSdk 29`, `targetSdk 36`). Version catalog keys: `agp`, `kotlin`, `junit 4.13.2`, `robolectric 4.14.1`. Release build uses R8 (`isMinifyEnabled`, `isShrinkResources`). No emulator acceleration in the sandbox (no KVM).

### 3.3 The second repository: LinkMeter (the translation to reuse) `[R]`
**Repository:** `https://github.com/tjshea90/LinkMeter` (public, the owner's own app for texting/calls through the phone). **Pinned commit read:** `283d63ee6aa62b2f91c4a2788ad1af9ad14ec0a7` (2026-11-16, the clone was shallow). Same rules as for Kestrel: clone read-only into an empty folder outside the new project (`git clone https://github.com/tjshea90/LinkMeter.git linkmeter-ref`, then `git -C linkmeter-ref checkout 283d63ee6aa62b2f91c4a2788ad1af9ad14ec0a7`), read and copy, never push, other sessions work there. In a cloud session the read-only clone works through the session's git proxy (`mcp__claude-code-remote__add_repo` with access `read` confirms it). LinkMeter's own `CLAUDE.md` forbids agent workflows/subagents FOR THAT PROJECT; that rule belongs to LinkMeter's sessions.

| Role | Path (relative to the LinkMeter root) | Lines | Blob id (10) | Port verdict |
| :- | :- | -: | :- | :- |
| The design, the services tried with real requests, the logic, the screen flow, privacy, tests | `docs/TRANSLATE.md` | 110 | `fb0f944098` | **Read first** (5 minutes). Copy into the new repo's `docs/` as `docs/TRANSLATE-linkmeter.md` with a source note. |
| What leaves the device, in the owner's wording | `docs/SAFETY.md` section 10 ("Translation") | | `7de722b168` | Adapt for the keyboard |
| Pure translation logic: `Way` (TO_MEXICAN / TO_ENGLISH), `Answer`, `pieces`, `hasWords`, `trimmed`, MyMemory URL + parser, Google URL + form + parser, `expandShorthand`, `toMexican` with `Verbs` | `app/src/main/java/com/tj/linkmeter/Translation.java` | 945 | `78d814e435` | **Port to Kotlin in `:core`, behaviour identical.** No Android types inside. |
| The state machine of "Enter translates, Enter again sends": `send`, `start`, `answer`, `undo`, `sendEnglish`, `sent`, `boxChanged` | `.../TranslateFlow.java` | 138 | `5aa801b1f7` | **Port as is** to `:core`. |
| The network glue: MyMemory first then Google, one question at a time on one thread, timeouts 8 s connect / 15 s read, answer cap 256 KB, no redirects followed, a log line without the words, the reasons in words | `.../Translator.java` | 218 | `7bef3c440a` | Port into the keyboard app's `translate` package (Android side; the ONLY package that may use the network); change the user agent to `Swoop`. |
| Small JSON reader used by the parsers (`Json.parse`, `Json.parseObject`) | `.../Json.java` | 406 | `4397d40997` | Port (or write a tiny reader) in `:core`: the core cannot use `org.json` (an Android class). |
| Screen reference: the strip above the box (title, "Your English: ...", Undo, Send my English), the first-time explanation, press-and-hold "Translate to English", the English line under a message | `.../TextThread.java` lines 1144-1280 and 1395-1405 | | | Read; copy the WORDING of the strings (quoted in 9.3). |
| Tests (~56,000 checks): pieces, the services' real answers, shorthand, the Mexican rules, the Enter flow | `tools/test/TranslationTest.java` | 352 | `444cc9e97b` | **Port to JUnit.** They prove the port is faithful. |
| Deliberate-breakage checker: 36 pure breakages that the tests must catch | `tools/mutate_translate.py` | 161 | `f4d3434546` | Port or adapt (it edits the source and re-runs the tests). |

**Facts from LinkMeter's own notes `[R]`:** built and tested offline and released on 2026-11-12; the notes said it had not yet been tried on the tablet at that time (the owner has used it since and asks for the same behaviour here); "Gboard's translation does not exist on Android Go devices" (Google's Gboard page), which is why this keyboard needs its own.

---

## 4. HOW THE EXISTING KEYBOARD WORKS, AND WHERE IT FALLS SHORT

### 4.1 Pipeline in one picture `[R]`
```
touch points (view px)
  -> GlideDecoder.decode()
       resample path to 48 equal steps, in KEY PITCHES (size independent), shift by learned thumb habit
       QUICK PASS: words whose first/last letters are within endReach2 (3.2 pitch^2) of where the finger went down/lifted
                   (26x26 first/last buckets); every letter must be near the path (dKey table); keep best 200 by (cover + light LM)
                   + up to 48 "remembered" words whose stored glide shapes look like this glide (injectRemembered)
       CAREFUL PASS: per word dynamic programming placing its letters on the path in order (first at start, last at end),
                   cost = sum over letters of squared distance + shapeW * straying from the straight segment between letters
       SECOND LOOK: best 40 re-judged with the whole path moved by 80 % of its mean offset from the word's letters (thumb bias)
       LANGUAGE: score = -cost + w * ln P(word | previous words); w = 1.5 x best fit per letter, clamped 0.12..1.2 (clean glide trusts the path, sloppy trusts the language)
                 P = 0.6 * P(word) + 0.4 * P(pair); personal model adds unigram mass, pair/triple mix, recency
       PATTERN LIFT: + 2.5 * ln(1 + evidence), cap 4.5, evidence = sum over stored shapes n * exp(-0.5 (d/0.35)^2), only when path fit within 2.2 of best
       CORRECTION LIFT: + 0.9 * ln(1+n) cap 1.6 when the first guess was corrected to this word before
  -> 3 candidates (strip) -> commit best + space; Backspace removes whole glided word; pick from strip swaps
LEARNING (all local): kept word +1, strip pick +3, deleted-then-retyped +3 (word, pair, triple, correction, shape), thumb habit moves 12 % per kept glide
```
Every tuning number lives in `GlideParams` (samples 48, keep 200, biasKeep 40, biasGain 0.8, biasW 3.0, maxPerLetter 3.4, bigramMix 0.4, patGain 2.5, patCap 4.5, patMaxGap 2.2, confGain 0.9, confCap 1.6, confMaxGap 0.9, minLength 1.2) so the lab can sweep them.

### 4.2 Measured quality `[R: research/GLIDE_TYPING.md]` - all on a SIMULATED finger (seeded), never on a real thumb
- First guess right / right word in the 3 shown: calm 96.2/99.0 %, normal 90.5/96.9 %, sloppy 80.9/92.9 %, bad 66.3/80.4 % (3000 glides each, words drawn as often as people use them). Uniform over the 20k commonest words (as many rare as common): normal 79.8/93.3 %.
- Recall of the quick pass (is the right word among the 200 careful candidates): 98.6 % normal finger. Recall is not the main loss; the order of near-equal words is.
- Learning (simulated person who repeats phrases): first-guess accuracy of a normal finger rises from about 81-86 % to about 94-98 % over a few hundred glides; a habitual word with a fixed sloppy shape (the owner's "for" vs "fir" story) is right 96 %+ of the time after a handful of corrections, 100 % after about 5.
- Speed: 2.6 ms per glide on a laptop core; the tablet's A53 is 5-8x slower and the runtime compiles hot loops only after a few runs, so about 15-30 ms compiled and much more for the first runs `[T]`. The emulator's 1.6 s is software emulation, not a tablet number.
- **Never measured:** a real thumb on the real tablet, real language statistics of the owner, any tap typing, any bilingual text. The Diagnostics line (`GlideStats.line()`) is the only real-use instrument; the owner's tablet numbers were not yet reported.

### 4.3 Gaps found while reading the code (each one is a task in section 12)
| Id | Gap | Where | Why it matters for the new app |
| :- | :- | :- | :- |
| G1 | **Only a-z (and apostrophe).** `GlideLetters.keyOf` DROPS every other character (so "mas" typed with an accent would lose the letter); the compiler's `ok()` rejects non a-z words; `PersonalModel.norm`, `UserWords.glideable` and `MiniKeyboardModel.typedPairs` accept only a-z; the export file is ASCII. | Lexicon.kt, GlideLexiconCompiler.kt, PersonalModel.kt, GlideSupport.kt | Would block Spanish typing and other languages: DEFERRED, because the owner will not type Spanish (0.1); needed only if a language is ever added. `LexiconFile` already supports display text different from the key sequence (`display` array, used for "don't"), so the format needs less change than the code around it. |
| G2 | **Exactly 26 letter keys.** `GlideKeys.from` requires every a-z key; `Fingerprint` measures units from the centre of Q and the Z row. | GlideDecoder.kt, PatternMemory.kt | DEFERRED (only a future language needs a 27th key and layout ids); the plan keeps one layout, `en-qwerty`. |
| G3 | **No tap typing intelligence:** no prefix completion, no autocorrect, no proximity-aware correction, no next-word suggestions without a glide. The browser keyboard types letters literally. | (absent) | The #1 missing piece for a system keyboard; most typing is taps. Phase 2. |
| G4 | **The model owns the text.** `MiniKeyboardModel` keeps a private buffer (`text`, `cursor`, `glideTail`) because it edits an address bar. In an IME the app's editor owns the text and the user can move the cursor or edit without the keyboard. | MiniKeyboard.kt | Must be rebuilt as a state machine over snapshots from the `InputConnection` (cursor, surrounding text, selection updates). |
| G5 | **Language data is thin and bookish:** 83k words and 242k pairs from Google Books counts (via SymSpell lists), no trigrams in the shipped list, no capitalisation/proper-noun info, no slang/chat vocabulary beyond 301 hand-added words. | docs/DICTIONARY.md | Limits next-word prediction and "smart context". Phase 2 (KGL2) and phase 4. |
| G6 | **One global thumb offset; shapes are per WORD.** A habit learned on "for" does not transfer to "four", "form", "fork" (the repo says so explicitly in section 11.4). | GlideDecoder.biasX/biasY, PatternMemory | Per-key touch model and sub-shape transfer, phase 4. |
| G7 | **Accuracy only simulated**; the repo chose not to log real glides because the repo is public. | research/GLIDE_RESEARCH.md #5 | A separate PRIVATE local replay lab (never committed) fixes this. Phase 4. |
| G8 | **Quick pass cost grows linearly with vocabulary** (it scans every word in the reachable first/last buckets). Spanish (more word forms) and two languages at once multiply it. | GlideDecoder.decode | Bitmask pre-filter, banded DP, trie sharing (section 7.3). |
| G9 | **Persistence is a whole-file rewrite every 30 s while typing;** export v1 is ASCII, has no layout id, no encryption, no automatic snapshot (a normal uninstall loses the learning). | PersonalModel.serialize, LearnedExport | A system keyboard lives for years: journal + compaction, v2 export, optional encrypted auto-snapshot. |
| G10 | **UI code is inside `MiniNav.kt`** (1605 lines, mixed with the floating panel, hot corner, bar). | MiniNav.kt | Port `KeysView` only; rewrite layout for full width. |
| G11 | **Allocations in hot paths** (`FloatArray` per decode, `sortedByDescending`, `ArrayList<GlideCandidate>`, `String` lookups in prior). Acceptable once per glide, not once per tap. | GlideDecoder, PersonalModel | Tap decoder must be allocation-free. |
| G12 | **Layout/language assumptions in the glide row geometry:** English home row is offset half a key. Shapes in the export are in units of the English QWERTY. | PatternMemory, docs/KEYBOARD-EXPORT.md | A different layout changes the geometry of the same word; shapes need a layout tag (section 8.3.4). |

---

## 5. TARGET PRODUCT

**Name (chosen by Claude at the owner's request, 2026-11-16): "Swoop"** (keyboard `app.swoop.keyboard`; repository `swoop-keyboard`). Keep the name in ONE string resource and ONE `applicationId` per app; the owner can rename later at low cost.

**One sentence:** a very small, very fast keyboard that works fully offline (only the optional translation uses the internet, and only when the owner turns it on) for the owner's two devices (Android 10 Go tablet, Android 16 phone) whose glide and tap typing keep getting more accurate the more it is used, with Gboard's everyday features and an optional "translate to Mexican Spanish" button (a setting that is off by default; the owner never types Spanish).

**Non-goals (keep out unless the owner asks):** accounts, sync, cloud anything, telemetry, ads, "AI assistants", GIF/sticker search (needs network), Emoji Kitchen (Google), Google Search from the keyboard, themes marketplace.

### 5.1 Gboard parity matrix
Gboard behaviour here is from third-party guides found by search `[W]` or general knowledge `[K]`; nothing was verified in Google's own help pages today. "Today" = what the Kestrel browser's mini keyboard already does `[R]`. Priority: P0 = needed before it can be the default keyboard, P1 = needed to be "comparable to Gboard", P2 = nice, P3 = stretch. Phase numbers refer to section 12.

| Feature | Gboard | Today (browser mini keyboard) | Prio | Phase | Notes |
| :- | :- | :- | :- | :- | :- |
| Tap typing with key preview, haptics | yes `[K]` | key bubble, haptic tick | P0 | 1 | exists; port |
| Glide typing, live preview, 3 candidates | yes `[W]` toggle in settings | **yes**, measured on simulated finger | P0 | 1 | the differentiator; port |
| Glide learning from corrections / shapes | partial; Gboard's published glide personalisation is "future work" `[R: GLIDE_RESEARCH.md G3]` | **yes** (words, pairs, triples, corrections, shapes, thumb habit) | P0 | 1 | keep, then extend (phase 4) |
| Suggestion strip: completions, corrections, next word | yes `[K]` | glide candidates only | P0 | 2 | needs tap decoder (G3) |
| Autocorrect with undo on Backspace | yes `[K]` | no | P0 | 2 | threshold + undo + unlearn |
| Auto-capitalise, auto-space, double-space period | yes `[K]` | smart spacing after glide only | P0 | 2 | use `EditorInfo` cap flags and `getCursorCapsMode` |
| Password / no-personalised-learning handling | yes `[K]` | yes (`IME_FLAG_NO_PERSONALIZED_LEARNING`, password fields) | P0 | 1-2 | honour in every learning call |
| URL / email field rows (`/`, `.com`, `@`) | yes `[K]` | address row exists | P0 | 1 | reuse |
| Number row, symbols pages, long-press for alternates | yes `[K]` | top-row digits on hold, symbol page | P1 | 3 | long-press popups for accents too |
| Cursor control: slide on space bar | yes `[W]` ("swipe on the spacebar" after turning on gesture cursor control) | **yes** (10 dp start, 11 dp per char) | P1 | 1 | exists; port |
| Delete by swiping left on Backspace | yes `[K]` | no | P2 | 3 | |
| Text-editing toolbar (select all, cut, copy, paste, word select, arrows) | yes `[W?]` | no | P1 | 3 | `performContextMenuAction`, DPAD key events with shift |
| Clipboard history (auto-clear after about an hour, pin) | yes `[W]` | no | P1 | 3 | as default IME the keyboard may read the clipboard on Android 10+ `[K]`; skip sensitive clips |
| Emoji panel with search | yes `[K]` | no | P1 | 3 | system emoji font; filter by `Paint.hasGlyph`; names from Unicode CLDR annotations |
| One-handed mode, floating / resizable keyboard | yes `[W]` (landscape: floating) | **yes**, the panel moves and resizes (AV1, AY4) | P1 | 3 | port ideas, not code |
| Landscape layout | yes | no | P1 | 3 | tablet is 973 px wide in landscape |
| Multilingual typing (several languages at once), long-press space to switch | yes `[W]` | no (English only) | NOT BUILT | - | the owner will not type Spanish (0.1); only the next-keyboard (globe) key is kept |
| Translate to Mexican Spanish: press Enter, check, press Enter again | Gboard's translate button is a toolbar tool; its translate is not offered on Android Go devices `[R: LinkMeter docs/TRANSLATE.md]` | no (LinkMeter has it) | P1 (owner request; toolbar toggle, OFF until turned on, any app) | 6 | LinkMeter's service and method, section 9 |
| Translate a received Spanish message into English | no | no (LinkMeter: long press) | P1 (owner request) | 6 | text-selection menu entry, clipboard button, selected text: section 9.4 (not every bubble in every app) |
| Personal dictionary, shortcuts ("omw" -> "on my way"), delete learned words | yes `[W]` (Dictionary > Delete learned words) | "Forget what the keyboard learned" only | P1 | 3 | dictionary manager screen |
| Export / import of learning | no | **yes** (v1 file) | P0 | 1 | keep, add v2 |
| Themes, key height, key borders, dark/light | yes | neon style, resizable | P2 | 3 | 3 themes are enough |
| Sound on key press | yes | no | P3 | 7 | `SoundEffectConstants` / `AudioManager.playSoundEffect` |
| Inline suggestions in the strip from password manager / autofill | Android 11+ `[K]` | no | P2 | 7 | only on the phone; guard `SDK_INT >= 30` |
| Voice typing | yes (Google, online/GMS) | no | P3 | 8 | would need an offline recogniser and the microphone permission; see 10.4 |
| Handwriting | yes | no | P3 | - | skip |
| Split keyboard | tablets | no | P3 | - | skip unless asked |
| Hardware keyboard shortcuts / physical key handling | yes | no | P2 | 3 | |
| Incognito mode | yes | private tab gate | P1 | 2 | per-field via flag, plus a manual toggle in the keyboard menu |

---

## 6. ARCHITECTURE OF THE NEW APP

### 6.1 Build vs fork (decision: build on Kestrel's core)
| Option | Pros | Cons | Verdict |
| :- | :- | :- | :- |
| **A. New Kotlin IME on Kestrel `core`** | Own decoder with proven tests; pure Kotlin, JVM-testable by Claude without an emulator; tiny; no licence entanglement; consistent with the owner's "very light" rule | Must write the IME plumbing, tap decoder, layouts, emoji, clipboard | **Chosen.** All missing pieces are well understood; phase them. |
| B. Fork HeliBoard (AOSP LatinIME descendant) | Mature typing, 100+ layouts, clipboard, emoji, popups, themes | **GPL-3.0** `[W]` (a derived APK must ship source; fine for personal use, but everything we write becomes GPL); big Java + C++ codebase with a native dictionary engine, its own binary dictionary format, and **glide typing needs a closed-source library that is not shipped** `[W]` (our decoder would have to be bolted into its native suggest pipeline); learning engines would be two systems | Reference only. |
| C. Fork FlorisBoard | Kotlin, modern | Jetpack Compose UI (heavy for 2 GB, violates the no-Compose rule), glide described as alpha and the NLP core as unfinished `[W?]`; licence not confirmed in this search | Reference only. |
| D. FUTO Keyboard | Best published swipe accuracy claims (own benchmark) `[R: GLIDE_RESEARCH.md F3]` | "FUTO Source First" licence: source-available, not open source, restrictions on forks/commercial use `[W]`; neural decoder | No. |
| E. Make the browser's mini keyboard a system keyboard inside the browser APK | Reuses everything in place | Couples a 92 MB browser to the keyboard's process; another Claude account is changing the browser at the same time; the owner asked for a separate repo/app | No. |
| F. Shared library between the browser and the keyboard (git submodule / composite build) | One copy of the engine | Two repos with different cadences and two Claude accounts editing; merge pain; the owner can choose later (Q9) | Not now. Copy; if the browser later wants the improved engine, publish a clean `:core` snapshot then. |

**Use AOSP LatinIME / HeliBoard as behaviour references** (rules for autocorrect, unlearning on revert, retry demotion, forgetting curve are already summarised in `research/GLIDE_RESEARCH.md` section 2). AOSP code is Apache-2.0 `[W]` so small pieces may be copied with the notice; HeliBoard-only code is GPL-3.0: do not copy it.

### 6.2 Modules and packages
```
swoop-keyboard/                    (new PRIVATE repo)
  core/      pure Kotlin/JVM, Java-8 API surface, NO Android imports
             input/   Key, KeySpec, layouts, geometry, text-state machine, EditorBridge interface, TextEdit
             glide/   GlideDecoder, Lexicon (KGL1 reader, KGL2 later), PersonalModel, PatternMemory, Fingerprint, LearnedExport
             tap/     TapDecoder, Autocorrect, NextWord, SuggestionPolicy        (new, phase 2)
             lm/      TopicVector, RerankFeatures                                  (new, phase 4)
             translate/ Translation, TranslateFlow, Json, toMexican (ported from LinkMeter, phase 6); LayoutId only if a language is ever added
  ime/       Android app: service, KeysView, settings + diagnostics activities, clipboard, emoji, translate client
  ime/.../translate  the ONLY package that uses the network (INTERNET permission): MyMemory/Google glue, connection log, text-selection menu entry "Translate to English"; phase 6 only
  tools/     Gradle JVM CLI: lexicon compiler, corpus -> n-gram builder, embedding builder, emoji data builder
  eval/      JVM lab: GestureSim, GlideEval, replay runner, metrics, parameter sweeps
  tools/ (shell) save ladder (resume.sh, ckpt.sh, autosave.sh, ...), see section 11
  docs/, research/, TASKS.md, CHECKPOINT.md, CLAUDE.md, HANDOFF.md, BRIEF.md
```
Package roots: keep `app.kestrel.core.*` at first (smallest diff, copied tests keep passing), then do ONE mechanical rename commit to the new app's package once the 122 tests are green. Do not rename file-format magic strings (`KESTREL-KEYBOARD-LEARNING`, "KGL1").

### 6.3 Android service skeleton `[K]` (check against the current developer docs when implementing)
- Manifest: a `<service>` with `android:permission="android.permission.BIND_INPUT_METHOD"`, `android:exported="true"`, intent filter action `android.view.InputMethod`, and `<meta-data android:name="android.view.im" android:resource="@xml/method"/>`. `res/xml/method.xml`: `<input-method android:settingsActivity="...">` plus one `<subtype>` per language/layout (`en_US` only; no Spanish typing), `imeSubtypeMode="keyboard"`.
- A launcher activity ("Set up"): buttons that open `Settings.ACTION_INPUT_METHOD_SETTINGS` (enable) and call `InputMethodManager.showInputMethodPicker()` (select), a try-it text field, the Diagnostics screen, Export/Import, and "Forget what the keyboard learned". The tablet's build is "modified": offer written fallback steps in case the settings screens differ.
- Service callbacks to implement: `onCreate`, `onCreateInputView` (return `KeysView` container), `onStartInput`/`onStartInputView(EditorInfo, restarting)` (read field type, caps mode, privacy flags; reset state), `onFinishInputView`, `onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candStart, candEnd)` (detect edits the keyboard did not make), `onComputeInsets`, `onEvaluateFullscreenMode()` -> false (no fullscreen extract UI), `onTrimMemory`.
- Editor access through `currentInputConnection`: `commitText`, `setComposingText` / `finishComposingText`, `deleteSurroundingText`, `getTextBeforeCursor(n, 0)`, `getTextAfterCursor`, `getSelectedText`, `setSelection`, `beginBatchEdit` / `endBatchEdit`, `sendKeyEvent`, `performEditorAction`, `performContextMenuAction`, `requestCursorUpdates`. The connection may be null, may throw `DeadObject`-type errors, and web views (Gecko, Chromium) provide limited implementations: wrap every call, never assume.
- Per-field facts from `EditorInfo`: `inputType` (class, variation = password / email / URI / multi-line, flags = capitalisation, auto-correct, no-suggestions), `imeOptions` (action = go/search/send/done/next, `IME_FLAG_NO_ENTER_ACTION`, `IME_FLAG_NO_PERSONALIZED_LEARNING` since API 26 = learn nothing), `actionLabel`, `packageName`, `initialSelStart/End`. The browser project already treats "no personalised learning" and password fields as "learn nothing" `[R: PageFieldInput.kt]`; keep that rule for ALL learning (words, pairs, shapes, touch offsets, replay log).
- Android 15/16 window behaviour: edge-to-edge is enforced for apps targeting API 35+ `[W]`. For an IME the pattern found in a downstream framework commit was: pad the keyboard's root view by the IME's own navigation-bar inset (`captionBars`) and the system navigation bar (`navigationBars`), and do not shrink the insets that `onComputeInsets` reports below what the framework expects `[W?]` (one commit, not official guidance). TASK KB-107 includes a test on the owner's Android 16 phone in both gesture and 3-button navigation, and reading the official "behavior changes" pages for 15 and 16 first.

### 6.4 The input state machine (replaces `MiniKeyboardModel`)
Principle: **the app's editor is the truth; the keyboard keeps only a short window and a few flags.**
- Snapshot on every start/selection update: up to 64 characters before the cursor, up to 16 after, selection range, the composing region the keyboard owns (if any), caps mode.
- Keyboard-owned state: `composing` (the word being tapped, set with `setComposingText`), `lastGlide` (tail string the glide added, for one-Backspace removal and strip swap), `lastAutocorrect` (original word, for undo), `shift`/`shiftLock`, current page (letters / symbols / emoji / clipboard).
- **Own-edit tracking:** after each edit the keyboard records the selection it expects; an `onUpdateSelection` that matches is ours, any other means the user or the app moved things: clear `composing`, `lastGlide`, `lastAutocorrect` and re-read the snapshot. Without this, Backspace after the user tapped elsewhere would delete the wrong text (this is the main bug class of third-party keyboards).
- Atomic replace (autocorrect, strip swap, undo): `beginBatchEdit`; `deleteSurroundingText(before, 0)`; `commitText(new, 1)`; `endBatchEdit`; verify `getTextBeforeCursor` first and refuse if the expected tail is not there.
- Tap word: letters go to `setComposingText` so the app can underline; space/punctuation/enter ends composition (autocorrect decision happens here). Glide word: committed directly with a trailing space (keep today's behaviour and "smart spacing", including `.com` and punctuation closing the word) and remembered in `lastGlide`.
- Enter: `performEditorAction(imeOptions action)` when the field defines an action and `IME_FLAG_NO_ENTER_ACTION` is not set, otherwise a real Enter key event (the browser's `PageFieldInput.enter()` shows the key-event form).
- Backspace: key events (`KEYCODE_DEL`) for single characters so web pages see key events; `deleteSurroundingText` for glide words and repeat bursts.
- Everything on the UI thread is O(1); decoding runs on worker threads and posts results back.

### 6.5 Threads and processes
- Process 1 (the keyboard): UI thread (drawing, touch, editor calls), `glide` worker (final decode, learning, save), `preview` worker (live guesses, its own decoder instance, at most one in flight, 110 ms cadence: the browser's proven design), IO executor (export/import, journal compaction). Same-thread rules as the browser: **one** thread owns the personal model.
- No second process or service for translation: requests run on one background thread of the keyboard's own process, only when the owner asks (9.2).
- No services other than the IME, no WorkManager, no alarms, no boot receiver. The keyboard does nothing while idle.

### 6.6 Layouts and geometry
- Keep `MiniKeyboardGeometry.boxes` (rows share width by weight, gap between keys). The glide decoder reads key centres from the SAME boxes the view draws, so the model and the pixels cannot disagree. Add `LayoutId` (for example `en-qwerty`, `es-mx-qwerty`), a per-layout row spec, and make `GlideKeys` take a list of letter keys instead of exactly 26 (G2).
- Sizes: portrait key pitch about 56 dp on the tablet; height setting 3 steps; landscape uses 973 px width: offer width cap + dock left/right/centre so a thumb reaches the keys (the browser's resizable/floating panel proved the owner uses one thumb).
- Long-press popups: alternates per key from the language (accents, symbols, digits on the top row). The browser's "hold a top-row letter for its digit" generalises.
- Three row-4 variants: letters bottom row with comma / period / space / enter-action; URL fields (`/`, `.com`); email fields (`@`, `.com`).

### 6.7 Rendering
One custom `View` per page, drawn with `Canvas` (as `KeysView` does): no `RecyclerView`, no XML inflation per key, no animation framework (animators are OFF on the owner's tablet; the browser uses its own clock `[R: HANDOFF.md]`). Pre-compute key rectangles and text baselines on size change, not per draw. Draw the trail from reused `Path`/`Paint`. Dirty-rect invalidation: a key press invalidates its rect, a glide invalidates the trail bounds plus the strip.

### 6.8 Privacy and security design
1. **Permissions are exactly the expected set: `INTERNET` (used by the optional translation only) and nothing else sensitive** (no `ACCESS_NETWORK_STATE`, `RECORD_AUDIO`, `READ_CONTACTS`, overlay or storage permission). CI step: `aapt2 dump permissions` is compared with a fixture. Because the keyboard holds the internet permission, "it cannot send anything" is replaced by the guard rails of 9.2 (one network package, source and dex scans, two hosts, kotlin-stdlib only, one gate, a connection log without the words). Contacts are never read; voice, if ever, is a separate optional APK.
2. `android:allowBackup="false"`, `android:usesCleartextTraffic="false"` with a network security config that allows no cleartext, `android:exported` only for the service, the launcher activity and the translate-menu activity (it must be exported for the text-selection menu).
3. Password-type fields and `IME_FLAG_NO_PERSONALIZED_LEARNING`: no learning, no replay log, no clipboard capture; still type normally; use a plain strip (no suggestions) for passwords.
4. Learned data lives in the app's private directory. The only things that leave are explicit owner actions: Export (file in Downloads) and the optional encrypted auto-snapshot (section 8.9).
5. Logs: debug logging never prints typed text; Diagnostics shows only counters and timings (the browser's `GlideStats.line()` is the model).
6. Signing: a private key in repository secrets (section 11.5), not the browser's public dev key.
7. Clipboard history is kept in memory plus an optional encrypted file; clips marked sensitive (`ClipDescription.EXTRA_IS_SENSITIVE`, API 33) are skipped `[K]`.
8. Crash loops must not leak or lock: crash counter in a file; three crashes within a minute -> switch to the minimal fallback layout (KB-106).

### 6.9 Persistence layout (private files dir)
| File | Content | Writer |
| :- | :- | :- |
| `lex/en.klx` | static word list (KGL1/KGL2) shipped in assets and copied to files on first run (so they can be mmapped and replaced by newer packs) | installer step |
| `personal/<lang>.journal` + `.snap` | personal model: snapshot plus append-only journal, compacted when the journal passes N KB; atomic temp+rename for the snapshot | glide worker |
| `touch/<layout>.txt` | per-key offset statistics | glide worker |
| `own_words.txt` | words the static lists lack | glide worker |
| `diag/*.txt` | counters only | any |
| `replay/*.bin` | opt-in local replay ring buffer (never exported automatically) | glide worker |
| Downloads (MediaStore) | exports/snapshots the owner asked for | export flow |

---

## 7. LIGHT AND FAST ON THE MT8168B

### 7.1 Budgets (targets are proposals `[I]`; every one needs a tablet measurement `[T]` before it is called met)
| Metric | Target | How to measure | Notes |
| :- | :- | :- | :- |
| APK size, English only | under 6 MB (R8 + resource shrink, no AndroidX) | CI step prints and asserts | `en.klx` alone is 2.7 MB `[R]` (about 1.4 MB packed) |
| Translation code (the `translate` package) | well under 100 KB of dex, no native code, no model | CI prints the size | online translator only (9.2) |
| Process memory while the keyboard is up | under 40 MB PSS | `adb shell dumpsys meminfo <package>` | a trivial Android process is already about 15-25 MB `[K]` |
| Memory while hidden | under 25 MB PSS; lexicon scratch and preview decoder freed on `onTrimMemory` | same | |
| First show after boot (cold) | under 400 ms to a drawn keyboard | logcat timestamps `onCreateInputView` to first `onDraw` | lexicon loads on a worker; keys work before the glide engine is ready |
| Key down -> key feedback | within one frame (16 ms) | `dumpsys gfxinfo <pkg> framestats` | no decoder call on the touch path |
| Tap -> text in the app | one frame plus the app's own IPC | | commit the literal letter immediately |
| Glide lift -> word in the text | p95 under 100 ms | the browser's own counter "word shown N ms after the lift" `[R]` | the counter must be rebuilt in the new Diagnostics |
| Live preview cadence | at most every 110 ms, only after 4 new points | | exists |
| Idle CPU | 0 (no timers, no polling) | `dumpsys cpuinfo` | |
| Allocation per frame / per tap | zero on the tap path; glide path allowed a few small arrays | Android Studio-less check: `-verbose:gc`/logcat GC lines in a 5-minute typing test | A53 has no spare cycles for GC |

### 7.2 Techniques, in the order to try them (each is a task or part of one in section 12)
1. **R8 full shrink, no AndroidX, kotlin-stdlib only**; keep the browser's compiler flags (`-Xno-param-assertions`, `-Xno-call-assertions`, `-Xno-receiver-assertions`) `[R]`. Add the profile installer only if a baseline profile is produced.
2. **Baseline profile + warm-up** `[K]`: the browser measured 5 s for the first decode on a software emulator and added a throw-away decode of "the" at start `[R]`. On a real A53 the first glides run in the interpreter until the JIT/AOT catches up. Ship a baseline profile covering the decoder loops, keep the warm-up on a worker thread, and document the developer command `adb shell cmd package compile -m speed-profile -f <package>` `[K]`.
3. **Bitmask pre-filter in the quick pass** `[I]`: store a 26-bit "letters present" mask per word (about 330 KB for 83k words). For a glide compute the mask of keys the path comes within `missLimit` of; a word is eligible only if its mask is a subset of the visited mask. This removes the per-letter loop for most words before any scoring. Expected: several-fold fewer `quickScore` evaluations; measure on the lab before trusting.
4. **Banded DP and early abandon** `[I]`: the careful pass tries every earlier step for every letter (about n^2 x letters). Letters occur in order, so each letter's step can be limited to a window around where the previous letters sit; abandon a word as soon as its partial cost exceeds the 40th best bound. Gate: top-3 identical to the full DP on the whole eval set (allow rare ties).
5. **Per-id arrays for personal counts** `[I]`: `PersonalModel.unigramMass(String)` does string hash lookups per candidate; map word -> lexicon id once, keep `FloatArray` by id. No per-candidate string work.
6. **Trie-shared DP** `[I]` (only if 3-5 are not enough, or for two big languages): words share prefixes, so DP rows can be computed once per prefix node and pruned by a beam (this is how published glide decoders scale). Larger change: require the A/B equality gate.
7. **mmap the cold parts** `[I]`: pair/trigram tables via `FileChannel.map` (file-backed pages are cheap for the low-memory killer to reclaim); keep the hot letter blob and probability arrays in heap arrays because `ByteBuffer` accessors on a 32-bit A53 are slower than array reads (not measured: test it).
8. **Allocation discipline** on the tap path: preallocated arrays, no lambdas per event, no `String` building per key, reuse `Rect`/`Path`.
9. **Draw less:** invalidate dirty rects; no overdraw (flat key fills); no hardware-layer alpha on big areas; key labels drawn with cached `Paint`s. The GPU is weak; the browser already limits itself the same way.
10. **`onTrimMemory`**: drop the preview decoder, scratch arrays, emoji and clipboard caches when the keyboard is hidden for a while; reload lazily.
11. **StrictMode in debug builds** (disk/network on the main thread = crash in CI).
12. **Native code only as a last resort** `[I]`: if after steps 2-6 the p95 glide decode is still over 60 ms on the tablet, port `quickScore` + `dp` to C++ with NEON for armeabi-v7a. Costs: NDK in CI, a second ABI test matrix, and Android 15+ devices require native libraries aligned for 16 KB pages `[K]`. Do not start here.

### 7.3 Evidence rule for any speed change
No optimisation is accepted without (a) the eval harness showing the same first-guess and top-3 accuracy on the fixed seed set (identical or within 0.1 point), and (b) a before/after timing on the tablet from the Diagnostics screen. Record both numbers in `research/PERF.md` in the new repo.

---

## 8. THE LEARNING ENGINE, VERSION 2 (the core of the owner's request)

### 8.1 What the owner asked for, in their words and as measurable goals
Owner (this request): "the more I type, the better it is at predicting the correct words based on both more commonly typed words with similar finger glide patterns, commonly typed words in general, smart predictions based on the context of the sentence already typed so far." Earlier (browser project, message 38): "I often type 'for' yet Gboard inputs 'fir' ... 99 % of the time I make that pattern I am trying to type 'for' ... I want this keyboard to learn intuitively ... and my corrections."

Turn the wish into numbers that the Diagnostics screen and the lab can report (section 8.5):
| Goal | Metric | Today (simulated, browser) | Target `[I]` |
| :- | :- | :- | :- |
| Habitual glide words become reliable | first-guess accuracy on words written 6+ times | simulated normal finger, no habit: 78.0 % nothing learned -> 94.6 % words/pairs/corrections -> 99.9 % with shapes; at a strong 16 dp habit offset: 34.3 -> 61.6 -> 84.6 % `[R: GLIDE_TYPING 11.3]` | >= 97 % on the owner's real top-300 words after 2 weeks of use |
| Words in general | first guess / top-3 over everyday text | 90.5 / 96.9 % normal finger (sim) | +2 points first guess on a full-width keyboard from key size alone; further gains from this section |
| Context | next-word prediction: keystroke saving and top-3 hit rate | not measured (no tap/next-word mode) | beat a plain bigram baseline by a margin the lab states (target: +10 % relative top-3 hit rate) |
| Bad learning is rare | autocorrect revert rate, "learned junk" count | not measured | revert rate under 5 % of autocorrects; zero junk words from stretched/noise input |
| Never slower | word shown after lift, p95 | not measured on tablet | p95 under 100 ms |

### 8.2 The scoring model (what the decoder maximises)
For a glide with path `g`, previous words `c` (context), user `u`, field class `f`, language set `L`:

```
S(w) =  - Cpath(w | g, touchModel_u, layout)               spatial cost (DP over letters; section 8.3.4 changes where key centres sit)
        + lam * ln P_lm(w | c, f, L)                        language: global n-gram  +  personal n-gram  +  recency  (8.3.1-8.3.3)
        + Lshape(w, g, u)                                    "this word drawn like this" evidence (existing PatternMemory, 8.3.4)
        + Lcorr(w, firstGuess, u)                            "A was corrected to B" (existing, capped)
        + Ltopic(w, c)                                       sentence/topic similarity (new, 8.3.3)
        + Llang(w | previous language tags)                  bilingual prior: DEFERRED, the owner will not type Spanish (8.3.6)
lam = clamp(1.5 * best Cpath per letter, 0.12, 1.2)           (existing adaptive weight: sloppy glide trusts language more)
```
Every term has a ceiling so that one signal cannot overrule the finger (the browser's rule: "the path cost still decides first; the language only weighs in when two words fit"). Taps use the same terms with a different spatial part (8.3.5).

### 8.3 Components

#### 8.3.1 Global language model v2 (KGL2)
Problem: today's list is 83k bookish words with unigram + bigram counts (Google Books via SymSpell lists) `[R]`. Next-word prediction and "smart context" need more.
- **Add trigrams** (and sentence-start `<s>` context) stored compressed. Typical on-device layout `[K]`: quantised log-probabilities in 8-16 bits, back-off weights, sorted arrays with prefix ranges (a KenLM-style trie is the reference design); budget 5-10 MB per language (Gboard states 5-10 MB for its decoder LM `[R: GLIDE_RESEARCH section 6]`). Measure the real size/speed trade: prune trigrams with count < k.
- **Better text sources for modern typing** (record every licence in `docs/DICTIONARY.md`): the SymSpell lists (MIT) stay as the base; add conversational/web corpora: OpenSubtitles-derived frequency lists (the `hermitdave/FrequencyWords` content is CC-BY-SA-4.0 per its README `[W]`; derived data inherits share-alike, acceptable for a personal non-commercial app but keep the notice), Wikipedia text (CC BY-SA `[K]`), news/web crawls (check each licence). The browser's rule applies: personal, non-commercial use allowed, licence recorded, notice travels with the data `[R: CLAUDE.md]`. Treat downloaded corpora as untrusted data: parse, never execute; keep them outside the repo; only the compiled list + notices go in.
- **Capitalisation & forms:** store the display form separately (the format already has `display` entries): "I", "iPhone", "McDonald's", sentence-start capitalisation, proper-noun flag (a word capitalised mid-sentence in the corpus). Autocapitalise from this plus the field's caps mode.
- **Frequency hygiene:** drop numeric/noise tokens; keep contractions (`don't`) with apostrophe-optional matching (`dont` -> don't); Gboard's lexicon also makes the apostrophe optional `[R: GLIDE_RESEARCH G2]`.
- **Compiler:** extend `GlideLexiconCompiler` (UTF-8, language tag, trigram input, pruning flags, size report). The format change is KGL2; the reader must also read KGL1 so the shipped English list keeps working until rebuilt.

#### 8.3.2 Personal model v2 (what the owner's typing teaches)
Keep what works (`PersonalModel` caps: 4000 words, 8000 pairs, 6000 triples, 3000 corrections; halving at total weight 6000; recency memory with 0.992 fade) `[R]`. Change:
1. **Learn from taps and glides alike** (today taps are learned only at "Go"): learn when a word is *committed* (space/punctuation/enter) and not reverted.
2. **Two-use rule** `[R: AOSP, GLIDE_RESEARCH O11]`: a new word may appear in the glide vocabulary immediately (names must be glideable after one use, as the browser does), but is only *suggested or used as an autocorrect target* after two uses.
3. **Forget by calendar time, not only by word count** (AOSP's forgetting curve: levels decay about every 15 days `[R: GLIDE_RESEARCH]`): weights decay slowly over months; a word untouched for 6 months drops out of the tables first when they are full.
4. **Tables keyed by a language/layout id** (only `en` exists now) so a language could be added later without a migration.
5. **Case forms:** remember "Juanita", "iPhone", "NASA" as typed.
6. **Noise guards** (existing `stretched` rule: no letter three times in a row), also: no tokens with digits, `@`, `/`, `.` (URLs, emails, handles) ever enter word tables; no one-letter tokens except as context.
7. **Per-id arrays** instead of string hash lookups in the decode path (speed, 7.2 item 5).
8. **Journal persistence** (6.9): append events, compact on idle, keep the v1 text format readable for import.
9. **Field-class affinity (optional, small):** counts per (word, field class: chat, search, mail, notes, URL, name) with a low weight; evaluate before keeping (8.5).

#### 8.3.3 Sentence-context features (the "smart prediction from the sentence so far")
Three cheap layers on top of n-grams. Each is a flag that only stays on if the lab proves a gain.
1. **Sentence position & punctuation state:** `<s>` context for the first word after `. ? !` or a new line; after `,` and `:`; after an opening quote. Also drives capitalisation.
2. **Topic vector** `[I]`: static word embeddings (d = 24-32, top 30k words, int8: about 1 MB) built offline by the tools module with PPMI + truncated SVD on the corpus (no neural training needed). At runtime keep an exponentially decayed vector of the last ~8 committed words (`c = sum gamma^i e(w_{t-i})`) and add `beta * cos(c, e(candidate))` for content words (stop-words excluded, so "the" is not lifted by topic). Cost per candidate: d multiply-adds; 200 candidates x 32 = trivial on an A53. This is what lets "doctor ... prescribed the ___" prefer "medication" over "meditation" when both fit the finger path, which bigrams cannot do. Also usable for next-word re-ranking: take the top 50 n-gram successors and re-score with the topic term.
3. **Language tags of the previous 2-3 words** (bilingual typing, 8.3.6).
4. *Optional stretch (only after 1-3 are measured):* a tiny character-aware recurrent LM (about 1-2 M parameters, int8, a few ms per rescoring of the top 5 candidates after commit). Google's own measurement: a 6.4 M-parameter neural LM gave only 0.26-1.19 % fewer modified words for 17-28 % more latency `[R: GLIDE_RESEARCH G4]`; do not build it unless the topic vector and n-grams plateau.

#### 8.3.4 Spatial model: per-key touch model + shape memory that transfers
Today: one global thumb offset (`biasX/biasY`, 12 % step per kept glide, max 0.6 pitch) and per-WORD shapes (up to 4 shapes x 1200 words) `[R]`. The browser's own notes say the shape of "for" does not help "four" `[R: GLIDE_TYPING 11.4]`. Plan:
1. **Per-key offset table** `[I]`, hierarchical shrinkage: `mu_k = (n_k * mean_k + m * mu_cluster(k)) / (n_k + m)` with cluster = one of ~7 key regions (what Gboard used: 7 clusters from a greedy split, last 800 taps `[R: GLIDE_RESEARCH G3]`) and global at the root; `m` about 20-50 (published minimums: 50 points per key-user in Yin et al., about 60 touches in Buschek et al. `[R: GLIDE_RESEARCH A5, A9]`). Clamp to 0.6 pitch. Exponential forgetting with a half-life of about 1500 taps. Separate tables per layout id and per posture (portrait/landscape; later one-handed).
2. **Where the evidence comes from:** (a) taps: the touch point versus the committed letter's key, only for words that were accepted unchanged (Gboard trains only on tap-typed words matching the final text `[R: G3]`); (b) glides: the touch-down point is a clean sample for the first letter and the lift point for the last letter (people aim the ends carefully, SHARK2 weights ends higher `[R: GLIDE_RESEARCH A1]`); do NOT use mid-path samples as key offsets (corner cutting biases them).
3. **Use in the decoder:** replace the key centre arrays `kx, ky` in `GlideDecoder` by `centre + mu_k` (a few lines; this is the elegant part: the DP, the quick pass and the second look all follow), keep the global second-look bias for what remains. In the tap decoder the same offsets shift the Gaussian per key (8.3.5). Published gains are small but real: Gboard measured -0.05 % to -1.9 % modified-word ratio and +0.2-0.6 % WPM at 90M+ words per arm; Yin: -13 % character error with user+key models; no study moves tap offsets to glides `[R: GLIDE_RESEARCH section 4]`. So treat the glide benefit as unknown `[T]` and the tap benefit as modest but cheap.
4. **Segment templates (transfer between words)** `[I]`, research-grade: from the DP alignment of each accepted glide, collect for every ordered key pair (a, b) the mean lateral deviation of the path from the straight line a->b (at the middle and at three quarters) and the overshoot beyond b. In the DP, replace the straight segment between consecutive letters by a two-piece polyline through the learned mid-point when `n(a,b)` is high enough, shrunk to straight with `n/(n+k)`. Result: the "o lands toward i" habit learned on "for" also corrects "form", "fork", "four", because they share f->o and o->r. Risk: wrong alignments reinforce errors, so learn only from kept glides with a good fit. Gate: must beat the per-key offsets alone in the lab.
5. **Whole-word shapes stay** (they are exact and proven in simulation) but become **layout-aware**: store the layout id; a v2 export line `s2 <layout> <word> <n> <32 numbers>`. Shapes recorded on `en-qwerty` are used only when that layout is active (G12).
6. **Collision sets** `[I]`: precompute for each word its neighbours in shape space (words whose ideal paths are within about 0.5 key: for/fir/fur/fit, feel/fell, tip/top). Show them to the learner: a correction inside a collision set is the strongest signal (existing `confusions` generalised), and the strip can prefer showing the habitual member of the set second even when its fit is slightly worse. Cheap, computed offline by the compiler from the lexicon and stored as ids.

#### 8.3.5 Tap decoder and autocorrect (new; phase 2)
Same lexicon, same personal model, different spatial term.
- **Spatial likelihood per tap:** for the touch (x, y) and each key k: `ln p = -0.5 * ((x - cx_k - mu_kx)^2 + (y - cy_k - mu_ky)^2) / sigma^2` with sigma about 0.4 pitch at first, learned per key later. Only the nearest few keys matter.
- **Search without a trie build:** the word list is stored sorted by key sequence, so every prefix is a contiguous id range (`BinaryLexicon.lowerBound` already exists). Beam search over prefix states `(lo, hi, cost)`; extend by the next letter with a binary search inside the range; keep the best 20-30 states; operations: match (cost from the tap), substitute with a neighbouring key (the proximity cost is already in the Gaussian), insert, delete, transpose adjacent letters (small fixed penalties). Completions = words under the best prefix states ranked by P(word | context). All arrays preallocated: this runs after every tap on an A53.
- **Policy** `[R: GLIDE_RESEARCH section 5/6 rules; K]`: autocorrect on space/punctuation only when (best candidate score - typed token score) exceeds a threshold and the typed token is not a known word (global or personal) unless the margin is large; never autocorrect inside URL/email/password fields, after a number, for a capitalised mid-sentence word that is unknown (probably a name); Backspace immediately after an autocorrect restores the typed token, learns it (weight 2) and records "do not correct X to Y" (a decaying counter). A word the owner reverted twice becomes a known word.
- **Missing-space / extra-space** repair is a later refinement (phase 2 or 3).
- **Suggestions strip content:** 3 slots: while typing: [typed token, best correction, best completion]; after a space: next-word predictions; after a glide: the 3 glide candidates (existing behaviour). Same probabilities as the glide, so the two modes learn from each other.

#### 8.3.6 Multilingual decoding (English + Mexican Spanish at once) - DEFERRED: the owner will NOT type Spanish (2026-11-16). Do not build. Kept only as a design note for a language that might be added one day
- Decode against all enabled languages in one pass with one prior per language: `ln P(w) = ln P_lang(w) + ln P(lang | previous language tags, user's language mix)`. Language tags come from lexicon membership of the last 2-3 committed words; the user's overall mix is a slowly updated count.
- Cost control: shared bitmask pre-filter (7.2 item 3); candidate budget per language proportional to its prior; second language's survivors evaluated only if the first language's best fit is poor or the prior favours it.
- Layout: accents are typed on the base letter (long press) and restored by the language model; Spanish adds an n-with-tilde key (a 27th key, G2). Glide keys are the base letters; the folded key sequence of "mas"/"mas with accent" is the same, the lexicon holds both display forms with different probabilities and the context decides (G1).
- Avoid letting one language's rare short words steal the other language's common ones: keep the per-language unigram scales comparable (normalise each list to its own total, as `UserWords.lexicon(total)` does for the personal list `[R]`).

#### 8.3.7 Online re-ranker (optional, phase 4 last)
A pairwise logistic model over a handful of features per candidate (spatial cost, per-letter cost, shape evidence, global LM, personal unigram/pair/triple, recency, topic, length, first/last-key distance, is-own-word). Update only on strong labels (strip pick over the first guess, retype after delete), `w += eta * (phi_chosen - phi_first)`, pull toward the hand-tuned defaults, clip each weight to 0.5x-2x of default, cap drift per session, reset button. The browser project listed this as "weak evidence, noisy labels" and did not build it `[R: GLIDE_RESEARCH item 3]`. **Build it only if the replay/shadow lab (8.5) shows measurable headroom that the hand-tuned terms leave**; otherwise skip and keep the explainable terms.

### 8.4 What the keyboard learns from, and how (labels)
| Event | Meaning | Learns |
| :- | :- | :- |
| Glide word kept (next word or punctuation follows, no Backspace within the retry window) | first candidate right | word +1, pair +1, triple +1, shape +1, first/last-letter touch samples, thumb habit step |
| Strip pick over the first guess | chosen right, first wrong | chosen +3 (word, pair, triple), correction (first -> chosen) +1, shape +3 for chosen and the lookalike shapes lose 2x similarity |
| Backspace right after a glide, then a different glide/word within 30 s | deleted word wrong, replacement right | replacement +3, correction +0.5, shape of the DELETED glide goes to the replacement if the straight-line shape of the replacement is within 1.15 key (existing rule `FIT_D`); deleted word not learned |
| Tap-typed word committed unchanged | right | word/pair/triple +1 (two-use rule before it may be suggested), tap offsets from the touch points |
| Autocorrect applied and not undone | weak evidence | the corrected word +1 only |
| Autocorrect undone with Backspace | the typed token was meant | typed token +2 and "never correct X to Y" counter; corrected word -1 (floor 0) |
| Same word deleted and typed again in the same position | the app/user is editing | learn nothing about the first attempt |
| Cursor moved by the user, text changed by the app (selection mismatch) | state unknown | drop pending learning events |
| Password field / no-personalised-learning flag / incognito toggle / replay disabled | private | learn nothing, log nothing |
Noise guards (stretched letters, tokens with digits/symbols) and caps apply to all rows. All weights shown are the browser's proven values `[R]` except the tap rows, which are new `[I]`.

### 8.5 Evaluation and the replay/shadow lab (this is what turns "feels better" into numbers)
The browser's biggest honest limitation: all accuracy numbers come from a seeded simulated finger; no real glides were ever recorded because the repo is public `[R: GLIDE_RESEARCH #5, GLIDE_TYPING 3]`. Fix it in three layers, none of which puts personal data in git:
1. **On-device counters (always on, no content):** per rolling window of 200 events: glides, kept / picked / deleted-right-away rates, retype rate, autocorrect applied / undone rates, suggestion-tap rate, next-word hit rate, decode and "word shown after lift" timings, learned word/pair/triple/shape counts. This is the existing `GlideStats` idea extended. Shown in Diagnostics with a Copy button (numbers only).
2. **Shadow decoding (on-device A/B, no data leaves)** `[I]`: an experiment flag runs a second parameter set / model variant on the same glide in the background; the UI always shows the baseline. After the person's final choice (kept, picked, retyped) the keyboard counts which variant had the final word first. Diagnostics prints "variant B would have been first N times vs A M times over K events". Costs one extra decode per glide only while an experiment is on. This lets the owner's real thumb judge a change without exporting anything.
3. **Opt-in local replay log + the `:eval` runner** `[I]`: a ring buffer (last 3000 glides) in the private directory: path points, timestamps, candidate list, final word, label (kept/picked/retyped). Off by default. Because it contains typed words, "Export replay file" writes it to Downloads only on an explicit tap; the owner attaches it to a Claude session (NOT pasted into chat: the save-ladder hooks copy chat text into the repository, see 11.6); the session processes it in its scratchpad outside the repo and commits only numbers. Use it to (a) calibrate `GestureSim`'s noise parameters to the owner's real finger, (b) tune `GlideParams` and the new weights, (c) measure the real value of each new feature, (d) catch regressions.
**Sample-size honesty** `[R]`: seeing a 1-point gain at 90 % when 3 % of glides differ needs about 2,350 paired glides; unpaired about 13,500 per arm. Shadow decoding is paired, which is why it is the main tool.
**Gates (a change merges only if):** the seeded simulator shows no first-guess or top-3 regression above 0.3 points across the calm/normal/sloppy/bad fingers and the three word mixes already in `GlideEval`; unit tests pass; timing on the tablet within budget; and, for learning features, the `GlideEval curve=1` style learning curve is not worse and `GlideShapeLearningTest`-style claims hold. Features with unproven real-world value ship OFF behind a Settings > Experiments switch until shadow data supports them.

### 8.6 Cold start
- **Import the browser's learning file** (`KESTREL-KEYBOARD-LEARNING 1`): `LearnedExport.read` already parses it; merge rule = larger count wins; thumb habit taken only if the keyboard has none; v1 shapes are in `en-qwerty` units so they load into the `en-qwerty` layout only `[R]`. The owner exports it from the browser (Settings > Export what the keyboard learned) and picks it in the new app. **KB-205 must do this before the owner switches default keyboards.**
- **"Teach from text":** owner picks a plain-text file (their own notes, exported messages) and the keyboard learns its words, pairs and triples with a bounded weight and the usual noise filters; an instant personalisation that does not need weeks of typing. Local file picker, no permissions.
- Page/site names and titles: the browser feeds visited sites' words to the glide vocabulary (`UserWords.wordsOfTitle`, `wordOfHost`) `[R]`; a keyboard has no such source. Instead: names of the owner's own contacts are NOT read (no permission); the "Teach from text" file and ordinary typing cover it.

### 8.7 What to expect, honestly
- The simulated numbers above (to 94-98 % on habitual words) assume a person who repeats their own phrases and a finger whose habit is stable; a real thumb may be less repeatable and the gains then shrink toward the "no habit" rows, "still positive, never negative in the lab" `[R]`.
- Full-width keys alone should help glide accuracy; the repo's size table suggests about +2 points first guess for +25 % key width at a loose finger `[R]`.
- The per-key touch model's published tap gains are small; segment templates and the topic vector are unproven for keyboards. They are cheap experiments with a clear gate, not promises.
- Spanish and bilingual quality depends mostly on the dictionary data, not on the decoder.

### 8.8 Forgetting, caps, privacy of the model
Caps stay (they bound memory and speed): 4000 words / 8000 pairs / 6000 triples / 3000 corrections / 1200 shape words / 3000 shapes / 800 own words per language, raised only if the tablet's timing allows. "Forget what the keyboard learned" deletes tables, shapes, touch model, own words, replay log, snapshots (and says so). A dictionary manager (phase 3) lists learned words with per-word delete.

### 8.9 Persistence, export v2, automatic snapshot
- **Export v2** (keep reading v1): UTF-8 (accented letters), header `KESTREL-KEYBOARD-LEARNING 2`, a `layout <id>` and `lang <code>` line before each block, `s2` shape lines, `touch <layout> <key> <n> <dx> <dy>` lines, `case <word> <form>` lines, the same `end <n>` check and the same bounds. Write it in `docs/KEYBOARD-EXPORT.md` section "Version 2" with an example that a unit test reads (the browser's trick that keeps docs from going stale).
- **Optional passphrase encryption** of the export with platform classes only (PBKDF2 + AES-GCM through `javax.crypto`, no dependency) `[K]`, because a system keyboard's learning file lists words typed in every app.
- **Automatic snapshot (opt-in):** once a week write `Swoop learning snapshot <date>` to Downloads via MediaStore and keep the last 3. The browser notes files created that way survive an uninstall `[R: KeyboardFlow, GLIDE_TYPING 8]`, so a reinstall (for example after a signing-key change, 11.5) does not erase months of learning. Default off, with the privacy warning shown at the switch.

---

## 9. TRANSLATION TO MEXICAN SPANISH: LinkMeter's service and method, inside the one keyboard app (a Gboard-style toolbar toggle, OFF until the owner turns it on; online; the owner never types Spanish)

### 9.0 What the owner asked for (2026-11-16) and the one-paragraph answer
Round 1: "Friendly tu. Mexican Spanish translations only, do not translate offline. Use the same service that my LinkMeter app uses (read the repo) and also the same method: press Enter and it automatically translates, then press Enter again and it sends. Also make it so I can long press Spanish messages and translate them to English just like the LinkMeter app does." Round 2: "Put everything into one keyboard app. Translate can be in any app but only if I turn on translate, just like on Gboard. [Own Send button] is ok. I want the easiest way to translate to English, fewest presses. Do not change LinkMeter."
**Answer:** the owner's LinkMeter app already contains this feature (`docs/TRANSLATE.md`, `Translation.java`, `Translator.java`, `TranslateFlow.java`, section 3.3). The keyboard ports its pure logic unchanged into `:core` and runs the web requests from a `translate` package inside the one keyboard app. A Translate button in the keyboard's toolbar turns the mode on, in any app, like Gboard's translate button `[W]`. While it is on, "Enter translates, Enter again sends" is LinkMeter's method (9.3), with one platform limit the owner accepted: a keyboard can press the Enter key but not another app's own Send button. Received Spanish goes to English by the route with the fewest presses (9.4). LinkMeter is not touched.

### 9.1 The service, exactly as LinkMeter uses it `[R]` (details of `docs/TRANSLATE.md`, tried with real requests there on 2026-11-12)
1. **MyMemory first** (`api.mymemory.translated.net`, by Translated; free, no account): `GET https://api.mymemory.translated.net/get?q=<text>&langpair=en|es-MX&onlyprivate=1&mt=1` (English to Mexican Spanish) and `langpair=es-MX|en` (the other way). Limits found by LinkMeter: **5,000 characters a day** anonymous, **at most 500 bytes per question** (LinkMeter cuts texts at 450 bytes). `onlyprivate=1` is essential: without it a sentence that a person once translated by hand beats the machine translation, and those are often program buttons ("Take it" came back as "Aceptar", "Can you drive?" as the formal "¿Puede conducir?"); with it LinkMeter got "Tómalo" and "¿Sabes conducir?" (friendly tu). With `es-MX` it writes Spanish as spoken in Mexico (papas, mesero, rentar, celular, departamento, tenis, alberca, chamarra), keeps emoji, links, phone numbers, times and line breaks, and turns "u coming 2nite? idk if i can make it tbh" into "¿Vienes esta noche? La verdad, no sé si voy a poder". The other way it understands Mexican slang ("qué onda güey ... ahorita no puedo, estoy chambeando" -> "What's up, dude? ... I can't right now, I'm working."). A used-up allowance is recognised (HTTP 429, `quotaFinished`, a text that starts with "MYMEMORY WARNING", `responseStatus` 429) and is never taken for a translation; HTML entities in the answer are written out.
2. **Google Translate when MyMemory is used up, busy or unreachable** (`translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t`, and `sl=auto&tl=en` for English; free, no account, no daily allowance). The text goes in the body of a POST (`q=...`, a long text in the address is refused). Its Spanish is neutral with words from Spain (patatas, camarero, coche, nevera, zapatillas, chaqueta, móvil...) and it needs texting shorthand written out first (`expandShorthand`: u, r, k, idk, tbh, ngl, imo, btw, lmk, wyd, hbu, brb, nvm, omw, 2nite, tmrw, rn, b4, gr8, bday, ppl, bc/cuz, w/, w/o and more). It may answer with a redirect to `google.com/sorry` (a picture puzzle) or HTTP 429/503: handled as "busy". **This is an unofficial endpoint** (the one behind Gboard's translation) `[K]`: it can change or block without notice; the plan treats it as a fallback, not as a guarantee.
3. **The safety layer `toMexican`** is applied to EVERY translation into Spanish, whichever service wrote it (9.6).
4. **Pieces:** a longer text is cut at a line break, else after a sentence, else between words, else between letters (never inside an emoji); the pieces put back together give the text exactly; pieces without letters (emoji, numbers) are not sent; spaces and line breaks around a piece stay where they were.
5. **Order and failure reasons in words:** MyMemory, then Google; if both fail the most helpful reason is shown ("The tablet is not on the internet ... is the phone's mobile data on?", "Both free translation services are busy or used up for today. Try again later, or send your English.", "The translation service did not answer in time.", and so on). Timeouts: 8 s to connect, 15 s to read; answers over 256 KB refused; redirects are NOT followed; one question at a time on one background thread; each question logged as service, time and result, never the words.
6. **What the daily allowance means in practice** `[I]`: 5,000 characters a day is roughly 50-100 ordinary text messages on MyMemory; after that Google answers. Over a carrier hotspot the address is shared with other customers, so the allowance may run out earlier than expected (LinkMeter's notes record Google's puzzle after about five questions from a shared sandbox address). MyMemory is documented to raise its allowance when an e-mail address is sent with the request `[K]`, but LinkMeter does not do it and the owner's address must not be sent to a third party without the owner's explicit decision: not planned.
7. **Not built in LinkMeter and not planned here:** translating received messages automatically as they arrive (each would use the allowance), and a large-language-model translation (it would need an API key and cost a little per text).

### 9.2 Architecture: one app, network confined by guard rails (owner's Q15)
- **`:core` (pure Kotlin, no Android):** `translate/Translation.kt` (Way, Answer, pieces, URLs and parsers, shorthand, `toMexican` and its verb rules), `translate/TranslateFlow.kt` (the Enter state machine), `translate/Json.kt`. Ported from LinkMeter with the same behaviour and the same tests (3.3). Pure logic: it builds URLs and parses answers but never opens a connection.
- **`:ime`, package `app.swoop.keyboard.translate` (the ONLY code allowed to touch the network):** `Translator` (ported glue: MyMemory then Google through `HttpURLConnection`, one background thread, timeouts 8 s / 15 s, answer cap 256 KB, redirects not followed, user agent `Swoop`), a **connection log** (service, milliseconds, result; never the words; shown in Settings > Translation and counted in Diagnostics as "translation requests today"), **Test the connection** (sends the single word "Hello" once), and the **text-selection menu activity "Translate to English"** (9.4). No bound service, no second process, no companion app, no boot receiver, no alarms, no notifications, no background work: the code runs only when the owner presses Enter with the toggle on, or asks for an English translation.
- **Guard rails that replace "the keyboard has no internet permission" (CI enforces every one; each is a task in KB-601):**
  1. The manifest's permission list is EXACTLY the expected set: `INTERNET` and nothing else sensitive (no `ACCESS_NETWORK_STATE`, `RECORD_AUDIO`, `READ_CONTACTS`, overlay or storage permission). `aapt2 dump permissions` is compared with a fixture.
  2. A source scan fails the build if any file outside `translate/` mentions `java.net`, `javax.net`, `android.net`, `okhttp`, `Socket`, `HttpURLConnection`, `URL(` or an `https://` literal; and a dex scan checks the same on the compiled classes.
  3. Exactly two hosts: `api.mymemory.translated.net` and `translate.googleapis.com`. A unit test builds every URL the code can produce and asserts the host; a test greps the package for other host names.
  4. Dependencies: kotlin-stdlib only (the dependency report is compared with a fixture): no SDK that could phone home.
  5. `android:usesCleartextTraffic="false"` and a network security config that permits no cleartext at all.
  6. ONE gate, `TranslateGate.mayTranslate(editorInfo, toggle, exclusions, ...)`, is the only way a request can start from typing, and a test proves it refuses password, URL, e-mail, number, phone and search fields, private fields, excluded apps, an OFF toggle, and an empty or over-long text. A second gate `TranslateGate.explicitRequest` covers the owner's explicit taps (menu entry, chip, selected text). A test asserts `Translator` has exactly these callers and no others.
  7. Nothing is sent unless the owner pressed Enter with the toggle on or tapped an English-translation action. The connection log and the Diagnostics counter make any unexpected traffic visible; Android's own Settings > Network > Data usage for the keyboard app shows it too `[K]`.
  8. Release signing uses the private key (11.5), so no one else can ship a different build over this one.
  9. Review rule for sessions: any change under `translate/`, in the manifest or in the dependencies is reported to the owner in plain words in the same reply.
- **Memory and speed:** no model, no native code; the translate package adds well under 100 KB of dex (7.1). Requests are one at a time on a background thread; the UI thread never waits.
- **Toggle OFF:** the Translator is never created, nothing is bound, nothing is sent. The INTERNET permission is present but unused; the guard rails above are what make that checkable.

### 9.3 The toolbar toggle and "Enter translates, Enter again sends"
**The toggle (like Gboard's translate button `[W]`):** a **Translate** button on the keyboard's toolbar. It starts OFF and stays OFF until the owner taps it. The first tap shows the explanation (9.5) with [Turn on] [Cancel]. While on: the button is lit, the Enter key carries a small "ES" mark, and the strip says what Enter will do. Tap again to turn it off. It is remembered across apps until the owner turns it off; Settings > Translation > "Turn translate off by itself": never (default), when the keyboard closes, after 10 minutes, after 1 hour. It works in ANY app and any ordinary text field; it never applies to password, visible-password, web-password, URL, e-mail-address, number, phone and search fields (`IME_ACTION_SEARCH`), to fields that set no-personalised-learning (private/incognito) unless "Also in private fields" is switched on, or to apps in "Never translate in these apps" (pre-filled with `com.tj.linkmeter`: LinkMeter already translates by itself and has its own Send button, and translating twice would turn Spanish into garbage; the owner can remove it).

**The flow** is a port of `TranslateFlow` (3.3), the same states and rules:
| State | Enter does | Why |
| :- | :- | :- |
| toggle on, nothing pending | read the field text, start a translation (`start`), show "Translating into Spanish (Mexico)..." | LinkMeter: the first press translates |
| translating | nothing | LinkMeter: `NOTHING` while an answer is on its way |
| answer arrives and the field still holds exactly the English that was asked | replace the whole field text with the Spanish (one batch edit), remember the English, show the check strip | LinkMeter: the Spanish goes into the box for the owner to check; nothing is ever sent that was not seen |
| answer arrives but the field changed meanwhile, or it failed | change nothing; show the reason ("Your text changed while it was being translated. Press Enter to translate it again." / the failure text) | LinkMeter |
| checking (Spanish in the field) | second press: see the table below; a word the owner changed first is sent too | LinkMeter: "Send again sends it, also with a word you changed" |
| checking, the field is emptied | the check is over; the next text is translated again | `boxChanged` |
Chips while checking: **Undo** (puts the English back) and **Send my English** (restores the English and performs Enter without translating). A failure sends nothing and offers Send my English. Strings are LinkMeter's with "Enter" for "Send":
- idle hint: "Your texts here are translated into Spanish (Mexico). Press Enter to see the Spanish before it goes."
- while asking: "Translating into Spanish (Mexico)..."
- checking: "This is the Spanish (Mexico), by MyMemory. Check it, then press Enter to send it." and below it "Your English: "...""
- failure (amber): the reason + " Press Enter to try again."

**Text rules:** the text must contain letters; at most 1,000 characters (longer: "Too long to translate here. Shorten it or send it as it is."); the English the keyboard reads is the whole field (text before plus after the cursor).

**What the second Enter does (owner accepted the limit, Q17):**
| Where | First Enter | Second Enter |
| :- | :- | :- |
| A field whose keyboard key is a Send / Go / Done action (many chat boxes) | translates | performs that action: the message goes |
| A multi-line box with its OWN Send button (most texting apps) | translates; the keyboard does NOT insert a new line | the keyboard cannot press the app's button: the strip says "The Spanish is ready: tap Send in the app." The owner taps Send. |
| Apps where Enter itself sends (some web chats) | translates | only with Settings > Translation > "In boxes with no Send key, the second Enter presses Enter" (global, default off): a real Enter key press goes to the app |

**Mechanics (EditorBridge, 6.4):** read with `getTextBeforeCursor` + `getTextAfterCursor` (bounded); remember the English; on the answer verify the field text is unchanged, then in one `beginBatchEdit`/`endBatchEdit`: `deleteSurroundingText(before, after)` and `commitText(spanish, 1)`; Undo does the same with the English. A selection change the keyboard did not make while checking keeps the check alive (the owner may be fixing a word). Leaving the field, the app, or turning the toggle off forgets everything and ignores a late answer (`seq` number, as in LinkMeter).

### 9.4 Spanish messages the owner receives, into English: the fewest presses
In LinkMeter the long press works because LinkMeter draws the message list itself. A keyboard only exists while a text field has focus and Android gives it no access to other apps' bubbles. These are the routes, counted in presses (a press = one tap or long press by the owner); all use the same service (MyMemory `es-MX|en`, then Google `sl=auto&tl=en`):
| Route | Presses | Works where | Notes |
| :- | :- | :- | :- |
| **A. Text-selection menu**: long-press the message, tap "Translate to English" in the selection menu; a small panel shows the English at once | **2** (3 if Android puts the entry under the menu's overflow "more" button) | apps and web pages that show Android's standard selection menu | This is the floor without an accessibility service, and it matches LinkMeter's long press + menu item. Android decides where custom entries sit in the menu `[K]`. Always available (an explicit act), independent of the toggle. |
| **B. Copy, then the keyboard chip**: long-press, tap Copy in the app's own message menu, open the keyboard (tapping the reply box, which the owner does anyway), tap the chip "Translate copied message to English" that appears by itself | **3** (+ the reply-box tap that happens anyway) | any app that has "Copy" on a message | The chip appears only while the toggle is ON, and only when text copied in the last 2 minutes does not look English (letters such as ñ ¿ ¡ and accented vowels, common Spanish words). The keyboard reads the clipboard locally to decide (allowed for the active keyboard on Android 10 and later `[W]`); the text is sent only when the owner taps the chip. |
| C. Reference: Google's own Tap to Translate | 3 (copy, then tap its floating bubble) | any app | Needs the "draw over other apps" permission `[W]`; we do not copy it. |
| D. Accessibility service: one long press anywhere | 1 | any app | NOT built: it must read other apps' screens, is heavy on a 2 GB Go tablet, invasive, and Android restricts it for sideloaded apps on newer versions `[K]`. Option Q20. |
| E. Selected text in the box being typed | 2 (select, tap the toolbar's Translate-selection button) | the box the owner is typing in | text that looks Spanish goes to English, anything else to Mexican Spanish |
**Result panel (A) and chip (B):** "English, by MyMemory: ..." with Copy and Close; kept in memory only, never saved (as in LinkMeter); tap outside to close. The selection-menu activity is a translucent activity in the same app (intent filter `android.intent.action.PROCESS_TEXT`, category DEFAULT, mime type `text/plain`, exported, label "Translate to English", read-only) `[W]`. Whether Kestrel G (GeckoView) shows such menu entries is unknown `[T]`.
**Acceptance (KB-603): count the presses on the tablet in the apps the owner names**, and write the result as a table "app -> best route -> presses" in `research/TRANSLATE_RESULTS.md`, so the owner knows what to use where. Each received-text translation spends the same daily allowance as outgoing ones (9.1 item 6). A long press inside the Kestrel browser belongs to that other project and can be asked of it separately.

### 9.5 Settings and the first-time explanation
Settings > Translation: **Show the Translate button** (default ON; OFF removes the button, the menu entry and the chip entirely); **Turn translate off by itself** (never / when the keyboard closes / 10 minutes / 1 hour); **Also in private fields** (off); **In boxes with no Send key, the second Enter presses Enter** (off); **Never translate in these apps** (list, pre-filled with LinkMeter); **Connection log**; **Test the connection**. Tone is fixed to the friendly "tu" (9.6). The first tap on the toolbar's Translate button shows, once, LinkMeter's explanation adapted: "When you press Enter, Swoop translates your English into Spanish the way it is spoken in Mexico and puts it in the box for you to check. Press Enter again to send it. Undo gives your English back, and "Send my English" sends it as you wrote it. To translate, the words of the text go over the internet to MyMemory, a translation service, or to Google Translate when MyMemory is busy. Nothing else is sent with them. While translate is on this happens in every app until you turn it off. A text you receive can be translated into English too: select it and choose Translate to English, or copy it and tap the chip." [Turn on] [Cancel].

### 9.6 The Mexican safety layer and "friendly tu" `[R]` + `[I]`
- **`toMexican` (ported with its tests):** words from Spain that read wrong in Mexico become Mexican, whole words only, only where they can mean nothing else: **coger** (vulgar there) -> **agarrar** in every verb form with the accent the new word needs (except "cojo/coja" meaning limping), **chaqueta** -> chamarra, **zapatillas** -> tenis; conducir -> manejar (not "conduce a la playa"), alquilar -> rentar, aparcar -> estacionar, enfadar -> enojar, patata -> papa, camarero -> mesero, coche -> carro, piscina -> alberca, apartamento -> departamento, zumo -> jugo, melocotón -> durazno, aparcamiento -> estacionamiento, guay -> padre, nevera -> refrigerador, bolígrafo -> pluma, tarta -> pastel, gambas -> camarones, ordenador -> computadora, gafas -> lentes, móvil -> celular (only as the phone), "vale" as OK -> "ok"; a noun whose gender changes takes its article and adjective along ("la nevera está vacía" -> "el refrigerador está vacío"); no vosotros forms (they become ustedes forms); Spanish that is already Mexican is left as it was and a second run changes nothing.
- **Friendly tu (owner decision):** English "you" carries no formality, so the services answer with "tu" for casual text; the risk is a formal "usted" answer (LinkMeter saw "¿Puede conducir?" only when human-memory matches were allowed, which `onlyprivate=1` prevents). The plan adds: (a) a test list of 30 casual English questions and requests (Can you drive? Do you want to eat? Are you coming?) that must come back in "tu" form in the recorded answers; (b) a conservative guard: if the translation of a casual text contains clear usted markers ("usted", "¿Puede...?", "¿Quiere...?", "¿Tiene...?" as questions), ask the other service once; if both answer formally, show that translation unchanged (never rewrite verbs by rule: that breaks sentences); (c) the Diagnostics counter "formal answers" so real use shows how often it happens. A formal-tone setting is not planned.
- The tables are LinkMeter's; extending them (new words, new gender pairs) is done with new tests in the ported suite, never by guessing.

### 9.7 Privacy, limits and failure behaviour
- **What leaves the device:** only when the owner presses Enter with the toggle on in an eligible field (the text of that one field), or taps an English-translation action on a received text: those words, over HTTPS, to MyMemory (or Google Translate). No name, no number, no other message, no app name. The connection log shows "translate (MyMemory)", the time and the result, never the words. What a service keeps on its side is its own business (MyMemory is asked for its machine translation only). Same statement as LinkMeter's `docs/SAFETY.md` section 10, with the added point that while the toggle is on this applies in every app.
- **Never:** in password fields or the other excluded fields (9.3), never with the toggle off (except the owner's explicit English-translation actions), never automatically for received messages.
- **Failure:** nothing is sent, the reason is shown in words, "Send my English" is offered; the keyboard keeps working with no translation. The tablet reaches the internet through the phone's hotspot: with the phone's mobile data off every translation fails with the "not on the internet" reason.
- **Visible state:** the lit toolbar button, the "ES" mark on Enter, and an "online" badge on the strip while a request is in flight make it impossible to translate by accident.

### 9.8 Tests and acceptance
- Port LinkMeter's `TranslationTest` (about 56,000 checks: pieces never inside an emoji, the services' real recorded answers, shorthand, every Mexican rule, 4,000 random texts stable and with no form of coger left, the flow) and its 36-breakage checker (3.3). **The port is accepted only when the same checks pass and the same breakages are caught.** Recorded answers, never live requests, in CI.
- Enter-flow tests on `FakeEditor` (the tables in 9.3 row by row, a late answer, a changed field, Undo, Send my English, leaving the field, toggle off, auto-off timer, password and other excluded fields, private field with and without the extra setting, the "never in these apps" list including `com.tj.linkmeter`, the 1,000-character limit) plus a random-event fuzz.
- **Guard-rail tests (9.2):** the permission list equals the fixture; the source and dex scans find no network code outside `translate/`; the host allowlist test; the dependency fixture; `TranslateGate` refusal cases; the caller-count test; nothing is created, allocated or logged while the toggle is off.
- A manual, dispatch-only "service check" workflow sends five fixed sentences to MyMemory and Google from a GitHub runner and prints what came back, so a session notices when a service changes (Google may refuse a runner's shared address; MyMemory's allowance is shared; run it rarely).
- Acceptance on the tablet (plain-words script for the owner): (1) tap Translate in the toolbar: the explanation appears; turn it on; (2) in a text app write "Can you pick me up at 5? I'm running late lol", press Enter: Spanish in the box, "tu" form; (3) press Enter again or tap the app's Send: it goes (or the strip says to tap Send); (4) Undo; Send my English; (5) turn the phone's data off: the reason is shown, nothing is sent; (6) long-press a Spanish message and choose Translate to English; (7) copy a message and tap the chip; (8) read the Connection log (no words in it); (9) turn Translate off: Enter behaves normally and the log stops growing. The owner says whether the Spanish reads right; defects go to the ported tables. KB-603 records the press counts.

---

## 10. OTHER GBOARD-LIKE FEATURES, SPECIFIED ENOUGH TO BUILD

### 10.1 Emoji
Use the system emoji font. Generate the emoji list and names from the Unicode emoji data and CLDR annotations (Unicode licence, permissive `[K]`) in `tools/`, English keywords, skin-tone and gender variants as long-press. Hide emoji the device cannot draw with `Paint.hasGlyph(String)` (API 23) `[K]` (the tablet's system image is old: newer emoji would show as boxes). Recently used + search. Memory: the list is a few hundred KB; load on first open.

### 10.2 Clipboard
`ClipboardManager` primary-clip listener while the keyboard is the active IME; keep the last 20 clips in memory, optional encrypted file; pin; auto-clear unpinned after 60 minutes (Gboard's behaviour per guides `[W]`); skip clips flagged sensitive (API 33) and clips copied from password fields where detectable. Show as a strip of chips on a panel; tap to paste.

### 10.3 Text editing toolbar and gestures
Panel with: select word, select all, cut, copy, paste, undo/redo where the editor supports it, arrows (DPAD key events; with shift held for selection), home/end. Implementation `[K]`: `performContextMenuAction(android.R.id.selectAll / cut / copy / paste)`, `setSelection`, key events with meta state. Space-bar slide for the cursor already exists (browser constants 10 dp start, 11 dp per char). New: Backspace swipe left deletes word by word (P2).

### 10.4 Voice typing (stretch, P3)
Google's recogniser is unavailable (no GMS). An offline recogniser would be a separate optional APK with the microphone permission (the keyboard APK keeps none). Candidates to evaluate later: small Kaldi-based models (Vosk, Apache-2.0 `[K]`, not verified here) or whisper.cpp tiny models (MIT `[K]`); expect heavy CPU on a 2 GHz A53 and large models; do NOT plan this before phases 1-7 are done.

### 10.5 Personal dictionary, shortcuts, learned-word manager
Screens: list learned words (search, delete one, delete all), add a word, add a shortcut ("omw" -> "on my way"), English only. Shortcuts expand on space and are suggestion-strip items.

### 10.6 Themes, size, accessibility
Three palettes (dark neon in the browser's style, plain dark, light), key height 3 steps, key border on/off, number row on/off, haptic strength (follows system touch feedback by default), sound off by default. TalkBack: content descriptions on keys and strip items, large-text friendly (font scale 1.2 on the tablet), minimum 48 dp touch height for toolbar buttons.

---

## 11. BOOTSTRAP: THE NEW REPOSITORY AND THE SAVE LADDER (do this first, KB-000)

The owner's standing rule for ALL projects: Claude can be cut off at any moment (usage limits), so every project needs an automatic save ladder to GitHub and a cold-resume protocol, so that a brand-new session can see what to resume, how, what is done and what is not, without losing anything. The Kestrel repository implements this; copy it.

### 11.0 MAKING THE REPOSITORY PRIVATE: WHAT THE OWNER DOES (plain words, about 10-15 minutes, on a phone or the tablet)
A private repository is the same as any other repository, but only you (and the apps you allow) can see it. The extra work is only: create it empty, let Claude in, and later sign in to GitHub to download APKs.

1. **Create it.** On the GitHub website, signed in as your usual account (the one that owns the Kestrel browser repository): tap the **+** at the top, then **New repository**. Repository name: `swoop-keyboard`. Choose **Private**. Leave **Add a README file**, **Add .gitignore** and **Choose a license** all OFF (the repository must start empty; Claude fills it). Tap **Create repository**. That is all that "private" takes.
2. **Let Claude in.** Two things, because Claude can only touch repositories you allow: (a) open https://github.com/apps/claude/installations/select_target , pick your account, choose **Only select repositories**, add `swoop-keyboard` (keep the Kestrel repository in the list too), and save; (b) check that GitHub is connected to your Claude account at https://claude.ai/connect-github . Claude cannot do either step for you. You can remove the access any time under GitHub, Settings, Applications.
3. **Start the new Claude session on that repository.** At claude.ai/code start a new session and choose `swoop-keyboard` as its repository. Attach `KEYBOARD-APP-PLAN.md` and say: "Read KEYBOARD-APP-PLAN.md completely, then do section 11 (bootstrap) and continue with section 12 in order." Claude's first message should show it can read and save to the repository; if it says access was denied, redo step 2. The session also needs READ access to the public Kestrel repository (keyboard code) and the public LinkMeter repository (translation code); Claude asks for it with its repository tool (public, read-only).
4. **Signing secrets (later, when task KB-003 asks).** In the repository on the GitHub website: Settings, then **Secrets and variables**, then **Actions**, then **New repository secret**, four times (Claude gives you the names and the values to paste, one at a time, in plain words). These are what keep other people's apps from replacing your keyboard.
5. **Downloading the APK from a private repository.** A Release link of a private repository only works while you are signed in to GitHub in that browser; a browser that is not signed in shows "404 Not Found" `[W]`. So sign in to GitHub once in the Kestrel browser on the tablet (and on the phone). If signing in on the tablet is a nuisance, Claude can instead set up a tiny PUBLIC repository `swoop-releases` that receives only the finished, signed APK (it contains no personal data; the private signing key is what protects it); that needs one more secret (a GitHub token) and one more session step; ask for it only if needed.
6. **What changes because it is private.**
   - Nobody else can see the code, the chat copies the save-ladder hooks write into the repository, or the tools that handle your learning data.
   - Free build minutes: GitHub Free gives private repositories 2,000 Actions minutes per month `[W]` (the docs I found did not say how Linux minutes are counted: check GitHub, Settings, Billing and plans, Usage). A normal build of this small app takes a few minutes, so hundreds of builds fit; an emulator lab uses more, so watch the usage page. A free emulator lab on a private repository is NOT guaranteed (KVM is available on GitHub's Linux runners, but I could not confirm it for private repositories `[W?]`); decide at KB-703.
   - Releases/APKs: see step 5.
7. **How to know it worked:** the repository page shows a "Private" label, the first Claude session can push to it, and CI (KB-001) runs.

### 11.1 Owner actions needed at the start (say them in plain words, one list)
1. Create the EMPTY PRIVATE repository `swoop-keyboard` and give Claude access to it: follow 11.0 (click by click).
2. In the Kestrel browser: Settings > "Export what the keyboard learned" (keeps a private file in Downloads). Not needed by Claude; the owner will import it into the new keyboard themselves at KB-205. Do not paste its contents anywhere.
3. When KB-003 asks: add the signing secrets to the repository (step-by-step text goes in the task). This must happen BEFORE the first APK the owner installs (11.5).
4. Install from the Release link; enable and select the keyboard (the Setup screen explains); keep the old keyboard installed until gate G2.

### 11.2 The ladder kit (copy from the pinned Kestrel commit, then adapt)
`[R: CLAUDE.md "The save ladder", tools/]` The rungs: 0 = a hook records every owner message verbatim into `INBOX.md` and pushes before any work; 0b = a hook records what Claude told the owner into `OUTBOX.md`; 1 = `tools/autosave.sh` after every Edit/Write/Bash tool call and on Stop: `git add -A`, secret scan, commit `auto-checkpoint:`, push; 2 = `bash tools/ckpt.sh "did" "next"` run by Claude after every completed step (rewrites `CHECKPOINT.md`, quick self-test, commit, push); 3 = `ship.sh` at real versions; 4 = GitHub Actions builds, signs, releases.
Files to copy (Kestrel repo root -> same path in the new repo): `.claude/settings.json`, `bootstrap.sh`, `ship.sh`, `tools/resume.sh` (157 lines), `tools/ckpt.sh` (224), `tools/autosave.sh` (114), `tools/capture_inbox.sh` (55), `tools/capture_outbox.sh` (98), `tools/posttool.sh`, `tools/sync.sh` (66), `tools/claim.sh` (63), `tools/install-hooks.sh` (243), `tools/session-root-hooks.json`, `tools/secretscan.sh` (79), `tools/toobig.sh` (52), `tools/test_handoff.sh` (273), `tools/push.sh`, `tools/unpushed.sh`, `tools/merge-newest.sh`, `tools/MIRROR_MAIN` (contains `off`), `tools/record-release.sh`, `tools/verify-apk.sh`.
How to adapt (do not skip, these scripts run after every tool call with the owner's authority):
1. READ every script completely before running it.
2. Replace Kestrel-specific names: banner text in `resume.sh`, file names (`BUILDLOG.md`, `TASKS.md`, `CHECKPOINT.md`, `HANDOFF.md`, `BRIEF.md`), Gradle module names in `ckpt.sh`/`test_handoff.sh`, and the release scripts' tag patterns (`kestrel-g-v...` -> the new app's pattern).
3. Make the capture hooks safe for this project: skip any owner message larger than about 20 KB, and any text that contains the string `KESTREL-KEYBOARD-LEARNING` (a pasted export), so learned data can never be copied into the repo by a hook (11.6).
4. Run `bash tools/test_handoff.sh` until it passes (it also fails if any workflow gets a `push:` trigger; keep that rule).
5. Verify the ladder works: make 2-3 tiny edits and confirm `git log --oneline -3` shows `auto-checkpoint:` commits AND that `git status -sb` shows the branch is not ahead of origin (pushes work).
6. Branch rule: develop and push only on the branch the session was assigned; never force-push; no PRs unless the owner asks; `main` is the owner's call.
`CLAUDE.md` is auto-loaded every session: keep it short and put the cold-resume block first (template in Appendix G). Several sessions/accounts may work in parallel: keep `tools/sync.sh`, `tools/claim.sh` and the per-branch cards in `.checkpoints/` (they are what stops two sessions from redoing the same task).

### 11.3 Documents to create at repository birth
`CLAUDE.md` (Appendix G), `HANDOFF.md` (the single entry point for a session with no context), `BRIEF.md` (decisions that do not change: device, privacy rules, the five decisions in section 1), `TASKS.md` (copy section 12 as `[ ]` boxes in the owner's words where possible; each box ticked only when written, tested, committed, naming the test or run that proves it), `CHECKPOINT.md` (written by `ckpt.sh`; "Do this next" is authoritative), `research/KEYBOARD-APP-PLAN.md` (THIS FILE, copied in so future sessions do not need the attachment), `docs/DICTIONARY.md` and `docs/KEYBOARD-EXPORT.md` (copied from Kestrel, then extended), `docs/ARCHITECTURE.md`, `docs/IF-THE-KEYBOARD-BREAKS.md` (plain words for the owner, KB-106). Research findings and design decisions go into `research/*.md` / `docs/*.md` the moment they exist, never only in the chat.

### 11.4 CI (GitHub Actions does the building)
`.github/workflows/android.yml`: triggers = tags and `workflow_dispatch` only (inputs: `full_build`, `note`); jobs = `check` (cheap gates), `test-jvm` (`./gradlew :core:test :ime:testDebugUnitTest`), `build` (release APK with R8, signed with the private key, `verify-apk.sh` checks the certificate against `signing/CERT_SHA256`), asserts: APK size under budget, **the merged manifest's permissions equal the expected set exactly (`INTERNET` for translation; no `ACCESS_NETWORK_STATE`/`RECORD_AUDIO`/`READ_CONTACTS`) and the network guard rails pass (9.2)** (`aapt2 dump permissions`), `minSdk 29`, then creates the GitHub Release with the APK and its sha256. The tag is created by the workflow server-side (a tag PUSH from the sandbox is refused, per the Kestrel notes `[R]`). Release order: bump versionCode/versionName -> `bash ship.sh "note"` -> dispatch -> wait with a background loop (a foreground `sleep` is blocked) -> Release shows the APK -> `bash tools/record-release.sh <tag> "note"` ONLY after the run is green -> give the owner the Release link as plain tappable text on its own line. Never ship a tag twice. Later add the emulator-lab workflow (KB-703) adapted from `emu-lab.yml`.

### 11.5 Signing (security-critical for a keyboard)
The Kestrel browser signs with a PUBLIC dev key committed in the repo so any session/CI produces update-compatible APKs `[R: CLAUDE.md]`. **Do not do that for the keyboard.** A system keyboard sees everything typed; Android refuses to update an app with a different certificate, which is the only thing that stops a malicious APK from silently replacing the keyboard. A public key removes that protection. Use a PRIVATE keystore held in GitHub Actions secrets.
Steps: (1) a session generates a keystore with `keytool` in its sandbox (never commits it, never prints the passwords into chat or files); (2) the owner stores the keystore (base64) and its passwords as repository secrets through the GitHub website (the session writes the click-by-click steps in plain words; Claude cannot create secrets itself); (3) the workflow decodes it at build time; (4) the certificate fingerprint (public) goes into `signing/CERT_SHA256`; (5) `verify-apk.sh` fails the build on any other certificate. **Order matters:** an APK signed by a different key cannot update an installed one (the owner would have to uninstall and lose the learned data, unless they exported it first). So do this BEFORE the first release the owner installs; until then CI may build unsigned/debug artifacts for tests only.

### 11.6 Data hygiene (the keyboard handles intimate data)
- Never commit: learning exports (magic `KESTREL-KEYBOARD-LEARNING`), replay logs, `personal/` files, screenshots or photos of the owner's screen, keystores, APKs. `.gitignore` them and make `tools/secretscan.sh` unstage/block them (allow the documented example in `docs/KEYBOARD-EXPORT.md` and test fixtures under a fixed folder).
- The capture hooks copy chat text into the repository. Therefore: the owner ATTACHES files (replay log, export) to a session instead of pasting them; sessions open them in the scratchpad outside the repo; only numbers go into the repo.
- Kestrel's repo is PUBLIC and its hooks publish the owner's chat there `[R: CLAUDE.md]`. The owner chose a PRIVATE repository for the keyboard (Q2, 0.1), which keeps the chat copies and the tooling private; the guards above still apply (a private repository can be shared or made public by mistake). The cost is GitHub's free build minutes for private repositories (2,000 per month on the Free plan `[W]`, see 11.0).
- Third-party data: keep corpora outside the repo; commit only compiled lists, notices and the scripts that make them; record sources, licences and SHA-256 in `docs/DICTIONARY.md`.

### 11.7 First hour for the new session (checklist)
1. Read this file fully. 2. Read the Kestrel `HANDOFF.md` and `docs/KEYBOARD-EXPORT.md` and LinkMeter's `docs/TRANSLATE.md`, skim `research/GLIDE_TYPING.md` sections 2, 8, 9, 11 (2 minutes each). 3. Q1-Q3 are already answered (0.1); do 11.0 with the owner if the repository does not exist yet, and ask only the open questions in section 15 that the next task needs, in one short message, using the defaults while waiting. 4. KB-000 (ladder + docs). 5. `bash tools/ckpt.sh "ladder works; plan in research/" "KB-001 gradle skeleton: see TASKS"`. 6. Continue with KB-001, KB-002 (the 122 tests green is the first real milestone).

---

## 12. THE STEP LIST (execute in order; tick boxes in TASKS.md; checkpoint after every step)

Sessions are rough `[I]` (one session = one Claude Code session of a few hours). **Gates** (G1-G5) are owner-visible milestones: stop and report in plain words, say what was NOT run on the tablet.

### Phase 0 - Bootstrap and safety net (about 1-2 sessions)
| Id | Task | Done when |
| :- | :- | :- |
| KB-000 | Ladder kit, docs at birth, this plan copied to `research/` (section 11) | `bash tools/test_handoff.sh` passes; 3 edits produce `auto-checkpoint:` commits and pushes; `CHECKPOINT.md` has a concrete "next" |
| KB-001 | Gradle skeleton: `:core` (JVM, Java-8 API surface, flags as in Kestrel), `:ime` (Android app, `minSdk 29`, `targetSdk 36`, namespace), `:tools`, `:eval`; version catalog like Kestrel's (agp 8.13.2, kotlin 2.3.10); dispatch-only CI | one green CI run (`full_build=false`) |
| KB-002 | Port `core/input` (all files in 3.1) with the 122 tests and `GestureSim`/`GlideEval`; copy `en.klx`, `LICENSE.txt`, `extra-words.txt`, `docs/DICTIONARY.md`; keep package names; compiler task `glideLexicon` works | `./gradlew :core:test` green with the same test counts; list is byte-identical (blob `63e399fbc6`) |
| KB-003 | Private signing key + secrets + CI verification (11.5) | CI builds a release APK whose certificate equals `signing/CERT_SHA256`; owner action done |
| KB-004 | Write `docs/ARCHITECTURE.md` from section 6, `docs/IF-THE-KEYBOARD-BREAKS.md`, `BRIEF.md` | files exist, linked from `HANDOFF.md` |

### Phase 1 - Walking skeleton: type, tap, glide in any app (3-4 sessions)
| Id | Task | Done when |
| :- | :- | :- |
| KB-100 | Manifest, `method.xml` (subtype `en_US`), `KeysService`, Setup activity (enable / select / try-it field), empty `KeysView` | on the emulator `ime list -a` shows it, it can be enabled and selected, a letter types into a test field |
| KB-101 | `EditorBridge` (interface in core, `InputConnection` implementation in `:ime`, `FakeEditor` in tests) and the input state machine (6.4): composing, own-edit tracking, batch replace, password/no-learning flags | >= 40 unit tests incl. a randomised event fuzz asserting the editor text equals the model after every event; selection-mismatch tests |
| KB-102 | Port `KeysView` (draw, taps, key bubble, haptics, Backspace repeat, shift / caps lock, symbols page, space slide, hold-for-digit); full-width geometry; bottom-row variants (default, URL, email, number/phone) | Robolectric pixel/touch tests ported from `MiniNavTest`/`MiniNavLookTest` adapted; emulator screenshot looks right |
| KB-103 | Wire the glide engine: `GlideTyping` on worker threads, warm-up, live preview, strip, one-Backspace-removes-word, swap from strip, learning events, save/load, Export/Import of v1 (`KeyboardFlow` port) | emulator lab: glide "hello" -> text `hello `; learning file written; export/import round trip |
| KB-104 | Enter / action keys per `imeOptions`, caps mode from `EditorInfo`, no-learning in private fields | unit + emulator tests on text, password, URL, email, number fields |
| KB-105 | Diagnostics screen with counters from 8.5 layer 1, memory, versions, timings, Copy button | text matches the browser's `GlideStats.line()` plus new counters |
| KB-106 | **Safety:** crash-loop guard -> minimal fallback layout without decoders; globe/next-keyboard key always visible; Setup screen button "go back to my previous keyboard"; plain-words doc | killing the service 3 times in a minute produces the fallback; test on emulator |
| KB-107 | First Release `0.1.0` via CI; owner installs on tablet and phone; check the keyboard window on Android 16 (gesture and 3-button navigation, section 6.3); first tablet numbers: decode ms, "word shown after lift", PSS, cold show | numbers pasted by the owner into chat and recorded in `research/TABLET_NUMBERS.md`. **GATE G1** |

### Phase 2 - Typing fundamentals: taps, autocorrect, suggestions, personal model v2 (6-8 sessions)
| Id | Task | Done when |
| :- | :- | :- |
| KB-200 | `TapDecoder` (8.3.5): prefix-range beam, Gaussian per key, allocation-free; tap simulator in `:eval` | tests: neighbour-key typos corrected, completions ranked by context; microbenchmark within budget |
| KB-201 | KGL2 format + compiler: UTF-8, language tag, display forms, trigrams, `<s>`, pruning, size report; rebuild English from added corpora with licences recorded | file size within budget; `LexiconFileTest` for KGL1 and KGL2; accuracy not below the KGL1 baseline on `GlideEval` |
| KB-202 | Suggestion strip, composing text, autocorrect policy, undo-on-Backspace with unlearn | policy tests (never correct URL/email/password/numbers; revert rate counter) |
| KB-203 | Auto-capitalisation, auto-space, double-space period, smart punctuation, apostrophe words; incognito toggle | field-type table tests |
| KB-204 | Personal model v2 (8.3.2): learn from taps, two-use rule, calendar decay, per-id arrays, case forms, journal persistence, migration of the v1 file | `PersonalModelTest` extended; speed test vs old model |
| KB-205 | Import of the browser export v1 and "Teach from text" | synthetic v1 file from the docs example imports; owner imports the real one |
| KB-206 | Dictionary manager (list / delete / add / shortcuts) and "Forget everything" covering all new tables | UI test; forget leaves no learned file |
| KB-207 | **Daily-driver week** on the tablet (owner switches default keyboard); collect Diagnostics; fix the top bugs | owner's report + counters. **GATE G2** |

### Phase 3 - Gboard-like everyday features (5-7 sessions)
| Id | Task | Done when |
| :- | :- | :- |
| KB-300 | Landscape layout; one-handed / dock / resize; key-height setting | emulator screenshots both orientations; thumb reach checked by the owner |
| KB-301 | Long-press popups (alternates, digits), number row option, full symbols pages | tests + owner check |
| KB-302 | Emoji panel with search and recents (10.1) | glyph filter tested with a fake `hasGlyph` |
| KB-303 | Clipboard panel (10.2) | sensitive clips skipped; 60-minute expiry test |
| KB-304 | Text-editing toolbar and Backspace swipe (10.3) | fake-editor tests; emulator check |
| KB-305 | Themes, haptics/sound settings, accessibility pass (10.6) | TalkBack labels present; contrast check |
| KB-306 | Next-keyboard (globe) key, hardware keyboard handling, IME switcher | works on tablet and phone. **GATE G3** = every P0/P1 row of the parity matrix (section 5.1) marked done or consciously deferred |

### Phase 4 - Learning engine v2 (5-8 sessions) - the core of the request, see section 8
| Id | Task | Done when |
| :- | :- | :- |
| KB-400 | Shadow decoder, extended counters, opt-in replay log, `:eval` replay runner, simulator calibration against the first real file | owner can switch an experiment on and read "A vs B" counts; replay runner reproduces the live results |
| KB-401 | Speed: bitmask pre-filter, banded DP / early abandon, per-id arrays (7.2 items 3-5) | identical top-3 on the eval seeds; tablet timing recorded in `research/PERF.md` |
| KB-402 | Per-key touch model from taps and glide ends (8.3.4 items 1-3) | `:eval` shows no regression and the lab's gain; shadow data later |
| KB-403 | Layout-aware shapes, export v2 (`s2`, `touch`, `case`), collision sets in the compiler | v2 example read by a unit test; v1 still imports |
| KB-404 | Topic vector (embedding builder in `:tools`, runtime feature) and language-tag features (8.3.3) | measured on held-out text; ships OFF unless the gate is met |
| KB-405 | Segment templates experiment (8.3.4 item 4) | lab result recorded; ship ON only if it beats KB-402 alone |
| KB-406 | Next-word prediction evaluation (top-3 hit rate, keystroke savings) with n-gram + personal + topic | numbers in `research/LEARNING_RESULTS.md` |
| KB-407 | Online re-ranker, only if the lab shows headroom (8.3.7) | otherwise write "not built, because ..." |
| KB-408 | Export v2 with optional passphrase encryption; opt-in weekly snapshot (8.9) | encrypted round trip test; wrong passphrase refused |
| KB-409 | Report: `research/LEARNING_RESULTS.md` with every number vs the targets in 8.1. **GATE G4** | owner reads the plain-words summary |

### Phase 5 - REMOVED (owner decision 2026-11-16: no Spanish typing). Do not build a Spanish layout, dictionary, accent handling or bilingual decoding.

### Phase 6 - Translation, LinkMeter's way, inside the one keyboard app (2-4 sessions; see section 9; can start right after G1, in parallel with phases 3-4)
| Id | Task | Done when |
| :- | :- | :- |
| KB-600 | Port LinkMeter's pure code to `:core/translate/` in Kotlin: `Translation`, `TranslateFlow`, `Json` (3.3), with its tests (`TranslationTest`, about 56,000 checks) and the 36-breakage checker | same checks pass; same breakages caught; no Android imports; recorded answers only |
| KB-601 | The `app.swoop.keyboard.translate` package: `Translator` glue (MyMemory then Google, one thread, timeouts, no redirects, user agent `Swoop`), connection log (no words), Test the connection, the `PROCESS_TEXT` activity "Translate to English" with a read-only panel; **plus the guard rails of 9.2 in CI**: exact permission fixture, source and dex scans, two-host allowlist test, dependency fixture, `usesCleartextTraffic=false`, `TranslateGate` tests | CI fails when any guard rail is broken (prove it by a deliberate break); emulator lab: the menu entry appears in a test app; the log never contains the words |
| KB-602 | Toolbar Translate toggle (OFF at install, lit when on, "ES" mark on Enter, first-tap explanation, auto-off timer), eligibility and exclusions (password/URL/e-mail/number/phone/search/private fields, "Never translate in these apps" pre-filled with `com.tj.linkmeter`), the Enter flow on `EditorBridge` (9.3), strip with Undo / Send my English, LinkMeter's wording, second-Enter rules | `FakeEditor` tests for every row of 9.3; emulator lab with a pretend translator: Enter -> Spanish in the field, Enter -> the action; nothing created while off |
| KB-603 | Received messages (9.4): the selection-menu route, the one-tap clipboard chip (only while the toggle is on), translate-selection button; **count the presses on the tablet in the apps the owner names** | table "app -> best route -> presses" in `research/TRANSLATE_RESULTS.md`; chip tests with a fake clipboard |
| KB-604 | Friendly-tu guard, 30-sentence casual list, formal-answer counter (9.6); docs: `docs/TRANSLATE.md` for Swoop and the keyboard's SAFETY wording | list passes on recorded answers |
| KB-605 | Dispatch-only "service check" workflow (9.8) and the quota/fallback behaviours verified with recorded answers (429, quotaFinished, puzzle redirect, timeouts) | workflow runs; tests green |
| KB-606 | Acceptance on the tablet and the phone with the owner's script (9.8) | owner's verdict recorded in `research/TRANSLATE_RESULTS.md` |

### Phase 7 - Hardening and release quality (3-4 sessions)
| Id | Task | Done when |
| :- | :- | :- |
| KB-700 | Baseline profile, R8 rules review, budgets enforced in CI (APK size, permissions, memory smoke) | CI fails when a budget is broken |
| KB-701 | Android 16 phone full pass (edge-to-edge, insets, predictive back in Settings, 16 KB page size if native code exists) | owner's phone report |
| KB-702 | Security review (exported components, file modes, logs, clipboard, replay log), run the `security-review` skill and a manual pass | findings fixed or recorded |
| KB-703 | Emulator lab in CI (adapt `emu-lab.yml`, `glide_emu.py`): typing, glide, strip, Backspace, cursor, password field, screenshots as artifacts only | lab green on dispatch |
| KB-704 | Plain-words user guide and About screen with all licences (SymSpell MIT notice, corpora, models) | present in the APK |
| KB-705 | Sign-off: daily driver on tablet and phone for 2 weeks, counters reviewed. **GATE G5** | owner says it replaced Gboard/the stock keyboard |

### Phase 8 - Stretch (only when asked): offline voice typing (10.4), split keyboard, handwriting, more languages.

**Critical path:** KB-000 -> 001 -> 002 -> 003 -> 100 -> 101 -> 102 -> 103 -> 107 (G1) -> 200/201 -> 202 -> 204 -> 205 -> 207 (G2) -> 300-306 (G3) -> 400 -> 401/402 -> 409 (G4). Phase 6 (translation: KB-600 to KB-606 can start right after G1) can run in parallel with phases 3-4 in a second session if the owner runs several accounts (claim tasks with `tools/claim.sh`). Estimated total `[I]`: about 20-30 sessions.

---

## 13. TESTING AND VERIFICATION STRATEGY

1. **JVM tests in `:core` are the main safety net** (fast, no emulator, pure Kotlin): keep all 122 inherited tests; add the state-machine fuzz (random taps, glides, Backspace, selection changes, app-side edits against `FakeEditor`; invariant: keyboard's belief equals editor text; never throws), hostile-input tests for every file parser (the browser's export tests are the model), surrogate pairs / emoji / right-to-left text.
2. **Accuracy gates** run as unit tests with fixed seeds (the browser pins minimum accuracies in `GlideDecoderTest`, `GlideShapeLearningTest`); new features add their own pinned claims. The `GlideEval` / `GlideHabitEval` command lines are documented in their file headers `[R]`.
3. **Robolectric** for `KeysView` drawing and touch dispatch with real pixels (the browser's `MiniNavLookTest`) `[R]`; do not expect `InputMethodService` itself to run well under Robolectric `[K]`: keep the service thin and the logic in `:core`.
4. **Emulator lab on GitHub runners (KVM)**: debug APK with a `FieldLab` activity containing every field type; `adb shell ime enable/set`; real multi-point touches written to the emulator's input device (the browser's `glide_emu.py` does this) because `input swipe` can only draw straight lines `[R]`; read the text back from the field. Screenshots are uploaded as workflow artifacts only (never committed).
5. **The tablet is the only judge of speed and feel.** A numbered owner script (plain words, 10 steps) per release asking for: (a) the Diagnostics copy, (b) "does typing in X lag", (c) a screenshot of the keyboard if something looks wrong. State in every report what was NOT run on the tablet.
6. **Performance regression**: CI cannot time an A53; track the ratio against a stored baseline in the same run (decode of a fixed glide set in JVM) and fail on a large ratio; the tablet numbers go to `research/PERF.md`.
7. **Privacy tests:** a unit test that no learning API is called when the field flags say private; a CI check of the manifest permissions; a test that the export of a model containing a password-field word never contains it.

---

## 14. RISK REGISTER

| Id | Risk | Likelihood / impact | Mitigation |
| :- | :- | :- | :- |
| R1 | Real-thumb accuracy lower than the simulation | medium / high | shadow decoder + replay lab (8.5); a bigger keyboard helps; keep the old keyboard until G2 |
| R2 | Tablet decode slower than 15-30 ms estimate; first runs slow | medium / medium | warm-up, baseline profile, 7.2 techniques; measure at G1 |
| R3 | A bug locks the owner out of typing | low / very high | KB-106 first; crash-loop fallback; keep previous keyboard installed; documented recovery |
| R4 | Editor state desynchronisation (wrong text deleted/replaced) | medium / high | own-edit tracking, verify-before-replace, fuzz tests |
| R5 | The free translation services change, rate-limit or fail (MyMemory 5,000 characters a day; Google's endpoint is unofficial and may show a picture puzzle or be blocked) | medium / medium | two services in order, clear reasons, "Send my English", recorded-answer tests, a dispatch-only service check, owner told plainly (9.1, 9.7) |
| R6 | Android 15/16 IME window/inset changes break layout on the phone | medium / medium | read behaviour-change pages before KB-107, test both navigation modes `[W?]` |
| R7 | Learning learns junk or reinforces a mistake (Fowler: 3 of 45 users got worse `[R]`) | medium / medium | two-use rule, noise guards, caps, decay, Forget, gates, experiments OFF by default |
| R8 | Privacy leak by tooling (hooks copy chat into git; exports committed) | medium / high | 11.6 rules; private repo default; scan extension |
| R9 | Public signing key / signing-key change forces uninstall and loses learning | medium / high | private key before first install (11.5); weekly snapshot option |
| R10 | Licence problems (CC-BY-SA data, MPL models, GPL code mixed in) | medium / low-medium | record licences; do not copy GPL code; keep notices in About; personal non-commercial use per owner |
| R11 | A translation reads formal ("usted"), Spain-like or odd | medium / medium | `onlyprivate=1`, LinkMeter's `toMexican` layer, the friendly-tu test list and guard (9.6) |
| R12 | Memory pressure on a 2 GB Go device kills the keyboard process | medium / medium | budgets in 7.1, trim, mmap cold data, translation runs on one background thread only on request (no model, no second process) |
| R13 | Another Claude account changes the Kestrel files being copied | low / low | pinned commit + blob ids; copy once; never depend on the live repo |
| R14 | Scope creep before basics are solid | high / medium | gates G1-G5; P0 before P1; ship phases as releases |
| R15 | A keyboard cannot press another app's own Send button or see other apps' message bubbles, so "Enter again sends" and "long press any message" fall short in some apps | certain / medium | tell the owner plainly (9.3, 9.4); per-app second-Enter option; selection menu, clipboard and selected-text routes |
| R16 | Double translation in LinkMeter (its own Send translates too) | medium / medium | `com.tj.linkmeter` is pre-filled in "Never translate in these apps" (the owner can remove it); LinkMeter itself is not changed (Q19) |
| R17 | While the toggle is on, everything typed in every ordinary field goes to a translation service | medium / high | OFF at install, first-tap explanation, lit button and "ES" mark, excluded field types, optional auto-off timer (Q21), connection log without words (9.5, 9.7) |
| R18 | The keyboard holds the INTERNET permission (a keyboard sees every key): a bug, a dependency or a later change could leak typing | low / very high | the guard rails of 9.2 enforced in CI, kotlin-stdlib only, private signing key, plain-words report of any change under `translate/` or the manifest |

---

## 15. QUESTIONS FOR THE OWNER (Q1-Q5 and Q15-Q19 are ANSWERED, see 0.1; the rest keep working with their defaults)

| Id | Question (plain words) | Status / default | Blocks |
| :- | :- | :- | :- |
| Q1 | Name of the keyboard | **Answered: Swoop** (rename later if wanted) | nothing |
| Q2 | Private or public repository | **Answered: private**; steps in 11.0 | KB-000 (owner creates the empty repo) |
| Q3 | Type in Spanish? | **Answered: no** | - |
| Q4 | Friendly "tu" or formal "usted"; translate received Spanish into English too? | **Answered: friendly "tu"; yes** | - |
| Q5 | Which translation engine; offline? | **Answered: LinkMeter's service (MyMemory, then Google), Mexican Spanish, NOT offline, same Enter method** | - |
| Q15 | Where does the internet permission live? | **Answered: everything in ONE keyboard app** (guard rails in 9.2) | - |
| Q16 | In which apps does translate work? | **Answered: any app, only while the owner has turned translate on (toolbar button, like Gboard)** | - |
| Q17 | Own Send button: you tap Send yourself? | **Answered: OK** | - |
| Q18 | Easiest way to translate received messages? | **Answered: the fewest presses** (9.4: 2 presses via the selection menu, 3 via copy + chip; measured in KB-603) | - |
| Q19 | Change LinkMeter? | **Answered: no, do not change it** | - |
| Q7 | Voice typing someday (needs a separate app with the microphone)? | no | phase 8 |
| Q8 | Typing sound on keys? | off | KB-305 |
| Q9 | Which hand do you type with and do you use the tablet mostly upright (portrait)? | right thumb, portrait | KB-300 |
| Q10 | May the keyboard keep an opt-in private log of glides so Claude can tune it (you attach the file to a chat; it is never put in the repository)? | off; ask at KB-400 | KB-400 |
| Q11 | Weekly automatic backup of what it learned (a file in Downloads, optionally password-protected)? | off; offered at KB-408 | KB-408 |
| Q12 | Replace the stock keyboard on both devices only after a two-week trial (G5)? | yes | none |
| Q14 | Should finished APKs be reachable without signing in to GitHub (a tiny public `swoop-releases` repo, 11.0 step 5)? | no: sign in on the tablet | KB-003 |
| Q20 | Two presses (long press + "Translate to English") is the floor for received messages. If you want ONE press (a long press anywhere in any app), the only way is an accessibility service that reads other apps' screens: heavy on this tablet, invasive, restricted by Android for sideloaded apps. Do you want it as a later option? | no | phase 8 |
| Q21 | Should translate switch itself off after a while so it is never left on by accident (when the keyboard closes, after 10 minutes, after an hour), or stay on until you turn it off as you asked? | stays on until you turn it off (the setting exists) | KB-602 |

---

## 16. SOURCES AND HOW MUCH TO TRUST THEM

**Repository facts `[R]`:** all from `tjshea90/Browser` at `063b2761fff54bc7b3f5c69a44f72c41541ee202` (files named in section 3) and, for translation, from `tjshea90/LinkMeter` at `283d63ee6aa62b2f91c4a2788ad1af9ad14ec0a7` (section 3.3). Key research files inside it: `research/GLIDE_TYPING.md`, `research/GLIDE_RESEARCH.md` (its SOURCES section lists the papers below with URLs), `docs/KEYBOARD-EXPORT.md`, `docs/DICTIONARY.md`, `docs/HARDWARE.md`, `BRIEF.md`, `CLAUDE.md`, `HANDOFF.md`.

**Web results of 2026-11-16 `[W]` (search snippets only; open the page before relying on a detail):**
- W1 Android edge-to-edge enforcement for targetSdk 35: https://developer.android.com/codelabs/edge-to-edge and https://developer.android.com/develop/ui/views/layout/insets
- W2 IME inset handling pattern in a downstream framework commit (treat as a lead, not guidance): https://gitlab.e.foundation/e/os/android_frameworks_base/-/commit/9622684b2ce8803e4402b16b859ef910314aaa40 ; a third-party keyboard overlap bug with the same symptom: https://bugs.kde.org/show_bug.cgi?id=498126
- W3 HeliBoard (GPL-3.0, no internet permission, glide needs a closed library not shipped): https://github.com/HeliBorg/HeliBoard
- W4 FlorisBoard roadmap (glide/NLP rework in progress): https://github.com/florisboard/florisboard/blob/master/ROADMAP.md
- W5 FUTO Keyboard licence discussion (source-available, restrictions): https://discuss.privacyguides.net/t/futo-keyboard/18896 and https://futo.tech/source-first
- W12 MT8168 specs (quad A53 up to 2 GHz, Mali-G52): https://www.notebookcheck.net/MediaTek-MT8168-Processor-Benchmarks-and-Specs.483534.0.html
- W15 Gboard features from third-party guides (not Google's help pages): https://maketecheasier.com/master-google-gboard-for-android , https://www.androidauthority.com/gboard-features-3533885/ , https://nextpit.com/gboard-for-android-tips-and-tricks

**Added 2026-11-16 (private repository, name, text-selection menu, clipboard) `[W]` snippets only:**
- W21 GitHub Free private-repository Actions minutes (2,000 per month), older Enterprise-docs copies only: https://docs.github.com/en/enterprise-server@3.13/get-started/learning-about-github/githubs-plans ; check the live page https://docs.github.com/en/billing
- W22 Private-repository release assets need authentication: https://github.com/orgs/community/discussions/47453
- W23 KVM-accelerated Android emulator on GitHub-hosted Linux runners (private-repository billing not confirmed): https://github.com/ReactiveCircus/android-emulator-runner and https://github.com/actions/runner-images/discussions/7191
- W25 "Swoop" keyboard name search found no Android keyboard of that name: web search 2026-11-16 (absence of results, not proof).
- W26 Custom text-selection actions with `ACTION_PROCESS_TEXT` (manifest filter, one activity per action, `EXTRA_PROCESS_TEXT_READONLY`, must stay exported): https://medium.com/androiddevelopers/custom-text-selection-actions-with-action-process-text-191f792d2999
- W27 Android 10 clipboard rule (only the default input method or the focused app can read it): https://developer.android.com/about/versions/10/privacy/changes
- W28 Gboard "Translate as you type" (a toolbar tool: pick the two languages, type, see a preview): https://support.google.com/gboard/answer/7421372 ; third-party guide: https://www.androidcentral.com/apps-software/how-to-use-gboard-to-automatically-translate-as-you-type
- W29 Google's Tap to Translate (copy, then tap a floating bubble; needs the draw-over-apps permission), third-party how-to: https://www.technipages.com/tap-to-translate-google-translate

**Science behind the decoder (from the repo's research file; not re-read today):** Ouyang et al., finite-state-transducer keyboard decoder (https://arxiv.org/pdf/1704.03987); Sivek and Riley, spatial-model personalisation in Gboard (https://arxiv.org/pdf/2209.11311); Neural Search Space in Gboard Decoder (https://arxiv.org/html/2410.15575v1); Kristensson and Zhai, SHARK2 (https://pokristensson.com/pubs/KristenssonZhaiUIST2004.pdf); Fowler et al., language-model personalisation (https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/43272.pdf); Yin et al., hierarchical spatial back-off (https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/41930.pdf); FUTO swipe report (https://arxiv.org/html/2606.25247, self-published).

---

## APPENDIX A - Glossary
**IME** = input method editor = Android's name for a keyboard app. **Glide / swipe typing** = drawing a word by sliding over letters. **Pitch** = distance between neighbouring keys of a row; the decoder measures in pitches so keyboard size does not matter. **DP** = dynamic programming (the careful pass). **LM** = language model. **PSS** = proportional set size (the memory figure Android reports per process). **KGL1/KGL2** = the word-list file formats. **Shape** = a glide cut into 16 points in key units. **Collision set** = words whose ideal paths nearly coincide (for / fir / fur). **Shadow decoder** = a second decoder variant run silently to compare. **Gate** = an owner-visible milestone with pass criteria. **Ckpt** = the checkpoint command of the save ladder.

## APPENDIX B - File formats in the existing code `[R]`
- **KGL1 word list** (big-endian): `"KGL1" | n | blobLen | nDisplay | nPairs | totalCount(long) | len[n] bytes | blob (letters 0..25) | logProb[n] shorts (1/128 nat) | display entries (id int, len byte, utf8) x nDisplay | pairOff[n+1] ints | pairId[nPairs] ints | pairLogProb[nPairs] shorts`. Words are sorted by key sequence then text, so prefix ranges are contiguous and `idOf` is a binary search. English: 82,971 words, 242,342 pairs.
- **Personal file** (`glide_personal.txt`, one entry per line): `w word weight`, `p before word weight`, `t before2 before word weight`, `c guessed meant weight`, `r word recency`, `s word n u0 v0 ... u15 v15` (tenths of a key, u right of the centre of Q, v rows below it). Numbers use a dot always; weights positive and at most 100,000.
- **Export v1:** `KESTREL-KEYBOARD-LEARNING 1`, optional `habit x y`, `own word times`, then the personal lines, `end <entries>`; reader refuses wrong magic/future version, skips unknown lines, bounds everything, rejects files over 3,000,000 characters (spec: `docs/KEYBOARD-EXPORT.md`).
- **Caps:** 4000 words, 8000 pairs, 6000 triples, 3000 corrections, 1200 shape-words, 3000 shapes, 800 own words; weights halve when total passes 6000; save at most every 30 s via temp file + rename.

## APPENDIX C - Commands (sandbox and device) `[K]` unless noted
- Local checks: `./gradlew :core:test`; `./gradlew :ime:testDebugUnitTest`; lint `./gradlew :ime:lintDebug` (NewApi matters with `minSdk 29`).
- Developer-machine/emulator device commands (the owner may not have a PC; the CI lab runs them): `adb shell ime list -a`, `adb shell ime enable <package>/<service>`, `adb shell ime set <package>/<service>`, `adb shell settings get secure default_input_method`, `adb shell dumpsys input_method`, `adb shell dumpsys meminfo <package>`, `adb shell dumpsys gfxinfo <package> framestats`, `adb shell cmd package compile -m speed-profile -f <package>`, `adb install -r <apk>`.
- Recovery on the tablet without a PC: Settings > System > Languages & input > On-screen keyboard > manage keyboards (menu wording differs on this modified build), or the keyboard-picker notification/icon shown while typing.

## APPENDIX D - Experiments to run (each has a gate in 8.5)
E1 tablet decode/preview/"word shown" timings (G1). E2 PSS and cold-show on tablet. E3 full-width key size effect on accuracy (compare 56 dp pitch vs the browser's 29.5 dp) using shadow counters. E4 per-key touch model (taps, glide ends). E5 segment templates. E6 topic vector for glide and for next-word. E7 two-use rule vs immediate use on junk rate. E8 autocorrect thresholds vs revert rate. E9 bitmask prefilter speed/accuracy equality. E10 how often MyMemory's 5,000 characters a day run out over the carrier hotspot and how often Google shows its picture puzzle (9.1). E11 how often a service answers in formal "usted" for casual English (9.6).

## APPENDIX E - What to ask the owner to run on the tablet at G1 (plain words)
1. Install from the Release link. 2. Settings > Languages & input > enable "Swoop", then pick it when a keyboard shows. 3. Type a sentence of about 20 words in any notes/search box, tap-typing. 4. Glide 20 different words. 5. Open the app's Diagnostics screen and press Copy; paste into chat. 6. Say which feels slower than before and where. 7. Do not paste the learning file.

## APPENDIX F - What this report did NOT verify (honest list)
- No code was written or run; no test was executed; nothing was run on the tablet, on an emulator, or on the phone.
- **Nothing was run.** LinkMeter's translation code and notes were READ, not executed; how MyMemory and Google behave is as LinkMeter's notes recorded on 2026-11-12 and may have changed (Google's endpoint is unofficial). The Claude sandbox refused scripted network probes, so no live request was tried.
- GitHub free-minute figures (2,000 per month for private repositories) come from older GitHub Enterprise Server documentation copies; the Linux minute multiplier and KVM availability for private repositories were not confirmed.
- Browser repo claims come from reading source and docs at the pinned commit; I did not read `MiniNav.kt` outside the keys-view region, the app's Settings code, or the test files' contents (only their counts).
- All web claims are search-result snippets (section 16); the page bodies were not read. Items marked `[W?]` were thin or conflicting: IME inset behaviour on Android 15/16, FlorisBoard's licence and NLP status, FUTO licence details, GitHub's private-repository minute counting and KVM, and (marked `[T]`) whether GeckoView's selection menu shows `PROCESS_TEXT` entries and whether the clipboard rule is unchanged on Android 16.
- Android API behaviour tagged `[K]` is from general knowledge; confirm against the current developer documentation when implementing.
- Speed, memory and accuracy targets are proposals `[I]`.
- The Mexican vocabulary seed (Appendix H) is from general knowledge and needs a native reader.

## APPENDIX G - Templates for the new repository
**`CLAUDE.md` (keep under about 120 lines; auto-loaded each session):**
```
# <Keyboard name> - working agreement (auto-loaded every session)
## Mission
Very light, fast, offline Android keyboard (system IME) for ONE tablet (MT8168B, Android 10 Go, API 29, 2 GB, 32-bit, no Google services) and an Android 16 phone.
Glide + tap typing that keeps learning from the owner; Gboard-like features; optional translation to Mexican Spanish using LinkMeter's service and method (MyMemory then Google; a toolbar toggle OFF until turned on, any app; Enter translates, Enter again sends). The ONE keyboard app holds the INTERNET permission, used only by the `translate` package under the guard rails in plan 9.2. No accounts, telemetry or ads. The owner will NOT type Spanish.
Plan and findings: research/KEYBOARD-APP-PLAN.md. Job list: TASKS.md. Where the last session stopped: CHECKPOINT.md. New with no context: HANDOFF.md.
## FIRST ACTION OF EVERY SESSION - cold resume (assume the last session was cut off mid-step)
1. The SessionStart hook ran tools/resume.sh (sync state, warnings, CHECKPOINT, open tasks, INBOX/OUTBOX tails). Not seen? bash bootstrap.sh and bash tools/install-hooks.sh, tell the owner.
2. CHECKPOINT.md "Do this next" is authoritative. Do not re-plan finished work. Anything in INBOX that TASKS lacks is a request nobody wrote down.
3. Verify the safety net: git log --oneline -3 shows auto-checkpoint: commits; if not, run bash tools/ckpt.sh "did" "next" by hand after every step.
## Save ladder (work is safe only once PUSHED)
0 capture_inbox (every owner message, verbatim, before any work) | 0b capture_outbox | 1 autosave after every tool call | 2 bash tools/ckpt.sh "did" "next" after every completed step (make "next" specific: file, function, command, run id) | 3 ship.sh | 4 GitHub Actions builds, signs, releases.
New owner request: write it into TASKS.md in their words as [ ] boxes and ckpt BEFORE any code. Tick a box only when written, tested, committed; name the proof.
Low on usage? Spend what is left on ckpt with an honest "next". Owner messages arriving mid-task: keep going, add each to TASKS.md, answer every one, re-list open items at the end of each reply.
## Rules
- Dependencies: kotlin-stdlib only (+ androidx.profileinstaller if a baseline profile is made). No AppCompat/Compose/Material/Room/WorkManager/coroutines.
- :core is pure Kotlin/JVM, Java-8 API surface; no Android imports. Never block the UI thread. No largeHeap. No native code unless research/PERF.md proves the need.
- Manifest permissions are EXACTLY `INTERNET` (translation only); no ACCESS_NETWORK_STATE, RECORD_AUDIO, READ_CONTACTS (CI asserts the list). ALL network code lives in `app.swoop.keyboard.translate`; CI scans for network classes elsewhere; two hosts only; kotlin-stdlib is the only dependency. allowBackup=false. Tell the owner in plain words about any change under `translate/` or the manifest.
- Learn nothing in password fields or when IME_FLAG_NO_PERSONALIZED_LEARNING is set. Never log typed text.
- Never commit: learning exports (magic KESTREL-KEYBOARD-LEARNING), replay logs, keystores, APKs, owner photos/screenshots. The owner attaches files; never paste them into chat.
- Never add a push: trigger to any workflow (autosave pushes after every tool call; minutes are scarce). Tags + workflow_dispatch only.
- Signing key is PRIVATE (repo secrets). Never ship a tag twice. Record a release only after its run is green.
- Say honestly what was NOT run on the tablet. Answer every owner message in plain words.
- Kestrel browser repo (tjshea90/Browser) is READ-ONLY reference at commit 063b2761; another Claude account works there. Never push to it. Same for LinkMeter (tjshea90/LinkMeter, commit 283d63ee), the READ-ONLY reference for translation.
## Branches
Develop and push ONLY on the branch the session was assigned; never force-push; no PRs unless asked; main is the owner's call.
```
**`TASKS.md`:** one line per task `- [ ] KB-101 EditorBridge + state machine (plan section 6.4) - done when: >= 40 tests incl. fuzz`; claimed as `- [~] ... {branch time}` by `tools/claim.sh`; ticked `- [x] ... - evidence: <test or run id>`.
**`CHECKPOINT.md`** (written by `ckpt.sh`): date, branch, "Done since last card", "Do this next" (exact files/commands), open questions for the owner, last green CI run id, heartbeat trigger id if one exists.
**Heartbeat for long jobs (from the Kestrel rules):** one scheduled wake every 2 hours bound to the session (prompt: resume the project via `bash tools/resume.sh`, continue "Do this next", stand down if another session's real work is under 60 minutes old); put its id in every ckpt "next"; disable it when the job ships. A dead session cannot restart itself; a wake needs available usage.

## APPENDIX H - LinkMeter's Mexican-Spanish rules, in one glance (orientation only: `Translation.java` and its tests are the authority) `[R]`
| Spain / neutral output | Becomes in Mexico | Note |
| :- | :- | :- |
| coger, coge, cojo, cogí, cogiendo, cógelo ... | agarrar, agarra, agarro, agarré, agarrando, agárralo | vulgar in Mexico; "cojo/coja" meaning limping is left |
| chaqueta | chamarra | "chaqueta" is slang there |
| zapatillas | tenis (article and adjective follow: "unos tenis blancos") | "zapatillas" = a woman's heels there |
| conducir | manejar | not for "leads to" |
| alquilar, aparcar, enfadar (all forms) | rentar, estacionar, enojar | |
| patata, camarero, coche, piscina, apartamento, zumo, melocotón, aparcamiento | papa, mesero, carro, alberca, departamento, jugo, durazno, estacionamiento | |
| nevera, bolígrafo, tarta, gambas, ordenador, gafas | refrigerador, pluma, pastel, camarones, computadora, lentes | gender follows ("la nevera está vacía" -> "el refrigerador está vacío") |
| móvil (the phone only), guay, "vale" as OK | celular, padre, ok | never "datos móviles", "vale la pena", "¿cuánto vale?" |
| vosotros, vuestro, estáis, tenéis ... | ustedes, su, están, tienen ... | Mexico has no vosotros |
Texting shorthand (u, r, k, idk, tbh, ngl, imo, btw, lmk, wyd, hbu, brb, nvm, omw, 2nite, tmrw, rn, b4, gr8, bday, ppl, bc, cuz, w/, w/o ...) is written out before a text goes to Google only (MyMemory reads it well by itself). Not converted on purpose: currencies, units, dates.
