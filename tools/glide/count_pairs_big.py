#!/usr/bin/env python3
"""R34a: counts which word follows which in a BIG web-text corpus (C4, ODC-BY: https://huggingface.co/datasets/allenai/c4) and writes the pair file that
`gradlew :tools:glideLexicon --args="--base en.klx --merge-pairs pairs.txt ..."` mixes into the word list (docs/DICTIONARY.md). Same rules as count_pairs.py (a stretch of plain words of the list; any
punctuation, digit, quote, line end or word that is not in the list ends the stretch, as the keyboard has no word before it either), but numpy, one JSON line per document, and shards of 150 million words.

  python3 -I tools/glide/count_pairs_big.py --klx ime/src/main/assets/glide/en.klx --jsonl-gz shard0.json.gz [--jsonl-gz shard1.json.gz ...] --out pairs_c4.txt [--min-count 8] [--top-k 40] [--max-docs N]

Reads the word list straight from the compiled file (no separate vocabulary dump). The text is only read, never executed; a truncated .gz (a partial download) is read up to its end. Counts only, nothing of the text is kept.
numpy is needed (python3 -m pip install numpy).
"""
import argparse
import gzip
import json
import re
import struct
import sys
import zlib

import numpy as np

TOKEN = re.compile(r"[a-z]+(?:'[a-z]+)?|\n|[^a-z\s]")


def read_vocab(path):
    b = open(path, "rb").read()
    magic, n, blob_len, n_display, n_pairs = struct.unpack_from(">5i", b, 0)
    assert magic == 0x4B474C31, "not a glide word list"
    pos = 28
    lens = b[pos:pos + n]; pos += n
    blob = b[pos:pos + blob_len]; pos += blob_len
    pos += 2 * n                                                    # log probabilities
    words = []
    o = 0
    for i in range(n):
        words.append("".join(chr(97 + c) for c in blob[o:o + lens[i]])); o += lens[i]
    for _ in range(n_display):
        wid = struct.unpack_from(">i", b, pos)[0]; l = b[pos + 4]; words[wid] = b[pos + 5:pos + 5 + l].decode("utf-8"); pos += 5 + l
    return words


def docs(path, max_docs):
    n = 0
    with gzip.open(path, "rb") as fh:
        try:
            for line in fh:
                try:
                    yield json.loads(line)["text"]
                except Exception:
                    continue
                n += 1
                if max_docs and n >= max_docs:
                    return
        except (EOFError, zlib.error, OSError):
            return                                                  # a partial download ends here


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--klx", required=True)
    ap.add_argument("--jsonl-gz", action="append", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--min-count", type=int, default=8)
    ap.add_argument("--top-k", type=int, default=40)
    ap.add_argument("--max-docs", type=int, default=0)
    ap.add_argument("--chunk-docs", type=int, default=20000)
    a = ap.parse_args()

    words = read_vocab(a.klx)
    idx = {w: i for i, w in enumerate(words)}
    V = len(words)
    keys = np.zeros(0, dtype=np.int64); cnt = np.zeros(0, dtype=np.int64)
    total_words = total_docs = 0

    def flush(buf):
        nonlocal keys, cnt
        if not buf:
            return
        ids = np.fromiter(buf, dtype=np.int64, count=len(buf))
        x = ids[:-1]; y = ids[1:]
        m = (x >= 0) & (y >= 0)
        k = x[m] * V + y[m]
        if k.size == 0:
            return
        u, c = np.unique(k, return_counts=True)
        allk = np.concatenate((keys, u)); allc = np.concatenate((cnt, c.astype(np.int64)))
        order = np.argsort(allk, kind="stable"); allk = allk[order]; allc = allc[order]
        first = np.concatenate(([True], allk[1:] != allk[:-1]))
        starts = np.nonzero(first)[0]
        keys = allk[starts]; cnt = np.add.reduceat(allc, starts)

    for path in a.jsonl_gz:
        buf = []
        nd = 0
        for text in docs(path, a.max_docs):
            nd += 1; total_docs += 1
            for t in TOKEN.findall(text.lower().replace("’", "'").replace("‘", "'")):
                i = idx.get(t, -1)
                buf.append(i)
                if i >= 0:
                    total_words += 1
            buf.append(-1)
            if nd % a.chunk_docs == 0:
                flush(buf); buf = []
                print(f"  {path}: {nd} docs, {total_words} listed words, {keys.size} distinct pairs", file=sys.stderr)
        flush(buf)
        print(f"{path}: {nd} docs", file=sys.stderr)

    keep = cnt >= a.min_count
    keys = keys[keep]; cnt = cnt[keep]
    first = keys // V; second = keys % V
    # at most top-k second words per first word (the most frequent)
    order = np.lexsort((-cnt, first))
    first = first[order]; second = second[order]; cnt = cnt[order]
    rank = np.zeros(first.size, dtype=np.int64)
    if first.size:
        starts = np.concatenate(([0], np.nonzero(first[1:] != first[:-1])[0] + 1))
        group_start = np.repeat(starts, np.diff(np.concatenate((starts, [first.size]))))
        rank = np.arange(first.size) - group_start
    sel = rank < a.top_k
    with open(a.out, "w", encoding="utf-8") as fh:
        for f, s, c in zip(first[sel], second[sel], cnt[sel]):
            fh.write(f"{words[f]} {words[s]} {int(c)}\n")
    print(f"docs {total_docs}, listed words {total_words}, pairs written {int(sel.sum())} (min count {a.min_count}, top {a.top_k} per word)", file=sys.stderr)


if __name__ == "__main__":
    main()
