#!/usr/bin/env python3
"""Writes the utterances of the SODA dialogues (AllenAI, CC BY 4.0: everyday conversations) to a text file, one utterance per line, for tools/glide/count_pairs.py.

Runs only on a CI runner with the network (dispatch-only workflow .github/workflows/count-pairs.yml); the text stays on the runner and is never committed:

  python3 tools/glide/fetch_soda.py --out soda.txt --dialogues 250000      (plain python3: the pip packages live outside the isolated mode's path)

Needs `pip install huggingface_hub pyarrow`. The dataset repository is listed first (file names change), the parquet files that hold a `dialogue` column are read in order until
--dialogues dialogues were written. Anything unexpected is printed and ends the script with a non-zero status (the workflow step is best-effort).
"""
import argparse
import sys


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--dialogues", type=int, default=250000)
    ap.add_argument("--repo", default="allenai/soda")
    a = ap.parse_args()
    from huggingface_hub import hf_hub_download, list_repo_files
    import pyarrow.parquet as pq

    files = None
    for rev in (None, "refs/convert/parquet"):
        try:
            allf = list_repo_files(a.repo, repo_type="dataset", revision=rev)
        except Exception as e:  # noqa: BLE001
            print("cannot list %s@%s: %s" % (a.repo, rev, e), file=sys.stderr)
            continue
        pqs = sorted(f for f in allf if f.endswith(".parquet"))
        train = [f for f in pqs if "train" in f.lower()] or pqs
        print("revision %s: %d files, %d parquet, %d for training: %s" % (rev, len(allf), len(pqs), len(train), train[:3]), file=sys.stderr)
        if train:
            files = (rev, train)
            break
    if not files:
        print("no parquet files found for %s" % a.repo, file=sys.stderr)
        return 1
    rev, train = files
    written = 0
    with open(a.out, "w", encoding="utf-8") as out:
        for name in train:
            path = hf_hub_download(a.repo, name, repo_type="dataset", revision=rev)
            pf = pq.ParquetFile(path)
            if "dialogue" not in pf.schema_arrow.names:
                print("%s has no dialogue column (%s)" % (name, pf.schema_arrow.names), file=sys.stderr)
                continue
            for batch in pf.iter_batches(columns=["dialogue"], batch_size=2000):
                for dlg in batch.column(0).to_pylist():
                    for utt in dlg or []:
                        utt = " ".join(str(utt).split())
                        if utt:
                            out.write(utt + "\n")
                    written += 1
                    if written >= a.dialogues:
                        print("wrote %d dialogues" % written, file=sys.stderr)
                        return 0
            print("%s done, %d dialogues so far" % (name, written), file=sys.stderr)
    print("wrote %d dialogues (all the files)" % written, file=sys.stderr)
    return 0 if written else 1


if __name__ == "__main__":
    sys.exit(main())
