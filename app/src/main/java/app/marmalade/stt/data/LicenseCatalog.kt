package app.marmalade.stt.data

import app.marmalade.stt.util.BugReportUrl

/**
 * Static catalog of the third-party open-source components Marmalade STT
 * ships in the APK or downloads on demand (speech models), surfaced by the
 * in-app "Open-source licenses" screen
 * ([app.marmalade.stt.ui.screen.LicensesScreen]).
 *
 * This mirrors the per-component summary in the repo-root `NOTICE.md`. That
 * is the human-readable repo document; this is the app-facing data behind
 * Settings → About → Open-source licenses. **Keep them in sync** —
 * `LicenseCatalogTest` fails when a component here is missing from NOTICE.md.
 * Every component added to the APK (native libraries, the bundled VAD model,
 * downloadable speech models) needs an entry in both, in the same change.
 *
 * ## Copyright handling (why this is per-component, not per-license)
 *
 * MIT and BSD license texts embed the licensor's copyright line *as part of
 * the license* — MIT's required notice and BSD clause 1. So a single shared
 * "MIT" / "BSD" body would display the wrong copyright holder for every
 * component except the one it was written for. Therefore:
 *
 *  - **MIT / BSD components** each carry their own exact license text
 *    ([Component.textAsset]) with the correct holder — sourced verbatim from
 *    the component where it vendors one, else the canonical body + the
 *    authoritative copyright line.
 *  - **Apache-2.0 / MPL-2.0 / OFL-1.1** (and other standalone bodies such as
 *    GPL or CC) have no embedded licensor copyright (attribution lives in
 *    NOTICE files / source headers / the font's name table), so those share one
 *    body ([License.sharedAsset]) and the component's [Component.copyright]
 *    supplies the attribution. Such a component must NOT set
 *    [Component.textAsset] to the shared body: that marks the body as
 *    embedding the copyright, and the attribution would never be shown.
 *
 * Full texts for the [License.sharedAsset] / [Component.textAsset] entries
 * live in `app/src/main/assets/licenses/`. Components with neither a shared
 * body nor a per-component text fall back to the upstream [License.url]
 * (none do today; `LicenseCatalogTest` requires every text to be bundled).
 */
object LicenseCatalog {

    /**
     * A license family referenced by one or more [Component]s.
     *
     * @param id         SPDX-style identifier, also the display/group name.
     * @param sharedAsset Body shared across all components on this license —
     *                    only set for licenses with **no embedded licensor
     *                    copyright** (Apache/OFL/GPL/CC). Null for MIT/BSD,
     *                    whose components must each supply [Component.textAsset].
     * @param url        Canonical upstream URL (browser fallback / "view online").
     */
    data class License(
        val id: String,
        val sharedAsset: String?,
        val url: String,
    )

    /** One shipped third-party component. */
    data class Component(
        /** Stable slug, used as the license-text screen's nav argument. */
        val key: String,
        val name: String,
        val role: String,
        /** Where it reaches the device: "APK", "APK (source)", etc. */
        val shipsIn: String,
        /** Must match a [License.id] in [licenses]. */
        val licenseId: String,
        /** Exact copyright/attribution line(s) — always shown for this component. */
        val copyright: List<String>,
        /**
         * Per-component exact license text under `assets/licenses/`. Set for
         * MIT/BSD components (the copyright is embedded in the file). Null for
         * components that use their license family's [License.sharedAsset].
         */
        val textAsset: String? = null,
        /** Optional clarification (dual-licensed sub-parts, port provenance, …). */
        val note: String? = null,
    ) {
        /**
         * The bundled asset to display for this component, if any: the
         * per-component [textAsset] wins, else the license family's shared
         * body. Null means "no bundled text — open [License.url] instead".
         */
        fun resolvedAsset(): String? = textAsset ?: licenseFor(licenseId)?.sharedAsset

        /**
         * True when [resolvedAsset] is a per-component file that already
         * embeds the copyright line (MIT/BSD) — so the text screen must NOT
         * also prepend [copyright] (it's already in the body). False when the
         * shared family body is used (Apache/OFL), where [copyright] is the
         * separate attribution to show above the body.
         */
        fun bodyEmbedsCopyright(): Boolean = textAsset != null
    }

