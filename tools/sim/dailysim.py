#!/usr/bin/env python3
"""tools/sim/dailysim.py - plays a day of keyboard + browser use on the emulator and writes a report (R17c). Standard library only.

  python3 -u -I tools/sim/dailysim.py --out DIR --swoop ime-debug.apk --kestrel kestrel.apk --browser-dir browser [--suite typing|pages|video|all] [--net hotspot|full] [--minutes 15]
  python3 -u -I tools/sim/dailysim.py --selftest        (no device: checks the parsers and the scoring)

What a person does, in this order (every step is a recorded scene; a step that cannot be done is a FINDING in the report, never a crash of the run):
  setup    install Swoop + Kestrel, make Swoop THE keyboard, start the lab server (tools/sim/labserver.py); the link is FULL speed by default (the owner is on fast wifi most of the time), the 5 Mbit/s hotspot is an optional second pass (--net hotspot)
  swoop    type and glide into Swoop's OWN field (the baseline: no browser in between)
  fields   the same into a real web page's fields in Kestrel (text, search, textarea, contenteditable, url, e-mail): what the PAGE received is read from the lab server, not from a screenshot
  address  the same into Kestrel's address-and-search box
  pages    real sites (--suite pages|all): load time, did it load, screenshots
  video    a film (--suite video|all)
Emulator numbers are NOT the tablet's (x86_64, software GPU, fast CPU): they are only comparable with other numbers of the same kind. The report says so.
"""
import argparse
import importlib.util
import json
import os
import random
import re
import subprocess
import sys
import threading
import time
import urllib.request
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


drive = _load("swoopdrive", os.path.join(HERE, "swoopdrive.py"))

SWOOP_PKG = "app.swoop.keyboard.debug"
SWOOP_IME = SWOOP_PKG + "/app.swoop.keyboard.KeysService"
KESTREL_PKG = "app.kestrel.browser.gecko.debug"
KESTREL_ACT = KESTREL_PKG + "/app.kestrel.browser.MainActivity"
PORT = 8099

TAP_PHRASES = [
    "hello world",
    "see you at 5",
    "what is the weather today",
    "best pizza near me",
    "i'm on my way, call you later",
    "How are you doing?",
    "thanks so much for your help",
]
FIELD_KINDS = ["text", "search", "textarea", "editable", "url", "email"]
GLIDE_WORDS = ["hello", "world", "thanks", "tomorrow", "because", "something", "going", "weather", "please", "important", "people", "about",
               "would", "really", "friends", "morning", "browser", "keyboard", "youtube", "google"]


