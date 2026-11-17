# OUTBOX - what Claude told the owner (the last reply of every turn), newest last. Written by tools/capture_outbox.sh on the Stop hook; key-shaped strings are redacted and long replies cut in the middle. The mirror of INBOX.md.

## 2026-11-17T00:34:49Z
> Phase 0 is done and Phase 1 is built except the first release, which needs one thing from you (item 1 below). None of it has been run on a real keyboard yet: this cloud machine has no emulator, so nothing has been tried on your tablet or phone.
>
> The CI check run passed. A full CI build is still running, and its test job and gates passed. I don't yet know whether its build step passed:
> https://github.com/tjshea90/Keyboard/actions/runs/43988177865
>
> ## What's done in the Keyboard repo
>
> - **Save system:** Copied from the browser repo into the Keyboard repo and renamed Swoop. Every tool call is auto-saved and pushed. I added privacy guards: learned data, replay logs, APKs and screenshots of your screen can't be committed. A pasted learning file, or any message over 20 KB, is never copied into the repo.
> - **Project plan and docs:** The plan is copied into `research/`. `CLAUDE.md`, `BRIEF.md`, `HANDOFF.md` and `TASKS.md` are written. A new session can resume from `CHECKPOINT.md`.
> - **Engine:** The browser's glide engine is ported with its tests, and the word list is byte-identical.
> - **Typing logic:** A new typing layer that works on top of any app's text box. I tested it with random typing sequences, many thousands of them. That found and fixed four real bugs in my own first draft. It also found one rare timing problem I did not fix (see below).
> - **Keyboard:** Full-width keys, glide with a trail and suggestion strip, Backspace repeat, hold a top-row letter for its digit, slide on space to move the cursor, two-thumb typing, and layouts for text, URL, e-mail, number and phone fields.
> - **Settings screens:** A setup screen with a glide on/off switch and export/import of what it learned (the browser's file imports too). A Diagnostics screen with a Copy button holds numbers only, never anything you typed.
> - **Safety:** A globe key on every layout, a "go back to my previous keyboard" button, and a safe mode. After three crashes in a minute it falls back to plain keys with no glide.
> - **Tests:** Core logic has 233 tests and the app has 78, all passing with lint green. A signed release APK built and verified locally with a throwaway key.
>
> ## What was not run
>
> Phase 1 is built except KB-107, the first release. KB-100, the IME service and setup screen, is deliberately not ticked off, because its test needs a real Android. The cheapest next fix is a free emulator test in CI, which GitHub runners allow.
>
> - **Not run on any device:** emulator, tablet or phone.
> - **Android 16:** not checked.
> - **Speed:** the glide speed on the tablet's slow processor is unmeasured.
> - **Known rare problem:** if Android is slow to tell the keyboard where the cursor is after Enter or a long cursor slide, the keyboard's idea of the text near the cursor can be wrong for a moment. It repairs itself on the next change. It's recorded as KB-101b, and the Diagnostics screen shows counters for it.
> - **Browser repo:** I only read it. Once I ran a read-only `git status` inside its folder; nothi
> [... 236 characters omitted ...]
>  repo. Instead I built a way to create the key inside GitHub. You add one secret in your browser:
>    - Open the Keyboard repo on github.com, then Settings, Secrets and variables, Actions, New repository secret.
>    - Name it `SWOOP_KEYSTORE_PASSPHRASE`. For the value, invent a sentence of 20 or more characters and keep it somewhere safe.
>    - Tell me "secret added". I then run the one-time key creation and the first signed build.
>    If you'd rather do it the plan's four-secret way, say so.
> 2. **Wake-up after a usage cut-off:** may I set up one recurring wake every 2 hours that resumes the work? It costs a little usage each time. I haven't created it.
> 3. **Later, no action now:** LinkMeter access (when I reach the translation work) and exporting the browser's learned file (when the import task starts).
>
> Next I'll do the emulator test in CI, then the tap-typing decoder and word suggestions.
<!-- sha:46edfced5400 -->

## 2026-11-17T00:53:55Z
> The repo is public now, so before the signing step I'm checking three things: that it really is public, how the earlier full CI build ended, and what the Browser's emulator workflow looks like (read-only).
<!-- sha:1ddc9971f101 -->

## 2026-11-17T00:58:40Z
> The first release is blocked on one thing only you can do. The signing secret isn't set on the repo, so GitHub refused to create the signing key. Until it exists I can't sign a release that will update cleanly later.
>
> **What to do (about a minute, on github.com)**
> 1. Open the Keyboard repo, then **Settings → Secrets and variables → Actions → New repository secret**.
> 2. Name: `SWOOP_KEYSTORE_PASSPHRASE`.
> 3. Value: **five or more random, unrelated words**, written as one line. The repo is public, so the encrypted key file is too, and a guessable sentence could be attacked offline. I raised the key file's slowdown to 10 million rounds, but a strong passphrase still matters.
> 4. Reply "secret added". I then create the key once, build, sign and publish `swoop-v0.1.0-alpha1` through GitHub Actions, and give you the Release link.
>
> **Already done for the release**
> - The repo is public. I updated the docs, hook text, Release text and CI notes that said "private", and added a README.
> - The Actions test build ran green with a debug test APK. The signing path is tested locally, but the real signed build hasn't run yet.
> - I started an emulator test on a GitHub runner (Android 10 emulator). It checks that the keyboard registers, can be switched on and chosen, shows in a text field, types real key taps, deletes with Backspace, accepts a glide, survives the globe key, and doesn't crash. It's the first test on a real Android. It's still running, and I'll check it in about 8 minutes and fix it if it fails.
>
> **Two things to know**
> - **Chat is public now.** The hooks copy our chat into `INBOX.md`/`OUTBOX.md` in the repo, so never paste a secret, passphrase or anything personal here. Say so if you want those chat copies switched off. That would weaken resuming after a usage cut-off.
> - **Not run yet:** the keyboard on the tablet or the phone, and Android 16.
>
> **Still open:** the secret (item 1 above), whether I may set a 2-hourly wake-up after usage cut-offs, and later the LinkMeter access and your browser's learning export.
<!-- sha:4633ce8db64d -->

## 2026-11-17T01:02:02Z
> Understood: the same setup as your other repos. That means a public dev signing key committed in the repo, and GitHub builds and signs with it, with no secret and no passphrase. I'm switching to that now. First I'm checking the emulator run.
<!-- sha:cdd12f89df98 -->

## 2026-11-17T01:17:16Z
> The first Release is out: GitHub Actions built and signed `swoop-v0.1.0-alpha1`, and the build checked the signature on the exact file it published.
>
> https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.1.0-alpha1
>
> **Signing, your way.** I dropped my secret and passphrase idea. Like the Browser repo, the dev key is committed in `signing/dev/` and GitHub signs with it, so no secret is needed. A second Android 10 emulator test passed on a GitHub runner. It installed the app, switched the keyboard on and chose it, showed it in a text field, typed "hi" from real key taps, deleted with Backspace, took a swipe as a glided word, survived the globe key, and logged no crash. That is the first time any of this has run on a real Android.
>
> **Install it**
> 1. Open the Release link on the tablet and tap the `.apk` file.
> 2. Open the Swoop app, switch the keyboard on and choose it.
> 3. Keep your old keyboard installed. The globe key switches back.
>
> Only install Swoop APKs from this repo's Releases. The signing key is public, so an APK from anywhere else could be signed to look like an update.
>
> **To clear this stage (G1)**, paste the Diagnostics numbers here once you've typed on the tablet and phone: Swoop app, Diagnostics, Copy. They hold counters and timings, never what you typed. The Android 16 phone is untested, and I can also run the emulator test on Android 16 if you want.
>
> **Still open**
> - **Wake-ups:** may I set a 2-hourly wake-up that resumes work after a usage cut-off? It costs a little usage each time.
> - **Next steps:** the tap-typing decoder and word suggestions come next, then LinkMeter translation when we get there.
> - **Public chat:** the repo is public, so our chat copies in `INBOX.md`/`OUTBOX.md` are public too, and I'll leave them on unless you say to switch them off.
>
> The first publish attempt failed once for a reason I couldn't see in the logs. A re-run of the same version went through, and I added error reporting so a repeat would show its reason.
<!-- sha:39d4255c8ab3 -->

## 2026-11-17T01:17:34Z
> That notification was the reminder I scheduled earlier to check the emulator run and finish the release. Both are already done: the emulator test passed on run 43993756467, and the release is published, so there is nothing left for it to do. I had already deleted the trigger, so it won't fire again.
>
> Nothing has changed since my last message. Still open:
> - The Diagnostics numbers from the tablet and phone, to clear this stage (G1).
> - Whether I may set up a 2-hourly wake-up after usage cut-offs.
<!-- sha:f5db3abdeec3 -->

## 2026-11-17T01:43:24Z
> You sent nine requests. Per my own rule I'm writing them into `TASKS.md` in your words first, then working through them in an order I'll state below. I'm starting with the view work (numbers row, instant glide, faster trail, quicker key pop-up), then suggestions and learning, then options and the settings redesign. The floating keyboard goes last because it's the largest.
<!-- sha:48ec47c49a87 -->

## 2026-11-17T02:31:42Z
> **Swoop 0.2.0-alpha1 is built, signed by GitHub and published.** It installs over the first alpha because the signing certificate is the same:
>
> https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.2.0-alpha1
>
> **What's in it, against your list:**
> 1. **Numbers row** is always on top, on the letters and the symbols page.
> 2. **Faster trail:** it is drawn on its own small layer and touches are delivered unbuffered.
> 3. **Floating keyboard:** not in this build. I'm on it now.
> 4. **Word suggestions with taps**, even with glide typing off. Tap a word in the strip to use it. A typed word that isn't a word shows in quotes so you can keep it.
> 5. **Instant glide:** it starts after about 5 dp of movement instead of at the key edge.
> 6. **Learning:** it learns your words, word pairs, three-word habits and corrections in every app. It also learns from a word you erase and retype, and from words you pick from the strip. A space fixes an obvious typo, one Backspace takes it back, and that word is never changed again. In my simulated test, next-word hits went from 0% to 96% after 300 sentences, and the first-letter completion hit rate from 63% to 98%.
> 7. **Options:** space after punctuation, double space for a full stop, capitals after a full stop, capital I, slide on the space bar, key pop-up, vibration, key sound, key height and light/dark/automatic colours.
> 8. **New settings screen:** a status card, a live copy of the keyboard that follows every change, and grouped cards with six accent colours. It also has Export, Import and Forget.
> 9. **Key pop-up:** it is removed at the moment of lift, before the key is sent to the app.
>
> **Not tested on your devices:** I can't run your tablet or phone here. Everything above is checked by automated tests (core 271, app 102, lint clean) and by a rendered picture of the settings screen. I haven't verified how fast the trail and pop-up feel, or how it behaves on Android 16. Please tell me what's slow or wrong.
>
> **Space after punctuation:** a bare address typed in a message, like `example.com`, can't be told apart from `ok.thanks`, so it gets a space. Address fields, anything with `@` or `/`, numbers and abbreviations like `e.g` are left alone, and there is a switch.
>
> **Open items:**
> - **N3 (floating keyboard, neon bar):** in progress.
> - **Your device check of the 0.2.0 items:** waiting on you.
> - **2-hourly wake-up after usage cut-offs:** still your call.
> - **Reading the LinkMeter repo for translation:** still your call, needed later.
>
> Back to the floating keyboard.
<!-- sha:90b2e9b73e95 -->

## 2026-11-17T02:49:12Z
> Waiting on the emulator run. Here is where things stand:
>
> - **0.2.0-alpha1:** don't install it. I found a bug in my own tests where the keyboard window can cover the whole screen.
> - **Fix:** it is written and tested (core 276, app 119, lint clean). I'm now proving it on a real Android 10 emulator on GitHub, together with the floating keyboard: a long press on the glowing bar lifts it off, the window turns into the whole screen, and the app behind still takes touches.
> - **Next:** if the emulator run is green, I'll ship 0.2.1-alpha1 and send you the link. If it shows a problem, I'll fix that first.
>
> **Still open:**
> - Your device check of the 0.2 features.
> - The 2-hourly wake-up after usage cut-offs, which is still your call.
> - Reading the LinkMeter repo for translation, still your call, needed later.
<!-- sha:7c918a39ad24 -->

## 2026-11-17T02:56:38Z
> The emulator run is going. I'll read the result when the timer finishes.
<!-- sha:7b80250bf59a -->

## 2026-11-17T03:03:36Z
> The emulator run is going. The new diagnostics should show where the long press on the neon bar stops.
<!-- sha:d55f213ef129 -->

## 2026-11-17T03:11:04Z
> Last emulator confirmation is running on the fixed build. If it still shows 0 failures, I'll ship 0.2.1-alpha1.
<!-- sha:d57e745efc73 -->
