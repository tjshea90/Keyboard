#!/usr/bin/env python3
"""tools/sim/labserver.py - the pages the daily-use simulation types into (R17c). Standard library only: python3 -I tools/sim/labserver.py <port> <logfile>

One page per kind of field, each with ONE big field that fills the upper half of the viewport (so a tap at a fixed place of the screen focuses it, whatever the toolbar height is):
  /f/text /f/search /f/textarea /f/editable /f/url /f/email      (/f/search is a form: Enter goes to /search?q=...)
The page reports what its own field receives (the SERVER side proof of what the keyboard really put into the page): every input / beforeinput / composition event goes to /log, and
  /state   -> JSON {"value": ..., "events": n, "comp": n, "last": ...} of the field of the page that reported last, so the driver can poll it
  /reset   -> forgets the state (call it before a typing round)
Everything is also written to <logfile> as JSON lines. Nothing here is part of either app. The emulator reaches the server as http://10.0.2.2:<port>/ (or localhost after `adb reverse`).
"""
import http.server
import json
import sys
import time
import urllib.parse

PAGE = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Typing lab: %(kind)s</title>
<style>
 body { font: 20px sans-serif; margin: 0; background: #fff; color: #111 }
 #hdr { height: 36px; line-height: 36px; padding: 0 10px; background: #def; font-size: 15px }
 #f { display: block; width: 96%%; margin: 8px 2%%; height: 42vh; font-size: 24px; padding: 8px; box-sizing: border-box; border: 2px solid #468 }
 #out { margin: 8px; font: 14px monospace; white-space: pre-wrap; color: #555 }
</style></head><body>
<div id="hdr">Typing lab: %(kind)s</div>
%(field)s
<div id="out">no input yet</div>
<script>
 var f = document.getElementById('f'), out = document.getElementById('out'), n = 0, comp = 0;
 function val() { return f.value !== undefined ? f.value : f.innerText; }
 function rep(ev, extra) {
   n++; var v = val(); out.textContent = ev + ' #' + n + ' len ' + v.length + ': ' + v.slice(-60);
   var u = '/log?kind=%(kind)s&ev=' + ev + '&n=' + n + '&comp=' + comp + '&v=' + encodeURIComponent(v) + (extra || '') + '&t=' + Date.now();
   try { var x = new XMLHttpRequest(); x.open('GET', u, true); x.send(); } catch (e) {}
 }
 f.addEventListener('input', function (e) { rep('input', '&it=' + encodeURIComponent(e.inputType || '') + '&comp_=' + (e.isComposing ? 1 : 0)); });
 f.addEventListener('compositionstart', function () { comp++; });
 f.addEventListener('keydown', function (e) { if (e.key === 'Enter') rep('enter'); });
 window.addEventListener('load', function () { rep('load'); });
</script></body></html>"""

FIELDS = {
    "text": '<input id="f" type="text" placeholder="type here" autocomplete="off">',
    "search": '<form action="/search" method="get"><input id="f" name="q" type="search" placeholder="search" autocomplete="off"></form>',
    "textarea": '<textarea id="f" placeholder="many lines" autocomplete="off"></textarea>',
    "editable": '<div id="f" class="big" contenteditable="true" style="border:2px solid #468"></div>',
    "url": '<input id="f" type="url" placeholder="https://" autocomplete="off">',
    "email": '<input id="f" type="email" placeholder="you@example.com" autocomplete="off">',
    # R40-3: a page that re-sets the field's value from its own state a moment after every change (the React / Flutter / Compose pattern). That ENDS the keyboard's composing word while the cursor stays: the owner's
    # "half-typed word stays and the picked word is added to it" case, made on purpose.
    "resetting": '<input id="f" type="text" placeholder="type here" autocomplete="off"><script>(function(){var f=document.getElementById("f");f.addEventListener("input",function(){setTimeout(function(){var v=f.value,s=f.selectionStart;f.value="";f.value=v;try{f.setSelectionRange(s,s);}catch(e){}},40);});})();</script>',
}

STATE = {"value": "", "events": 0, "comp": 0, "last": "", "kind": ""}
LOGFILE = None


def log(obj):
    obj["ts"] = round(time.time(), 3)
    if LOGFILE:
        with open(LOGFILE, "a", encoding="utf-8") as fh:
            fh.write(json.dumps(obj, ensure_ascii=False) + "\n")


class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def _send(self, body, ctype="text/html; charset=utf-8", code=200):
        b = body.encode("utf-8") if isinstance(body, str) else body
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(b)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(b)

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = urllib.parse.parse_qs(u.query)
        if u.path.startswith("/f/"):
            kind = u.path[3:]
            if kind not in FIELDS:
                return self._send("no such kind", code=404)
            return self._send(PAGE % {"kind": kind, "field": FIELDS[kind]})
        if u.path == "/search":
            v = (q.get("q") or [""])[0]
            log({"ev": "submit", "value": v})
            STATE["last"] = "submit"
            return self._send("<!doctype html><title>Search</title><body style='font:22px sans-serif'><h3>Search results for</h3><p>%s</p>" % (v.replace("<", "&lt;")))
        if u.path == "/log":
            g = lambda k: (q.get(k) or [""])[0]
            n = int(g("n") or 0)
            if g("ev") == "load" or g("kind") != STATE.get("kind") or n >= STATE.get("events", 0):          # the page numbers its events: one that arrives late (the reports travel on separate requests) must not overwrite a newer value
                STATE.update(value=g("v"), events=n, comp=int(g("comp") or 0), last=g("ev"), kind=g("kind"))
            log({"ev": g("ev"), "kind": g("kind"), "n": g("n"), "comp": g("comp"), "it": g("it"), "isComposing": g("comp_"), "value": g("v")})
            return self._send("ok", "text/plain")
        if u.path == "/state":
            return self._send(json.dumps(STATE), "application/json")
        if u.path == "/reset":
            STATE.update(value="", events=0, comp=0, last="", kind="")
            log({"ev": "reset"})
            return self._send("ok", "text/plain")
        if u.path == "/":
            return self._send("<!doctype html><title>Typing lab</title><body style='font:22px sans-serif'>" + "".join("<p><a href='/f/%s'>%s</a></p>" % (k, k) for k in FIELDS))
        return self._send("not found", code=404)


def main():
    global LOGFILE
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8099
    LOGFILE = sys.argv[2] if len(sys.argv) > 2 else None
    http.server.ThreadingHTTPServer(("0.0.0.0", port), H).serve_forever()


if __name__ == "__main__":
    main()