# ---------------------------------------------------------------------------------------------------------------------------------------------- the report
class Report:
    def __init__(self, out):
        self.out = out
        os.makedirs(out, exist_ok=True)
        self.events = open(os.path.join(out, "events.jsonl"), "a", encoding="utf-8")
        self.rows = []      # (scene, status, detail)
        self.facts = []
        self.t0 = time.time()

    def event(self, scene, **kw):
        kw.update(scene=scene, t=round(time.time() - self.t0, 1))
        self.events.write(json.dumps(kw, ensure_ascii=False) + "\n")
        self.events.flush()

    def row(self, scene, status, detail=""):
        self.rows.append((scene, status, detail))
        print("  %-5s %-34s %s" % (status, scene, detail), flush=True)
        self.event(scene, status=status, detail=detail)
        try:
            self.write()          # the report on disk is always current: a run that is cut off or killed by a step timeout still leaves everything it found
        except Exception:
            pass

    def fact(self, text):
        self.facts.append(text)
        print("  info  " + text, flush=True)

    def write(self):
        n = {"OK": 0, "FAIL": 0, "INFO": 0, "WARN": 0}
        for _, s, _ in self.rows:
            n[s] = n.get(s, 0) + 1
        lines = ["# Daily-use simulation: keyboard + browser", "",
                 "Emulator numbers are NOT the tablet's (x86_64 with a software GPU and a fast CPU, not a 4 x A53 Mali tablet): they compare with other runs of the same kind, never with the device.", ""]
        lines += ["- " + f for f in self.facts]
        lines += ["", "| result | count |", "| :- | -: |"] + ["| %s | %d |" % (k, v) for k, v in n.items() if v]
        lines += ["", "| scene | result | detail |", "| :- | :- | :- |"] + ["| %s | %s | %s |" % (a, b, c.replace("|", "/")) for a, b, c in self.rows]
        open(os.path.join(self.out, "report.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
        return n


# ---------------------------------------------------------------------------------------------------------------------------------------------- the lab server (on this machine)
def state():
    try:
        return json.loads(urllib.request.urlopen("http://127.0.0.1:%d/state" % PORT, timeout=3).read().decode())
    except Exception:
        return {}


def reset():
    try:
        urllib.request.urlopen("http://127.0.0.1:%d/reset" % PORT, timeout=3).read()
    except Exception:
        pass


def wait_state(pred, timeout=30.0, step=0.5):
    end = time.time() + timeout
    st = state()
    while time.time() < end:
        st = state()
        if pred(st):
            return st
        time.sleep(step)
    return st


# ---------------------------------------------------------------------------------------------------------------------------------------------- scoring
def edit_distance(a, b):
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
    return prev[-1]


def words_hit(wanted, got):
    """How many of the wanted words are, in order, the words of the text that came out (a glide adds a space after each word)."""
    toks = got.split()
    return sum(1 for w, g in zip(wanted, toks) if w == g.lower()), len(wanted)


# ---------------------------------------------------------------------------------------------------------------------------------------------- the device
def sh(cmd, **kw):
    return drive.shell(cmd, **kw)


def adb(*a, **kw):
    return drive.adb(*a, **kw)


def device_facts(rep):
    rep.fact("android %s sdk %s abi %s, %s, %s, low_ram=%s" % (sh("getprop ro.build.version.release").strip(), sh("getprop ro.build.version.sdk").strip(), sh("getprop ro.product.cpu.abi").strip(),
             sh("wm size").strip().replace("\n", " "), sh("wm density").strip().replace("\n", " "), sh("getprop ro.config.low_ram").strip()))


def install(rep, apk, pkg):
    try:
        out = adb("install", "-r", "-t", apk, check=False, timeout=300)
        ok = "Success" in out
        rep.row("install " + pkg, "OK" if ok else "FAIL", out.strip().splitlines()[-1] if out.strip() else "no output")
        return ok
    except Exception as e:
        rep.row("install " + pkg, "FAIL", str(e)[:120])
        return False


def choose_swoop(rep):
    for _ in range(30):
        if SWOOP_PKG in sh("ime list -a -s", check=False):
            break
        time.sleep(2)
    sh("ime enable " + SWOOP_IME, check=False)
    sh("ime set " + SWOOP_IME, check=False)
    cur = sh("settings get secure default_input_method", check=False).strip()
    rep.row("Swoop is the keyboard", "OK" if cur == SWOOP_IME else "FAIL", cur)
    return cur == SWOOP_IME


def screen():
    return drive.screen_size()


def open_url(url, pkg_act=KESTREL_ACT):
    sh("am start -a android.intent.action.VIEW -d '%s' -n %s" % (url, pkg_act), check=False)


def keyboard_showing():
    return "mInputShown=true" in sh("dumpsys input_method | grep mInputShown", check=False)


def device_ok(timeout=25):
    """True when the emulator answers a trivial command within [timeout] seconds (a hung Android makes every adb call wait for minutes)."""
    try:
        r = subprocess.run([drive.ADB, "shell", "true"], capture_output=True, timeout=timeout)
        return r.returncode == 0
    except Exception:
        return False


def recover_device(rep, why, wait=180):
    """After a hang (an adb call that timed out, Android's own watchdog restarting system_server): wait for the system to come back and say so in the report. False when it does not."""
    rep.row("emulator", "WARN", "no answer (%s): waiting up to %d s for Android to come back" % (why[:90], wait))
    end = time.time() + wait
    while time.time() < end:
        if device_ok(20):
            try:
                if "1" in adb("shell", "getprop sys.boot_completed", check=False, timeout=20):
                    rep.row("emulator", "INFO", "answers again")
                    return True
            except Exception:
                pass
        time.sleep(5)
    rep.row("emulator", "FAIL", "did not come back within %d s" % wait)
    return False


def shot(rep, name):
    try:
        data = adb("exec-out", "screencap", "-p", text=False, timeout=60)
        open(os.path.join(rep.out, name + ".png"), "wb").write(data)
    except Exception:
        pass


def start_server(rep):
    log = os.path.join(rep.out, "lab-server.jsonl")
    p = subprocess.Popen([sys.executable, "-B", "-I", os.path.join(HERE, "labserver.py"), str(PORT), log], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1.0)
    adb("reverse", "tcp:%d" % PORT, "tcp:%d" % PORT, check=False)
    return p


# ---------------------------------------------------------------------------------------------------------------------------------------------- scenes
def jitter_for(word, km, sigma_pitch, rnd):
    """Where a real thumb lands: every letter's touch is off the key centre by a random amount (sigma in key pitches)."""
    q, w = km.keys.get("k71"), km.keys.get("k77")
    pitch = abs(w[0] - q[0]) if q and w else 40
    return {ch: (rnd.gauss(0, sigma_pitch * pitch), rnd.gauss(0, sigma_pitch * pitch)) for ch in set(word)}


def type_round(rep, scene, field_value, tap_target, glide=True):
    """One kind of field: tap it, wait for the keyboard, tap-type the phrases and glide the words; [field_value]() returns what the field holds."""
    x, y = tap_target
    km = None
    for attempt, dy in enumerate((0, 40, -40, 80)):          # the page may still be settling: tap again a little lower / higher before giving up
        drive.tap(x, y + dy)
        time.sleep(2.0)
        km = drive.wait_settled(timeout=10)
        if km is not None and km.page == "letters" and keyboard_showing():
            break
        km = None if not keyboard_showing() else km
        if km is not None:
            break
    if km is None:
        rep.row(scene + " keyboard", "FAIL", "Swoop did not show (no key map in logcat); keyboard showing=%s" % keyboard_showing())
        return
    rep.row(scene + " keyboard", "OK", "Swoop shows: %d keys mapped" % len(km.keys))
    # tapping
    ok = tot = 0
    for ph in TAP_PHRASES:
        reset_field = getattr(field_value, "clear", None)
        if reset_field:
            reset_field()
        t0 = time.time()
        miss = drive.type_text(ph)          # lower-case words at a human 6 keys a second in one go; a capital, a digit or a symbol through Shift / ?123 the way a thumb does it
        took = time.time() - t0
        time.sleep(0.8)
        got = field_value()
        d = edit_distance(ph.lower(), got.lower().rstrip())
        tot += 1
        ok += 1 if d == 0 else 0
        rep.row(scene + " tap '%s'" % ph[:24], "OK" if d == 0 else "WARN", "got %r (distance %d, %.1fs for %d characters%s)" % (got[:50], d, took, len(ph), ", %d keys missing" % miss if miss else ""))
        drive.backspaces(len(got) + 2)          # clear for the next phrase (Backspace through the key map of the page the keyboard shows now)
        time.sleep(0.8)
    rep.row(scene + " tap summary", "INFO", "%d of %d phrases exact" % (ok, tot))
    if not glide:
        return
    # gliding
    if drive.STATS.get("raw_dead"):
        rep.row(scene + " glide", "FAIL", "raw touch injection does not work in this run (no root?): glides cannot be played")
        return
    hit = n = 0
    got_all = ""
    lats = []
    for w in GLIDE_WORDS:
        try:
            ev0 = state().get("events", 0)
            t_end = drive.glide_word(w)
        except Exception as e:
            rep.row(scene + " glide '%s'" % w, "FAIL", str(e)[:100])
            continue
        st = wait_state(lambda s: s.get("events", 0) > ev0, timeout=6.0, step=0.03)
        if st.get("events", 0) > ev0:
            lats.append(time.time() - t_end)
        time.sleep(0.4)
    time.sleep(1.0)
    got_all = field_value()
    hit, n = words_hit(GLIDE_WORDS, got_all)
    # the same words with a sloppy thumb (every letter lands about 0.2 key off the centre, the way a real finger does): a second line in the report
    rnd = random.Random(17)
    before = got_all
    for w in GLIDE_WORDS:
        try:
            drive.glide_word(w, jitter=jitter_for(w, km, 0.2, rnd))
        except Exception:
            continue
        time.sleep(0.5)
    time.sleep(1.0)
    got2 = field_value()[len(before):] if field_value().startswith(before) else field_value()
    hit2, n2 = words_hit(GLIDE_WORDS, got2)
    rep.row(scene + " glide sloppy thumb", "INFO", "%d of %d words decoded right with a thumb 0.2 key off the centre: %r" % (hit2, n2, got2[:160]))
    rep.event(scene, glide_sloppy_hit=hit2, glide_sloppy_n=n2)
    lats.sort()
    lat = "median %.2fs, slowest %.2fs over %d words" % (lats[len(lats) // 2], lats[-1], len(lats)) if lats else "no word appeared"
    rep.row(scene + " glide summary", "INFO", "%d of %d words decoded right: %r; finger-up to word on the page: %s" % (hit, n, got_all[:160], lat))
    rep.event(scene, glide_hit=hit, glide_n=n, lat_median=(lats[len(lats) // 2] if lats else None), lat_max=(lats[-1] if lats else None))


class PageValue:
    def __call__(self):
        return state().get("value", "").replace("\xa0", " ")          # a contenteditable box keeps its spaces as no-break spaces

    def clear(self):
        pass


def pss_mb(pkg):
    """Total PSS of an app's processes in MB (dumpsys meminfo), or None."""
    out = sh("dumpsys meminfo %s" % pkg, check=False)
    m = re.search(r"^\s*TOTAL(?: PSS:)?\s+(\d+)", out, re.M)
    return round(int(m.group(1)) / 1024.0, 1) if m else None


def scene_memory(rep, tag):
    """What the keyboard and the browser hold in RAM now (the tablet has 2 GB and a 128 MB heap class)."""
    s, k = pss_mb(SWOOP_PKG), pss_mb(KESTREL_PKG)
    rep.row("memory " + tag, "INFO", "Swoop %s MB PSS, Kestrel %s MB PSS" % (s, k))
    rep.event("memory", tag=tag, swoop_mb=s, kestrel_mb=k)


def scene_fields(rep):
    w, h = screen()
    kinds = FIELD_KINDS
    for kind in kinds:
        reset()
        open_url("http://localhost:%d/f/%s" % (PORT, kind))
        st = wait_state(lambda s: s.get("kind") == kind and s.get("last") == "load", timeout=240 if kind == kinds[0] else 60)
        if st.get("last") != "load":
            rep.row("field %s page loaded" % kind, "FAIL", "the page never reported (Kestrel not started or the page did not load)")
            shot(rep, "field-%s-noload" % kind)
            continue
        rep.row("field %s page loaded" % kind, "OK")
        time.sleep(1.5)
        type_round(rep, "field %s" % kind, PageValue(), (w // 2, int(h * 0.30)), glide=True)
        shot(rep, "field-%s" % kind)
        sh("input keyevent KEYCODE_BACK", check=False)
        time.sleep(1.0)


AUTOFIX = [("teh ", "the "), ("dont ", "don't "), ("recieve ", "receive "), ("helo ", "hello "), ("im ", "I'm "), ("wierd ", "weird ")]


def scene_autocorrect(rep):
    """A word typed with a slip and then a space: the keyboard puts the right word in, inside a real Gecko field (the engine replaces composing text), and one Backspace gives the typed word back."""
    w, h = screen()
    reset()
    open_url("http://localhost:%d/f/text" % PORT)
    st = wait_state(lambda s: s.get("kind") == "text" and s.get("last") == "load", timeout=60)
    if st.get("last") != "load":
        rep.row("autocorrect page loaded", "FAIL", "the page never reported")
        return
    time.sleep(1.5)
    drive.tap(w // 2, int(h * 0.30))
    time.sleep(2.0)
    km = drive.wait_settled(timeout=15)
    if km is None or km.page != "letters":
        rep.row("autocorrect keyboard", "FAIL", "Swoop did not show")
        return
    right = 0
    for typed, want in AUTOFIX:
        n0 = state().get("events", 0)
        drive.type_text(typed)
        wait_state(lambda s: s.get("events", 0) > n0, timeout=5.0, step=0.2)
        time.sleep(1.0)
        got = state().get("value", "")
        ok = got.strip() == want.strip()
        right += 1 if ok else 0
        rep.row("autocorrect '%s'" % typed.strip(), "OK" if ok else "WARN", "the page holds %r, wanted %r" % (got, want))
        drive.backspaces(1)                                                         # Backspace right after the fix: the word as typed comes back
        time.sleep(1.0)
        back = state().get("value", "")
        rep.row("autocorrect '%s' undo" % typed.strip(), "INFO", "after one Backspace the page holds %r" % back)
        drive.backspaces(len(back) + 3)
        time.sleep(0.8)
    rep.row("autocorrect summary", "INFO", "%d of %d slips put right by the space" % (right, len(AUTOFIX)))
    rep.event("autocorrect", right=right, n=len(AUTOFIX))
    sh("input keyevent KEYCODE_BACK", check=False)


HALF_WORDS = [("because", 3), ("tomorrow", 3), ("important", 3)]          # the word glided, and how many of its first letters stay when the Backspaces are done (about half)


def composing_lost_reports():
    """Debug log: selection reports in which the app (Gecko) took away the composing word that the keyboard still believed in and nothing of ours explains (the case R40-3 fixed)."""
    out = adb("logcat", "-d", "-s", "SwoopDiag:D", check=False)
    return sum(1 for l in out.splitlines() if "sel report" in l and "cand=-1/-1" in l and "ours=false" in l and "composing=true" in l)


def scene_halfword(rep):
    """R40-3, owner 2026-11-28: "i glide a word and the keyboard gets it wrong so i delete half the word and press the correct word which is suggested, but it leaves the half typed word and simply adds the
    correct word to it". The same steps inside a real Gecko field: glide a word (clean, and with a sloppy thumb), Backspace until about half is left, tap a suggestion chip (the keyboard's debug log gives
    the chips' centres, never their words). The field must then hold ONE word: never the half word with the pick stuck on, never a space between them."""
    w, h = screen()
    reset()
    open_url("http://localhost:%d/f/text" % PORT)
    st = wait_state(lambda s: s.get("kind") == "text" and s.get("last") == "load", timeout=60)
    if st.get("last") != "load":
        rep.row("half word page loaded", "FAIL", "the page never reported")
        return
    time.sleep(1.5)
    drive.tap(w // 2, int(h * 0.30))
    time.sleep(2.0)
    km = drive.wait_settled(timeout=15)
    if km is None or km.page != "letters":
        rep.row("half word keyboard", "FAIL", "Swoop did not show")
        return
    rnd = random.Random(23)
    lost0 = composing_lost_reports()
    clean = tried = 0
    for word, keep in HALF_WORDS:
        for chip, sigma in ((0, 0.0), (1, 0.45)):
            label = "half word '%s' chip %d%s" % (word, chip, " (sloppy thumb)" if sigma else "")
            if not keyboard_showing():                    # a tap that missed the strip may have opened another screen: back to the field
                sh("input keyevent KEYCODE_BACK", check=False)
                time.sleep(1.0)
                drive.tap(w // 2, int(h * 0.30))
                time.sleep(2.0)
            km = drive.wait_settled(timeout=10)
            if km is None or km.page != "letters":
                rep.row(label, "WARN", "Swoop not showing, step skipped")
                continue
            ev0 = state().get("events", 0)
            try:
                drive.glide_word(word, km=km, jitter=jitter_for(word, km, sigma, rnd) if sigma else None)
            except Exception as e:
                rep.row(label, "FAIL", "glide: " + str(e)[:100])
                continue
            wait_state(lambda s: s.get("events", 0) > ev0, timeout=6.0, step=0.1)
            time.sleep(1.0)
            glided = PageValue()()
            drive.backspaces(max(0, len(glided) - keep))
            time.sleep(1.6)
            half = PageValue()()
            want = "chip%d" % chip
            km2 = drive.wait_keymap(page="letters", need=(want,), timeout=8)
            if km2 is None or want not in km2.keys:
                rep.row(label, "INFO", "glided %r, Backspaces left %r, but the strip showed no chip %d to tap" % (glided, half, chip))
                drive.backspaces(len(half) + 3)
                time.sleep(0.6)
                continue
            n0 = state().get("events", 0)
            drive.tap(*km2.keys[want])
            wait_state(lambda s: s.get("events", 0) > n0, timeout=5.0, step=0.2)
            time.sleep(1.2)
            after = PageValue()()
            tok, prefix = after.strip(), half.strip()
            if after == half:
                rep.row(label, "INFO", "glided %r, Backspaces left %r, the chip tap changed nothing" % (glided, half))
            else:
                tried += 1
                bad = (" " in tok) or (len(prefix) >= 2 and tok.count(prefix) > 1)
                clean += 0 if bad else 1
                rep.row(label, "FAIL" if bad else "OK", "glided %r, Backspaces left %r, picked a chip: the page holds %r%s" % (glided, half, after, "  <- the half word was NOT replaced" if bad else ""))
                shot(rep, "halfword-%s-%d%s" % (word, chip, "s" if sigma else ""))
            drive.backspaces(len(after) + 3)
            time.sleep(0.8)
    lost = max(0, composing_lost_reports() - lost0)
    rep.row("half word summary", "INFO", "%d of %d picks left one clean word; the app took the keyboard's composing word away by itself %d time(s) during these steps" % (clean, tried, lost))
    rep.event("halfword", clean=clean, tried=tried, app_ended_composing=lost)
    sh("input keyevent KEYCODE_BACK", check=False)
    time.sleep(1.0)


# ---------------------------------------------------------------------------------------------------------------------------------------------- Kestrel's own screen
def ui_dump():
    sh("uiautomator dump /sdcard/sim_ui.xml", check=False)
    try:
        return ET.fromstring(adb("exec-out", "cat", "/sdcard/sim_ui.xml", check=False))
    except Exception:
        return None


def center(node):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    if not m:
        return None
    x1, y1, x2, y2 = (int(v) for v in m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def dismiss_dialogs(root=None):
    """Kestrel asks before a site may use the location or send notifications (the Google results page asked: a dialog on top hides the address box from the UI dump). A person taps Block; so does the simulation."""
    root = root if root is not None else ui_dump()
    if root is None:
        return False
    for n in root.iter("node"):
        if n.get("text", "").strip().upper() in ("BLOCK", "DENY", "NOT NOW", "DON'T ALLOW"):
            c = center(n)
            if c:
                drive.tap(*c)
                time.sleep(0.8)
                return True
    return False


def find_edit(root, *hints):
    """The first EditText whose hint or text contains one of the hints (case-insensitive); with no hints (or none matching) the first EditText of Kestrel's own window: the address box holds the address, so its hint is not in the dump."""
    if root is None:
        return None
    edits = [n for n in root.iter("node") if n.get("class") == "android.widget.EditText" and n.get("package", KESTREL_PKG) == KESTREL_PKG]
    for n in edits:
        blob = (n.get("hint", "") + " " + n.get("text", "") + " " + n.get("content-desc", "")).lower()
        if any(h.lower() in blob for h in hints):
            return n
    return edits[0] if edits else None


HEALTH_LOADED = re.compile(r"page loaded host=(\S*).*?paint=(\S+).*?done=(\S+)")


def health_lines():
    return adb("logcat", "-d", "-s", "KestrelHealth:V", check=False).splitlines()


def wait_page_loaded(n_before, timeout=60.0):
    """The first 'page loaded' Health line that Kestrel wrote after [n_before] lines: (seconds, line) or (None, last line)."""
    t0 = time.time()
    last = ""
    while time.time() - t0 < timeout:
        ls = health_lines()
        for line in ls[n_before:]:
            last = line
            if "page loaded" in line:
                return time.time() - t0, line
        time.sleep(1.0)
    return None, last


def address_text():
    n = find_edit(ui_dump(), "address", "type a url", "search")
    return n.get("text", "") if n is not None else None


def last_start_input():
    """Swoop's newest 'startInput' diagnostics line (what kind of field it was told about), or ''."""
    lines = [l for l in adb("logcat", "-d", "-s", "SwoopDiag:D", check=False).splitlines() if "startInput" in l]
    return lines[-1].split("SwoopDiag:")[-1].strip() if lines else ""


def scene_address(rep):
    """Typing into Kestrel's own address-and-search box with Swoop (the keyboard shows the browser layout there), pressing Go, and watching the page load."""
    open_url("http://localhost:%d/" % PORT)
    time.sleep(6.0)
    tests = [("tap", "example.com", "example.com"), ("tap", "en.wikipedia.org", "en.wikipedia.org"), ("glide", ["weather", "today"], "weather today "),
             ("glide", ["browser", "keyboard", "google"], "browser keyboard google ")]
    for mode, what, expected in tests:
        scene = "address %s %s" % (mode, what if isinstance(what, str) else " ".join(what))
        open_url("http://localhost:%d/" % PORT)           # start every test from the plain local page: after a heavy page the bar can be tucked away
        time.sleep(3.0)
        ui = ui_dump()
        if dismiss_dialogs(ui):                              # a permission dialog of the page before (location) covers Kestrel: Block it
            rep.fact("a permission dialog was on top of Kestrel; the simulation tapped Block")
            ui = ui_dump()
        node = find_edit(ui, "type address", "type a url", "search")
        if node is None:
            time.sleep(2.0)
            node = find_edit(ui_dump(), "type address", "type a url", "search")
        if node is None:
            rep.row(scene, "FAIL", "no address box found in Kestrel's screen")
            shot(rep, "address-nobox")
            continue
        drive.tap(*center(node))
        time.sleep(1.5)
        km = drive.wait_settled(timeout=15)
        if km is None or km.page != "letters":
            rep.row(scene + " keyboard", "FAIL", "Swoop did not show for the address box (keyboard showing=%s)" % keyboard_showing())
            shot(rep, "address-nokeyboard")
            continue
        si = last_start_input()
        rep.row(scene + " keyboard", "OK" if "omnibox=true" in si else "WARN", "Swoop shows; browser layout keys: %s; field line: %s" % (", ".join(k for k in ("k2f", "k2e", "go") if k in km.keys), si[:150]))
        t0 = time.time()
        if mode == "tap":
            drive.type_text(what)
        else:
            for w in what:
                drive.glide_word(w, km)
                time.sleep(0.6)
        time.sleep(0.8)
        got = address_text()
        if got is not None and expected.strip() not in got:
            rep.fact("the address box after typing held %r (wanted %r)" % (got, expected))
        shot(rep, "address-typed-%d" % len(rep.rows))
        ok = got is not None and got.strip() == expected.strip()
        rep.row(scene + " text", "OK" if ok else "WARN", "the box holds %r, wanted %r (%.1fs of typing)" % (got, expected, time.time() - t0))
        n0 = len(health_lines())
        km = drive.read_keymap() or km
        if "go" in km.keys:
            drive.tap(*km.keys["go"])
        else:
            sh("input keyevent KEYCODE_ENTER", check=False)
        secs, line = wait_page_loaded(n0, timeout=75.0)
        if secs is None:
            rep.row(scene + " page", "FAIL", "no 'page loaded' line within 75 s; last Health line: %s" % line[-120:])
        else:
            rep.row(scene + " page", "OK", "loaded after %.1fs: %s" % (secs, line.split("KestrelHealth")[-1].strip()[:140]))
        shot(rep, "address-page-%d" % len(rep.rows))
        time.sleep(1.0)


def scene_private(rep):
    """A page field in a PRIVATE tab must make Swoop learn nothing: the field says so (IME_FLAG_NO_PERSONALIZED_LEARNING) and Swoop's debug line shows noLearn=true; a normal tab shows noLearn=false."""
    w, h = screen()
    for label, private in (("normal tab", False), ("private tab", True)):
        reset()
        url = "http://localhost:%d/f/text" % PORT
        if private:
            sh("am start -n %s --es kestrel_debug privtab --es kestrel_arg '%s'" % (KESTREL_ACT, url), check=False)
        else:
            open_url(url)
        st = wait_state(lambda s: s.get("last") == "load", timeout=60)
        if st.get("last") != "load":
            rep.row("field %s page loaded" % label, "FAIL", "the page never reported")
            continue
        time.sleep(1.5)
        before = adb("logcat", "-d", "-s", "SwoopDiag:D", check=False).count("startInput")
        drive.tap(w // 2, int(h * 0.30))
        time.sleep(2.5)
        lines = [l for l in adb("logcat", "-d", "-s", "SwoopDiag:D", check=False).splitlines() if "startInput" in l]
        last = lines[-1] if lines and len(lines) > before else (lines[-1] if lines else "")
        m = re.search(r"noLearn=(\w+)", last)
        got = m.group(1) if m else None
        want = "true" if private else "false"
        rep.row("field %s noLearn" % label, "OK" if got == want else "FAIL", "Swoop's field line: %s" % last.split("SwoopDiag:")[-1].strip()[:150])
        shot(rep, "private-%s" % ("on" if private else "off"))
        sh("input keyevent KEYCODE_BACK", check=False)
        time.sleep(1.0)


PAGES = ["https://en.wikipedia.org/wiki/Android_(operating_system)", "https://news.ycombinator.com/", "https://lite.cnn.com/", "https://developer.mozilla.org/en-US/docs/Web/CSS",
         "https://developer.android.com/guide", "https://docs.python.org/3/tutorial/index.html", "https://kotlinlang.org/docs/home.html", "https://lobste.rs/", "https://apnews.com/",
         "https://archive.org/", "https://text.npr.org/", "https://slashdot.org/", "https://github.com/mozilla/gecko-dev", "https://duckduckgo.com/?q=kestrel+browser+android"]


def load_numbers(line):
    m = re.search(r"paint=(-?\d+)ms done=(-?\d+)ms blocked=(\d+).*?ok=(\w+)", line)
    return (int(m.group(1)), int(m.group(2)), int(m.group(3)), m.group(4)) if m else None


def _one_page(rep, url, paints, dones):
    n0 = len(health_lines())
    open_url(url)
    secs, line = wait_page_loaded(n0, timeout=100.0)
    host = url.split("/")[2]
    if secs is None:
        rep.row("page " + host, "FAIL", "no 'page loaded' line within 100 s (last Health line: %s)" % line[-100:])
        shot(rep, "page-%s-fail" % host)
        return
    nums = load_numbers(line)
    if nums:
        paint, done, blocked, ok = nums
        if paint >= 0:
            paints.append(paint)
        dones.append(done)
        rep.row("page " + host, "OK" if ok == "true" else "WARN", "first paint %s ms, loaded %s ms, %d requests blocked, ok=%s" % (paint, done, blocked, ok))
        rep.event("page", host=host, paint_ms=paint, done_ms=done, blocked=blocked, ok=ok)
    else:
        rep.row("page " + host, "INFO", line[-140:])
    time.sleep(2.0)
    shot(rep, "page-%s" % host)
    # a person scrolls: two flicks down
    w, h = screen()
    for _ in range(2):
        drive.inject([(w // 2, int(h * 0.75)), (w // 2, int(h * 0.45)), (w // 2, int(h * 0.25))], per_chunk=2, pause=0.02)
        time.sleep(0.8)


def scene_pages(rep, minutes):
    """Real sites, one after the other, the way a person opens them: how long to the first paint and to the end of the load, how many requests the shields blocked, did it load.
    A site that hangs the emulator is a finding of its own (the run waits for Android to come back and goes on)."""
    end = time.time() + minutes * 60
    paints, dones = [], []
    fails_in_a_row = 0
    for i, url in enumerate(PAGES):
        if time.time() > end:
            rep.row("pages", "INFO", "stopped after the time limit of %d minutes" % minutes)
            break
        before = len(dones)
        try:
            _one_page(rep, url, paints, dones)
        except Exception as e:
            host = url.split("/")[2]
            rep.row("page " + host, "FAIL", "the step broke: %s %s" % (type(e).__name__, str(e)[:100]))
            if not recover_device(rep, "%s while loading %s" % (type(e).__name__, host)):
                break
        fails_in_a_row = 0 if len(dones) > before else fails_in_a_row + 1
        if fails_in_a_row >= 3:
            rep.row("pages", "FAIL", "three sites in a row did not load: the emulator's graphics or Kestrel is gone (a system_server watchdog restart loop); the scene stops here instead of waiting 100 s per site")
            break
        if i % 4 == 3:
            scene_memory(rep, "after %d pages" % (i + 1))
    if dones:
        dones.sort()
        paints.sort()
        rep.row("pages summary", "INFO", "%d pages: first paint median %s ms (slowest %s), load finished median %s ms (slowest %s)" % (
            len(dones), paints[len(paints) // 2] if paints else None, paints[-1] if paints else None, dones[len(dones) // 2], dones[-1]))


VIDEOS = ["https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
          "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4"]       # (a 1080p film took the emulator's software graphics down: not in the list)
FILM_LINES = re.compile(r"player|film|stats|inline|hand ?over|native|playback|dropped|rebuffer|stall|decoder|video info|probe page", re.I)
FILM_BAD = re.compile(r"\b(error|errors|failed|failure|rebuffer\w*|timeout|timed out)\b|(?<![0-9])[1-9][0-9]* stall|ui STALL", re.I)      # a stall COUNT above zero or a blocked UI thread; "0 stall(s)" is good news


def _one_film(rep, url, seconds):
    """Opens a 10 second film, presses play like a person (Kestrel never plays a film by itself), lets it run, closes the player (its own `stats` line is written then) and reads what Kestrel's Health lines said."""
    name = url.rsplit("/", 1)[-1][:34]
    n0 = len(health_lines())
    open_url(url)
    secs, line = wait_page_loaded(n0, timeout=60.0)
    if secs is None:
        rep.row("video " + name, "FAIL", "the page did not load within 60 s (last Health line: %s)" % line[-90:])
        shot(rep, "video-%d-noload" % len(rep.rows))
        sh("input keyevent KEYCODE_BACK", check=False)
        return
    time.sleep(2.0)
    w, h = screen()
    drive.tap(w // 2, int(h * 0.40))
    time.sleep(min(6.0, seconds / 2.0))
    shot(rep, "video-%d-playing" % len(rep.rows))
    time.sleep(max(1.0, seconds - min(6.0, seconds / 2.0)))
    sh("input keyevent KEYCODE_BACK", check=False)
    time.sleep(2.5)
    new = health_lines()[n0:]
    keep = [l.split("KestrelHealth")[-1].strip() for l in new if FILM_LINES.search(l) and "ui prof" not in l and "cosmetic" not in l]
    bad = [l for l in keep if FILM_BAD.search(l)]
    rep.row("video " + name, "WARN" if bad else "INFO", "page loaded in %.1fs; %d player lines in %ds%s" % (secs, len(keep), seconds, ("; PROBLEMS: " + " | ".join(bad[:2])[:220]) if bad else ""))
    for l in keep[-12:]:
        rep.fact("video line: " + l[:210])


def scene_video(rep, seconds=14):
    """Films: does the page load, does the film start when pressed, what do the player's own lines (picture bitrate, stalls, dropped frames) say."""
    for url in VIDEOS:
        try:
            _one_film(rep, url, seconds)
        except Exception as e:
            rep.row("video " + url.rsplit("/", 1)[-1][:30], "FAIL", "the step broke: %s %s" % (type(e).__name__, str(e)[:100]))
            if not recover_device(rep, "%s during a film" % type(e).__name__):
                break


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="sim-out")
    ap.add_argument("--swoop")
    ap.add_argument("--kestrel")
    ap.add_argument("--browser-dir", default="browser")
    ap.add_argument("--suite", default="typing")
    ap.add_argument("--net", default="full")
    ap.add_argument("--minutes", type=int, default=15)
    ap.add_argument("--quick", action="store_true", help="fewer phrases, words and field kinds (a run of about 10 minutes instead of 30)")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    if a.selftest:
        assert edit_distance("kitten", "sitting") == 3 and words_hit(["a", "b"], "a b ") == (2, 2) and words_hit(["a", "b"], "a x") == (1, 2)
        print("selftest ok")
        return 0
    if a.quick:
        global TAP_PHRASES, GLIDE_WORDS, FIELD_KINDS
        TAP_PHRASES = [TAP_PHRASES[i] for i in (0, 1, 4, 5)]          # plain words, a digit (the keyboard stays on the symbols page), an apostrophe and a comma, a capital and a question mark
        GLIDE_WORDS = GLIDE_WORDS[:10]
        FIELD_KINDS = ["text", "textarea", "editable"]
    rep = Report(a.out)
    srv = None
    try:
        device_facts(rep)
        adb("logcat", "-G", "16M", check=False)          # Gecko is chatty: keep the keyboard's lines (and the browser's Health lines) for the whole run
        rep.fact("suite=%s net=%s" % (a.suite, a.net))
        if not (a.swoop and a.kestrel):
            rep.row("arguments", "FAIL", "--swoop and --kestrel are needed")
            return 1
        who = sh("id", check=False)
        if "uid=0" not in who:
            adb("root", check=False)                          # raw touch injection (glides, fast taps) writes to the touch device: it needs root, which the emulator allows
            time.sleep(3)
            adb("wait-for-device", check=False, timeout=60)
            who = sh("id", check=False)
        rep.fact("adb shell runs as: %s" % who.strip()[:70])
        if "uid=0" not in who:
            rep.row("adb root", "FAIL", "the shell is not root: raw touch injection cannot work, taps fall back to `input tap` and glides cannot be played")
        ok1 = install(rep, a.swoop, "Swoop")
        ok2 = install(rep, a.kestrel, "Kestrel")
        if not (ok1 and ok2):
            return 1
        choose_swoop(rep)
        srv = start_server(rep)
        scene_memory(rep, "before")
        if a.suite in ("typing", "all"):
            scene_fields(rep)
            scene_autocorrect(rep)
            scene_halfword(rep)
            scene_address(rep)
            scene_private(rep)
            scene_memory(rep, "after typing")
        if a.suite in ("pages", "all"):
            scene_pages(rep, a.minutes)
            scene_memory(rep, "after pages")
        if a.suite in ("video", "all"):
            scene_video(rep)
            scene_memory(rep, "after video")
    except Exception as e:
        rep.row("run", "FAIL", "the run itself broke: %s" % str(e)[:160])
    finally:
        try:
            adb("logcat", "-d", "-t", "4000", check=False)
            open(os.path.join(a.out, "logcat-tail.txt"), "w", encoding="utf-8", errors="replace").write(adb("logcat", "-d", "-t", "6000", check=False))
        except Exception:
            pass
        if srv:
            srv.terminate()
        st = drive.STATS
        if drive.STATS.get("raw_dead"):
            rep.row("raw touch injection", "FAIL", "no key press followed a raw batch twice: every tap went through `input tap`")
        rep.fact("driver: %d key-map reads (%.2f s each on average), %d tap batches with %d taps, the first batch lost and sent again %d time(s)" % (
            st["reads"], st["read_s"] / max(1, st["reads"]), st["batches"], st["taps"], st["first_batch_lost"]))
        n = rep.write()
        print("daily sim done: %s" % n)
    return 0


if __name__ == "__main__":
    sys.exit(main())
