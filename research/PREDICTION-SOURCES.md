# Predictive engines, data and papers that could make Swoop's glide and next-word prediction more accurate (N14)

Owner, 2026-11-17: "See what predictive engines are online to help teach the keyboard to make it as accurate as possible at predicting words in glide type".

**Evidence tags:** [W] a research helper read it on a web page, [K] general knowledge not re-checked. **Everything below was collected by a helper agent and relayed; the main session did not open these pages itself.** Treat licences as "to be re-checked before any file is copied into the repo". Section D lists what could not be verified.

## The one hard rule
Swoop has NO internet permission and must never send typed text anywhere (CLAUDE.md, plan 9.2). So "online" can only mean: data sets, word lists, n-gram counts, algorithms and open-source code that are downloaded ON A BUILD MACHINE (a dispatch-only GitHub Actions job, never the phone) and compiled into a small asset, or ideas re-implemented in our own Kotlin. A live cloud prediction service is out.

## A) What exists

| Source | What it is | Licence | Size | Helps with | Fit | URL |
|---|---|---|---|---|---|---|
| FlorisBoard `StatisticalGlideTypingClassifier` | Kotlin SHARK2-style decoder: prune by start/end key and path length, resample to 200 points, shape + location scored as Gaussians, times a frequency prior [W] | Apache-2.0 [W] | small | design check of OUR decoder | high | github.com/florisboard/florisboard |
| AnySoftKeyboard `GestureTypingDetector` | corner-based matching, direction and start/end penalties, frequency bonus [W] | Apache-2.0 repo, file has no header [W] | small | little to borrow (weaker than resampling) | low | github.com/AnySoftKeyboard/AnySoftKeyboard |
| HeliBoard / OpenBoard / AOSP LatinIME | glide needs a closed Google library; no open one exists (README) [W] | HeliBoard GPL-3.0 [W] | n/a | dictionary format, user-history ideas | low | github.com/Helium314/HeliBoard |
| FUTO Keyboard / swipe-library / weights | neural swipe decoder (encoder + optional decoder + optional context LM that reranks and predicts next words) [W] | keyboard: Source First 1.1; library GPL-3.0+; weights: FUTO Model Weights License 1.0 ("powered by FUTO Swipe" notice) [W] | encoder 1.0 MB int8, decoder 1.25 MB, context LM 6.25 MB | design reference, ceiling numbers | medium | swipe.futo.tech, huggingface.co/futo-org/futo-swipe |
| **FUTO swipe corpus** | donated swipes of Wikipedia-style sentences on a web QWERTY, JSON/Parquet with normalised x, y, t, word, sentence [W] | **MIT** [W] | about 1.04M swipes (swipe-1), 12,278 sessions | **evaluating and tuning our decoder** | **high** | huggingface.co/datasets/futo-org/swipe.futo.org |
| How We Swipe (Leiva et al., MobileHCI 2021) | 109,338 swipes from 1,338 users, noisy [W] | not confirmed (OSF record shows a licence id; a snippet said MIT) | about 70 MB | second evaluation set | medium | osf.io/sj67f |
| CleverKeys | Kotlin keyboard, small CTC conv encoder (~2.9 MB ONNX) + trie beam search in Kotlin, SHARK2 fallback; claims 89.3 % top-1 on its own 2,400 swipes (self-reported) [W] | GPL-3.0 [W] | APK ~25 MB | proof the architecture works; design only | low | github.com/tribixbite/cleverkeys |
| SymSpell data (what we use today) | 82,765 words, ~243K bigrams; Google Books Ngram crossed with SCOWL per its README [W] | MIT; ngram data CC BY 3.0 [W] | tiny | baseline, book-style English | baseline | github.com/wolfgarbe/SymSpell |
| wordfreq data | unigrams blended from Twitter, Reddit, OpenSubtitles, SUBTLEX, Wikipedia, books; frozen 2021 [W] | code Apache; **data CC BY-SA 4.0** [W] | n/a | slang and casual unigram weights | medium-high | github.com/rspeer/wordfreq |
| hermitdave FrequencyWords | OpenSubtitles 2016/2018 word counts (spoken register), unigrams only [W] | code MIT; **content CC BY-SA 4.0** [W] | en_50k and larger | casual unigram weights | medium | github.com/hermitdave/FrequencyWords |
| OPUS OpenSubtitles | dialogue text, best conversational register; asks for a link back and a citation, no licence text found [W] | **unclear** | large | bigram / trigram COUNTS | medium (legal risk) | opus.nlpl.eu |
| Vertanen mobile-text LMs + data | 341K sentences mined from 300 mobile devices; 1-4-gram word LMs, 64K vocab, small and large pruned versions [W] | none stated [W] | mixture trained on 504M words | the best TEXTING-register LM found | high, if the author permits | digitalcommons.mtu.edu/mobiletext/ |
| NUS SMS corpus | real donated SMS [W] | unverified (a mirror asks for an e-mail) | unverified | evaluation, maybe training | medium | github.com/WING-NUS/nus-sms-corpus (404 for the helper) |
| SODA | everyday conversations that look LLM-generated, quality uneven [W] | CC BY 4.0 [W] | about 1.49M dialogues | chat-style bigrams / trigrams | medium | huggingface.co/datasets/allenai/soda |
| Tatoeba | short everyday sentences, weekly export [W] | CC BY 2.0 FR, some CC0 [W] | large | supplement | low-medium | tatoeba.org/en/downloads |
| Enron Mobile | BlackBerry e-mails, business-leaning [W] | none stated | n/a | evaluation | low-medium | keithv.com/software/enronmobile |
| Helium314 aosp-dictionaries | `.dict` files with bigrams: 160,715 words (2014 legacy), 279,960 (experimental US); source lists CC BY 4.0 (inferred Leipzig-based) [W] | CC BY 4.0 (source lists) | n/a | format and word-list build script ideas | low-medium | codeberg.org/Helium314/aosp-dictionaries |
| Google Books Ngram v3 | n-grams seen > 40 times [W] | CC BY 3.0 [W] | huge | fallback only, book register | low | storage.googleapis.com/books/ngrams/books/datasetsv3.html |
| Palin et al. typing dataset (37k typists) | keystrokes + sentences; no touch coordinates mentioned [W] | none stated | 4.7 GB | tap-error statistics | low | userinterfaces.aalto.fi/typing37k/ |

