"""Reads a `dumpsys window windows` dump and says whether the InputMethod window is (nearly) the whole screen, as it must be while the keyboard floats.
usage: float_check.py [--show] DUMP SCREEN_HEIGHT_PX      exit 0 = floating window found, 1 = window too small, 2 = no InputMethod window."""
import re
import sys

show = "--show" in sys.argv
args = [a for a in sys.argv[1:] if a != "--show"]
text = open(args[0], encoding="utf-8", errors="replace").read()
screen_h = float(args[1])
for block in re.split(r"\n\s*Window #\d+ ", text):
    if "InputMethod" not in block.split("\n", 1)[0]:
        continue
    m = re.search(r"(?:mFrame|frame)=\[(\d+),(\d+)\]\[(\d+),(\d+)\]", block)
    h = int(m.group(4)) - int(m.group(2)) if m else 0
    ins = re.search(r"mTouchableInsets=(\d+)", block)
    reg = re.search(r"mGivenTouchableRegion=([^\n]*)", block)
    ci = re.search(r"mGivenContentInsets=([^\n]*)", block)
    print("window height", h, "of", screen_h, "touchableInsets", ins.group(1) if ins else None,
          "region", reg.group(1)[:90] if reg else None, "contentInsets", ci.group(1)[:60] if ci else None)
    if show:
        sys.exit(0)
    sys.exit(0 if h >= 0.85 * screen_h else 1)
print("no InputMethod window in the dump")
sys.exit(2)
