#!/usr/bin/env python3
"""Counts which word follows which in everyday English text and writes the pair file that `gradlew :tools:glideLexicon --args="--base en.klx --merge-pairs ..."` mixes into the word list
(docs/DICTIONARY.md, "Casual pairs").

Run it with `python3 -I` on TEXT that was downloaded outside the repository (one sentence or utterance per line, optionally .bz2 / .gz; a Tatoeba export has tab-separated columns id, language, sentence: --tsv-column 3):

  python3 -I tools/glide/count_pairs.py --vocab vocab.txt --text eng_sentences.tsv.bz2 --tsv-column 3 --out pairs.txt --starters starters.txt

* A pair is two neighbouring words INSIDE one stretch of plain words: a full stop, comma, bracket, quote, dash or a number ends the stretch (the keyboard does the same: after "hello," it
  knows no word before, see InputController.twoBefore).
* Only words of the vocabulary count (vocab.txt = `--dump-vocab` of the compile tool); apostrophes are kept ("don't"), curly ones are made straight, everything is lowercase.
* Sentences that mention the names the Tatoeba corpus is full of (Tom, Mary ...) are skipped: they would make "tom" follow everything.
* Output "first second count", only pairs seen at least --min-count times, at most --top-k second words per first word (the most frequent), and a stats line on stderr.
* --starters: the words that begin a stretch, most frequent first ("word count"), for the suggestions at the start of a sentence.
Standard library only; the text is only read, never executed.
"""
import argparse
import bz2
import collections
import gzip
import re
import sys

SPLIT = re.compile(r"[^a-z' ]+")
NAMES = {"tom", "mary", "tom's", "mary's", "john", "john's", "alice", "boston", "bob", "tom'll", "ken", "ann"}


def open_text(path):
    if path.endswith(".bz2"):
        return bz2.open(path, "rt", encoding="utf-8", errors="replace")
    if path.endswith(".gz"):
        return gzip.open(path, "rt", encoding="utf-8", errors="replace")
    return open(path, "r", encoding="utf-8", errors="replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--vocab", required=True, help="the words of the list, one per line")
    ap.add_argument("--text", action="append", required=True, help="a text file (repeat for several); one sentence per line")
    ap.add_argument("--tsv-column", type=int, default=0, help="1-based column of the sentence in a tab-separated file (0 = the whole line)")
    ap.add_argument("--out", required=True)
    ap.add_argument("--starters", default=None)
    ap.add_argument("--min-count", type=int, default=3)
    ap.add_argument("--top-k", type=int, default=60)
    ap.add_argument("--max-lines", type=int, default=0, help="stop after this many lines of each file (0 = all)")
    a = ap.parse_args()

    vocab = set()
    with open(a.vocab, encoding="utf-8") as fh:
        for line in fh:
            w = line.strip()
            if w:
                vocab.add(w)
    pairs = collections.Counter()
    starters = collections.Counter()
    sentences = tokens = skipped = 0
    for path in a.text:
        n = 0
        with open_text(path) as fh:
            for line in fh:
                n += 1
                if a.max_lines and n > a.max_lines:
                    break
                if a.tsv_column:
                    cols = line.rstrip("\n").split("\t")
                    if len(cols) < a.tsv_column:
                        continue
                    line = cols[a.tsv_column - 1]
                line = line.lower().replace("’", "'").replace("‘", "'").replace("`", "'")
                if any(nm in line for nm in ("tom", "mary", "john ", "boston")):
                    # cheap pre-check, then the exact one on the words
                    if NAMES.intersection(re.findall(r"[a-z']+", line)):
                        skipped += 1
                        continue
                sentences += 1
                for seg in SPLIT.split(line):
                    toks = [t.strip("'") for t in seg.split()]
                    toks = [t for t in toks if t]
                    if not toks:
                        continue
                    if toks[0] in vocab:
                        starters[toks[0]] += 1
                    prev = None
                    for t in toks:
                        tokens += 1
                        if t in vocab:
                            if prev is not None:
                                pairs[prev + " " + t] += 1
                            prev = t
                        else:
                            prev = None
    by_first = collections.defaultdict(list)
    for key, c in pairs.items():
        if c >= a.min_count:
            first, second = key.split(" ")
            by_first[first].append((c, second))
    kept = 0
    with open(a.out, "w", encoding="utf-8") as out:
        for first in sorted(by_first):
            for c, second in sorted(by_first[first], key=lambda x: (-x[0], x[1]))[:a.top_k]:
                out.write("%s %s %d\n" % (first, second, c))
                kept += 1
    if a.starters:
        with open(a.starters, "w", encoding="utf-8") as out:
            for w, c in starters.most_common(60):
                out.write("%s %d\n" % (w, c))
    print("sentences %d (skipped %d with a name), tokens %d, distinct pairs %d, kept %d (min-count %d, top-k %d), first words %d" %
          (sentences, skipped, tokens, len(pairs), kept, a.min_count, a.top_k, len(by_first)), file=sys.stderr)


if __name__ == "__main__":
    main()
