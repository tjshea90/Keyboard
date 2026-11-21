# R30 - "adopt gboard's trained shape model if possible, or build one from the internet" (owner 2026-11-21)

Nothing here ran on a tablet or phone. Facts below come from web pages read by a helper (2026-11-21); items marked UNVERIFIED must be checked against the licence file itself before a file is committed.

## 1. Gboard's own model: not adoptable
Gboard (and Google's glide-typing library) is closed source and proprietary; HeliBoard and OpenBoard themselves say their glide typing only works with Google's closed library, which cannot be bundled. Its trained models exist only inside the Gboard app,
under Google's terms, and this repo is PUBLIC. So: no. What IS reusable is the published idea (R20 section 3, `R29`: a network trained on real swipes, a language model in the search) and, since 2024-26, OPEN data and OPEN models of the same kind.

## 2. What exists on the internet (licences as reported; check the file)
| what | licence | size | notes |
| :- | :- | :- | :- |
| FUTO swipe corpus `huggingface.co/datasets/futo-org/swipe.futo.org` (the one RealSwipeEval already reads a slice of) | MIT | 1 043 789 swipes, 12 278 sessions, ~2 GB | real swipes: points {t,x,y} 0..1, canvas size, target word |
| FUTO paper "FUTO Swipe: Layout-Agnostic Neural Swipe Decoding" (arXiv 2606.25247, FUTO, 24 Jun 2026) | paper; says "we release the trained models" (weights URL and weights licence NOT STATED in what was read) | encoder 635K params (2.6 MB fp32), decoder 304K | top-1 92.94 / top-3 97.46 (QWERTY, 162 185-word list, beam 100, no context LM) vs SHARK2 80.05 / 90.47; real-user KASROZ 97.68 / 99.47 |
| `xxparthparekhxx/keyboard` + HF `compose-keyboard-swipe-encoder` (`swipe_encoder.bin`) | MIT (README); next-word table ODC-BY | 634K params, 2.5 MB fp32 (BatchNorm folded) | the FUTO encoder design: 64 resampled points x 8 channels (Savitzky-Golay position, velocity, acceleration, speed, curvature), TCN of 5 ConvNeXt-v2 blocks (dilations 1,2,3,5,8, width 128, depthwise 7), spectral head = 64 cosine coefficients per step, key logits = coefficients x cosine basis of the key centres => the LAYOUT IS A RUNTIME INPUT (our tablet keys work without retraining); CTC + trie beam search. 47 552 FUTO test swipes, 150 289-word list: 89.07 / 93.98 / 95.02 (top-1/3/10) |
| HF `youcoded/swipe-typing` (`swipe-model.bin`) | MIT | 529K params, 2.1 MB | 2 conv + 2-layer BiGRU(128) + pointer key scorer, CTC; 2 000 real FUTO test swipes: first 90.40, top-3 94.95 |
| HF `giofolf/omniglide` | MIT | ~2 MB | architecture not stated |
| `proshian/neural-swipe-typing` | NO licence stated | - | transformer, weights not linked |
| CleverKeys | GPL-3.0 | 2.9 MB | copyleft: do not copy code or weights |
| "How We Swipe" (CHI/MobileHCI 2021) | CC BY 4.0 (paper); data on osf.io/sj67f, page needs JavaScript, UNVERIFIED | thousands of volunteers | second real-swipe source |
For comparison, the SHIPPED Swoop decoder (R29, phone keys, 1 338 FUTO swipes, our word list): first / top-3 87.9 / 94.7 (everyday words 91.0 / 96.8). The open CTC encoders alone reach 89-90 / 94-95 on their test sets: a different test set and word list, so NOT comparable until both are run on the SAME swipes.

## 3. The plan (measure first, ship only if it wins)
1. Get ONE open encoder's weights (first choice: `swipe_encoder.bin`, MIT, same design as FUTO's, layout-agnostic) and its exact training / inference code semantics (feature extraction, normalisation, cosine basis, BatchNorm folding): read `ml/` of the repo; re-implement the forward pass in plain Kotlin in `:core` (conv1d + depthwise + GRN + GELU, ~600K weights, int8 optional).
2. Golden-vector check: a Python (numpy) reference forward pass of the same weights vs the Kotlin one on 50 real swipes: outputs equal to 1e-4.
3. Use as a SHAPE SCORER next to the shipped decoder: per-key log-probs per frame -> CTC log-likelihood of each candidate word (candidates from the shipped decoder, widened if recall needs it) -> `cost = shippedCost + w * (-ctcLogP)`; `w` and a length term tuned on one slice of real swipes, reported on a DIFFERENT slice (different donors: RealSwipe byte offsets) so the test is honest. Keep our language model, personal counts, shape memory and learning on top (Gboard's advantage is exactly this combination).
4. Adopt only if first-choice / top-3 beat the shipped decoder on the held-out slice AND latency on the tablet class is bearable (target < 40 ms per glide on a Cortex-A53; the paper measured 7.6 ms on a Pixel 4 efficiency core in ExecuTorch, plain Kotlin will be slower: measure on the JVM, say plainly that the tablet was not measured).
5. Ship as alpha10 with the weights in `ime/src/main/assets` (or `core` resources), a licence NOTICE in About (MIT attribution of FUTO / the encoder's author), and the model switchable in Settings (default on only if step 4 passes).
6. Gboard's other half (neural NEXT-WORD model, CIFG LSTM 1.4 MB, federated): the same idea exists openly (`xxparthparekhxx` ships a next-word table, ODC-BY from C4); R31 candidate, not started.

## 4. Status
Step 0 done (this file, TASKS R30). Next: step 1 (fetch the weights file and the training code to `core/build/` ONLY: git-ignored scratch; commit weights only after the licence file is read and the golden test passes).
