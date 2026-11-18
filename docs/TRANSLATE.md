# Translate (English to Mexican Spanish) - how it works

Source of the method: the owner's LinkMeter app (read-only reference, commit 283d63ee, `docs/TRANSLATE.md`, `Translation.java`, `Translator.java`,
`TranslateFlow.java`). The logic is ported to Kotlin; the plan is `research/KEYBOARD-APP-PLAN.md` section 9. This file describes what the code
does today. The owner never types Spanish.

## What the owner sees (plan 9.3)
A Translate button on the keyboard toolbar (next to clipboard and settings). It is OFF until the owner turns it on. While it is on, Enter in an
ordinary text field translates the field's English into Spanish (Mexico) and puts the Spanish in the field for checking; Enter again does the
normal Enter / editor action (send). Undo gives the English back; "Send my English" sends it untranslated. Any failure leaves the English text
exactly as it was.

## Where the code lives
| Part | Where | Network? |
| :- | :- | :- |
| Pure logic: pieces, URLs, answer parsers, shorthand, Mexican safety layer, friendly-tu guard, the engine, the Enter state machine | `core/.../app/kestrel/core/translate/` | no (a test fails on any `java.net`) |
| The only network code: `HttpsTransport`, `Translator`, `TranslateGuard` | `ime/.../app/swoop/keyboard/translate/` | yes |

### Core
- `Translation` - `Way` (TO_MEXICAN, TO_ENGLISH), `pieces` (cut long text), `myMemoryUrl`, `googleUrl`/`googleForm`, `parseMyMemory`, `parseGoogle`,
  `expandShorthand` (texting shorthand written out, for Google only), `toMexican` (words from Spain made Mexican; `coger` -> `agarrar` etc.).
- `TranslationEngine(http, log)` - LinkMeter's method: MyMemory, then Google when MyMemory fails, then `toMexican`, then the friendly-tu guard.
  Blocking; takes an `Http` (the one door to the network) so tests use a fake. Polls a `cancelled()` flag between pieces.
- `FriendlyTu` - detects clear "usted" markers when the English talks to "you"; the engine then asks the other service once and shows the
  first answer unchanged if both are formal (`TranslateResult.formal = true`, for a Diagnostics counter). It never rewrites verbs.
- `TranslateFlow` - states OFF / IDLE / TRANSLATING / TRANSLATED, auto-off timer, stale-answer protection, Undo, Send my English.
- `TranslateResult` + `ErrorCode` - result with a whole-sentence reason and a code: NO_INTERNET, TIMEOUT, BUSY, SERVICE_ERROR, BAD_ANSWER,
  TOO_LONG, NOTHING_TO_TRANSLATE, FIELD_CHANGED, CANCELLED, UNEXPECTED.
- `Json` - tiny parser (the core cannot use `org.json`).

### Android side (the only network code)
- `Translator(log)`: `translate(text, done)`, `translate(text, way, done)`, `cancel()`. One daemon worker thread that exits when idle. Each request
  has an increasing id; a cancelled or superseded request is dropped without a callback and its connection is aborted.
- `HttpsTransport`: `HttpURLConnection`, HTTPS only, connect 8 s, read 15 s, 30 s total, answer cap 256 KB, redirects NOT followed, no cache,
  `Connection: close`, `User-Agent: Swoop`. Error messages carry only an exception class name, never an address (the address holds the text).
- `TranslateGuard`: the host allowlist, checked before any connection is opened.

## The two hosts (and nothing else)
1. `api.mymemory.translated.net` - MyMemory by Translated, free, no account. `GET /get?q=<text>&langpair=en|es-MX&onlyprivate=1&mt=1`
   (`es-MX|en` for the other direction). `onlyprivate=1` asks for the machine translation only (without it a human-made match, often a program
   button, can win). Limits found by LinkMeter: about 5,000 characters a day anonymous, at most 500 bytes per question (the code cuts at 450 bytes).
2. `translate.googleapis.com` - Google's unofficial endpoint behind Gboard's translation, free, no account. `POST /translate_a/single?client=gtx&sl=en&tl=es&dt=t`
   with `q=<text>` in the body. It can change or block without notice; it is the fallback only. It may answer a redirect to its picture puzzle or
   HTTP 429/503: treated as "busy".
