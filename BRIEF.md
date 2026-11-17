# BRIEF - decisions that do not change (read once; the full reasoning is in research/KEYBOARD-APP-PLAN.md)

## What this is
**Swoop**: a standalone Android system keyboard (an IME) built on the glide-typing engine of the owner's Kestrel browser. Own repository (`tjshea90/Keyboard`, PRIVATE), own app (`app.swoop.keyboard`, module `:ime`), one APK built and signed by GitHub Actions.
"Swoop" is a placeholder name: one string resource and one applicationId. The magic string `KESTREL-KEYBOARD-LEARNING` and the word-list magic `KGL1` are file formats shared with the browser: never rename them.

## The devices (docs/HARDWARE.md has the long form)
- **Tablet**: MediaTek MT8168B ("Alps 711"), quad Cortex-A53 (in-order, slow), Android 10 **Go** (API 29), 2 GB RAM (`low_ram`, 128 MB heap class), **32-bit userspace**, 600x973 px at 170 dpi (~564x916 dp, font scale 1.2), **no Google Play, no Google services**, internet only through a phone hotspot (~5 Mbit/s cap, flaky; unmetered per the owner).
- **Phone**: Android 16. Code is written for API 29 and branches on `Build.VERSION.SDK_INT` above it. Edge-to-edge / IME insets on Android 15/16 need a real check (KB-107).
- No emulator acceleration in the cloud sandbox: say honestly what was never run on a device. GitHub's Linux runners DO have KVM (emulator lab, KB-703).

## The five decisions that shape everything (plan section 1)
1. **A keyboard sees every keystroke.** One app (owner's Q15), so the `INTERNET` permission lives inside it, used ONLY by the optional translation (`app.swoop.keyboard.translate`, phase 6). CI-enforced guard rails (plan 9.2): exact permission list (`tools/expected-permissions.txt`), all network code in one package with a source and dex scan, two hosts, kotlin-stdlib as the only dependency, one gate, nothing sent unless the owner turned translate on, a connection log that never holds the words. Until KB-601 the list is EMPTY: the keyboard cannot reach the network at all.
2. **Pure Kotlin decoder first**; native code only if measured on the tablet to be necessary (`research/PERF.md`).
3. **The learning engine is the product** (plan section 8): layered scoring (shape + language + personal + context), per-key touch model, shape memory, a replay lab, strict accuracy gates.
4. **Translation reuses LinkMeter's proven online method** (MyMemory es-MX, then Google, then LinkMeter's Mexican-Spanish layer; friendly "tu"; Enter translates, Enter again sends; Spanish into English for received messages). No model, no key, no account. Toolbar toggle, OFF until the owner turns it on, any app. The owner will NOT type Spanish: no Spanish layout/dictionary/bilingual decoding.
5. **Safety first for a default keyboard:** a broken keyboard on a tablet with no other keyboard locks the owner out of typing. KB-106 (crash-loop fallback, globe key, "go back to my previous keyboard") comes before any feature work beyond the skeleton. The old keyboard stays installed until gate G2.

## Owner decisions (2026-11-16) - they override anything older
Q1 name Swoop | Q2 repo (private at first; the owner made it PUBLIC on 2026-11-17: free Actions minutes and emulators, chat copies are public) | Q3 no Spanish typing | Q4 friendly "tu" and Spanish-to-English for received messages | Q5 LinkMeter's service and method, online | Q15 everything in ONE app | Q16 translate works in any app but only while turned on (Gboard-style toolbar button) |
Q17 in apps with their own Send button, Enter translates and the owner taps the app's Send | Q18 received messages: the fewest presses (selection-menu route = 2) | Q19 do NOT change LinkMeter. Open questions with defaults: plan section 15.

## Privacy (the keyboard handles intimate data)
- Never commit: learning exports, personal model files, replay logs, keystores, APKs, screenshots/photos of the owner's screen. `.gitignore` + `tools/secretscan.sh` enforce it; the capture hooks refuse to copy a pasted export (or a message over 20 KB) into INBOX/OUTBOX. The owner ATTACHES files to a chat; sessions read them from the scratchpad and put only numbers in the repo.
- No learning, no replay log, no clipboard capture in password fields or when `IME_FLAG_NO_PERSONALIZED_LEARNING` is set. No logging of typed text. Diagnostics = counters and timings only.
- `allowBackup=false`, `usesCleartextTraffic=false`, exported only the IME service, the launcher activity and (phase 6) the translate-menu activity.

## Engineering rules (plan 2.3)
kotlin-stdlib only (no AppCompat/Compose/Material/Room/WorkManager/coroutines); `:core` pure JVM on the Java 8 API surface (`-Xjdk-release=8`); never block the UI thread; no `largeHeap`; zero allocation on the tap path; the keyboard does nothing while idle (no timers, no services other than the IME, no boot receiver, no background network).
Budgets (plan 7.1, proposals until measured on the tablet): APK < 6 MB, PSS < 40 MB with the keyboard up, < 25 MB hidden, glide lift -> word p95 < 100 ms, cold first show < 400 ms.

## The references (READ-ONLY, another Claude account works in them)
`tjshea90/Browser` at commit `063b2761fff54bc7b3f5c69a44f72c41541ee202` (the glide engine; checked out at `/home/user/Browser` in the cloud sandbox when present: read with absolute paths or `git -C`, NEVER `cd` there, its hooks would run) and `tjshea90/LinkMeter` at `283d63ee6aa62b2f91c4a2788ad1af9ad14ec0a7` (translation; fetched at KB-600). Blob ids of every file to port are in plan section 3.1: `git -C <ref> rev-parse --short=10 HEAD:<path>` must match before copying.

## Gates (owner-visible milestones: stop and report in plain words, say what was NOT run on the tablet)
G1 first release on tablet + phone with real numbers (KB-107) | G2 a week of daily driving (KB-207) | G3 every P0/P1 parity row done or consciously deferred (KB-306) | G4 learning report vs targets (KB-409) | G5 two weeks as the daily driver (KB-705).
