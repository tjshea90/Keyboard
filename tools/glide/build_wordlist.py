#!/usr/bin/env python3
"""Builds the word/count list that the glide lexicon compiler reads (docs/DICTIONARY.md, "How the file is made").

Run it with `python3 -I` on DATA that was downloaded into folders OUTSIDE the repository. It only reads those files (a msgpack reader of our own is
included, nothing from the downloaded packages is imported or run):

  python3 -I tools/glide/build_wordlist.py --wordfreq large_en.msgpack.gz --hermit en_full.txt --symspell frequency_dictionary_en_82_765.txt \
      --scowl <scowl>/final --extra tools/glide/extra-words.txt --out words.txt

What it does (the reasoning is in docs/DICTIONARY.md):
  * the VOCABULARY is every word of SCOWL 2020.12.07 up to size 70 (spell-checker dictionaries: inflections, lemmas), the bigger sizes only when a frequency source has seen the word,
    plus words that are not in SCOWL (slang, brands, web names) only when they are frequent in at least two of the three frequency sources;
  * the COUNT of a word is a log-linear blend of three frequency sources (wordfreq 3.1.1 large_en, OpenSubtitles via hermitdave FrequencyWords, SymSpell/Google Books), so a word has to be
    plausible in casual AND in written English to rank high; a word nobody has counted gets a tiny count (present, but it loses to every common word);
  * the counts are scaled to add up to TOTAL (the scale the personal model was tuned for) and written as "word count".
"""
import argparse
import gzip
import math
import os
import re
import struct
import sys

TOTAL = 541808760578  # what the counts of the old list added up to; the personal model's weights (PersonalModel.UNIGRAM_COUNT) are on this scale


def unpack_all(data):
    """A minimal msgpack reader: enough for the wordfreq data files (ints, strings, arrays, maps)."""
    pos = 0

    def u(fmt, n):
        nonlocal pos
        v = struct.unpack(fmt, data[pos:pos + n])[0]
        pos += n
        return v

    def rd():
        nonlocal pos
        b = data[pos]
        pos += 1
        if b <= 0x7F:
            return b
        if b >= 0xE0:
            return b - 256
        if 0xA0 <= b <= 0xBF:
            n = b & 0x1F
            s = data[pos:pos + n].decode('utf-8')
            pos += n
            return s
        if 0x90 <= b <= 0x9F:
            return [rd() for _ in range(b & 0x0F)]
        if 0x80 <= b <= 0x8F:
            return {rd(): rd() for _ in range(b & 0x0F)}
        if b == 0xC0:
            return None
        if b in (0xD9, 0xDA, 0xDB):
            n = u('>B', 1) if b == 0xD9 else u('>H', 2) if b == 0xDA else u('>I', 4)
            s = data[pos:pos + n].decode('utf-8')
            pos += n
            return s
        if b == 0xDC:
            return [rd() for _ in range(u('>H', 2))]
        if b == 0xDD:
            return [rd() for _ in range(u('>I', 4))]
        if b == 0xDE:
            return {rd(): rd() for _ in range(u('>H', 2))}
        if b == 0xCC:
            return u('>B', 1)
        if b == 0xCD:
            return u('>H', 2)
        if b == 0xCE:
            return u('>I', 4)
        raise ValueError('unsupported msgpack byte %#x' % b)

    out = []
    while pos < len(data):
        out.append(rd())
    return out


def load_wordfreq(path):
    """word -> frequency (a fraction of all words). The file is one array: a header map, then one array of words per step of 1/100 of a decibel below 1.0."""
    items = unpack_all(gzip.open(path).read())
    items = items[0] if isinstance(items[0], list) else items
    assert items[0].get('format') == 'cB', items[0]
    res = {}
    for i, words in enumerate(items[1:]):
        f = 10 ** (-i / 100.0)
        for w in words:
            res[w] = max(res.get(w, 0.0), f)
    return res


def load_counts(path):
    res, total = {}, 0
    with open(path, encoding='utf-8', errors='replace') as fh:
        for line in fh:
            a = line.split()
            if len(a) == 2 and a[1].isdigit():
                res[a[0]] = int(a[1])
                total += int(a[1])
    return res, total


WORD = re.compile(r"^[a-z]+(?:'[a-z]+)?$")
CONTRACTION = re.compile(r"^[a-z]+'(?:t|s|d|ll|re|ve|m)$")
# 's is a contraction only after these (otherwise it is a possessive, and "dog's" would compete with "dogs" on the same keys)
S_OK = {'it', 'that', 'what', 'there', 'here', 'he', 'she', 'who', 'where', 'how', 'let', 'when', 'why', 'everyone', 'somebody', 'someone', 'nobody', 'everybody', 'everything', 'something', 'nothing', 'one', 'which'}