Licences/terms: both are public free web endpoints used without keys, as LinkMeter does; no SDK, no library, no account, no e-mail is sent.

## What is sent
Only the text the owner chose to translate: the whole content of one field, only after Enter with the toggle on (or an explicit "translate to
English" action), at most 1,000 characters, over HTTPS. No app name, no account, no identifier, no other message. The text is never logged: the
optional connection log gets service, milliseconds and result only. Limits per question: 450 bytes (MyMemory), 4,000 bytes (Google); longer text goes in pieces cut at line
breaks, sentences, words, then letters (never inside an emoji).

## Failure behaviour
| Situation | What happens |
| :- | :- |
| Phone data / hotspot off | NO_INTERNET: "The tablet is not on the internet ... is the phone's mobile data on?" Text untouched. |
| MyMemory used up (429, `quotaFinished`, "MYMEMORY WARNING", status 429) | Google answers. |
| Both busy | one sentence: "Both free translation services are busy or used up for today." Offer: send the English. |
| Timeout | TIMEOUT: "The translation service did not answer in time." (8 s connect, 15 s read) |
| Answer unreadable / empty / over 256 KB | BAD_ANSWER, text untouched |
| Field changed while waiting | the answer is dropped (FIELD_CHANGED); `Translator.cancel()` is called on edits |
| Toggle off / leaving the field | `Translator.cancel()`, `TranslateFlow.setOn(false)` / `leaveField()`: late answers are ignored |
| Formal "usted" answer for a casual text | the other service is asked once; both formal: shown unchanged, flagged |

## R24 (2026-11-18): "could not be reached (SocketException)", and "it won't turn off"
- **Failures say which step broke.** `TransportException.Kind` now has DROPPED (a connection was made and then reset / aborted / closed / cut off), BLOCKED (Android refused the socket: EPERM / EACCES),
  SECURE (the secure handshake failed: certificate, wrong device date) besides NO_INTERNET / TIMEOUT / TOO_BIG / OTHER. `HttpsTransport.classify` decides from the exception alone; the error carries a
  `SafeReason` (core): the class name plus an errno name from an allowlist or a known harmless phrase, NEVER an address or text. Plain-words sentences are in `TranslationEngine.failure`.
- **One quick retry.** A DROPPED connection is retried once after 600 ms on a fresh connection (not for timeouts, blocks, handshakes; an abort ends the wait). The JDK and Android's own HTTP stack already retry one
  dropped connection silently, so this is the second line, mostly for a flaky hotspot.
- **Diagnostics > "Test translation connection"** (`ConnectionProbe`, the second file that may open a socket; the guard test lists both): internet permission, device clock, then per service name lookup, TCP connect (IPv4
  and IPv6 separately), secure handshake and a real answer for the fixed word "hello", ending with a verdict in plain words. Contacts only the two hosts; no typed text, no addresses in the result.
  **Diagnostics also lists the last 20 connections** (`ConnectionLog`: time, service, ms, result code and safe reason; never words).
- **Evidence the services work:** `RealServicesCheck` (skipped unless `SWOOP_REAL_SERVICES=1`) runs the real engine through the real transport; it passed from the build sandbox and from a GitHub runner on 2026-11-18
  (MyMemory answered all three sentences; Google answered the probe word). `emu-smoke.yml` with input `mode: net-probe` runs the keyboard's own probe on Android 10 and Android 16 emulators (`tools/emu/net_probe.sh`).
- **Toggle (R24c):** turning translating OFF works in every field (it used to refuse in a password box / address bar / with the Settings switch off, because "not available in this box" was checked first). Enter never
  translates where translating is not allowed (password box, address bar, safe mode, Settings off), whatever the toggle says. The key's lit state is set from the real state whenever a field starts and
  whenever a keys view is attached. The Settings switch "Translate icon" off also switches the toggle off.

## R25 (2026-11-18): "Android is not letting the keyboard use the socket" - what each Android limit does to a background app (measured)
Owner's connection test said Android refuses the keyboard a network socket (EPERM / EACCES). Measured on an Android 10 emulator (GitHub runner, `emu-smoke.yml` mode net-probe with scenarios=true; the probe runs with the app
sent to the background, the way a keyboard process runs):

