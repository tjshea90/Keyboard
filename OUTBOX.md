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
