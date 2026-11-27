#!/usr/bin/env python3
"""fit-brief.py - keeps the SessionStart briefing under Claude Code's hook-output cap.

Claude Code cuts hook output above 10,000 characters to a 2,000-character preview and saves the rest in a file nobody opens: the
briefing, including CHECKPOINT.md "Do this next", is then silently lost. A new session branch merges every other branch first, so
the briefing grows with the number of branches, claims and lanes, which the small fixture repo of tools/test_handoff.sh never has.

usage: python3 -I tools/fit-brief.py [budget] < briefing.txt     prints the hook JSON; budget = characters of that JSON (default 9800)
Under the budget the text passes untouched. Over it, the least important parts go first, one step at a time, until it fits:
  1 old "sync: MERGED" lines (the 3 newest stay)   2 the RULES block (CLAUDE.md has it)   3 TASKS list down to 10 items
  4 INBOX / OUTBOX tails   5 the "Last ten checkpoints" list   6 hard cut.
The header, warnings, claims and CHECKPOINT.md ("Do this next") survive steps 1-5. A step whose text it does not recognise does nothing.
"""
import json
import re
import sys

BUDGET = int(sys.argv[1]) if len(sys.argv) > 1 else 9800
CUT_NOTE = "\n[... briefing cut to fit the hook cap: run  bash tools/resume.sh --text  for all of it]"


def wrap(text):
    return json.dumps({"hookSpecificOutput": {"hookEventName": "SessionStart", "additionalContext": text}})


def fits(text):
    return len(wrap(text)) <= BUDGET


def merged(t):
    lines = t.split("\n")
    idx = [i for i, l in enumerate(lines) if l.startswith("  sync: MERGED ")]
    if len(idx) <= 3:
        return t
    old = idx[:-3]
    lines[old[0]] = "  sync: MERGED %d older branches (collapsed to fit the hook cap; git log --merges -30)" % len(old)
    for i in old[1:]:
        lines[i] = None
    return "\n".join(l for l in lines if l is not None)


def rules(t):
    note = '- Full rules: CLAUDE.md. Checkpoint after EVERY step: bash tools/ckpt.sh "did" "next"'
    return re.sub(r"(?s)(\n######## RULES[^\n]*\n).*\Z", lambda m: m.group(1) + note, t)


def tasks(t, keep=10):
    m = re.search(r"(?s)(\n######## TASKS\.md - the )\d+( NEWEST[^\n]*\n)(.*?)(\n  \(\d+ done[^\n]*)", t)
    if not m or len(m.group(3).split("\n")) <= keep:
        return t
    body = "\n".join(m.group(3).split("\n")[-keep:])
    return t[:m.start()] + m.group(1) + str(keep) + m.group(2) + body + m.group(4) + t[m.end():]


def tails(t, inbox=420, outbox=300):
    for name, n in (("INBOX", inbox), ("OUTBOX", outbox)):
        m = re.search(r"(?s)(\n######## %s\.md tail[^\n]*\n)(.*?)(?=\n######## |\Z)" % name, t)
        if m and len(m.group(2)) > n + 4:
            t = t[:m.start(2)] + "..." + m.group(2).rstrip("\n")[-n:] + "\n" + t[m.end(2):]
    return t


def ckpts(t, keep=3):
    return re.sub(r"(?s)(## Last ten checkpoints\n```\n)(.*?)(```)",
                  lambda m: m.group(1) + "\n".join(m.group(2).split("\n")[:keep]) + "\n" + m.group(3), t, count=1)


def cut(t):
    lo, hi = 0, len(t)
    while lo < hi:  # the longest prefix, ended at a line break, that still fits
        mid = (lo + hi + 1) // 2
        if fits(t[:mid].rsplit("\n", 1)[0] + CUT_NOTE):
            lo = mid
        else:
            hi = mid - 1
    return t[:lo].rsplit("\n", 1)[0] + CUT_NOTE


def main():
    t = sys.stdin.read()
    for step in (merged, rules, tasks, tails, ckpts, cut):
        if fits(t):
            break
        t = step(t)
    print(wrap(t))


main()
