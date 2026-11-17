#!/usr/bin/env python3
"""tools/sim/dailysim.py - plays a day of keyboard + browser use on the emulator and writes a report (R17c). Standard library only.

  python3 -u -I tools/sim/dailysim.py --out DIR --swoop ime-debug.apk --kestrel kestrel.apk --browser-dir browser [--suite typing|pages|video|all] [--net hotspot|full] [--minutes 15]
  python3 -u -I tools/sim/dailysim.py --selftest        (no device: checks the parsers and the scoring)

What a person does, in this order (every step is a recorded scene; a step that cannot be done is a FINDING in the report, never a crash of the run):
  setup    install Swoop + Kestrel, make Swoop THE keyboard, start the lab server (tools/sim/labserver.py), the 5 Mbit/s hotspot is already imitated by the emulator options
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
import re
import subprocess
import sys
import threading
import time
import urllib.request

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
def type_round(rep, scene, field_value, tap_target, glide=True):
    """One kind of field: tap it, wait for the keyboard, tap-type the phrases and glide the words; [field_value]() returns what the field holds."""
    x, y = tap_target
    drive.tap(x, y)
    time.sleep(2.0)
    km = drive.wait_keymap("letters", need=["k61"], timeout=12)
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
        miss = drive.type_text(ph)
        time.sleep(0.8)
        got = field_value()
        d = edit_distance(ph.lower(), got.lower().rstrip())
        tot += 1
        ok += 1 if d == 0 else 0
        rep.row(scene + " tap '%s'" % ph[:24], "OK" if d == 0 else "WARN", "got %r (distance %d, %.1fs%s)" % (got[:50], d, time.time() - t0, ", %d keys missing" % miss if miss else ""))
        # clear for the next phrase: select all + delete through the page (long Backspace is slow): the driver uses repeated Backspace through the keyboard
        for _ in range(len(got) + 2):
            km2 = drive.read_keymap() or km
            if "back" in km2.keys:
                drive.tap(*km2.keys["back"])
        time.sleep(0.5)
    rep.row(scene + " tap summary", "INFO", "%d of %d phrases exact" % (ok, tot))
    if not glide:
        return
    # gliding
    hit = n = 0
    got_all = ""
    for w in GLIDE_WORDS:
        try:
            drive.glide_word(w)
        except Exception as e:
            rep.row(scene + " glide '%s'" % w, "FAIL", str(e)[:100])
            continue
        time.sleep(0.5)
    time.sleep(1.0)
    got_all = field_value()
    hit, n = words_hit(GLIDE_WORDS, got_all)
    rep.row(scene + " glide summary", "INFO", "%d of %d words decoded right: %r" % (hit, n, got_all[:160]))


class PageValue:
    def __call__(self):
        return state().get("value", "")

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
    kinds = ["text", "search", "textarea", "editable", "url", "email"]
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="sim-out")
    ap.add_argument("--swoop")
    ap.add_argument("--kestrel")
    ap.add_argument("--browser-dir", default="browser")
    ap.add_argument("--suite", default="typing")
    ap.add_argument("--net", default="hotspot")
    ap.add_argument("--minutes", type=int, default=15)
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    if a.selftest:
        assert edit_distance("kitten", "sitting") == 3 and words_hit(["a", "b"], "a b ") == (2, 2) and words_hit(["a", "b"], "a x") == (1, 2)
        print("selftest ok")
        return 0
    rep = Report(a.out)
    srv = None
    try:
        device_facts(rep)
        rep.fact("suite=%s net=%s" % (a.suite, a.net))
        if not (a.swoop and a.kestrel):
            rep.row("arguments", "FAIL", "--swoop and --kestrel are needed")
            return 1
        ok1 = install(rep, a.swoop, "Swoop")
        ok2 = install(rep, a.kestrel, "Kestrel")
        if not (ok1 and ok2):
            return 1
        choose_swoop(rep)
        srv = start_server(rep)
        scene_memory(rep, "before")
        if a.suite in ("typing", "all"):
            scene_fields(rep)
            scene_memory(rep, "after typing")
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
        n = rep.write()
        print("daily sim done: %s" % n)
    return 0


if __name__ == "__main__":
    sys.exit(main())
