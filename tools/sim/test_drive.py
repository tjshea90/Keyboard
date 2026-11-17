#!/usr/bin/env python3
"""tools/sim/test_drive.py - tests the emulator driver's typing logic (swoopdrive.py) against a FAKE keyboard, with no device: standard library only.

  python3 -I tools/sim/test_drive.py

The fake keeps the same rules as Swoop's real pages (ImeLayout / KeysView.emit): the letters page with Shift (one-shot), ?123 to the first symbols page, =\\< to the second, ABC back, a digit leaves the symbols page up,
one other symbol or a space sends the keyboard back to the letters. The driver must type any text through it, never loop, and never type a first batch twice.
"""
import importlib.util
import os
import sys
import types

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("swoopdrive", os.path.join(HERE, "swoopdrive.py"))
drive = importlib.util.module_from_spec(spec)
spec.loader.exec_module(drive)


def _rows(strings, y, x0=31, dx=60):
    out = {}
    for i, c in enumerate(strings):
        out["k%x" % ord(c)] = (x0 + dx * i, y)
    return out


LAYOUT = {
    "letters": {**_rows("qwertyuiop", 723), **_rows("asdfghjkl", 794, 61), **_rows("zxcvbnm", 865, 91),
                "shift": (31, 865), "back": (569, 865), "sym": (31, 936), "k2c": (100, 936), "k20": (300, 936), "k2e": (500, 936), "go": (569, 936)},
    "symbols": {**_rows("1234567890", 723), **_rows("@#$%&-+()/", 794), **_rows("*\"':;!?", 865, 91),
                "sym": (31, 865), "back": (569, 865), "abc": (31, 936), "k2c": (100, 936), "k20": (300, 936), "k2e": (500, 936), "go": (569, 936)},
    "symbols2": {**_rows("~`|", 723), "sym": (31, 865), "back": (569, 865), "abc": (31, 936), "k20": (300, 936), "go": (569, 936)},
}


class Fake:
    def __init__(self, lose_first=False):
        self.page, self.shift, self.out, self.events = "letters", 0, [], 0
        self.lose_first, self.batches = lose_first, 0
        self.glides = []

    def keymap(self):
        return drive.Keymap(self.page, self.shift, dict(LAYOUT[self.page]))

    def read_keymap(self):
        return self.keymap()

    def key_events(self):
        return self.events

    def tap(self, x, y):
        name = min(LAYOUT[self.page], key=lambda n: (LAYOUT[self.page][n][0] - x) ** 2 + (LAYOUT[self.page][n][1] - y) ** 2)
        kx, ky = LAYOUT[self.page][name]
        if (kx - x) ** 2 + (ky - y) ** 2 > 28 ** 2:
            return
        if name == "shift":
            self.shift ^= 1
        elif name == "sym":
            self.page = {"letters": "symbols", "symbols": "symbols2", "symbols2": "symbols"}[self.page]
        elif name == "abc":
            self.page = "letters"
        elif name == "back":
            if self.out:
                self.out.pop()
        elif name == "go":
            pass
        elif name.startswith("k"):
            c = chr(int(name[1:], 16))
            if self.page == "letters":
                if c.isalpha() and self.shift:
                    c = c.upper()
                self.out.append(c)
                if c.isalpha():
                    self.shift = 0
            else:
                self.out.append(c)
                if not c.isdigit():
                    self.page = "letters"
            self.events += 1

    def inject_taps(self, points, hold=0.04, gap=0.10):
        self.batches += 1
        if self.lose_first and self.batches == 1:
            return
        for (x, y) in points:
            self.tap(x, y)

    def inject(self, points, **kw):
        self.glides.append(list(points))

    def text(self):
        return "".join(self.out)


def install(fake):
    drive.read_keymap = fake.read_keymap
    drive.tap = fake.tap
    drive.inject_taps = fake.inject_taps
    drive.inject = fake.inject
    drive.key_events = fake.key_events
    drive.time = types.SimpleNamespace(time=lambda: 1.0e9 + fake.events * 0.01 + fake.batches * 100.0, sleep=lambda s: None)
    drive.STATS.update({"reads": 0, "read_s": 0.0, "first_batch_lost": 0, "batches": 0, "taps": 0, "last_batch": 0.0})


def check(cond, what):
    if not cond:
        print("FAIL: " + what)
        sys.exit(1)
    print("ok    " + what)


def main():
    f = Fake(); install(f)
    miss = drive.type_text("hello world")
    check(f.text() == "hello world" and miss == 0, "lower-case words: %r" % f.text())

    f = Fake(); install(f)
    miss = drive.type_text("How are you doing?")
    check(f.text() == "How are you doing?" and miss == 0 and f.page == "letters", "a capital and a question mark: %r page %s" % (f.text(), f.page))

    f = Fake(); install(f)
    drive.type_text("see you at 5")
    check(f.text() == "see you at 5" and f.page == "symbols", "a digit leaves the symbols page up: %r page %s" % (f.text(), f.page))
    drive.type_text("pm")
    check(f.text() == "see you at 5pm" and f.page == "letters", "the next letters go back to the letters first: %r page %s" % (f.text(), f.page))

    f = Fake(); install(f)
    drive.type_text("i'm on my way, call you later")
    check(f.text() == "i'm on my way, call you later", "an apostrophe through ?123 and a comma on the letters page: %r" % f.text())

    f = Fake(); install(f)
    drive.type_text("call 911 now")
    check(f.text() == "call 911 now" and f.page == "letters", "several digits in a row: %r page %s" % (f.text(), f.page))

    f = Fake(); install(f)
    miss = drive.type_text("price ~ 5")
    check(f.text() == "price ~ 5" and miss == 0, "a symbol of the second page: %r (missing %d)" % (f.text(), miss))

    f = Fake(lose_first=True); install(f)
    drive.type_text("hello")
    check(f.text() == "hello" and drive.STATS["first_batch_lost"] == 1, "a lost first batch is sent again once, not twice: %r lost=%d" % (f.text(), drive.STATS["first_batch_lost"]))

    f = Fake(); install(f)
    f.page = "symbols"
    km = drive.to_letters()
    check(km.page == "letters" and f.page == "letters", "to_letters taps ABC from the symbols page")
    drive.glide_word("hello", km)
    check(len(f.glides) == 1 and len(f.glides[0]) > 10, "a glide is one path of many points")
    try:
        drive.path_points("!!!", km)
        check(False, "a word without any letter key raises")
    except KeyError:
        check(True, "a word without any letter key raises a clear error, not an IndexError")

    f = Fake(); install(f)
    f.out = list("abc")
    drive.backspaces(2)
    check(f.text() == "a", "backspaces: %r" % f.text())
    print("driver tests passed")


if __name__ == "__main__":
    main()
