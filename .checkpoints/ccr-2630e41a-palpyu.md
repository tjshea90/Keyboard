# CHECKPOINT 780 — read me first, then TASKS.md

**Written:** 2026-11-19T00:07:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-2630e41a-palpyu` · **builds on:** `cec0939` (this checkpoint is the commit after it)

## Just done
swoop-v0.7.0-alpha6 (code 17) DISPATCHED ONCE: android.yml run 44426423457 on sha cec0939 (queued 2026-11-19T00:07Z). Do NOT dispatch again. R25 (socket creation EPERM on the owner's tablet) still open: battery saver ruled out by the owner's retest; waiting for power-off/on, Settings > Apps > Swoop switches, then export + reinstall.

## Do this next
wait for run 44426423457 green (~12 min), confirm Release swoop-v0.7.0-alpha6 has the APK (get_release_by_tag), then bash tools/record-release.sh swoop-v0.7.0-alpha6 with the IDENTICAL note (see CHECKPOINT ckpt 779), give the owner the Release link on its own line. If red: get_job_logs failed_only run_id, fix, ship 0.7.0-alpha7 code 18. Owner open: results of full power-off/on, Settings>Apps>Swoop network switches, VPN/firewall apps, then export-learned + reinstall (the likely fix if a stale permission record)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cec0939 ckpt 779: gated swoop-v0.7.0-alpha6 (ime code 17) and pushed it: handoff self-test, unit
  fc6b8ff ckpt 777: pre-ship: 0.7.0-alpha6: the translation connection test names a refused socket
  21947b4 ckpt 772: R25d: owner's full Diagnostics paste received (same probe run as R25c + 14 EPE
  c4b6a87 ckpt 768: R25c: owner's exact Test translation connection output received (group OK, bat
  365909c ckpt 763: swoop-v0.7.0-alpha5 RELEASED AND RECORDED (run 44421068703 green; Release swoo
  b076cb5 ckpt 760: swoop-v0.7.0-alpha5 (code 16) DISPATCHED ONCE: android.yml run 44421068703 on 
  2688041 ckpt 759: gated swoop-v0.7.0-alpha5 (ime code 16) and pushed it: handoff self-test, unit
  7c4dd02 ckpt 757: pre-ship: 0.7.0-alpha5: the translation connection test now tells apart a refu
  66682b9 ckpt 753: R25: measured table (Android 10 emulator, background app): Data Saver -> ECONN
  837228c ckpt 746: R25 EVIDENCE (run 44414888824, Android 10 emulator): Data Saver ON + metered W
```