### Papers
- SHARK2: Kristensson and Zhai, UIST 2004 (pokristensson.com/pubs/KristenssonZhaiUIST2004.pdf) [W]; Zhai and Kristensson, CHI 2003 [W].
- FUTO Swipe (arXiv 2606.25247, 2026): best accuracy reference found. On its own test split with a 162K-word lexicon a SHARK2-style baseline reaches 80.05 / 90.47 / 94.54 % top-1 / top-3 / top-10 and its neural decoder 92.94 / 97.46 / 98.60; the **frequency prior carries most of the gain** over a raw CTC model [W].
- Ouyang et al., Gboard FST decoder, arXiv 1704.03987 (2017) [W]. Hard et al., arXiv 1811.03604 (2018): a 670-unit CIFG LSTM, 10K vocab, 1.4M parameters / 1.4 MB shipped, beat the Katz 5-gram baseline (~1.25M n-grams) in top-1 recall (16.4 % vs 13.0 % on server logs; the live gain was small: 5.82 % vs 5.24 %) [W]. Zhang et al., "Neural Search Space in Gboard Decoder", arXiv 2410.15575: only 0.26-1.19 % fewer modified words [W].
- Vertanen and Kristensson, NLE 2021 [W]; Leiva et al., "How We Swipe", MobileHCI 2021 [W]; Adhikary and Vertanen on language-model personalisation (in simulation a unigram cache cut typing WER 5.7 % -> 4.6 %; from a search snippet) [W]; Heafield, KenLM, WMT 2011 (code LGPL) [W]; Chu et al., WordGesture-GAN, CHI 2023 (synthetic gestures; no code found) [W]; Grammarly engineering blog on swipe decoding [W].
- [K] only, not re-checked: Pauls and Klein 2011; Stolcke entropy pruning; Chen and Goodman; Brants "stupid backoff".

