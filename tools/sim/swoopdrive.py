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


def inject_taps(points, hold=0.04, gap=0.10):
    """A run of taps at human speed in ONE shell call: press, [hold] seconds, lift, [gap] seconds, the next one. (`input tap` costs 0.3 s a key; a thumb taps 5 to 8 keys a second.)"""
    path, mx, my = touch_device()
    sw, sh = screen_size()
    blob = b""
    for (x, y) in points:
        rx, ry = int(round(x / sw * mx)), int(round(y / sh * my))
        blob += _ev(3, 0x2f, 0) + _ev(3, 0x39, 1) + _ev(3, 0x35, rx) + _ev(3, 0x36, ry) + _ev(3, 0x3a, 100) + _ev(0, 0, 0)       # down
        blob += _ev(3, 0x39, -1) + _ev(0, 0, 0)                                                                                 # up
    with tempfile.TemporaryDirectory() as td:
        f = os.path.join(td, "st.bin")
        open(f, "wb").write(blob)
        adb("push", f, "/data/local/tmp/st.bin")
    down = 6 * 24
    up = 2 * 24
    cmds = []
    at = 0
    for _ in points:
        cmds.append("dd if=/data/local/tmp/st.bin of=%s bs=24 skip=%d count=6 2>/dev/null; sleep %s; dd if=/data/local/tmp/st.bin of=%s bs=24 skip=%d count=2 2>/dev/null; sleep %s" % (path, at // 24, hold, path, (at + down) // 24, gap))
        at += down + up
    cmds.append("rm -f /data/local/tmp/st.bin")
    shell("; ".join(cmds), timeout=60 + len(points))


# ---------------------------------------------------------------------------------------------------------------------------------------------- the key map
_KEYMAP = re.compile(r"SwoopDiag: keymap page=(\w+) shift=(\d) (.*)$")
STATS = {"reads": 0, "read_s": 0.0, "first_batch_lost": 0, "batches": 0, "taps": 0}


class Keymap:
    def __init__(self, page, shift, keys):
        self.page, self.shift, self.keys = page, shift, keys

    def char(self, c):
        """The centre of the key that types [c]; a letter is found under either case (the keys read as capitals while Shift is on, and their place is the same)."""
        xy = self.keys.get("k%x" % ord(c))
        if xy is None and c.isalpha():
            xy = self.keys.get("k%x" % ord(c.upper() if c.islower() else c.lower()))
        return xy

    def __repr__(self):
        return "Keymap(%s shift=%d %d keys)" % (self.page, self.shift, len(self.keys))


_last_km = {"v": None}


def read_keymap():
    """The newest key map the keyboard logged. The log is a ring buffer that a chatty page (Gecko) can push the one line out of: then the last one seen is returned (the layout only changes when the keyboard logs a new line)."""
    t0 = time.time()
    out = adb("logcat", "-d", "-s", "SwoopDiag:D", check=False)
    STATS["reads"] += 1
    STATS["read_s"] += time.time() - t0
    last = None
    for line in out.splitlines():
        m = _KEYMAP.search(line)
        if m:
            last = m
    if not last:
        return _last_km["v"]
    keys = {}
    for part in last.group(3).split():
        k, _, xy = part.partition("=")
        if "," in xy:
            x, y = xy.split(",")
            keys[k] = (int(x), int(y))
    _last_km["v"] = Keymap(last.group(1), int(last.group(2)), keys)
    return _last_km["v"]


def clear_log():
    adb("logcat", "-c", check=False)


def key_events():
    """How many key presses the keyboard has logged so far (debug build: 'key after' lines): the proof that touches reach it."""
    return adb("logcat", "-d", "-s", "SwoopDiag:D", check=False).count("key after")


def wait_state(page="letters", shift=None, timeout=5.0, need=()):
    """The newest key map once it is on [page] (and [shift] when given, and has the keys in [need]); the last one seen when it never was."""
    end = time.time() + timeout
    km = None
    while True:
        km = read_keymap()
        if km and km.page == page and (shift is None or km.shift == shift) and all(n in km.keys or (n == "k61" and "k41" in km.keys) for n in need):
            return km
        if time.time() > end:
            return km
        time.sleep(0.3)


def wait_settled(page="letters", need=("k61",), timeout=20.0):
    """A key map that did not change between two reads half a second apart: the keyboard window has stopped moving (a slide-in at the first draw would put every tap off)."""
    end = time.time() + timeout
    prev = None
    km = None
    while time.time() < end:
        km = read_keymap()
        if km and km.page == page and all(n in km.keys or n.replace("k61", "k41") in km.keys for n in need):
            if prev is not None and prev.keys == km.keys:
                return km
            prev = km
        time.sleep(0.6)
    return km


def wait_keymap(page=None, timeout=15.0, need=None):
    """Polls logcat until a key map (of [page]) that has the keys in [need] shows up."""
    end = time.time() + timeout
    km = None
    while time.time() < end:
        km = read_keymap()
        if km and (page is None or km.page == page) and (not need or all(n in km.keys or (n == 'k61' and 'k41' in km.keys) for n in need)):
            return km
        time.sleep(0.4)
    return km


def to_letters(timeout=6.0):
    """The keyboard on its letters page (the digits keep the symbols page up until a space or ABC): the key map of the letters page."""
    km = read_keymap()
    if km is not None and km.page != "letters" and "abc" in km.keys:
        tap(*km.keys["abc"])
        time.sleep(0.15)
        km = wait_state("letters", timeout=timeout)
    elif km is None or km.page != "letters":
        km = wait_state("letters", timeout=timeout)
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


def _taps(points, hold, gap):
    """A batch of taps in one shell call. The very first batch after the keyboard appeared was lost once on the emulator (the raw touch path needs a first event before it works): a first batch that raises no key press is sent again, and counted (STATS['first_batch_lost'])."""
    STATS["taps"] += len(points)
    check = STATS["batches"] == 0
    before = key_events() if check else 0
    inject_taps(points, hold=hold, gap=gap)
    STATS["batches"] += 1
    if check:
        time.sleep(0.5)
        if key_events() <= before:
            STATS["first_batch_lost"] += 1
            inject_taps(points, hold=hold, gap=gap)


def _plain(c, km):
    """A character that the letters page types with one tap and no state change: a lower-case letter, a space, or a key it shows (comma, full stop)."""
    return c == " " or (c.isascii() and c.isalpha() and c.islower()) or (c in ",." and km.char(c) is not None)


def type_text_fast(text, km=None, hold=0.04, gap=0.10):
    """Letters and spaces only, on the letters page: all the taps of [text] in one call, at human speed. Returns the number of characters that could not be tapped."""
    km = km or to_letters()
    if km is None:
        raise RuntimeError("the keyboard is not showing (no key map)")
    pts, missing = [], 0
    for c in text:
        xy = km.char(c.lower() if c.isalpha() else c)
        if xy is None:
            missing += 1
        else:
            pts.append(xy)
    if pts:
        _taps(pts, hold, gap)
    time.sleep(0.4)
    return missing


def type_text(text, per_key_pause=0.0, log=None, hold=0.04, gap=0.10):
    """Types [text] the way a thumb does, with every page change a person makes: lower-case letters, spaces and the keys of the letters page in runs (one shell call each), a capital through Shift, a digit or a
    symbol through ?123 (a digit leaves the symbols page up, one symbol sends the keyboard back to the letters). Returns the number of characters that could not be typed (no such key)."""
    missing = 0
    km = to_letters()
    if km is None:
        raise RuntimeError("the keyboard is not showing (no key map in logcat; is it a debug build and the field focused?)")
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if km.page == "letters" and _plain(c, km):
            j = i
            while j < n and _plain(text[j], km):
                j += 1
            run = text[i:j]
            pts = [km.char(x) for x in run]
            _taps([p for p in pts if p], hold, gap)
            time.sleep(0.15)
            i = j
            continue
        if km.page != "letters" and km.char(c) is not None:             # still on the symbols page after a digit: the next digit / symbol is right here
            tap(*km.char(c))
            time.sleep(0.12)
            km = wait_state("symbols", timeout=1.5) if c.isdigit() else (to_letters() or km)
            i += 1
            continue
        if km.page != "letters":
            km = to_letters() or km
        if c.isascii() and c.isalpha() and c.isupper():
            if km.shift == 0 and "shift" in km.keys:
                tap_key(km, "shift")
                km = wait_state("letters", shift=1, timeout=3.0) or km
            xy = km.char(c)
            if xy is None:
                missing += 1
            else:
                tap(*xy)
                time.sleep(0.12)
            km = wait_state("letters", shift=0, timeout=3.0) or km
        elif km.char(c) is not None and km.page == "letters":          # e.g. an apostrophe some layouts keep on the letters page
            tap(*km.char(c))
            time.sleep(0.12)
        else:
            tap_key(km, "sym") if "sym" in km.keys else None
            sk = wait_state("symbols", timeout=3.0)
            if sk is not None and sk.char(c) is None and "sym" in sk.keys:     # the second symbols page (=\<)
                tap_key(sk, "sym")
                sk = wait_state("symbols2", timeout=3.0) or sk
            if sk is not None and sk.char(c) is not None:
                tap(*sk.char(c))
                time.sleep(0.12)
                km = wait_state("symbols", timeout=1.5) if c.isdigit() else (to_letters() or sk)
            else:
                missing += 1
                if log:
                    log("cannot type %r (not on the symbols pages)" % c)
                km = to_letters() or km
        i += 1
        if per_key_pause:
            time.sleep(per_key_pause)
    return missing


def backspaces(count, gap=0.06):
    """[count] taps on Backspace (the key map of the page the keyboard shows now)."""
    km = read_keymap()
    if km is not None and "back" in km.keys and count > 0:
        inject_taps([km.keys["back"]] * count, hold=0.03, gap=gap)


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
    if not cs:
        raise KeyError("no letter key of %r on the %s page" % (word, km.page))
    if len(cs) < 2:
        return cs * 2
    pts = [cs[0]]
    for (ax, ay), (bx, by) in zip(cs, cs[1:]):
        n = max(2, int(((bx - ax) ** 2 + (by - ay) ** 2) ** 0.5 / step_px))
        for s in range(1, n + 1):
            pts.append((ax + (bx - ax) * s / n, ay + (by - ay) * s / n))
    return pts


def glide_word(word, km=None, jitter=None):
    km = km if (km is not None and km.page == "letters") else to_letters()
    if km is None:
        raise RuntimeError("the keyboard is not showing (no key map)")
    inject(path_points(word, km, jitter=jitter))
    return time.time()          # the moment the finger lifted (the caller measures how long the word takes to appear)
