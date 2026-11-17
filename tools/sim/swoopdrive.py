"""tools/sim/swoopdrive.py - drives the Swoop keyboard on an emulator the way a thumb does (R17c1).

Standard library only (run it with `python3 -I`). Key positions come from the keyboard ITSELF: a DEBUG build of Swoop logs one line per layout / page change to logcat (tag SwoopDiag):
    keymap page=letters shift=0 k71=41,901 k77=120,901 ... shift=33,1170 back=... sym=... go=...
(a one-character key is k<hex code point>; screen pixels; geometry only, never text). Taps go through `input tap`; glides are raw multi-touch events written into the emulator's touch device in
one pushed file, a few events every few milliseconds (the same idea as the Browser lab's glide_emu.py), so the keyboard sees a finger that moves, not a jump.
Nothing here is part of either app.
"""
import os
import re
import struct
import subprocess
import tempfile
import time

ADB = os.environ.get("ADB", "adb")


def adb(*args, check=True, text=True, timeout=120):
    r = subprocess.run([ADB, *args], capture_output=True, text=text, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError("adb %s failed: %s" % (" ".join(args), (r.stderr or "").strip()))
    return r.stdout


def shell(cmd, **kw):
    return adb("shell", cmd, **kw)


# ---------------------------------------------------------------------------------------------------------------------------------------------- the screen and the touch device
_dev = {}


def touch_device():
    """(path, max_x, max_y) of the emulator's multi-touch device (the lowest numbered one reaches the main display)."""
    if _dev:
        return _dev["v"]
    out = shell("getevent -p")
    best = None
    for block in out.split("add device")[1:]:
        m = re.search(r"(/dev/input/event(\d+))", block)
        if m and "multi_touch" in block and (best is None or int(m.group(2)) < best[0]):
            best = (int(m.group(2)), m.group(1), block)
    if not best:
        raise RuntimeError("no multi-touch input device found")
    mx = my = 32767
    for code, name in (("0035", "x"), ("0036", "y")):
        mm = re.search(code + r"\s*:\s*value \d+, min \d+, max (\d+)", best[2])
        if mm:
            if name == "x":
                mx = int(mm.group(1))
            else:
                my = int(mm.group(1))
    _dev["v"] = (best[1], mx, my)
    return _dev["v"]


def screen_size():
    m = re.search(r"(\d+)x(\d+)", shell("wm size").split("Override size:")[-1])
    return int(m.group(1)), int(m.group(2))


def _ev(t, c, v):
    return struct.pack("<qqHHi", 0, 0, t, c, v)


def inject(points, per_chunk=4, pause=0.03, hold=0.0):
    """Press at points[0], move through the rest, lift at the last point: a chunk of touch events every `pause` seconds."""
    path, mx, my = touch_device()
    sw, sh = screen_size()

    def raw(p):
        return int(round(p[0] / sw * mx)), int(round(p[1] / sh * my))

    chunks = []
    x, y = raw(points[0])
    chunks.append(_ev(3, 0x2f, 0) + _ev(3, 0x39, 1) + _ev(3, 0x35, x) + _ev(3, 0x36, y) + _ev(3, 0x3a, 100) + _ev(0, 0, 0))
    mids = points[1:-1]
    for i in range(0, len(mids), per_chunk):
        b = b""
        for p in mids[i:i + per_chunk]:
            x, y = raw(p)
            b += _ev(3, 0x35, x) + _ev(3, 0x36, y) + _ev(0, 0, 0)
        chunks.append(b)
    x, y = raw(points[-1])
    chunks.append(_ev(3, 0x35, x) + _ev(3, 0x36, y) + _ev(0, 0, 0) + _ev(3, 0x39, -1) + _ev(0, 0, 0))
    blob = b"".join(chunks)
    with tempfile.TemporaryDirectory() as td:
        f = os.path.join(td, "sd.bin")
        open(f, "wb").write(blob)
        adb("push", f, "/data/local/tmp/sd.bin")
    cmds = []
    at = 0
    for idx, c in enumerate(chunks):
        n = len(c) // 24
        cmds.append("dd if=/data/local/tmp/sd.bin of=%s bs=24 skip=%d count=%d 2>/dev/null; sleep %s" % (path, at, n, pause))
        if idx == 0 and hold > 0:
            cmds.append("sleep %s" % hold)
        at += n
    cmds.append("rm -f /data/local/tmp/sd.bin")
    shell("; ".join(cmds))


def tap(x, y):
    shell("input tap %d %d" % (x, y))


# ---------------------------------------------------------------------------------------------------------------------------------------------- the key map
_KEYMAP = re.compile(r"SwoopDiag: keymap page=(\w+) shift=(\d) (.*)$")


class Keymap:
    def __init__(self, page, shift, keys):
        self.page, self.shift, self.keys = page, shift, keys

    def char(self, c):
        return self.keys.get("k%x" % ord(c))

    def __repr__(self):
        return "Keymap(%s shift=%d %d keys)" % (self.page, self.shift, len(self.keys))


def read_keymap():
    """The newest key map the keyboard logged, or None."""
    out = adb("logcat", "-d", "-s", "SwoopDiag:D", check=False)
    last = None
    for line in out.splitlines():
        m = _KEYMAP.search(line)
        if m:
            last = m
    if not last:
        return None
    keys = {}
    for part in last.group(3).split():
        k, _, xy = part.partition("=")
        if "," in xy:
            x, y = xy.split(",")
            keys[k] = (int(x), int(y))
    return Keymap(last.group(1), int(last.group(2)), keys)


def clear_log():
    adb("logcat", "-c", check=False)


def wait_keymap(page=None, timeout=15.0, need=None):
    """Polls logcat until a key map (of [page]) that has the keys in [need] shows up."""
    end = time.time() + timeout
    km = None
    while time.time() < end:
        km = read_keymap()
        if km and (page is None or km.page == page) and (not need or all(n in km.keys for n in need)):
            return km
        time.sleep(0.4)
    return km


# ---------------------------------------------------------------------------------------------------------------------------------------------- typing
def tap_key(km, name):
    xy = km.keys.get(name)
    if not xy:
        raise KeyError("no key %s on the %s page" % (name, km.page))
    tap(*xy)
    time.sleep(0.12)


def tap_char(km, c):
    xy = km.char(c)
    if not xy:
        raise KeyError("no key %r on the %s page" % (c, km.page))
    tap(*xy)
    time.sleep(0.10)


def type_text(text, per_key_pause=0.0, log=None):
    """Types [text] by tapping: letters and space on the letters page, one other character at a time through ?123 (Swoop returns to the letters after one symbol, a digit stays on the symbols page).
    Returns the number of characters that could not be typed (no such key)."""
    missing = 0
    clear_log()
    km = wait_keymap("letters", need=["k61"])
    for c in text:
        if km is None:
            raise RuntimeError("the keyboard is not showing (no key map in logcat; is it a debug build and the field focused?)")
        if km.page != "letters":
            km = wait_keymap("letters", need=["k61"]) or km
        if km.char(c.lower()) and (c.isalpha() or c == " "):
            if c.isupper() and km.shift == 0:
                tap_key(km, "shift")
                km = wait_keymap("letters", need=["k61"]) or km
            tap_char(km, c.lower() if c.isalpha() else c)
        else:
            clear_log()
            tap_key(km, "sym")
            sk = wait_keymap("symbols", need=["k31"])
            if sk and sk.char(c):
                tap_char(sk, c)
            else:
                if log:
                    log("cannot type %r (not on the first symbols page)" % c)
                missing += 1
                tap_key(sk, "abc") if sk and "abc" in sk.keys else None
            clear_log()
            km = wait_keymap("letters", need=["k61"]) or km
        if per_key_pause:
            time.sleep(per_key_pause)
    return missing


def path_points(word, km, step_px=6.0, jitter=None):
    """Straight lines between the centres of the word's letters; jitter = {letter: (dx, dy)} in pixels moves where the finger lands for that letter."""
    cs = []
    for ch in word.lower():
        xy = km.char(ch)
        if not xy:
            continue
        x, y = xy
        if jitter and ch in jitter:
            x += jitter[ch][0]
            y += jitter[ch][1]
        if not cs or cs[-1] != (x, y):
            cs.append((x, y))
    if len(cs) < 2:
        return cs * 2
    pts = [cs[0]]
    for (ax, ay), (bx, by) in zip(cs, cs[1:]):
        n = max(2, int(((bx - ax) ** 2 + (by - ay) ** 2) ** 0.5 / step_px))
        for s in range(1, n + 1):
            pts.append((ax + (bx - ax) * s / n, ay + (by - ay) * s / n))
    return pts


def glide_word(word, km=None, jitter=None):
    km = km or wait_keymap("letters", need=["k61"])
    if km is None:
        raise RuntimeError("the keyboard is not showing (no key map)")
    inject(path_points(word, km, jitter=jitter))
    return time.time()          # the moment the finger lifted (the caller measures how long the word takes to appear)
