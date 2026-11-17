# CHECKPOINT 397 — read me first, then TASKS.md

**Written:** 2026-11-17T22:28:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `3fbce27` (this checkpoint is the commit after it)

## Just done
BOTH RELEASES PUBLISHED AND RECORDED: Swoop 0.5.0-alpha4 (https://github.com/tjshea90/Keyboard/releases/tag/swoop-v0.5.0-alpha4, code 9, APK 1.97 MB, run 44187302136, commit 7323b76, en.klx inside the APK = the new list sha aec767c5...) and Kestrel G 0.3.0-alpha68 (https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha68, code 169, APK 96.1 MB + .dm, Browser run 44188402392, commit cef3338a, recorded by 24421394 on my branch ccr-5531c47b-hl1gzo; the other lane ccr-e89b9dfe-pl3lk5 untouched and idle; next free number for it alpha69 / code 170). Simulation read: pages (2 runs: 10 sites load fine at full speed; pypi.org and a 1080p film each took the EMULATOR's graphics down: SurfaceFlinger on software GL, system_server watchdog loop; not a Kestrel defect shown), films (Kestrel's own player inline: first picture 1202 ms, 0 stalls, 5 dropped of ~300 frames, software decoder), findings in Browser docs/handoff/SIM_FINDINGS_2026-11-17.md. After the release I also found the book pairs made the strip offer vulgar words unprompted (alpha3: 15 of 5000 contexts, alpha4: 6): Suggester.VULGAR now keeps them out of NEXT-WORD guesses only (0 of 5000, test PredictionYardstickTest.theStripNeverOffersAVulgarWordOnItsOwn, SuggesterTest) - committed on my branch, NOT in alpha4. dailysim.py: pypi.org and the 1080p film removed from the lists, a scene stops after 3 dead sites, film check no longer flags '0 stall(s)'.

## Do this next
1) read the typing run 44187463954 (new word list on the emulator; artifact daily-sim-typing) and the hotspot pages run 44187266683 when they finish: any regression in Swoop alpha4 (typing exact? autocorrect? memory) means a Swoop alpha5 (also carries the VULGAR filter); if clean, the vulgar filter simply waits for the next release; 2) final plain-words report to the owner with both Release links (already published, recorded in both BUILDLOGs) and what is NOT proven on the real tablet/phone; end the reply with the amcheckpoint block (health red) as the active-memory hook asked.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  67fccbc ckpt 390: Swoop 0.5.0-alpha4 RELEASED (run 44187302136 green: check, test-jvm, build wit
  7323b76 ckpt 386: gated swoop-v0.5.0-alpha4 (ime code 9) and pushed it: handoff self-test, unit 
  d40bb1f ckpt 380: R17c: the long run (daily-sim suite=all, full speed, run 44164818105, 20:01-20
  6d47af1 ckpt 369: R17c: emulator run 3 (typing quick, full speed, adb root) = FIRST FULL SUCCESS
  7509392 ckpt 356: R17c: first emulator run READ (run 44150728400, typing quick, full speed, Andr
  dccedcb ckpt 338: KB-406 prediction yardstick built (EvalCorpus: 168 original everyday sentences
  9a770b8 ckpt 332: OWNER (R17e): 'Most of the time i won't have the 5mbit speed cap' recorded in 
  ac28c95 ckpt 328: Keyboard test suites green again after today's work (3 policy / ordering fixes
  73e11f5 ckpt 324: daily-sim driver fixed after the first runs (runs 44145772330 / 44146747558 / 
  1e98758 ckpt 320: R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandO
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