| limit | what the app sees |
| :- | :- |
| Data Saver on, Wi-Fi metered, Swoop NOT on the Unrestricted data list | every connect: **ECONNREFUSED** (Kind.REFUSED, "the connection was refused ... usually Data Saver") |
| same, Swoop on the Unrestricted data list (`cmd netpolicy add restrict-background-whitelist`) | works |
| Battery saver on | connect **times out** |
| Doze (force-idle) | connect **times out** |
| App standby, rare bucket, device unplugged | works |

So none of Android's data / power limits gives EPERM / EACCES. A socket refusal means: the process is not in the `inet` group (EACCES from every socket() on the 4.9 kernels of Android 10 tablets; can happen after an update that
ADDED the INTERNET permission, Swoop did in 0.4.0, until a restart: the probe reads `/proc/self/status` below API 33 and says so), or a firewall / VPN / ad-block app, a vendor "block this app's internet" switch, or a parental-control /
school profile (netfilter DROP gives EPERM). The data-saver "refused" case has its own message; the diagnostics screen has buttons for the Unrestricted data page and the app settings. Data Saver's state cannot be READ by
Swoop (ConnectivityManager.getRestrictBackgroundStatus needs the network-state permission, which Swoop does not hold): the probe says so instead of guessing.

## Guard rails (plan 9.2) provided by this work
- `tools/expected-permissions.txt` lists exactly `android.permission.INTERNET`; the manifest has that one `uses-permission` and
  `android:usesCleartextTraffic="false"`; `TranslateGuardTest` checks both, and that no other URL/host appears in the translate package.
- `TranslateGuardTest` also fails if any other Kotlin file mentions `java.net`, `javax.net`, `android.net`, `okhttp`, `HttpURLConnection`, `Socket(`
  or an `https://` literal; `TranslateCoreGuardTest` does the same inside `:core`.
- Still to do by the main session (not done here): the `TranslateGate` (eligible fields, exclusions), the dex scan and the dependency fixture in CI,
  a network-security-config xml, the old `KeysServiceTest.noPermissionsAreRequested` must now expect INTERNET.

## How the UI uses it (summary)
```
val flow = TranslateFlow(autoOffMs = ...)         // one per keyboard, :core
val translator by lazy { Translator(log) }        // created only when the toggle is turned on
// Enter key:
when (flow.enter(fieldText, now)) {
  SEND      -> performNormalEnter(); flow.sent()
  TRANSLATE -> { val n = flow.start(fieldText, now)
                 translator.translate(fieldText) { r -> ui.post { flow.answer(n, currentFieldText(), r, now)?.let(::replaceField) ?: showError(flow.why()) } } }
  TOO_LONG  -> showError(flow.why())
  NOTHING   -> {}
}
// field edited by the app / typing (not by us): if (flow.fieldChanged(text, now)) translator.cancel()
```

## Tests
Recorded-style answers only (hand-written in the services' documented shapes; no test touches the real services): `TranslationTest`,
`TranslationEngineTest` (fake `Http`), `FriendlyTuTest` (30 casual sentences), `TranslateFlowTest` (incl. random-event fuzz),
`JsonTest`, `TranslateCoreGuardTest` in `:core`; `HttpsTransportTest` (real transport against a local `com.sun.net.httpserver` fake),
`TranslatorTest`, `TranslateGuardTest` in `:ime`.

## What was built (session note)
Built by a helper agent for KB-600 (logic port), KB-601 (Translator + guard rails partly) and KB-604 (friendly tu); nothing ticked in TASKS.md.
Not ported from LinkMeter: its 56,000-check suite and the 36-breakage mutation script (a fresh, smaller suite was written instead; porting
`tools/test/TranslationTest.java` and `tools/mutate_translate.py` is still open for KB-600's "same checks, same breakages" criterion).
Not verified: the real services were never called (unit tests use fakes and a local server), the Spanish tables were ported from the Java source read through
the GitHub tools and spot-tested only, nothing was run on the tablet or an emulator.
