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
