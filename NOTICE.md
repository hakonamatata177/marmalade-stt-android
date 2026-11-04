# NOTICE

This file summarizes the licensing of **marmalade-stt-android** and the
third-party components it builds on. It is informational; the binding
texts are the per-component licenses referenced below.

## Source code and APK: MIT

The Marmalade STT source code in this repository is licensed under the
**MIT License** — see [`LICENSE`](LICENSE).

The distributed APK is MIT too. Unlike Marmalade TTS, it contains no GPL
or LGPL code: nothing in it needs espeak-ng. Keeping it that way is a hard
rule for this repository (see `docs/ARCHITECTURE.md`, D2 and D3). Every
third-party component in the APK is listed below under its own license.
All code in it is under permissive licenses (MIT, BSD, Apache-2.0) except
**Eigen**, which is **MPL-2.0** and is compiled into both native libraries.
MPL-2.0 is file-level copyleft: it covers Eigen's own files only, and their
source is available as described under "Eigen source (MPL-2.0)" below. The
brand fonts are OFL-1.1.

## In-app licenses screen

The running app shows the same list under **Settings → About → Open-source
licenses**, with the full license text for each component bundled in the
APK under `app/src/main/assets/licenses/`. That screen is driven by
`app/src/main/java/app/marmalade/stt/data/LicenseCatalog.kt`. **Keep this
file and the catalog in sync**; `LicenseCatalogTest` fails if a catalog
component or its license is missing here.

License texts that embed the licensor's copyright (MIT, BSD) ship per
component with the correct holder. Standalone bodies that embed no
copyright (Apache-2.0, MPL-2.0, OFL-1.1) are shared, with the attribution
shown per component. The LLVM C++ runtime's notice (Apache-2.0 WITH
LLVM-exception plus its legacy MIT/NCSA texts) and ONNX Runtime 1.28.0's
`ThirdPartyNotices.txt` ship verbatim as texts of their own.

## Per-component summary

