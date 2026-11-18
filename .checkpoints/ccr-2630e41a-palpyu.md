# CHECKPOINT 670 — read me first, then TASKS.md

**Written:** 2026-11-18T19:38:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `1fd6368` (this checkpoint is the commit after it)

## Just done
wrote owner's R24a (translation SocketException) + R24b (more glide research) into TASKS.md; read Translator/HttpsTransport/TranslationEngine: error text shows only class name and both services fail the same way => systemic socket problem on the device (EPERM/reset), not one flaky service

## Do this next
R24a: read ime call path (grep Translator/TranslateGate in ime), check .github/workflows/emu-smoke.yml for an emulator hook, then write translate/ConnectionProbe + safer error reasons + one retry + Diagnostics button + dispatch-only service-check workflow; R24b after: read research/R23c-gesture-length.md + R20-glide-personalization.md first

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  9020608 ckpt 667: Swoop 0.7.0-alpha3 RELEASED (run 44254953577 green, APK on Release swoop-v0.7.
  4c4d8bf ckpt 663: R22 Kestrel port in progress on Browser branch claude/keyboard-caps-space-resi
  3d8c0e4 ckpt 658: 0.7.0-alpha3 (code 14) shipped to GitHub, android.yml dispatched ONCE at ~06:5
  06f1fac ckpt 657: gated swoop-v0.7.0-alpha3 (ime code 14) and pushed it: handoff self-test, unit
  9995f5d ckpt 656: pre-ship: 0.7.0-alpha3: hold Backspace to delete faster then whole words, clip
  74e5d4d ckpt 649: R23c gesture-length term evaluated (research/R23c-gesture-length.md, GlideLeng
  0db3f9a ckpt 639: R23a-e core+views done: held Backspace speeds up then deletes words (KeysView+
  649cee3 ckpt 623: wrote owner's third batch R23a-e into TASKS.md (backspace hold accelerates to 
  4d9bb2b ckpt 615: R22 plan written: docs/KESTREL-PORT-R22.md. Browser repo cloned shallow at /ho
  ca9737d ckpt 613: wrote owner's R22 'put all the updates into kestrel keyboard' into TASKS.md. s
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