    /** The app's licensing posture, shown at the top of the screen. */
    object POSTURE {
        const val SOURCE = "Source code — MIT. Every source file in this " +
            "project is MIT-licensed."
        const val BINARY = "App binary — MIT. The APK contains no GPL or " +
            "LGPL code. Every third-party component in it is listed below " +
            "under its own license: permissive (MIT, BSD, Apache-2.0) apart " +
            "from Eigen, whose MPL-2.0 is file-level copyleft (its unmodified " +
            "source is linked below), and the brand fonts, which are OFL-1.1."
        const val SOURCE_LABEL = "Source code"
        const val SOURCE_URL = BugReportUrl.REPO_URL
    }

    val licenses: List<License> = listOf(
        // MIT: no shared body — each component supplies its own exact text
        // with the correct copyright holder.
        License("MIT", null, "https://opensource.org/license/mit"),
        License("BSD-3-Clause", null, "https://opensource.org/license/bsd-3-clause"),
        License("BSD-2-Clause", null, "https://opensource.org/license/bsd-2-clause"),
        // Standalone body with no embedded licensor copyright — shared.
        License(
            "Apache-2.0", "Apache-2.0.txt",
            "https://www.apache.org/licenses/LICENSE-2.0",
        ),
        // The LLVM runtime's notice bundles Apache-2.0 + the LLVM Exceptions
        // with the legacy NCSA/MIT texts, which embed a copyright line — so
        // it ships as a per-component text, like MIT.
        License(
            "Apache-2.0 WITH LLVM-exception", null,
            "https://llvm.org/LICENSE.txt",
        ),
        // MPL-2.0 is a standalone body: the notice lives in each file header.
        License(
            "MPL-2.0", "MPL-2.0.txt",
            "https://www.mozilla.org/MPL/2.0/",
        ),
        // Not one license: the notices file Microsoft publishes for the
        // third-party code statically linked into libonnxruntime.so, each
        // entry carrying its own license and holder (MIT, BSD, Apache-2.0,
        // MPL-2.0 for Eigen, …). Reproduced verbatim as one text.
        License(
            "Third-party notices", null,
            "https://github.com/microsoft/onnxruntime/blob/v1.28.0/ThirdPartyNotices.txt",
        ),
        // OFL keeps the font's copyright notice separate from the license
        // body (the notice travels with the font files), so the body is
        // shareable like Apache and Component.copyright carries the per-font
        // attribution.
        License(
            "OFL-1.1", "OFL-1.1.txt",
            "https://openfontlicense.org",
        ),
        // Model licenses (the speech engines). None embeds a licensor
        // copyright, so each body is shared and Component.copyright carries
        // the attribution.
        License(
            "CC-BY-4.0", "CC-BY-4.0.txt",
            "https://creativecommons.org/licenses/by/4.0/legalcode",
        ),
        // Tatoeba's licence, the legal code in French (the only official text).
        License(
            "CC-BY-2.0-FR", "CC-BY-2.0-FR.txt",
            "https://creativecommons.org/licenses/by/2.0/fr/",
        ),
        License(
            "OpenMDW-1.1", "OpenMDW-1.1.txt",
            "https://openmdw.ai/license/1-1/",
        ),
    )