## B) Ranked plan (what would pay off most, in order)
1. **An offline test-and-tuning harness for the glide decoder** (effort M, licence risk low). A dispatch-only GitHub Actions job downloads the FUTO swipes (MIT) and How We Swipe on the runner, keeps them OUT of the repo, replays them through Swoop's `GlideDecoder` and prints top-1/3/10. Then tune the decoder's weights (shape, location, length, frequency). Expect real gains from tuning alone. Caveats: the prompts are Wikipedia sentences and the web keyboard's key geometry is not documented (the coordinates must be mapped onto our key boxes).
2. **Move the language model toward casual, chat-style English** (effort M, risk medium). Unigrams from wordfreq / hermitdave (CC BY-SA: the compiled asset must then carry that licence and a notice); bigrams and trigrams from SODA, Tatoeba and, as counts only, OpenSubtitles; vet the vocabulary against SCOWL / the AOSP list; entropy-prune; quantise log-probabilities to 8 bits. Target about 1-1.5M n-grams in 5-8 MB. Benefit: next-word prediction and glide ties such as "ur", "gonna", "lol". The Vertanen texting LMs are the best match for typed text but their terms are unstated: asking the author is the owner's call.
3. **Compare our decoder with the FlorisBoard recipe** (effort S, no risk): 200-point resampling, separate shape and location channels, loop variants for double letters, start/end-key pruning plus a length gate, a previous-word bigram rerank of the top N.
4. **Tune personal learning** (effort S): interpolate a unigram/bigram cache with the static model, set its decay, test on simulated noisy typing.
5. **Later, only if 1-4 are not enough: a small CTC conv encoder in pure Kotlin** (effort L): about 0.6M parameters, int8, about 1 MB, trained in CI on the MIT FUTO swipes, run on a trie beam search like CleverKeys, no native runtime. Expected ceiling roughly 10+ points top-1 over a template baseline, but only after the harness exists.

## C) Do not use
- FUTO Keyboard code (Source First), FUTO swipe-library (GPL-3.0+), HeliBoard / OpenBoard / CleverKeys code (GPL): design inspiration only.
- ExecuTorch, ONNX Runtime, llama.cpp: native code, large, 32-bit ARM support unchecked (and our rules say no native code without proof).
- SUBTLEX-US (a secondary source lists CC BY-NC-ND), DailyDialog (CC BY-NC-SA), EmpatheticDialogues (CC BY-NC [K]), Blended Skill Talk (conflicting licence statements).
- Raw Twitter / Reddit dumps (wordfreq's own notes: Twitter data could never be redistributed; Reddit sells its archives) [W].
- Norvig's count files (no data licence stated; derived from an LDC-distributed corpus) [W]; raw Google Books n-grams (huge, book register); Leipzig corpora until the licence conflict (CC BY vs CC BY-NC) is resolved.
- Any live prediction service (needs internet and would send typed text).

## D) Could not be verified (do not rely on these)
- Whether the FUTO web canvas coordinates map directly onto key positions; the How We Swipe licence; the NUS SMS licence, size and current location; Vertanen's data terms; the OPUS OpenSubtitles licence text and size; that open AOSP has no open gesture decoder (inferred from HeliBoard's README); Tatoeba's English sentence count; FUTO's "26 % fewer errors than Gboard" claim (a forum comment); CleverKeys' 89.3 % (self-reported); FlorisBoard's constants (22.08, 0.5109; from an AI summary, not re-read); Gboard's current model sizes (only 2018 figures); any decode time on a Cortex-A53; the aosp-dictionaries English source and licence; that the SymSpell list really is Google Books ∩ SCOWL (README claim only).

## E) What this means for Swoop today (main session's reading, not the helper's)
- Our decoder is already a SHARK2-family design (shape cost + language prior, a learned finger bias, remembered personal glide shapes); the published numbers say the **language prior and the vocabulary matter as much as the path model**, which is where the personal learning (N6) and a chat-register word list (item 2) act.
- The biggest unknown is how accurate our decoder is on REAL human swipes. We only have simulated paths (`eval/` GestureSim). Item 1 (real swipes, MIT) is the first thing to do before changing anything, because without a number we cannot tell a tuning from a regression.
- Licence care: CC BY-SA data would make the compiled asset CC BY-SA. That is acceptable for a public personal repo but must be stated in `ime/src/main/assets/glide/LICENSE.txt` and `docs/DICTIONARY.md`.