def read_scowl(folder, max_level, with_levels):
    """word -> smallest SCOWL size it appears in, from the US-spelling lists, the shared lists and the contractions (lowercase only: names are not words to glide)."""
    best = {}
    for name in sorted(os.listdir(folder)):
        m = re.match(r'^(english|american)-(words|contractions)\.(\d+)$', name)
        if not m:
            continue
        level = int(m.group(3))
        if level > with_levels:
            continue
        with open(os.path.join(folder, name), encoding='latin-1') as fh:
            for line in fh:
                w = line.strip()
                if m.group(2) == 'contractions':
                    w = w.lower()
                if not WORD.match(w):
                    continue
                if level < best.get(w, 999):
                    best[w] = level
    return best


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--wordfreq', required=True)
    ap.add_argument('--hermit', required=True)
    ap.add_argument('--symspell', required=True)
    ap.add_argument('--scowl', required=True)
    ap.add_argument('--extra', action='append', default=[])
    ap.add_argument('--out', required=True)
    ap.add_argument('--report', default=None)
    ap.add_argument('--scowl-all', type=int, default=70, help='SCOWL sizes up to this are all admitted')
    ap.add_argument('--scowl-seen', type=int, default=95, help='SCOWL sizes up to this are admitted when a frequency source has seen the word')
    ap.add_argument('--wf-new', type=float, default=5e-6, help='a word that is not in SCOWL needs at least this wordfreq frequency ...')
    ap.add_argument('--hd-new', type=int, default=300, help='... and at least this many OpenSubtitles occurrences')
    ap.add_argument('--short2', type=float, default=1.5e-5, help='a 2-letter word needs at least this wordfreq frequency')
    ap.add_argument('--short3', type=float, default=1e-6, help='a 3-letter word needs at least this wordfreq frequency')
    ap.add_argument('--total', type=int, default=TOTAL)
    a = ap.parse_args()

    wf = load_wordfreq(a.wordfreq)
    hd, hd_total = load_counts(a.hermit)
    sy, sy_total = load_counts(a.symspell)
    sc = read_scowl(a.scowl, a.scowl_all, a.scowl_seen)
    print('sources: wordfreq %d, hermit %d (total %d), symspell %d, scowl %d' % (len(wf), len(hd), hd_total, len(sy), len(sc)), file=sys.stderr)

    extras = {}
    for path in a.extra:
        with open(path, encoding='utf-8') as fh:
            for line in fh:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                p = line.split()
                if len(p) == 2 and p[1].isdigit():
                    extras[p[0]] = max(int(p[1]), extras.get(p[0], 0))

    sy_min = min(sy.values())
    sy_floor = 0.5 * sy_min / sy_total
    wf_floor = 5e-9
    hd_floor = 0.5 / hd_total

    vocab = {}  # word -> why
    for w, level in sc.items():
        f = wf.get(w, 0.0)
        if len(w) == 1 and w not in ('a', 'i'):
            continue
        if "'" in w and not (CONTRACTION.match(w) and (not w.endswith("'s") or w[:-2] in S_OK)):
            continue
        if level <= a.scowl_all:
            vocab[w] = 'scowl%d' % level
        elif (f > 0 or w in sy) and (level <= 80 or hd.get(w, 0) >= 5):
            vocab[w] = 'scowl%d+seen' % level
    # a glide over two or three letters matches a lot of junk by accident, so short words need real use (the dictionary alone does not make "xu" or "tui" a word to offer)
    for w in list(vocab):
        n = len(w.replace("'", ""))
        if w in extras or n >= 4:
            continue
        f = wf.get(w, 0.0)
        if n == 1 and w not in ('a', 'i'):
            del vocab[w]
        elif n == 2 and f < a.short2:
            del vocab[w]
        elif n == 3 and f < a.short3:
            del vocab[w]
    # words SCOWL does not have: slang, brands, web names, texting words
    for w, f in wf.items():
        if w in vocab or w in sc:
            continue
        if not WORD.match(w) or len(w) < 2:
            continue
        if "'" in w and not (CONTRACTION.match(w) and (not w.endswith("'s") or w[:-2] in S_OK)):
            continue
        if re.search(r'(.)\1\1', w):
            continue
        if f < a.wf_new or hd.get(w, 0) < a.hd_new:
            continue
        if len(w) == 2 and f < a.short2:
            continue
        vocab[w] = 'new'
    for w in extras:
        if WORD.match(w) and w not in vocab:
            vocab[w] = 'extra'

    # the blend: weighted mean of ln(probability); a word a source cannot know (not in SCOWL, so not in the SymSpell list) is judged by the others only
    WF_W, HD_W, SY_W = 0.40, 0.30, 0.30
    mass = {}
    for w, why in vocab.items():
        num = WF_W * math.log(max(wf.get(w, 0.0), wf_floor)) + HD_W * math.log(max(hd.get(w, 0) / hd_total, hd_floor))
        den = WF_W + HD_W
        if w in sy:
            num += SY_W * math.log(sy[w] / sy_total)
            den += SY_W
        elif w in sc:
            num += SY_W * math.log(sy_floor)
            den += SY_W
        mass[w] = math.exp(num / den)
    s = sum(mass.values())
    counts = {w: max(1, int(round(m / s * a.total))) for w, m in mass.items()}
    for w, c in extras.items():
        if w in counts:
            counts[w] = max(counts[w], c)  # "at least this count", like the compiler does for --extra

    with open(a.out, 'w', encoding='utf-8') as fh:
        for w, c in sorted(counts.items(), key=lambda kv: -kv[1]):
            fh.write('%s %d\n' % (w, c))
    why_n = {}
    for w in counts:
        k = vocab[w].split('+')[0]
        why_n[k] = why_n.get(k, 0) + 1
    print('words: %d  by origin: %s' % (len(counts), ', '.join('%s %d' % kv for kv in sorted(why_n.items()))), file=sys.stderr)
    if a.report:
        with open(a.report, 'w', encoding='utf-8') as fh:
            for w, c in sorted(counts.items(), key=lambda kv: -kv[1]):
                fh.write('%s\t%d\t%s\twf=%.2e\thd=%d\tsy=%d\n' % (w, c, vocab[w], wf.get(w, 0), hd.get(w, 0), sy.get(w, 0)))


if __name__ == '__main__':
    main()
