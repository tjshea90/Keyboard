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

## 5. The open encoder, exactly (read from the upstream source, MIT, Copyright (c) 2026 Parth Parekh; `ml/swipe/model.py`, `preprocess.py`, `tools/export_weights.py`; file `swipe_encoder.bin` 2 529 224 bytes sha256 d569395f022496a7dd4ae6cb4199306a2ed2f0a0957f9423fd8e249d53b58e9d, fetched by `R30FetchTest` into core/build/r30, git-ignored)
* **File:** `SWEN`, i32 version=1, i32 count=69, then per tensor `[i32 nameLen][name][i32 ndim][i32 dims...][f32 data...]`, little-endian. Tensors: `savgol`(3x7), `stem.w`(128x8x5) `stem.b`, per block i=0..4 `b{i}.dw.w`(128x1x7, BatchNorm already folded) `.dw.b`, `.pw1.w`(512x128) `.pw1.b`, `.grn.g`/`.grn.b`(256), `.pw2.w`(128x256) `.pw2.b`, `.se1.w`(32x128) `.se1.b`, `.se2.w`(128x32) `.se2.b`; `adapter.w`(256x128x2, BN folded) `adapter.b`, `coeff.w`(64x256) `coeff.b`, `gate.w`(1x256) `gate.b`.
* **Input:** the swipe's (x, y) in 0..1 of the keyboard canvas, resampled to **64 points evenly spaced IN TIME** (a 60 Hz linear pass first, then 64 linear samples over the whole duration; time spacing keeps the dwell signal). Features (8 x 64): Savitzky-Golay (window 7, order 2, replicate padding) position x,y / velocity x,y / acceleration x,y (the kernel row 2 is already x2: use the export's row as is), speed = sqrt(vx^2+vy^2+1e-8), curvature = heading change atan2(vy,vx) wrapped to (-pi,pi], clamped to +-2 (first step 0). Conv is correlation with the stored (already flipped) kernels.
* **Net:** stem conv1d 8->128 k5 pad2; 5 blocks (dilations 1,2,3,5,8): `h = dw(x)` (depthwise k7, dilation d, pad 3d, BN folded) -> `pw1` (128->512) -> GLU (first half * sigmoid(second half), 256) -> GRN (over time: gx = L2 norm over T per channel, nx = gx / (mean over channels of gx + 1e-6), out = gamma*(x*nx) + beta + x) -> `pw2` (256->128) -> squeeze-excite (mean over time, fc1 128->32 ReLU, fc2 32->128 sigmoid, scale) -> residual add. Adapter conv1d 128->256 k2 stride 2 (BN folded) -> T=32. `coeff` 256->64 and `gate` 256->1 per frame.
* **Layout-agnostic head:** key centres normalised to 0..1 of the SAME canvas; Phi[k][u*8+v] = cos(pi*u*x_k) * cos(pi*v*y_k); z[t][k] = sum_c coeff[t][c] * Phi[k][c]; log P(key k at t) = log_softmax_k(z) + log sigmoid(gate); log P(blank) = log sigmoid(-gate) (blank = class K, last). CTC over words (letters a-z = key ids 0..25 for the 26 letter keys).
* **Cost:** about 35 M multiply-adds per swipe (per block ~6.4 M: pw1 4.2 M, pw2 2.1 M) -> plain Kotlin on a Cortex-A53 is probably 100-300 ms: MUST be measured/optimised (JVM here; tablet not measured), run off the UI thread, and the shipped decoder's answer is available at once while the net re-ranks.
* **Golden vectors:** the upstream repo checks in a torch dump `app/src/test/resources/swipe_reference.bin` (magic `SWRF`, i32 1, then 5 arrays each [i32 n][f32 data]: xy(2x64), keys(26x2), feats(8x64), logp(32x27), gate(32)) from `torch.manual_seed(0)`; use it as the parity test without needing torch (fetch with R30FetchTest into core/build/r30).
* **Upstream scoring (for reference):** trie-constrained CTC beam (width 100) + length term + log word frequency (`swipe_words.txt`, 150 289 words, score = round(log2(count)*10) 1..255). We will NOT use their beam; we rescore OUR decoder's candidates with the CTC log-likelihood and combine with our own prior.