    /**
     * Declaration order is display order. Components are grouped by
     * [Component.licenseId] in the UI, preserving this order.
     */
    val components: List<Component> = listOf(
        Component(
            key = "marmalade-stt",
            name = "Marmalade STT", role = "This app", shipsIn = "APK (source; libmarmalade-cpu.so)",
            licenseId = "MIT",
            copyright = listOf("Copyright (c) 2026 marmalade-stt contributors"),
            textAsset = "MIT.txt",
            note = "Includes the Granite formatter (assets/granite/formatter.int8.onnx), a model this " +
                "project trained from scratch. Its training text is credited below.",
        ),

        // ---- Native speech runtime: libsherpa-onnx-jni.so --------------------
        // Built from source (docs/ARCHITECTURE.md D3) from the pinned
        // third_party/ submodules. Verified against the arm64-v8a build's
        // compiled sources and the linked .so's debug info; see NOTICE.md.
        Component(
            key = "sherpa-onnx",
            name = "sherpa-onnx", role = "Speech recognition and VAD runtime (v1.13.8, TTS off)",
            shipsIn = "APK (libsherpa-onnx-jni.so, built from source; Kotlin API in source)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright (c) 2022-2026 Xiaomi Corporation",
                "Other holders named in the compiled source headers: Cisco Systems, " +
                    "Ceva Inc, Posit Software PBC, Uniphore, Tilde SIA, Pingfeng Luo, " +
                    "zengyw, Matias Lin, Milan Leonard, Sangeet Sagar, Jingzhao Ou, " +
                    "zhifu gao, and others; Kaldi-derived code: Microsoft Corporation, " +
                    "Johns Hopkins University, Saarland University, Karel Vesely, " +
                    "Pegah Ghahremani, Tanel Alumae",
            ),
            note = "Built with TTS off, so no espeak-ng or piper code is in the " +
                "APK. Eight Kotlin API files are vendored into " +
                "com.k2fsa.sherpa.onnx, unmodified except OfflineStream.kt (cancel). " +
                "Modified: the build applies upstream " +
                "sherpa-onnx commit 2edc882f (#3965, Whisper text decoding) to " +
                "v1.13.8's offline-recognizer-whisper-impl.h, " +
                "offline-recognizer-whisper-tpl-impl.h, " +
                "offline-recognizer-cohere-transcribe-impl.h and text-utils-test.cc, " +
                "and our own changes to offline-whisper-greedy-search-decoder.cc " +
                "(no per-second cap on Whisper's output) and to offline-stream.h/.cc, " +
                "the Whisper, Canary and Qwen3-ASR decoders and jni/offline-stream.cc " +
                "(a decode in progress can be cancelled).",
        ),
        Component(
            key = "kaldi-native-fbank",
            name = "kaldi-native-fbank", role = "Audio feature extraction (v1.22.3)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright (c) 2022-2025 Xiaomi Corporation",
                "Copyright (c) 2024 Brno University of Technology",
                "Kaldi-derived code: Copyright 2009-2011 Karel Vesely; Petr Motlicek; " +
                    "Saarland University; 2014-2016 Johns Hopkins University",
            ),
            note = "Modified: our own change to whisper-feature.h, whisper-feature.cc " +
                "and rfft.cc (a faster FFT for Whisper's features).",
        ),
        Component(
            key = "kissfft",
            name = "KISS FFT", role = "FFT used by kaldi-native-fbank (commit febd4ca)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "BSD-3-Clause",
            copyright = listOf("Copyright (c) 2003-2010 Mark Borgerding . All rights reserved."),
            textAsset = "kissfft.txt",
        ),
        Component(
            key = "kaldi-decoder",
            name = "kaldi-decoder", role = "CTC decoding (v0.3.0)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright (c) 2022-2023 Xiaomi Corporation",
                "Kaldi-derived code: Copyright 2009-2018 Microsoft Corporation, " +
                    "Johns Hopkins University, Saarland University, Ondrej Glembek, " +
                    "Yanmin Qian, Lukas Burget, Mirko Hannemann, Guoguo Chen, " +
                    "Zhehuai Chen",
            ),
        ),
        Component(
            key = "kaldifst",
            name = "kaldifst", role = "Kaldi FST and I/O utilities (v1.8.0)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright (c) 2020-2023 Xiaomi Corporation",
                "Kaldi-derived code: Copyright 2009-2018 Microsoft Corporation, " +
                    "Johns Hopkins University, Saarland University, Brno University " +
                    "of Technology, University of Southern California, Telepoint " +
                    "Global Hosting Service LLC, Karel Vesely, Ondrej Glembek, " +
                    "Yanmin Qian, Jan Silovsky, Gilles Boulianne, Tanel Alumae, " +
                    "Frantisek Skala, Arnab Ghoshal, Guoguo Chen, Hainan Xu, " +
                    "Xiaohui Zhang, Dogan Can",
            ),
            note = "kaldifst's LICENSE: \"The copyright model is that authors (or " +
                "their employers, if noted in individual files) own their " +
                "individual contributions.\" Its basic-filebuf.h is a modified " +
                "copy of libc++'s std::basic_filebuf; see the LLVM C++ runtime entry.",
        ),
        Component(
            key = "openfst",
            name = "OpenFst", role = "Finite-state transducers (csukuangfj fork, tag v1.8.5-2026-07-09)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "Apache-2.0",
            copyright = listOf("Copyright 2005-2026 Google LLC."),
        ),
        Component(
            key = "simple-sentencepiece",
            name = "simple-sentencepiece", role = "Subword tokenizer (v0.7)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "Apache-2.0",
            copyright = listOf("Copyright 2024 Wei Kang (wkang@pku.edu.cn)"),
            note = "Its bundled ThreadPool (zlib license, Jakob Progsch) is " +
                "replaced by an empty stub when building for Android, so it " +
                "does not ship. The darts-clone code it contains does.",
        ),
        Component(
            key = "darts-clone",
            name = "darts-clone", role = "Double-array trie (inside simple-sentencepiece)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "BSD-2-Clause",
            copyright = listOf("Copyright (c) 2008-2014, Susumu Yata All rights reserved."),
            textAsset = "darts-clone.txt",
        ),
        Component(
            key = "eigen",
            name = "Eigen", role = "Linear algebra (5.0.1, header-only)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "MPL-2.0",
            copyright = listOf(
                "Copyright (C) Benoit Jacob, Gael Guennebaud and the Eigen " +
                    "contributors named in each file header",
            ),
            note = "Unmodified. Source: https://gitlab.com/libeigen/eigen, tag " +
                "5.0.1 (commit bc3b39870ecb690a623a3f49149a358b95c5781d), also " +
                "in this app's source repository as the third_party/eigen submodule.",
        ),
        Component(
            key = "nlohmann-json",
            name = "JSON for Modern C++ (nlohmann/json)", role = "JSON parsing (v3.12.0)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "MIT",
            copyright = listOf(
                "Copyright (c) 2013-2025 Niels Lohmann",
                "Copyright (c) 2008-2009 Björn Hoehrmann",
            ),
            textAsset = "nlohmann-json.txt",
        ),
        Component(
            key = "llvm-libcxx",
            name = "LLVM C++ runtime (libc++, libc++abi, libunwind)",
            role = "C++ standard library (Android NDK r26d, statically linked)",
            shipsIn = "APK (compiled into libsherpa-onnx-jni.so)",
            licenseId = "Apache-2.0 WITH LLVM-exception",
            copyright = listOf(
                "Copyright (c) 2009-2019 by the contributors listed in CREDITS.TXT",
                "Copyright (c) 2009-2014 by the contributors listed in CREDITS.TXT",
            ),
            textAsset = "llvm-libcxx.txt",
            note = "Also covers kaldifst's basic-filebuf.h, a modified copy of " +
                "libc++'s std::basic_filebuf (via Kaldi), used under the legacy " +
                "libc++ MIT license. CREDITS.TXT: " +
                "https://github.com/llvm/llvm-project/blob/main/libcxx/CREDITS.TXT",
        ),

        // ---- ONNX Runtime: libonnxruntime.so from Maven ----------------------
        Component(
            key = "onnxruntime",
            name = "ONNX Runtime", role = "Neural network inference (1.28.0, from Maven)",
            shipsIn = "APK (libonnxruntime.so, libonnxruntime4j_jni.so and its Java API)",
            licenseId = "MIT",
            copyright = listOf("Copyright (c) Microsoft Corporation"),
            textAsset = "onnxruntime.txt",
        ),
        Component(
            key = "onnxruntime-notices",
            name = "ONNX Runtime third-party notices",
            role = "Libraries statically linked into ONNX Runtime " +
                "(XNNPACK, protobuf, Abseil, FlatBuffers, Eigen, …)",
            shipsIn = "APK (inside libonnxruntime.so)",
            licenseId = "Third-party notices",
            copyright = listOf(
                "Notices as published by Microsoft with ONNX Runtime 1.28.0 " +
                    "(ThirdPartyNotices.txt) — each entry names its own holder",
            ),
            textAsset = "onnxruntime-third-party-notices.txt",
            note = "ONNX Runtime 1.28.0 compiles in Eigen (MPL-2.0) at commit " +
                "1d8b82b0740839c0de7f1242a3585e3390ff5f33 on Eigen's 3.4 branch, " +
                "pinned in its cmake/deps.txt. Source: https://gitlab.com/libeigen/eigen.",
        ),

        // ---- Bundled model ------------------------------------------------------
        Component(
            key = "silero-vad",
            name = "Silero VAD", role = "Voice activity detection model (v4.0, ONNX export by k2-fsa)",
            shipsIn = "APK (assets/vad/silero_vad.onnx)",
            licenseId = "MIT",
            copyright = listOf("Copyright (c) 2020-present Silero Team"),
            textAsset = "silero-vad.txt",
            note = "The 16 kHz-only ONNX conversion of Silero VAD v4.0 published " +
                "by the k2-fsa / sherpa-onnx project (no retraining). Runs on-device only.",
        ),

        // ---- Smart Turn end-of-dictation (#43) ----------------------------------
        // The model is bundled in the APK (#44; pinned + hash-checked by
        // SmartTurnAssetTest); its feature extraction is ported into the app's
        // source. Review: docs/model-reviews/smart-turn-v3.md.
        Component(
            key = "smart-turn-v3",
            name = "Pipecat Smart Turn v3.2",
            role = "End-of-dictation model: decides when you have finished speaking (int8 CPU ONNX)",
            shipsIn = "APK (bundled)",
            licenseId = "BSD-2-Clause",
            copyright = listOf("Copyright (c) 2024–2025, Daily"),
            textAsset = "smart-turn.txt",
            note = "smart-turn-v3.2-cpu.onnx from huggingface.co/pipecat-ai/smart-turn-v3, unmodified. " +
                "Fine-tuned by Daily from OpenAI's Whisper Tiny encoder (see the OpenAI Whisper entry). " +
                "Runs on-device only.",
        ),
        Component(
            key = "pipecat-whisper-features",
            name = "Pipecat Whisper feature extraction",
            role = "Whisper log-mel features that Smart Turn reads, ported to Kotlin",
            shipsIn = "APK (source, ported)",
            licenseId = "BSD-2-Clause",
            copyright = listOf("Copyright (c) 2024–2026, Daily"),
            textAsset = "pipecat.txt",
            note = "Ported from pipecat's src/pipecat/audio/turn/smart_turn/_whisper_features.py " +
                "(https://github.com/pipecat-ai/pipecat, commit 31aa4ac6), which pipecat vendored from " +
                "Hugging Face Transformers' Whisper feature extractor (see that entry).",
        ),
        Component(
            key = "transformers-whisper-features",
            name = "Hugging Face Transformers (Whisper feature extractor)",
            role = "Origin of the Whisper log-mel feature code that pipecat vendored and this app ports",
            shipsIn = "APK (source, ported via pipecat)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright 2022 The HuggingFace Inc. team.",
                "Copyright 2023 The HuggingFace Inc. team and the librosa & torchaudio authors.",
            ),
            note = "From transformers/models/whisper/feature_extraction_whisper.py and " +
                "transformers/audio_utils.py (https://github.com/huggingface/transformers). Modified: " +
                "pipecat reduced them to a numpy-only extractor, and this app ported that to Kotlin.",
        ),

        // ---- Training text of the Granite formatter (#33) ---------------------
        // The formatter (assets/granite/formatter.int8.onnx) is this app's own
        // model, under its MIT licence; these are the texts it learned from.
        Component(
            key = "tatoeba-sentences",
            name = "Tatoeba English sentences",
            role = "Training text of the Granite formatter (case, punctuation, apostrophes)",
            shipsIn = "Not shipped: the formatter model in the APK was trained on it",
            licenseId = "CC-BY-2.0-FR",
            copyright = listOf("The Tatoeba contributors (https://tatoeba.org)"),
            note = "English sentences from Tatoeba's export, downloaded 2026-09-27. The formatter also " +
                "learned from Mozilla Common Voice's community-submitted sentences (CC0 1.0). It labels the " +
                "case and punctuation of words it is given and can't reproduce the training text.",
        ),
        Component(
            key = "common-pile-news-cc-by",
            name = "CC BY news text (Common Pile)",
            role = "Training text of the Granite formatter (case, punctuation, apostrophes)",
            shipsIn = "Not shipped: the formatter model in the APK was trained on it",
            licenseId = "CC-BY-4.0",
            copyright = listOf(
                "Global Voices, Factly, Milwaukee Neighborhood News Service, New Canadian Media, Alt News, " +
                    "360info, Balkan Diskurs, Freedom of the Press Foundation, Mekong Eye, ZimFact, " +
                    "Minority Africa and Solutions Journalism",
            ),
            note = "Articles these publishers release under CC BY 4.0, as collected in the Common Pile news " +
                "dataset (https://huggingface.co/datasets/common-pile/news). Only documents on the " +
                "publishers' own sites were used; none share-alike.",
        ),

        // ---- Downloadable speech models (not in the APK) -------------------------
        // Fetched on demand from the Engines screen (SttEngineCatalog), all from
        // our marmalade-stt-engines GitHub releases (#75). Listed here because
        // the weights reach the device through the app. Each model
        // is credited with every upstream whose weights it contains.
        Component(
            key = "openai-whisper",
            name = "OpenAI Whisper",
            role = "Speech recognition engine Whisper Large v3 Turbo (int8 ONNX export by k2-fsa); and " +
                "Whisper Tiny, the encoder Smart Turn v3.2 was fine-tuned from",
            shipsIn = "APK (inside the bundled Smart Turn weights); download (Engines screen: large-v3-turbo; " +
                "from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "MIT",
            copyright = listOf("Copyright (c) 2022 OpenAI"),
            textAsset = "whisper.txt",
        ),
        // Norwegian engines from the fork's manifest (install/ExtraEngines.kt, tools/norsk/).
        Component(
            key = "nb-whisper",
            name = "NB-Whisper (National Library of Norway)",
            role = "Norwegian speech recognition engines NB-Whisper Tiny, Base and Small " +
                "(int8 ONNX export made with sherpa-onnx's Whisper script)",
            shipsIn = "Download (Engines screen; from github.com/hakonamatata177/marmalade-stt-android, release modeller)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "NB-Whisper by the National Library of Norway (NbAiLab, https://huggingface.co/NbAiLab), " +
                    "fine-tuned from OpenAI Whisper (Copyright (c) 2022 OpenAI, MIT).",
                "Modified for this app: converted to ONNX and quantized to int8, without retraining.",
            ),
        ),

        // The user engines are Parakeet Ultra, Parakeet 110M, Parakeet 0.6B ja
        // (#60), Canary, Nemotron 3.5, Whisper turbo (#45) and Qwen3-ASR (#50;
        // all 30 of its languages since #88); Granite is a developer engine (#13), hidden
        // unless Settings → Developer engines is on. The same rules for all:
        // pinned, hashed, reviewed. Names are the upstream models' (attribution);
        // a role says "shown as" where the Engines screen's name differs a lot (#67).
        Component(
            key = "parakeet-ultra",
            name = "Moondream Parakeet Ultra",
            role = "Speech recognition engine (the default), 25 European languages: Moondream's post-trained " +
                "Parakeet TDT 0.6B v3 (int8 ONNX conversion for this app)",
            shipsIn = "Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "CC-BY-4.0",
            copyright = listOf(
                "Created by Moondream (M87 Labs, Inc.) (https://huggingface.co/moondream/parakeet-ultra), " +
                    "a post-trained version of NVIDIA's parakeet-tdt-0.6b-v3 " +
                    "(https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3). " +
                    "No copyright notice is supplied with either model.",
                "Modified for this app: converted to ONNX and quantized to int8 " +
                    "(https://github.com/maxwhipw/marmalade-stt-engines, release v1).",
            ),
            note = "Licensed under CC BY 4.0. These files are an adaptation: Moondream post-trained " +
                "NVIDIA's model; its weights were then loaded into NVIDIA's v3 model graph and tokenizer, " +
                "exported to ONNX (transducer only; the voice-activity head removed) and int8-quantized, " +
                "without retraining. Provided as-is, without warranties (see Section 5 of the license). " +
                "Runs on-device only.",
        ),
        Component(
            key = "parakeet-tdt_ctc-110m",
            name = "NVIDIA Parakeet TDT_CTC 110M",
            role = "English speech recognition engine, shown as Parakeet 110M (int8 ONNX export by k2-fsa)",
            shipsIn = "Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "CC-BY-4.0",
            copyright = listOf(
                "Created by NVIDIA NeMo and Suno.ai (https://huggingface.co/nvidia/parakeet-tdt_ctc-110m). " +
                    "No copyright notice is supplied with the model.",
                "Modified by the k2-fsa / sherpa-onnx project: converted to ONNX and quantized to int8 " +
                    "(sherpa-onnx release asr-models, " +
                    "sherpa-onnx-nemo-parakeet_tdt_transducer_110m-en-36000-int8.tar.bz2).",
                "Modified for this app: re-saved in ONNX Runtime's pre-optimised format (.ort, ONNX Runtime 1.28.0), " +
                    "the same graphs, without retraining (https://github.com/maxwhipw/marmalade-stt-engines, release v9).",
            ),
            note = "Licensed under CC BY 4.0. These files are an adaptation: the NeMo checkpoint exported " +
                "to ONNX (transducer branch only) and int8-quantized, without retraining, then re-saved as " +
                "ONNX Runtime files for faster loading; earlier installs keep the k2-fsa files, re-hosted " +
                "unchanged from a byte-identical copy at huggingface.co/punitd/" +
                "sherpa-onnx-nemo-parakeet_tdt_transducer_110m-en-36000-int8. Provided as-is, without " +
                "warranties (see Section 5 of the license). Runs on-device only.",
        ),
        Component(
            key = "parakeet-tdt_ctc-0.6b-ja",
            name = "NVIDIA Parakeet TDT_CTC 0.6B ja",
            role = "Japanese speech recognition engine, shown as Parakeet Japanese (int8 ONNX export by k2-fsa)",
            shipsIn = "Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "CC-BY-4.0",
            copyright = listOf(
                "Created by NVIDIA (https://huggingface.co/nvidia/parakeet-tdt_ctc-0.6b-ja). " +
                    "No copyright notice is supplied with the model.",
                "Modified by the k2-fsa / sherpa-onnx project: CTC branch exported to ONNX and quantized " +
                    "to int8 (huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt_ctc-0.6b-ja-35000-int8).",
            ),
            note = "Licensed under CC BY 4.0. These files are an adaptation: the NeMo checkpoint's CTC " +
                "branch exported to ONNX (the TDT branch is not exported) and int8-quantized, without " +
                "retraining. Provided as-is, without warranties (see Section 5 of the license). Runs " +
                "on-device only. Trained on the ReazonSpeech corpus (Japanese TV broadcast audio, Reazon " +
                "Holdings, CDLA-Sharing-1.0, used under Japanese Copyright Act Art. 30-4).",
        ),
        Component(
            key = "canary-180m-flash",
            name = "NVIDIA canary-180m-flash",
            role = "Speech recognition engine: English, German, Spanish, French " +
                "(8-bit ONNX export by k2-fsa)",
            shipsIn = "Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "CC-BY-4.0",
            copyright = listOf(
                "Created by NVIDIA (https://huggingface.co/nvidia/canary-180m-flash). " +
                    "No copyright notice is supplied with the model.",
                "Modified by the k2-fsa / sherpa-onnx project: ONNX conversion and 8-bit weight quantization.",
            ),
            note = "Licensed under CC BY 4.0. The files are the ONNX conversion that the k2-fsa / " +
                "sherpa-onnx project publishes; it re-exports the weights and quantizes them to 8 bits, " +
                "without retraining. Provided as-is, without warranties. Transcription only; runs " +
                "on-device only.",
        ),
        Component(
            key = "nemotron-3.5-asr-streaming",
            name = "NVIDIA Nemotron 3.5 ASR Streaming 0.6B",
            role = "Streaming speech recognition engine for Hindi, Arabic and Vietnamese (int8 ONNX export by k2-fsa)",
            shipsIn = "Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "OpenMDW-1.1",
            copyright = listOf(
                "Nemotron 3.5 ASR Streaming 0.6B by NVIDIA " +
                    "(https://huggingface.co/nvidia/nemotron-3.5-asr-streaming-0.6b). " +
                    "NVIDIA publishes no copyright line; this is the attribution.",
                "Modified for this app: re-saved in ONNX Runtime's pre-optimised format (.ort, ONNX Runtime 1.28.0), " +
                    "the same graphs, without retraining (https://github.com/maxwhipw/marmalade-stt-engines, release v10).",
            ),
            note = "The 1120 ms streaming export, which the k2-fsa / sherpa-onnx project exported to " +
                "ONNX and quantized to int8 without retraining, re-saved as ONNX Runtime files for faster " +
                "loading (earlier installs keep k2-fsa's files). Runs on-device only.",
        ),
        Component(
            key = "granite-speech-5.0-470m-turboctc",
            name = "IBM Granite Speech 5.0 470M TurboCTC",
            role = "Developer English speech recognition engine (int8 ONNX export by qwertz92)",
            shipsIn = "Download (Engines screen, developer engines; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Created by IBM, the IBM Granite Speech Team " +
                    "(https://huggingface.co/ibm-granite/granite-speech-5.0-470m-turboctc). " +
                    "No copyright notice is supplied with the model.",
                "Modified by qwertz92: converted to ONNX and quantized to int8 " +
                    "(https://huggingface.co/qwertz92/granite-speech-5.0-470m-turboctc-onnx).",
            ),
            note = "Licensed under Apache-2.0; neither repository has a LICENSE file, so the text is " +
                "bundled here. The ONNX graph is a modified version of IBM's checkpoint: exported with " +
                "one operation rewritten to an equivalent and its weights quantized to int8, without " +
                "retraining. tokenizer.json is IBM's, unchanged. Runs on-device only.",
        ),
        Component(
            key = "qwen3-asr-0.6b",
            name = "Qwen3-ASR 0.6B",
            role = "Speech recognition engine for 30 languages, recommended for Chinese and Korean (int8 ONNX export)",
            shipsIn = "Download (Engines screen; from github.com/maxwhipw/marmalade-stt-engines)",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright 2026 Alibaba Cloud",
                "Copyright 2024 Alibaba Cloud",
                "Modified for this app: re-saved in ONNX Runtime's pre-optimised format (.ort, ONNX Runtime 1.28.0), " +
                    "the same graphs, without retraining (https://github.com/maxwhipw/marmalade-stt-engines, release v11).",
            ),
            note = "Alibaba Cloud's Qwen3-ASR, built on the Qwen3-0.6B language model (whose tokenizer " +
                "it uses). The files are a third-party ONNX export with int8 quantization " +
                "(Wasser1462), re-hosted unchanged by the k2-fsa / sherpa-onnx project, then re-saved " +
                "as ONNX Runtime files for faster loading (earlier installs keep those files); no " +
                "retraining. Runs on-device only.",
        ),

        Component(
            key = "androidx",
            name = "AndroidX / Compose / Kotlin / Hilt / DataStore", role = "App framework",
            shipsIn = "APK",
            licenseId = "Apache-2.0",
            copyright = listOf(
                "Copyright (c) The Android Open Source Project, JetBrains, and Google",
            ),
            note = "Includes Jetpack Navigation, Lifecycle, kotlinx.coroutines, " +
                "kotlinx.serialization (JSON), " +
                "Dagger, the Material icons drawn in-app, and their transitive " +
                "dependencies (Okio, Guava's listenablefuture, javax.inject / " +
                "jakarta.inject, JetBrains annotations, JSpecify — all " +
                "Apache-2.0; plus the jsr305 annotations (BSD)). Two of them " +
                "ship small native libraries: libandroidx.graphics.path.so and " +
                "libdatastore_shared_counter.so.",
        ),
        Component(
            key = "marmalade-ui-android",
            name = "Marmalade UI (marmalade-ui-android)",
            role = "The design library this app shares with Marmalade TTS: colours, surfaces, fonts, motion and cards",
            shipsIn = "APK (built from source, a git submodule)",
            licenseId = "MIT",
            copyright = listOf(
                "Copyright (c) 2026 marmalade-tts contributors",
                "Copyright (c) 2026 marmalade-ui-android contributors",
            ),
            textAsset = "marmalade-ui-android.txt",
            note = "Brings the Manrope and Momo Trust Display fonts below.",
        ),
        // Notices below verified against each bundled TTF's name table
        // (nameID 0), per the copyright-licensing skill.
        Component(
            key = "manrope",
            name = "Manrope", role = "Brand font (body / headings)",
            shipsIn = "APK",
            licenseId = "OFL-1.1",
            copyright = listOf(
                "Copyright 2019 The Manrope Project Authors " +
                    "(https://github.com/sharanda/manrope)",
            ),
        ),
        Component(
            key = "momo-trust-display",
            name = "Momo Trust Display", role = "Brand font (wordmark)",
            shipsIn = "APK",
            licenseId = "OFL-1.1",
            copyright = listOf(
                "Copyright 2024 The Momo Trust Project Authors " +
                    "(https://github.com/typeassociates/MomoTrustDisplay)",
            ),
        ),
        Component(
            key = "fredoka",
            name = "Fredoka", role = "Brand font (wordmark fallback)",
            shipsIn = "APK",
            licenseId = "OFL-1.1",
            copyright = listOf(
                "Copyright 2016 The Fredoka Project Authors " +
                    "(https://github.com/hafontia/Fredoka-One)",
            ),
        ),
    )

    /** Look up a [License] by id, or null if unknown. */
    fun licenseFor(id: String): License? = licenses.firstOrNull { it.id == id }

    /** Look up a [Component] by its [Component.key], or null if unknown. */
    fun componentFor(key: String): Component? = components.firstOrNull { it.key == key }

    /**
     * Components grouped by license, preserving [licenses] declaration order
     * for the groups and [components] order within each group. Only licenses
     * that actually have components are returned.
     */
    fun groupedByLicense(): List<Pair<License, List<Component>>> =
        licenses.mapNotNull { license ->
            val members = components.filter { it.licenseId == license.id }
            if (members.isEmpty()) null else license to members
        }
}
