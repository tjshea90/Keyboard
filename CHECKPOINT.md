# CHECKPOINT 284 — read me first, then TASKS.md

**Written:** 2026-11-17T08:35:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9cd8405a-34e17r` · **builds on:** `17eb3c4` (this checkpoint is the commit after it)

## Just done
M6 default sentence caps, M9 glide-vs-tap (stayedOnKey + minLength 0.45), M1 new Gboard-like layout (ROW_UNITS 11.5, ?123 bottom right, enter on home row), M5 symbols auto-return, toolbar strip (cog/clipboard/ES), M2 ClipboardStore+ClipboardPanel+ClipboardHost wired, EmojiMap + emoji asset, settings rows; two helper agents running (lexicon rebuild ae9303e340af4888b, translate engine aa9042f6cd2a71e87)

## Do this next
TODO in order: (1) fix ime tests broken by layout (KeysViewTest, KeyboardPresenterTest, SetupActivityTest) via ./gradlew :ime:testDebugUnitTest; (2) UserDictionary in core + GlideTyping block/add API + DictionaryActivity (M12,M15) + presenter long-press confirm (KeysView.onCandidateLong/onRemoveWord already exist); (3) emoji chips in presenter (EmojiMap + Paint.hasGlyph; assets/emoji/en.tsv); (4) M8 squeeze docked keyboard in KeyboardRoot/GripBar (prefs DOCK_FRAC/DOCK_X exist); (5) wire translate when helper aa90 finishes (toolbar TOOL_TRANSLATE -> presenter.onToggleTranslate); (6) M14 persistence tests; (7) version 0.4.0 + ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  625c67b ckpt 283: recorded the owner's 16 scheduled requests M1-M16 in TASKS.md
  ec5e72e ckpt 282: owner's reversal done: the 123 key and symbols page are back (space bar 4.9 un
  4a31f70 ckpt 279: owner reversed N11 (wants the 123 key back: he needs the special symbols). Cod
  56c0800 ckpt 275: suggester restores apostrophes (dont -> don't, im -> I'm, ill -> I'll only whe
  4eecb24 ckpt 272: N10-N14 done in code and tests: glide survives an address bar's inline complet
  7fd6628 ckpt 253: owner's new requests (Brave strip tap does nothing, remove 123 and globe keys,
  8ea6088 ckpt 249: RELEASE swoop-v0.2.1-alpha1 (code 3) published (run 44009075923 green, pinned 
  6f0e9b4 ckpt 245: swoop-v0.2.1-alpha1 (code 3) gated and android.yml dispatched (floating keyboa
  a7d984f ckpt 244: gated swoop-v0.2.1-alpha1 (ime code 3) and pushed it: handoff self-test, unit 
  5cb34be ckpt 241: N3 floating keyboard proven on an Android 10 emulator (run 44007433798: 0 fail
```

(32 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