## 6. RESULT of step 3 (2026-11-21, `ShapeNetEvalTest`, REAL swipes through the REAL decoder; ts reader bug fixed first)

Setup: shipped decoder (`GlideParams.forPitchDp`, with the word before as context) gives its top 40 words; the open network gives log P(word | path) (CTC forward sum over the word's letters); hybrid score = shipped score + w * log P. The data are real FUTO dev swipes, never committed (core/build only).
Slice A = first 8 MB of dev.jsonl (1338 swipes whose word is in our list); slice B = bytes 100 MB.. of dev.jsonl (1765 swipes, other donors). w is meant to be tuned on A and only read on B.
Input forms: A = the corpus' own 0..1 canvas; B = THIS keyboard's area (x / 292 dp, y / 161 dp, key centres likewise) = what the IME can do.

| | slice A top1 / top3 | slice B top1 / top3 |
| :- | :- | :- |
| oracle recall of the 40 candidates | 96.6 % | 97.6 % |
| shipped decoder | 87.9 / 94.7 % | 88.8 / 95.8 % |
| network alone (rank the 40 by log P), canvas form | 88.5 / 95.5 % | 89.3 / 96.1 % |
| network alone, keyboard-area form | 87.4 / 95.4 % | 88.6 / 95.7 % |
| HYBRID w = 0.5, keyboard-area form | 91.9 / 95.7 % | 92.3 / 96.9 % |
| HYBRID w = 1.0, keyboard-area form | 91.9 / 96.0 % | 92.4 / 96.9 % |
| HYBRID w = 0.5, canvas form | 92.5 / 95.7 % | 92.5 / 96.9 % |

So: about +4 points first-choice and +1 point top-3 on BOTH slices, with the weight chosen on A alone, which is five times the best single change of the earlier rounds (+0.7). The network alone is only about as good as the shipped decoder: the gain comes from adding two DIFFERENT views of the same swipe (our geometric cost + language prior, its learned shape). The w curve is flat from 0.35 to 2 (92.1-92.5 on A and B), so the choice is not fragile.
Cost on the sandbox JVM (x86): about 15-22 ms per forward pass warm (one thread), 104 ms the very first (cold JIT); the Cortex-A53 tablet will be several times slower (to be measured on the device; not run here).
Not yet checked: the held-out TEST split, the short-word confusions the owner reported, a confidence gate to skip the network when the shipped answer is clearly right, and a tablet-shaped canvas.

### 6b. Second run: short words, the confidence gate and the FUTO TEST split (same day)

w = 0.7 (picked on slice A, inside the flat 0.35-2 region); keyboard-area form (what the IME can hand over) unless said.

| | shipped top1 / top3 | hybrid top1 / top3 | short words (<= 4 letters) hit | "short meant, 2+ longer chosen" (the -> there, and -> access) |
| :- | :- | :- | :- | :- |
| slice A (dev, n 1338) | 87.9 / 94.7 | 91.7 / 95.9 | 90.2 -> 92.7 % | 11 -> 4 |
| slice B (dev, other donors, n 1765) | 88.8 / 95.8 | 92.4 / 96.9 | 92.3 -> 94.5 % | 4 -> 0 |
| slice C (TEST split, n 2281) | 90.6 / 96.1 | 94.3 / 97.6 | 94.6 -> 96.2 % | 3 -> 1 |

So +3.8 / +3.6 / +3.7 points first choice on three disjoint real-swipe sets, including the FUTO test split the network was never tuned on, and the owner's kind of error (a short word read as a longer one) falls by about two thirds.
Confidence gate (run the network only when the shipped first choice is NOT clearly ahead, margin = best score minus second best): the lowest-margin 25 % of swipes already give 91.4 / 91.9 / 94.2 against 91.8 / 92.4 / 94.3 for all swipes; 50 % gives 91.7 / 92.3 / 94.4. So the network needs to run on about a third of the glides to keep almost the whole gain: that is the latency answer for the tablet.
Decision: ADOPT as a re-ranker of the shipped decoder's candidates (R30e).
