# R27b - "learn context and grammar for better word prediction" (owner 2026-11-19)

## What Swoop already learns (so the question is what is MISSING)
* Context from the shipped list: word pairs (`bigramMix` 0.4 of the prior) from conversational sentences; the word before and the two before (`previousTwo`).
* From the owner: word counts, pairs, three-word phrases (3 267 on his tablet), "meant B when guessed A" corrections (492), sentence starters, capital forms, remembered glide shapes; recency of use. All on the device, nothing learned in password or no-personalisation fields.
* Next-word box after a glide (middle box), and since R26a the word before is re-judged by the word after it (research/R24-glide-next.md section 7.1: +0.7 to +0.8 points first choice, measured).
That is a word-level n-gram model with personal adaptation: the same family as the n-gram baseline of Gboard's next-word model and of SwiftKey before its neural model.

## What "grammar" would add, ranked (sources are from memory and NOT re-read this session: [S])
1. Word-class (POS-like) back-off for pairs the list does not know: P(word | prev) ~ P(class(word) | class(prev)) x P(word | class) [S: Brown et al. 1992, class-based n-grams for sparse data]. Cheap (a byte per word and a 48 x 48 table). TESTED below: negative.
2. A neural language model (Gboard: CIFG, 1.4 M parameters, 1.4 MB, federated training [S: Hard et al. 2018]): the real answer for grammar, but needs training data and compute that a 2 GB tablet with no network and kotlin-stdlib only cannot provide: not built.
3. Personal class transitions (learn the OWNER's grammar: how his sentences move between classes, 48 x 48 counts): only meaningful after 1 works.
4. Hand rules (a / an, to + verb, "I" + verb): small, but each rule needs its own measurement; not started.

## The experiment (real sentences, real swipes)
`ClassModelBuilder` (test side) clusters the 8 000 commonest words of the shipped list into 48 classes by which words tend to precede them (k-means on sqrt P(word | context word), 250 context words, built in about 1 s), takes class transitions and class masses from the whole pair list, and `GlideDecoder.classModel` uses
`alpha x trans[class(prev)][class(word)] x share(word) / mass[class(word)]` in place of "0" for a pair the list does not know. Scored on the real swipes that have a word before them (slice A: 5 238 swipes, 4 300 everyday words), phone settings of R24b:

| alpha | all words first / top-3 | everyday words first / top-3 |
| :- | :- | :- |
| 0 (off) | 88.8 / 95.1 | 92.1 / 97.1 |
| 0.5 | 88.7 / 95.1 | 92.0 / 97.0 |
| 1 | 88.6 / 95.1 | 91.9 / 97.0 |
| 2 | 88.4 / 95.0 | 91.7 / 97.0 |
| 4 | 88.0 / 95.0 | 91.4 / 97.0 |

**It does not help and gets worse as it is trusted more.** Likely reasons (not proven): (1) the classes and transitions come from the conversational pair list while the corpus is encyclopaedic text, so the grammar learned does not match the text it is tested on (the owner's own writing is conversational, but there is no
real swipe data of his to test that); (2) giving unseen pairs probability blurs the signal that a SEEN pair is likelier than an unseen one. Not shipped. The model, its builder and the decoder hook stay in the code, off (no asset ships, `classModel` is null), so that a better variant can be measured the same way:
back-off weighted Katz-style instead of added, classes learned from the owner's text, or classes from a matching corpus.

## Verdict
Context learning is already strong (pairs, three-word phrases, corrections, and now the next word re-judging the word before). A grammar layer needs data that is not available here to be measured, and the cheapest version was measured and is negative. Recorded; nothing for the owner to switch on.