| Component | Role | Where it ships | License |
|---|---|---|---|
| Marmalade STT | This app, including its Granite formatter model (`assets/granite/formatter.int8.onnx`) and `libmarmalade-cpu.so`, a small C helper that pins threads to CPU cores | APK (source; `libmarmalade-cpu.so`) | **MIT** |
| sherpa-onnx | Speech recognition and VAD runtime, v1.13.8 built from source with TTS off — Copyright (c) 2022-2026 Xiaomi Corporation and the other contributors named in its file headers | APK (`libsherpa-onnx-jni.so`; eight Kotlin API files vendored in source) | Apache-2.0 |
| kaldi-native-fbank | Audio feature extraction, v1.22.3 — Copyright (c) 2022-2025 Xiaomi Corporation; Brno University of Technology; Kaldi authors | APK (compiled in) | Apache-2.0 |
| KISS FFT | FFT used by kaldi-native-fbank, commit febd4ca — Copyright (c) 2003-2010 Mark Borgerding | APK (compiled in) | BSD-3-Clause |
| kaldi-decoder | CTC decoding, v0.3.0 — Copyright (c) 2022-2023 Xiaomi Corporation; Kaldi authors (Microsoft Corporation, Johns Hopkins University, …) | APK (compiled in) | Apache-2.0 |
| kaldifst | Kaldi FST and I/O utilities, v1.8.0 — Copyright (c) 2020-2023 Xiaomi Corporation; Kaldi authors. Its LICENSE: "The copyright model is that authors (or their employers, if noted in individual files) own their individual contributions." | APK (compiled in) | Apache-2.0 |
| OpenFst | Finite-state transducers, csukuangfj fork at tag v1.8.5-2026-07-09 — Copyright 2005-2026 Google LLC. | APK (compiled in) | Apache-2.0 |
| simple-sentencepiece | Subword tokenizer, v0.7 — Copyright 2024 Wei Kang | APK (compiled in) | Apache-2.0 |
| darts-clone | Double-array trie inside simple-sentencepiece — Copyright (c) 2008-2014, Susumu Yata | APK (compiled in) | BSD-2-Clause |
| Eigen | Linear algebra, 5.0.1, header-only, unmodified — Eigen contributors (Benoit Jacob, Gael Guennebaud and others named in each file) | APK (compiled in) | **MPL-2.0** |
| JSON for Modern C++ (nlohmann/json) | JSON parsing, v3.12.0 — Copyright (c) 2013-2025 Niels Lohmann; includes Björn Hoehrmann's UTF-8 decoder (MIT) | APK (compiled in) | MIT |
| LLVM C++ runtime (libc++, libc++abi, libunwind) | C++ standard library from Android NDK r26d, statically linked (`c++_static`); also kaldifst's `basic-filebuf.h`, a modified copy of libc++'s `std::basic_filebuf` | APK (compiled in) | Apache-2.0 WITH LLVM-exception (legacy code: MIT / NCSA) |
| ONNX Runtime | Neural network inference, 1.28.0 — Copyright (c) Microsoft Corporation | APK (`libonnxruntime.so`, `libonnxruntime4j_jni.so` and the Java API, from Maven) | MIT |
| ONNX Runtime third-party notices | Libraries statically linked into ONNX Runtime (XNNPACK, protobuf, Abseil, FlatBuffers, Eigen, …) | APK (inside `libonnxruntime.so`) | Third-party notices: per entry, as published with ONNX Runtime 1.28.0 (MIT, BSD, Apache-2.0, **MPL-2.0** for Eigen, …) |
| Silero VAD | Voice activity detection model, v4.0 — Copyright (c) 2020-present Silero Team | APK (`assets/vad/silero_vad.onnx`) | MIT |
| Speed calibration clip | 3.6 s spoken English sentence ("Please attach the file…") that the app decodes once to measure the phone's speed (#24). Made for this project with the Kokoro-82M text-to-speech model (Apache-2.0); no recording of a person | APK (`assets/calibration/speech.wav`) | MIT (part of Marmalade STT) |
| Pipecat Smart Turn v3.2 | End-of-dictation model (int8 CPU ONNX), fine-tuned from OpenAI Whisper Tiny — Copyright (c) 2024–2025, Daily | APK (bundled) | BSD-2-Clause |
| Pipecat Whisper feature extraction | Whisper log-mel features that Smart Turn reads, ported to Kotlin from pipecat — Copyright (c) 2024–2026, Daily | APK (source, ported) | BSD-2-Clause |
| Hugging Face Transformers (Whisper feature extractor) | Origin of the feature code pipecat vendored — Copyright 2022 The HuggingFace Inc. team.; Copyright 2023 The HuggingFace Inc. team and the librosa & torchaudio authors. | APK (source, ported via pipecat) | Apache-2.0 |
| Tatoeba English sentences | Training text of the Granite formatter (the app's own model, in the APK) — by the Tatoeba contributors | Not shipped (training text) | CC-BY-2.0-FR |
| CC BY news text (Common Pile) | Training text of the Granite formatter — Global Voices, Factly, Milwaukee Neighborhood News Service and nine other publishers | Not shipped (training text) | CC-BY-4.0 |
| OpenAI Whisper | Speech recognition engine Whisper Large v3 Turbo (int8 ONNX export by k2-fsa); and Whisper Tiny, the encoder Smart Turn v3.2 was fine-tuned from — Copyright (c) 2022 OpenAI | APK (inside the bundled Smart Turn weights); download (Engines screen: large-v3-turbo; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | MIT |
| NB-Whisper (National Library of Norway) | Norwegian speech recognition engines NB-Whisper Tiny, Base and Small (int8 ONNX export made with sherpa-onnx's Whisper script), fine-tuned from OpenAI Whisper by the National Library of Norway (NbAiLab) | Download (Engines screen; from github.com/hakonamatata177/marmalade-stt-android, release modeller; this fork only), not in the APK | Apache-2.0 |
| Moondream Parakeet Ultra | Speech recognition engine (the default), 25 European languages: a post-trained Parakeet TDT 0.6B v3, int8 ONNX conversion for this app — created by Moondream (M87 Labs, Inc.) from NVIDIA's model | Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | CC-BY-4.0 |
| NVIDIA Parakeet TDT_CTC 110M | Speech recognition engine (English; shown in the app as Parakeet 110M), int8 ONNX export by k2-fsa — created by NVIDIA NeMo and Suno.ai | Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | CC-BY-4.0 |
| NVIDIA Parakeet TDT_CTC 0.6B ja | Speech recognition engine (Japanese; shown in the app as Parakeet Japanese), int8 ONNX export of the CTC branch by k2-fsa — created by NVIDIA; trained on the ReazonSpeech corpus | Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | CC-BY-4.0 |
| NVIDIA canary-180m-flash | Speech recognition engine (en/es/de/fr), 8-bit ONNX export by k2-fsa — created by NVIDIA | Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | CC-BY-4.0 |
| NVIDIA Nemotron 3.5 ASR Streaming 0.6B | Streaming speech recognition engine for Hindi, Arabic and Vietnamese (the 1120 ms export), int8 ONNX by k2-fsa — by NVIDIA | Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | OpenMDW-1.1 |
| Qwen3-ASR 0.6B | Speech recognition engine for 30 languages (recommended for Chinese and Korean), int8 ONNX export — Copyright 2026 Alibaba Cloud; Qwen3-0.6B tokenizer, Copyright 2024 Alibaba Cloud | Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | Apache-2.0 |
| IBM Granite Speech 5.0 470M TurboCTC | Developer speech recognition engine (English), int8 ONNX export by qwertz92 — created by IBM (IBM Granite Speech Team) | Download (Engines screen, developer engines; from github.com/maxwhipw/marmalade-stt-engines), not in the APK | Apache-2.0 |
| AndroidX / Compose / Kotlin / Hilt / DataStore | App framework, with Jetpack Navigation, Lifecycle, kotlinx.coroutines, kotlinx.serialization (JSON), Dagger, Material icons and their transitive dependencies (Okio, Guava listenablefuture, javax.inject / jakarta.inject, JetBrains annotations, JSpecify — Apache-2.0; jsr305 annotations — BSD) | APK (including two small AndroidX native libraries, `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so`) | Apache-2.0 |
| Marmalade UI (marmalade-ui-android) | The design library this app shares with Marmalade TTS (colours, surfaces, fonts, motion, cards), a git submodule under `third_party/marmalade-ui-android` built from source — Copyright (c) 2026 marmalade-tts contributors; Copyright (c) 2026 marmalade-ui-android contributors | APK | MIT |
| Manrope | Brand font (body / headings) — Copyright 2019 The Manrope Project Authors | APK | OFL-1.1 |
| Momo Trust Display | Brand font (wordmark) — Copyright 2024 The Momo Trust Project Authors | APK | OFL-1.1 |
| Fredoka | Brand font (wordmark fallback) — Copyright 2016 The Fredoka Project Authors | APK | OFL-1.1 |

Font copyright lines are taken from each bundled font file's name table.
Manrope and Momo Trust Display ship from the Marmalade UI library's
resources, Fredoka from the app's own.

## Native speech runtime

### sherpa-onnx, built from source

`lib/<abi>/libsherpa-onnx-jni.so` is built from source by
`app/src/main/cpp/CMakeLists.txt` (`docs/ARCHITECTURE.md`, D3). It is
**not** the prebuilt sherpa-onnx AAR, which statically links espeak-ng
(GPL-3.0). The build sets `SHERPA_ONNX_ENABLE_TTS=OFF`, so neither
espeak-ng nor piper-phonemize is compiled, and the Gradle native-license
guard fails the build if either string appears in a packaged `.so`.

Everything compiled into the library comes from git submodules under
`third_party/`, pinned to the versions sherpa-onnx v1.13.8 asks for:

| Submodule | Upstream | Pin |
|---|---|---|
| `sherpa-onnx` | https://github.com/k2-fsa/sherpa-onnx | v1.13.8 (`11afbd00`), plus the upstream patches below |
| `kaldi-native-fbank` | https://github.com/csukuangfj/kaldi-native-fbank | v1.22.3 (`b09e686f`), plus our patch below |
| `kissfft` | https://github.com/mborgerding/kissfft | commit `febd4cae` |
| `kaldi-decoder` | https://github.com/k2-fsa/kaldi-decoder | v0.3.0 (`62fda2d7`) |
| `kaldifst` | https://github.com/k2-fsa/kaldifst | v1.8.0 (`ab5bdd01`) |
| `openfst` | https://github.com/csukuangfj/openfst | v1.8.5-2026-07-09 (`f702a631`) |
| `eigen` | https://gitlab.com/libeigen/eigen | 5.0.1 (`bc3b3987`) |
| `simple-sentencepiece` | https://github.com/pkufool/simple-sentencepiece | v0.7 (`62dd423d`) |
| `json` | https://github.com/nlohmann/json | v3.12.0 (`55f93686`) |

**Modified files.** The build applies the patches in
`third_party/patches/sherpa-onnx/` to a copy of sherpa-onnx's sources, so
the shipped library differs from v1.13.8 (Apache-2.0). Upstream commits keep
their original header; our own patches say so in theirs:

- `0001-byte-level-bpe-itn-3965.patch`: upstream commit `2edc882f`
  (k2-fsa/sherpa-onnx #3965, by Paolo Antinori), which changes
  `offline-recognizer-whisper-impl.h`,
  `offline-recognizer-whisper-tpl-impl.h`,
  `offline-recognizer-cohere-transcribe-impl.h` and `text-utils-test.cc`
  so Whisper no longer drops characters split across byte-level BPE tokens.
- `0002-whisper-no-per-second-token-cap.patch`: our own change (#37) to
  `offline-whisper-greedy-search-decoder.cc`, which removes the decoder's
  cap of 6 tokens per second of audio so Whisper no longer cuts sentences
  short.
- `0003-cancellable-offline-decodes.patch`: our own change (#49) to
  `offline-stream.h`, `offline-stream.cc`, `offline-whisper-decoder.h`,
  `offline-whisper-greedy-search-decoder.h`,
  `offline-whisper-greedy-search-decoder.cc`,
  `offline-recognizer-whisper-impl.h`, `offline-recognizer-canary-impl.h`,
  `offline-recognizer-qwen3-asr-impl.cc` and `jni/offline-stream.cc`, which
  lets the app stop a decode in progress (`OfflineStream.cancel`, so the
  vendored `OfflineStream.kt` gains that one function).
- `0004-cancel-terminates-encoder-run.patch`: our own change (#54) to
  `offline-stream.h`, `offline-stream.cc`, `offline-whisper-model.h`,
  `offline-whisper-model.cc`, `offline-canary-model.h`,
  `offline-canary-model.cc`, `offline-qwen3-asr-model.h`,
  `offline-qwen3-asr-model.cc`, `offline-recognizer-whisper-impl.h`,
  `offline-recognizer-canary-impl.h`, `offline-recognizer-qwen3-asr-impl.h`
  and `offline-recognizer-qwen3-asr-impl.cc`, which makes that cancel also
  stop the encoder run in progress (ONNX Runtime's `RunOptions` terminate
  flag).
- `0005-whisper-token-budget.patch`: our own change (#54) to
  `offline-whisper-decoder.h`, `offline-whisper-greedy-search-decoder.h`,
  `offline-whisper-greedy-search-decoder.cc` and
  `offline-recognizer-whisper-impl.h`, which lets the app set how many
  tokens one Whisper decode may produce (the stream option
  `max_new_tokens`).
- `0006-parallel-session-create.patch`: our own change (#78) to
  `offline-whisper-model.cc`, `offline-canary-model.cc`,
  `offline-qwen3-asr-model.cc` and `offline-recognizer-qwen3-asr-impl.cc`,
  which creates those models' ONNX Runtime sessions, and Qwen3-ASR's
  tokenizer, on two threads at once so the engines load faster.

The build also applies `third_party/patches/kaldi-native-fbank/` to a copy
of kaldi-native-fbank's sources, so the shipped feature extraction differs
from v1.22.3 (Apache-2.0):

- `0001-whisper-feature-kissfft.patch`: our own change (#53) to
  `whisper-feature.h`, `whisper-feature.cc` and `rfft.cc`. Whisper's log-mel
  uses kaldi-native-fbank's kissfft-based `Rfft` instead of a recursive FFT
  with a naive DFT (code from whisper.cpp, MIT, which the patch removes), and
  `Rfft` builds its FFT plan once instead of on every frame. The changed
  lines are marked "Marmalade STT".

The component list above was checked against the arm64-v8a build (the
sources in `build.ninja`, their header closure, and the debug info of the
linked `.so`), not only against the submodule list. What that check found:

- **darts-clone ships.** simple-sentencepiece's `ssentencepiece.cc`
  includes its copy of darts-clone's `darts.h` (BSD-2-Clause, Susumu Yata).
- **simple-sentencepiece's ThreadPool does not ship.** Its `threadpool.h`
  (zlib license, Jakob Progsch and Václav Zeman) is overwritten with an
  empty stub by sherpa-onnx's `cmake/simple-sentencepiece.cmake` when
  building for Android. That happens on a copy in the build tree, so the
  submodule stays clean. No ThreadPool code is compiled.
- **libc++-derived code in kaldifst ships.** kaldifst's `basic-filebuf.h`
  (from Kaldi) is a modified copy of libc++'s `std::basic_filebuf`, under
  libc++'s legacy MIT / NCSA dual license. It is covered by the LLVM C++
  runtime entry.
- **The LLVM C++ runtime ships.** The library links the NDK's `c++_static`,
  so libc++, libc++abi and libunwind from Android NDK r26d are compiled in.
  They are Apache-2.0 WITH LLVM-exception. The exception waives the
  attribution conditions for runtime code embedded this way, so the entry is
  a courtesy, but it also carries the legacy MIT notice.
- **Eigen's non-MPL files do not ship.** Eigen is primarily MPL-2.0; its
  `COPYING.BSD`, `COPYING.APACHE` and `COPYING.MINPACK` cover third-party
  code in a few files. Three such files are in the header closure of
  `Eigen/Dense`: `Half.h` (BSD, Fabian Giesen), `BFloat16.h` (Apache-2.0,
  The TensorFlow Authors) and `MKL_support.h` (BSD-3-Clause, Intel). None of
  them contributes code to the linked library (the app does not use
  half-precision, bfloat16 or MKL). The Eigen code that ships is MPL-2.0.
- **nlohmann/json's third-party parts.** The shipped code includes Björn
  Hoehrmann's UTF-8 decoder (MIT, credited in the bundled text) and a
  small Abseil-derived helper (upstream marks the file MIT; its README
  credits Abseil under Apache-2.0). Hedley (CC0-1.0) is macros only. The
  bundled text reproduces the README's credits for all of these.

### Eigen source (MPL-2.0)

Eigen reaches the APK twice, in both cases unmodified and header-only. The
MPL-2.0 text is bundled in the app; Eigen's source is available here:

- **In `libsherpa-onnx-jni.so`:** Eigen **5.0.1**, commit
  `bc3b39870ecb690a623a3f49149a358b95c5781d`, from
  **https://gitlab.com/libeigen/eigen** (tag `5.0.1`). The same source is in
  this repository as the `third_party/eigen` submodule. The code that uses
  it (sherpa-onnx and kaldi-decoder) includes only `Eigen/Dense`.
- **In `libonnxruntime.so`:** ONNX Runtime 1.28.0 compiles in Eigen at
  commit `1d8b82b0740839c0de7f1242a3585e3390ff5f33` on Eigen's 3.4 branch,
  pinned in ONNX Runtime's `cmake/deps.txt` at tag `v1.28.0`. Source:
  **https://gitlab.com/libeigen/eigen**.

### ONNX Runtime

`libonnxruntime.so` comes from the Maven artifact
`com.microsoft.onnxruntime:onnxruntime-android:1.28.0`, unmodified.
`libsherpa-onnx-jni.so` links against that `libonnxruntime.so`
dynamically. Since #32 the artifact's Java API (`ai.onnxruntime`) and its JNI
binding (`libonnxruntime4j_jni.so`) ship too, unmodified: the Granite engine
runs on them. The app bundles ONNX Runtime's `LICENSE` (MIT) and
`ThirdPartyNotices.txt`, both from tag `v1.28.0` of
https://github.com/microsoft/onnxruntime.

### Silero VAD model

`app/src/main/assets/vad/silero_vad.onnx` is one of the three models bundled
in the APK; the others are the Granite formatter (next section) and Smart
Turn v3.2 (see "Smart Turn end-of-dictation").
Speech-recognition models are downloaded separately (D5); see
"Downloadable speech models" below.

> `silero_vad.onnx` (643,854 bytes, SHA-256
> 9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6) is the Silero VAD v4.0 voice activity
> detection model by the Silero Team (https://github.com/snakers4/silero-vad), distributed under the MIT
> License. This copy is the 16 kHz-only ONNX conversion published by the k2-fsa / sherpa-onnx project
> (https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx, introduced in
> https://github.com/k2-fsa/sherpa-onnx/pull/2372). That conversion re-exported the upstream v4.0 TorchScript
> model to ONNX, removed the 8 kHz branch and simplified the graph, without retraining. The model runs entirely
> on-device to detect the start and end of speech. It does not transmit, store or identify audio.

The bundled license text is Silero's `LICENSE` at tag v4.0
(https://github.com/snakers4/silero-vad/blob/v4.0/LICENSE). The model
ethics review is in
[`docs/model-reviews/silero-vad.md`](docs/model-reviews/silero-vad.md).

### Granite formatter model

`app/src/main/assets/granite/formatter.int8.onnx` restores case, punctuation
and apostrophes in the Granite engine's lowercase text (#33). It is this
project's own model, trained from scratch (no pretrained weights) with
`tools/asr-bench/granite/format_model.py`, and ships under the app's MIT
license.

> `formatter.int8.onnx` (7,528,683 bytes, SHA-256
> 3e311315fa95d393d417192d9a9e32c7ce2c56d0b1b2b3cfad51ab3b174e265e) is the Granite formatter, Marmalade STT's own model,
> trained from scratch on: sentences from Tatoeba (https://tatoeba.org) by the Tatoeba contributors, licensed
> CC BY 2.0 FR (https://creativecommons.org/licenses/by/2.0/fr/); sentences from Mozilla Common Voice
> (https://github.com/common-voice/common-voice), CC0 1.0; and news text published under CC BY 4.0
> (https://creativecommons.org/licenses/by/4.0/) by Global Voices, Factly, Milwaukee Neighborhood News
> Service, New Canadian Media, Alt News, 360info, Balkan Diskurs, Freedom of the Press Foundation, Mekong Eye,
> ZimFact, Minority Africa and Solutions Journalism, as collected in the Common Pile `news` dataset
> (https://huggingface.co/datasets/common-pile/news). The model can't reproduce text from these sources: it
> only labels the case and punctuation of words it is given. It runs entirely on-device.

Whether trained weights adapt their training text is unsettled, so the
sources are credited as CC BY asks. The CC BY 2.0 FR legal code (French, the
only official text) and CC BY 4.0 are bundled in the app
(`assets/licenses/`). The model ethics review is in
[`docs/model-reviews/granite-formatter.md`](docs/model-reviews/granite-formatter.md);
the measurements are in
[`docs/benchmarks/2026-09-granite-formatting.md`](docs/benchmarks/2026-09-granite-formatting.md).

## Smart Turn end-of-dictation

The default end-of-dictation mode (#43) asks a small model whether the
speaker has finished, instead of waiting out a fixed silence. The model is
bundled in the APK (#44), pinned by revision and hash; a unit test checks the
committed file against the hash below. The code that turns audio into the
model's input is ported into the app.

### Pipecat Smart Turn v3.2 model

> `smart-turn-v3.2-cpu.onnx` (8,679,182 bytes, SHA-256
> 2bb026316b14a660486a75b1733cd3fbab8c2fd0314dc9af7be49f8cca967e4f) is the int8 CPU build of Smart Turn v3.2, an
> end-of-turn detection model by Daily (https://github.com/pipecat-ai/smart-turn), distributed under the BSD
> 2-Clause License. It is bundled unmodified, as downloaded from
> https://huggingface.co/pipecat-ai/smart-turn-v3/resolve/f766f81d3cfdf7737ac64aad813d91bbfd56bf93/smart-turn-v3.2-cpu.onnx
> (commit `f766f81d3cfdf7737ac64aad813d91bbfd56bf93`, "Smart Turn v3.2", 2026-01-07). Daily fine-tuned it from
> OpenAI's Whisper Tiny encoder (MIT) with a small classifier head, on its published datasets
> (https://huggingface.co/datasets/pipecat-ai/smart-turn-data-v3.2-train): mostly synthetic speech, plus human
> recordings made with the speakers' consent. The model runs entirely on-device and outputs only whether the speaker
> sounds finished. It does not transmit, store or identify audio.

The model card's license is `bsd-2-clause` with no further terms; the model
repository has no LICENSE file, so the bundled text is the `LICENSE` of
https://github.com/pipecat-ai/smart-turn (unchanged since commit `24c72033`,
2025-03-06), "Copyright (c) 2024–2025, Daily". Whisper Tiny's notice is the
OpenAI Whisper MIT text already listed. The training datasets carry no
license of their own; none of their audio ships. The model ethics review is
in [`docs/model-reviews/smart-turn-v3.md`](docs/model-reviews/smart-turn-v3.md).

### Whisper log-mel feature code (ported from pipecat)

The app computes Smart Turn's input (80-bin Whisper log-mel features over an
8 s window) with a Kotlin port of pipecat's
`src/pipecat/audio/turn/smart_turn/_whisper_features.py`
(https://github.com/pipecat-ai/pipecat, the file's only commit `31aa4ac6`,
2026-05-21), made via marmalade-stt-cli's Python port. The algorithm is
kept bit-faithful so the model sees the features it was trained on.

- **pipecat:** BSD 2-Clause License, "Copyright (c) 2024–2026, Daily" (the
  file's header and pipecat's `LICENSE` at that commit, bundled as its own
  text).
- **Hugging Face Transformers:** pipecat's file says it is "Vendored from
  `transformers.WhisperFeatureExtractor`" and mirrors
  `transformers.models.whisper.feature_extraction_whisper` and
  `transformers.audio_utils` (https://github.com/huggingface/transformers,
  Apache-2.0). Their headers: "Copyright 2022 The HuggingFace Inc. team." and
  "Copyright 2023 The HuggingFace Inc. team and the librosa & torchaudio
  authors." Transformers has no NOTICE file to pass on. Changes: pipecat cut
  the extractor down to numpy-only code with fixed Smart Turn parameters;
  this app ported that to Kotlin.

The asr-bench harness (`tools/asr-bench/`) carries a Python copy of the same
code for measurement. It is not in the APK.

## Downloadable speech models

Speech-recognition models are not in the APK. The user downloads them from
the **Engines** screen, file by file, from release assets of our
https://github.com/maxwhipw/marmalade-stt-engines repository (one release per
engine, pinned by tag; a published release's files are never replaced), and
each file is checked against the SHA-256 below before it is kept
(`app/src/main/java/app/marmalade/stt/install/SttEngineCatalog.kt`). Parakeet
Ultra is our own conversion; every other engine's files are re-hosted there
unchanged, byte for byte, from the source named in its section below (#75).
That repository carries each model's attribution in its `NOTICE` and the
license texts in `LICENSES/`, and names its assets `<engine id>-<file>`. They
are listed here and in the in-app licenses screen because the app fetches
them and runs them.

The user engines (#45) are Parakeet Ultra, Parakeet TDT_CTC 110M,
Parakeet TDT_CTC 0.6B ja (#60), canary-180m-flash, Nemotron 3.5 ASR Streaming (1120 ms; Hindi, Arabic, Vietnamese),
Whisper large-v3-turbo and Qwen3-ASR 0.6B (Chinese and Korean in #50; all 30 of its languages since #88). Granite Speech is a **developer
engine** (#13): hidden unless Settings → Developer engines is on, offered
so it can be benchmarked. They are held to the same rules: pinned, hashed,
license-listed and ethics-reviewed before they are selectable.

### OpenAI Whisper large-v3-turbo (int8, k2-fsa export)

> The Whisper Large v3 Turbo engine is OpenAI's Whisper `large-v3-turbo` checkpoint
> (https://github.com/openai/whisper), distributed under the MIT License, in the int8 ONNX conversion
> published by the k2-fsa / sherpa-onnx project at
> https://huggingface.co/csukuangfj/sherpa-onnx-whisper-turbo (commit `2ca6ff69fc878651b770880507669577ac41c2ff`):
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `turbo-encoder.int8.onnx` | 674,716,297 | `b02dcdf54f348741e93fe732b67d933c8dcb6735655f710640143081db38878b` |
> | `turbo-decoder.int8.onnx` | 361,080,764 | `20accd02388482eb3a46bd615631adfdc85e1eb2c7db9ea3f02a40ffe6b81547` |
> | `turbo-tokens.txt` | 816,730 | `b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126` |
>
> k2-fsa's conversion loaded OpenAI's original checkpoint, exported it to ONNX with sherpa-onnx's
> `scripts/whisper/export-onnx.py` and quantized the weights to int8 with ONNX Runtime, without retraining.
> large-v3-turbo is OpenAI's large-v3 with its decoder reduced from 32 to 4 layers and fine-tuned. The model runs
> entirely on-device. It does not transmit or store audio.
>
> The app downloads these files, unchanged, from release `v6` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `whisper-turbo-<file>`).

The binding text is `LICENSE` of https://github.com/openai/whisper (commit `6e3be77e`, the file's only revision),
which covers the code and model weights, including large-v3-turbo (added in commit `25e5c364`). Copyright line:
"Copyright (c) 2022 OpenAI". The k2-fsa Hugging Face repository has no license file or license tag; the upstream
MIT terms govern. The model ethics review is in
[`docs/model-reviews/whisper-family.md`](docs/model-reviews/whisper-family.md).

### Moondream Parakeet Ultra (int8, our conversion)

> The Parakeet Ultra engine (the default) is Moondream's `parakeet-ultra`
> model (https://huggingface.co/moondream/parakeet-ultra, commit `73175eb7`), created by
> Moondream (M87 Labs, Inc.), a post-trained version of NVIDIA's `parakeet-tdt-0.6b-v3`
> (https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3). Both are licensed under the Creative
> Commons Attribution 4.0 International License (CC BY 4.0,
> https://creativecommons.org/licenses/by/4.0/legalcode). These files are a modified version:
> Moondream post-trained NVIDIA's model. For this app, Moondream's weights were loaded into
> NVIDIA's v3 model graph and tokenizer (`parakeet-tdt-0.6b-v3.nemo`, SHA-256
> 3cbdc85877e668ca7b82d0d56770eb1fac76691f55d6b97545e8d61ca588d10d; the mel filterbank and
> window are v3's), exported to ONNX (transducer only; the voice-activity head removed) and
> quantized to int8, without retraining (`tools/parakeet-ultra/convert.py`). They are published
> as release `v1` of https://github.com/maxwhipw/marmalade-stt-engines:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `encoder.int8.onnx` | 652,282,526 | `7ef3ca4ffb49d60d4faa757b2930c8cec9927e73c24f6155d5b23a00229a7ed6` |
> | `decoder.int8.onnx` | 11,845,274 | `1fab98fe6c12aded87d2da66272cc9e148d0a0044ce3850a12fe56302ec4a922` |
> | `joiner.int8.onnx` | 6,355,277 | `8ba94c6919c17a6bd27368fb89533628a72ffc81d4acedebf3cdcb2cae331dbb` |
> | `tokens.txt` | 93,939 | `d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d` |
>
> Moondream does not disclose its post-training data. The model runs entirely on-device. It
> does not transmit or store audio.

Neither Moondream nor NVIDIA supplies a copyright notice with the model; the credit is to them
as creators. The model is provided as-is, without warranties (Section 5 of the license). The
CC BY 4.0 legal code is bundled in the app (`assets/licenses/CC-BY-4.0.txt`). The hosting repo
carries the same attribution in its `NOTICE` and the legal code in `LICENSES/CC-BY-4.0.txt`;
its release assets are named `parakeet-ultra-<file>`. The conversion tooling (NeMo,
ONNX Runtime; Apache-2.0 / MIT) does not ship with the model. The model ethics review is in
[`docs/model-reviews/parakeet-ultra.md`](docs/model-reviews/parakeet-ultra.md); its base model's, NVIDIA's
Parakeet TDT 0.6B v3 (trained on NVIDIA's Granary corpus and NeMo ASR Set 3.0), in
[`docs/model-reviews/parakeet-tdt-0.6b-v3.md`](docs/model-reviews/parakeet-tdt-0.6b-v3.md).

### NVIDIA Parakeet TDT_CTC 110M (int8, k2-fsa export via a mirror)

> The Parakeet TDT_CTC 110M engine (a user engine) is the
> `parakeet-tdt_ctc-110m` model (https://huggingface.co/nvidia/parakeet-tdt_ctc-110m),
> created by NVIDIA NeMo and Suno.ai and licensed under the Creative Commons
> Attribution 4.0 International License (CC BY 4.0,
> https://creativecommons.org/licenses/by/4.0/legalcode). These files are a modified
> version: the k2-fsa / sherpa-onnx project converted the NeMo checkpoint to ONNX,
> kept only the transducer branch, and quantized the weights to int8, without
> retraining. k2-fsa publishes them in the sherpa-onnx `asr-models` release as
> `sherpa-onnx-nemo-parakeet_tdt_transducer_110m-en-36000-int8.tar.bz2`
> (https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models, SHA-256
> `f628312e9fdf8686374cb01a69425c41732529d540860311f16f37cbc32cfe9b`). The files
> below are byte-identical copies of the files in that archive, taken from
> https://huggingface.co/punitd/sherpa-onnx-nemo-parakeet_tdt_transducer_110m-en-36000-int8,
> commit `66a4fa70643dc7ce25c9b38b2f87e1b35ddad33d`:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `encoder.int8.onnx` | 131,113,202 | `0f35509ddeb9b39002fb077d979a9fe74f06eb0bc4dd5c34f512f82e5111d657` |
> | `decoder.int8.onnx` | 3,955,863 | `f7c331c5504c2e593c76ed22b728e3f554af6c4a383dde862e719ced08b1da19` |
> | `joiner.int8.onnx` | 1,411,403 | `bf7dff69e9f2cdbe9943d70da358f38b361c115ba0105bae7e908e0d6ec782f6` |
> | `tokens.txt` | 9,953 | `450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10` |
>
> The app downloads these files, unchanged, from release `v2` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `parakeet-tdt-110m-en-<file>`).
>
> Since #78 the app downloads the model files re-saved in ONNX Runtime's pre-optimised
> format: we converted the ONNX files above with ONNX Runtime 1.28.0
> (`python -m onnxruntime.tools.convert_onnx_models_to_ort`, optimization style
> Fixed, target arm). The graphs are the same, without retraining; the format
> only loads faster. From release `v9` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `parakeet-tdt-110m-en-<file>`):
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `encoder.int8.ort` | 131,508,352 | `fe0bab3304a93108bea8f5a63b64f4b422679a95da815ec400ecb4fd704a404f` |
> | `decoder.int8.ort` | 3,962,088 | `6486b1f9ba5555985e1dd5c1c61a17c7267a73054116676b367ca4d30a15fe5a` |
> | `joiner.int8.ort` | 1,418,128 | `87ac4bf5573c73887d33d1c1532451f328597d79b3e63c4874e2baae6d187bb2` |
> | `tokens.txt` | 9,953 | `450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10` |
>
> Engines installed before keep the files above.
> The model runs entirely on-device. It does not transmit or store audio.

No copyright notice is supplied with the model; the credit is to its creators. The model
is provided as-is, without warranties (Section 5 of the license). The mirror repository has
no license file or card and adds no terms; the SHA-256 pins make its bytes k2-fsa's. The
model ethics review is in
[`docs/model-reviews/parakeet-tdt_ctc-110m.md`](docs/model-reviews/parakeet-tdt_ctc-110m.md).

### NVIDIA Parakeet TDT_CTC 0.6B ja (int8, k2-fsa export)

> The Parakeet TDT_CTC 0.6B (Japanese) engine (a user engine) is NVIDIA's
> `parakeet-tdt_ctc-0.6b-ja` model (https://huggingface.co/nvidia/parakeet-tdt_ctc-0.6b-ja,
> commit `44edb27e`), created by NVIDIA and licensed under the Creative Commons
> Attribution 4.0 International License (CC BY 4.0,
> https://creativecommons.org/licenses/by/4.0/legalcode). These files are a modified
> version: the k2-fsa / sherpa-onnx project exported the model's CTC branch to ONNX
> and quantized the weights to int8, without retraining; the TDT branch is not
> exported. They are published at
> https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt_ctc-0.6b-ja-35000-int8,
> commit `bef18eb066808c90bd0f5df5be685767b0732de8`:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `model.int8.onnx` | 655,542,604 | `3addd00ef5bd1742078389e540b77394e4a508bdf2f4c9ad1b4a76d93e76598e` |
> | `tokens.txt` | 28,557 | `732f64c53909f2620c713f4106b487d92e6f54a6915b3cd3d1dbd32f9f4f392a` |
>
> The app downloads these files, unchanged, from release `v3` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `parakeet-ja-<file>`).
> The model runs entirely on-device. It does not transmit or store audio.

No copyright notice is supplied with the model; the credit is to NVIDIA as its creator. The
model is provided as-is, without warranties (Section 5 of the license). The CC BY 4.0 legal
code is bundled in the app (`assets/licenses/CC-BY-4.0.txt`). The k2-fsa Hugging Face
repository has no README, license file or license tag and adds no terms; the conversion
tooling (sherpa-onnx, Apache-2.0) does not ship with the model. Trained on the ReazonSpeech
corpus (Japanese TV broadcast audio, Reazon Holdings, CDLA-Sharing-1.0, used under Japanese
Copyright Act Art. 30-4). The model ethics review is in
[`docs/model-reviews/reazonspeech-japanese.md`](docs/model-reviews/reazonspeech-japanese.md).

### NVIDIA canary-180m-flash (int8, k2-fsa export)

> The Canary engine is NVIDIA's `canary-180m-flash` model
> (https://huggingface.co/nvidia/canary-180m-flash), created by NVIDIA and
> distributed under the Creative Commons Attribution 4.0 International License
> (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), in the 8-bit ONNX
> conversion published by the k2-fsa / sherpa-onnx project at
> https://huggingface.co/csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8,
> commit `9077164e0d3dd1d5353743e89ceaa1d3a770838c`:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `encoder.int8.onnx` | 132,678,643 | `7a75b4e2a5857a6dcc0819503bbe3fad66943db4a3ccf21d3f27c633667d303f` |
> | `decoder.int8.onnx` | 74,437,848 | `e41a2ab9c0c2fe81a1e8ade5a45fb02a74bc4db7d1f91b89a54a25e2cf79cba2` |
> | `tokens.txt` | 53,555 | `2dae6fc7815f9640645e0c765522b278ee0cef49b482d91f6913e334628d3e77` |
>
> **Modified:** k2-fsa's conversion exported the published checkpoint to ONNX
> with sherpa-onnx's `scripts/nemo/canary/export_onnx_180m_flash.py` and
> quantized the weights to 8 bits with ONNX Runtime, without retraining. The
> model is provided as-is, without warranties (CC BY 4.0, Section 5). It runs
> entirely on-device and is used only to transcribe. It does not transmit or
> store audio.
>
> The app downloads these files, unchanged, from release `v4` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `canary-180m-flash-<file>`).

NVIDIA supplies no copyright notice with the model, so none is reproduced
here; attribution is to NVIDIA as creator. The CC BY 4.0 legal code is
bundled in the app (`assets/licenses/CC-BY-4.0.txt`). The k2-fsa Hugging Face
repository has no license file or license tag; the conversion tooling
(sherpa-onnx, Apache-2.0) does not ship with the model. The model ethics
review is in
[`docs/model-reviews/canary-180m-flash.md`](docs/model-reviews/canary-180m-flash.md).

### NVIDIA Nemotron 3.5 ASR Streaming 0.6B (int8, k2-fsa export)

> The Nemotron 3.5 streaming engine is NVIDIA's
> `nemotron-3.5-asr-streaming-0.6b` model (https://huggingface.co/nvidia/nemotron-3.5-asr-streaming-0.6b),
> distributed under the OpenMDW License Agreement, version 1.1, in the int8 ONNX conversion published by
> the k2-fsa / sherpa-onnx project, exported with a 1120 ms chunk:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `encoder.int8.onnx` | 657,601,521 | `2fff2166acaa535bd969fb223c1f0783d71029f143cb298bc54c2afe85abf772` |
> | `decoder.int8.onnx` | 14,978,075 | `19f9c98fc6d0a2c33a65a43b36fdb2e914c26c0aa9764be3aebc502a1e982fb0` |
> | `joiner.int8.onnx` | 9,504,438 | `4101c7c679a0bc30483794b27a059e34e79232aa2068d78d51231a22c8b0d7ce` |
> | `tokens.txt` | 131,440 | `729cc103155bafa785f9cd45746cd41cabe97eab7182fc04d594129587958f8a` |
>
> https://huggingface.co/csukuangfj2/sherpa-onnx-nemotron-3.5-asr-streaming-0.6b-1120ms-int8-2026-06-11,
> commit `cba1c96ca5ef0e8393b50584ae153a79145dc492`. The 560 ms export left the app in #37. k2-fsa's conversion exported the published checkpoint to
> ONNX with sherpa-onnx's `scripts/nemo/nemotron-3.5-asr-streaming-0.6b/export_onnx.py` at a fixed chunk size and
> quantized the weights to int8 with ONNX Runtime, without retraining. The model runs entirely on-device. It does
> not transmit or store audio.
>
> The app downloads these files, unchanged, from release `v5` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `nemotron-3-5-streaming-1120ms-<file>`).
>
> Since #78 the app downloads the model files re-saved in ONNX Runtime's pre-optimised
> format: we converted the ONNX files above with ONNX Runtime 1.28.0
> (`python -m onnxruntime.tools.convert_onnx_models_to_ort`, optimization style
> Fixed, target arm). The graphs are the same, without retraining; the format
> only loads faster. From release `v10` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `nemotron-3-5-streaming-1120ms-<file>`):
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `encoder.int8.ort` | 657,662,096 | `804929d57aa6a661fee7ff9ea6eeecb4c544b995cc40c1338b496c4931737eb6` |
> | `decoder.int8.ort` | 14,986,960 | `6159f5af06499469a32d47e57115a8de37e4708c5e9ed61bf694926df50b3571` |
> | `joiner.int8.ort` | 9,511,160 | `9cf237a2533855cb987b1d4a4d12379834cd1749c25e6d63bbac353c7a7bd3df` |
> | `tokens.txt` | 131,440 | `729cc103155bafa785f9cd45746cd41cabe97eab7182fc04d594129587958f8a` |
>
> Engines installed before keep the files above.

The license text is bundled in the app as `OpenMDW-1.1.txt`: `1.1/LICENSE.OpenMDW-1.1` of
https://github.com/OpenMDW/openmdw (commit `b26b32b3`; the file's only revision), identical to
https://openmdw.ai/license/1-1/. NVIDIA's model card names it as the governing terms and publishes no copyright
line. OpenMDW-1.1 asks redistributors to keep a copy of the agreement and all notices of origin. Our engines
repository redistributes these files, so release `v5` keeps the agreement (`LICENSES/OpenMDW-1.1.txt`) and NVIDIA's
credit and the export's origin (`NOTICE`); the app credits NVIDIA and shows the license too. The k2-fsa repositories add no license of their own. The model ethics review is in
[`docs/model-reviews/nemotron-3.5-asr-streaming-0.6b.md`](docs/model-reviews/nemotron-3.5-asr-streaming-0.6b.md).

### IBM Granite Speech 5.0 470M TurboCTC (int8, qwertz92 export; developer engine)

> The Granite Speech engine (developer engines only) is IBM's
> `granite-speech-5.0-470m-turboctc` model
> (https://huggingface.co/ibm-granite/granite-speech-5.0-470m-turboctc), created by the
> IBM Granite Speech Team and distributed under the Apache License 2.0. The ONNX file is a
> modified version: a community conversion by qwertz92 exported IBM's checkpoint
> (`model.safetensors`, SHA-256
> 8b98a8c34fd5fcb081caef719638eded31bb6d197d62053eefc5c1703aaf1ad4) to ONNX, replacing two
> `unfold` operations with mathematically equivalent `avg_pool1d` ones, and quantized the
> weights to int8 with ONNX Runtime, without retraining. It is published, with its
> conversion scripts, at https://huggingface.co/qwertz92/granite-speech-5.0-470m-turboctc-onnx,
> commit `e6e3b4d6f590ac51a05f25028b53591d4b260786`:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `onnx/model_int8.onnx` | 551,294,349 | `8173a50ea67f864801971a40622a2e5c7d62fd230912bde94b745f92c74d60e9` |
> | `tokenizer.json` | 1,137,492 | `3ee80b02f0119a040a70eb909c20fac8271c173d7e71d195a3b35f77780061e6` |
>
> `tokenizer.json` is IBM's, byte for byte (the same SHA-256 as IBM's repository at commit
> `286456107c8ba1161f5c22dfe85466402c88333b`). The app computes the model's audio features
> and decodes its output itself (a Kotlin port of transformers' feature extractor and
> greedy CTC decoding). IBM trained the model on publicly available speech data (MLS,
> YODAS, Common Voice, LibriSpeech, VoxPopuli, AMI, Earnings-22) and synthetic speech.
> The model runs entirely on-device. It does not transmit or store audio.
>
> The app downloads these files, unchanged, from release `v7` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `granite-speech-5-470m-<file>`).

Apache-2.0 (shared body). IBM's model repository has no LICENSE or NOTICE file (its card
and tag say Apache-2.0) and supplies no copyright line, so the credit is to IBM as creator;
qwertz92's repository has the Apache-2.0 tag and no LICENSE file either. The Apache-2.0
text is bundled in the app (`assets/licenses/Apache-2.0.txt`). The conversion tooling
does not ship with the model. The model ethics review is in
[`docs/model-reviews/granite-speech-5.0-470m-turboctc.md`](docs/model-reviews/granite-speech-5.0-470m-turboctc.md).

### Qwen3-ASR 0.6B (int8 ONNX export)

> The Qwen3-ASR engine is Alibaba Cloud's Qwen3-ASR 0.6B model
> (https://huggingface.co/Qwen/Qwen3-ASR-0.6B, commit `5eb14417`), distributed
> under the Apache License 2.0, in the int8 ONNX conversion that the k2-fsa /
> sherpa-onnx project re-hosts at
> https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25,
> commit `68818b2313fe77bd06f6a7c5068ff3ef59d02b8a`:
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `conv_frontend.onnx` | 44,148,281 | `d22dc4423e0940e49884e903d2ea2f7e5567c14fc1aed97e4e26d6b8f208ef9e` |
> | `encoder.int8.onnx` | 182,491,662 | `60748d3e6744a57c9c91e1b17424a6c2990567e8adceb0783940c03ed98fa9d9` |
> | `decoder.int8.onnx` | 755,914,231 | `4f6885be5959ae26af3089d38ee7972c5fafbeeb1cf8d5e76eab6d8b61ca5771` |
> | `tokenizer/merges.txt` | 1,671,853 | `8831e4f1a044471340f7c0a83d7bd71306a5b867e95fd870f74d0c5308a904d5` |
> | `tokenizer/tokenizer_config.json` | 12,487 | `4942d005604266809309cabc9f4e9cb89ce855d59b14681fdc0e1cc62ea26c4c` |
> | `tokenizer/vocab.json` | 2,776,833 | `ca10d7e9fb3ed18575dd1e277a2579c16d108e32f27439684afa0e10b1440910` |
>
> Qwen3-ASR 0.6B pairs an audio encoder with a decoder built on the Qwen3-0.6B
> language model (Apache-2.0), whose tokenizer the three tokenizer files are,
> unchanged. The ONNX files were modified from the original checkpoint: a
> third-party export (https://github.com/Wasser1462/Qwen3-ASR-onnx, published at
> https://modelscope.cn/models/zengshuishui/Qwen3-ASR-onnx) converted it to ONNX
> and quantized the weights to int8 with ONNX Runtime, without retraining; k2-fsa
> re-hosts those files byte-for-byte. The app installs the tokenizer files next to
> the model files. The model runs entirely on-device. It does not transmit or
> store audio.
>
> The app downloads these files, unchanged, from release `v8` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `qwen3-asr-0-6b-<file>`).
>
> Since #78 the app downloads the three model files (the tokenizer files unchanged) re-saved in ONNX Runtime's pre-optimised
> format: we converted the ONNX files above with ONNX Runtime 1.28.0
> (`python -m onnxruntime.tools.convert_onnx_models_to_ort`, optimization style
> Fixed, target arm). The graphs are the same, without retraining; the format
> only loads faster. From release `v11` of
> https://github.com/maxwhipw/marmalade-stt-engines (assets `qwen3-asr-0-6b-<file>`):
>
> | File | Bytes | SHA-256 |
> |---|---|---|
> | `conv_frontend.ort` | 44,167,416 | `a4e120496b7b66f452303121def15c3204c4042af2523e08dad7486fec216f71` |
> | `encoder.int8.ort` | 182,684,104 | `3cbaab59db7ec7900a57aee077fd9213fa48624ca8205bfba289ba76740bd750` |
> | `decoder.int8.ort` | 756,556,392 | `2c56875df87054decd46e08eff5f46920960c989ddeb641becd6809d34392cff` |
> | `merges.txt` | 1,671,853 | `8831e4f1a044471340f7c0a83d7bd71306a5b867e95fd870f74d0c5308a904d5` |
> | `tokenizer_config.json` | 12,487 | `4942d005604266809309cabc9f4e9cb89ce855d59b14681fdc0e1cc62ea26c4c` |
> | `vocab.json` | 2,776,833 | `ca10d7e9fb3ed18575dd1e277a2579c16d108e32f27439684afa0e10b1440910` |
>
> Engines installed before keep the files above.

Apache-2.0 (shared body). Copyright lines, from the upstream LICENSE files:
"Copyright 2026 Alibaba Cloud" (`LICENSE` of https://github.com/QwenLM/Qwen3-ASR,
commit `9567667698`, the file's only revision) and "Copyright 2024 Alibaba Cloud"
(https://huggingface.co/Qwen/Qwen3-0.6B/blob/main/LICENSE). Neither upstream ships
a NOTICE file. The k2-fsa repository has no license file or tag and adds no terms.
Qwen does not disclose the model's training data; see the model ethics review in
[`docs/model-reviews/qwen3-asr-0.6b.md`](docs/model-reviews/qwen3-asr-0.6b.md).


