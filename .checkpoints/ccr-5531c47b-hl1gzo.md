# CHECKPOINT 320 — read me first, then TASKS.md

**Written:** 2026-11-17T18:32:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `5e07247` (this checkpoint is the commit after it)

## Just done
R17b4 Kestrel hand-over coded + tested in the Browser repo on my branch (HandOverProvider: framework-only ContentProvider, not exported, read grant per intent, only cache/handover, read only; KeyboardFlow.handOverToSwoop: export text -> cache file -> explicit ACTION_SEND intent to Swoop's ReceiveLearnedActivity; Settings row 'Send what the keyboard learned to Swoop' only when Swoop is installed; manifest provider). Engine copy re-synced into Kestrel (pin = Keyboard f8c95fd, FieldKind.OMNIBOX), Browser :core:test + :app/:geckoapp unit suites green. dailysim: sloppy-thumb glide pass, pages + video scenes added. Run 44147785336 (typing, net full: baseline + address box + private tab scenes) is running.

## Do this next
read run 44147785336 report.md; fix what it shows; then dispatch daily-sim with suite=all net=hotspot (real pages + films under the 5 Mbit link); then release: Keyboard ime/build.gradle.kts 0.5.0-alpha4 code 9 + bash ship.sh; Browser: bump geckoapp versionCode 168 / 0.3.0-alpha67, bash /home/user/Browser/tools/sync-ext-version.sh, bash /home/user/Browser/ship.sh gecko '<note>' (absolute paths, never cd), check no other android.yml run is queued/running in the Browser repo first (release-gecko lock: a second pending run REPLACES the first pending one), dispatch android.yml app=gecko full_build=true ONCE, record-release after green

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  1b1466f ckpt 319: R17b2/b3 Kestrel side coded in the Browser repo on MY branch only (auto-checkp
  05c757b ckpt 318: R17b: Explore agent's survey of Kestrel text input saved to research/KESTREL_I
  8c4064c ckpt 317: R17c1: tools/sim/dailysim.py (scene runner: device facts, install both apps, m
  fb4a7e9 ckpt 316: R17c1 started (new files only, nothing in the Browser repo touched): .github/w
  7272fbb ckpt 315: R17b3 Swoop side: new FieldKind.OMNIBOX (core FieldInfo/ImeLayout): Kestrel's 
  6efe83a ckpt 314: OWNER REQUEST 17 captured in TASKS.md (R17a signing stays public = decided; R1
  ef73fe8 ckpt 313: Handoff manifest saved by the context-handoff plugin (.handoff/handoff.md + .j
  c611db1 ckpt 312: SWOOP 0.5.0-alpha3 SHIPPED AND RECORDED (android.yml run 44139548579 green, Re
  badbfc0 ckpt 311: swoop-v0.5.0-alpha3 (ime code 8) DISPATCHED ONCE (the first attempt, run 44137
  9cb2b3b ckpt 310: gated swoop-v0.5.0-alpha3 (ime code 8) and pushed it: handoff self-test, unit 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
