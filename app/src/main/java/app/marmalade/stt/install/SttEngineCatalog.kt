package app.marmalade.stt.install

import androidx.annotation.StringRes
import app.marmalade.stt.R
import app.marmalade.stt.speech.FinishedUtterances
import app.marmalade.stt.textproc.Phantoms
import app.marmalade.stt.textproc.Recognizer

/**
 * Which recognizer family a catalog entry is. Decides how the speech layer
 * builds a sherpa-onnx config from the installed files, and which session
 * type runs it (ARCHITECTURE.md D4).
 */
enum class EngineFamily {
    /** Offline Whisper (encoder + decoder + tokens), run as simulated streaming over Silero VAD. */
    WHISPER,

    /** Offline NeMo transducer (Parakeet TDT: encoder + decoder + joiner + tokens), same session as Whisper. */
    NEMO_TRANSDUCER,

    /**
     * Offline NeMo CTC (Parakeet TDT_CTC 0.6B ja's CTC branch, #60): one
     * graph, its [FileRole.ENCODER], plus tokens. Same session as Whisper.
     */
    NEMO_CTC,

    /** Offline NeMo Canary (encoder + decoder + tokens), same session as Whisper. */
    CANARY,

    /** Offline Qwen3-ASR (conv frontend + encoder + LLM decoder + BPE tokenizer), same session as Whisper. */
    QWEN3_ASR,

    /**
     * True streaming NeMo transducer (Nemotron 3.5, cache-aware):
     * sherpa-onnx's OnlineRecognizer emits partials as audio arrives. Silero
     * VAD still decides where utterances start and end.
     */
    STREAMING_TRANSDUCER,

    /**
     * Offline CTC (IBM Granite Speech, #32): one ONNX graph run on ONNX
     * Runtime's Java API with the app's own front-end, not sherpa-onnx. The
     * graph is its [FileRole.ENCODER] and the vocabulary its [FileRole.TOKENS]
     * (`tokenizer.json`). Same session as Whisper.
     */
    GRANITE_CTC,
    ;

    /** True for the family that decodes audio as it arrives instead of re-decoding whole utterances. */
    val streaming: Boolean get() = this == STREAMING_TRANSDUCER

    /** Whose phantom phrases `:textproc` checks this family's text against (#57). */
    val recognizer: Recognizer
        get() = when (this) {
            WHISPER -> Recognizer.WHISPER
            QWEN3_ASR -> Recognizer.QWEN3
            CANARY -> Recognizer.CANARY
            NEMO_TRANSDUCER, NEMO_CTC, STREAMING_TRANSDUCER, GRANITE_CTC -> Recognizer.OTHER
        }
}

/** What a downloaded file is for. The speech layer looks files up by role, never by name. */
enum class FileRole {
    /** The encoder; for a single-graph model (NeMo CTC, Granite) the whole model. */
    ENCODER,
    DECODER,
    JOINER,

    /** The vocabulary: sherpa-onnx's `tokens.txt`, or Granite's `tokenizer.json`. */
    TOKENS,

    /** Qwen3-ASR's convolutional audio frontend. */
    CONV_FRONTEND,

    /** Qwen3-ASR's BPE tokenizer: vocab.json, merges.txt and tokenizer_config.json, side by side. */
    TOKENIZER_VOCAB,
    TOKENIZER_MERGES,
    TOKENIZER_CONFIG,
}

/**
 * One file of a model, downloaded on its own from a URL pinned to an
 * immutable revision and checked against [sha256] before it is kept.
 *
 * @property name file name inside the engine's install directory.
 */
data class ModelFile(
    val role: FileRole,
    val name: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
)

/**
 * The languages a user can pick for an engine that takes a language hint.
 *
 * @property codes BCP-47 language codes in display order. [AUTO] means the
 *   model detects the language itself.
 * @property default the code used until the user picks one.
 */
data class LanguageOptions(val codes: List<String>, val default: String) {
    init {
        require(default in codes) { "default $default is not one of $codes" }
    }

    companion object {
        /** Let the model detect the language. */
        const val AUTO = ""
    }
}

/**
 * One installable speech engine (a model plus the family that runs it).
 *
 * @property id stable key: install directory name, settings value, state key.
 * @property displayName the name users see, untranslated (#67): short and
 *   recognisable, with no underscores, no architecture jargon (TDT_CTC) and no
 *   "(English)" (the LANGUAGES spec says that). NOTICE.md and
 *   [app.marmalade.stt.data.LicenseCatalog] keep the upstream model name.
 * @property description the card's one-line summary, a string resource so
 *   it is translated (#51).
 * @property languages BCP-47 codes the model transcribes; `mul` for a long list.
 * @property licenseKey the model's [app.marmalade.stt.data.LicenseCatalog]
 *   component key, so the Engines screen can link to its license text.
 * @property developer hidden unless Settings → Developer engines is on. These
 *   are candidates Max benchmarks, not engines offered to everyone.
 * @property livePartials offline families only: re-decode the utterance in
 *   progress every 200 ms for live partials. False for models whose decode is
 *   too slow for that; they show text when each utterance ends.
 * @property languageOptions the per-engine language setting, or null for a
 *   model that takes no hint (English-only, or detects the language itself).
 * @property chunkMs streaming engines: the chunk the export was made for, so
 *   how often text arrives. sherpa-onnx bakes it into the ONNX files.
 * @property lowerCaseLatin the model writes Latin letters in lower case only, so
 *   cleanup puts short runs in capitals as acronyms ("fbi" → "FBI"; Parakeet ja, #60).
 * @property preSpeechPhantom text the model writes for the noise before speech
 *   (Parakeet ja's 「うん」, #60): dropped when it is a session's first text and
 *   more speech follows ([isPreSpeechPhantom]).
 * @property previousFiles the file set an older app version installed, when
 *   [files] changed format (#78: `.onnx` → pre-optimised `.ort`). An install
 *   whose record names exactly this set stays Installed and runs from these
 *   files ([ModelInstaller.installedModel]); nothing downloads them any more.
 *   Set it with [SttEngineCatalog.replacingFiles].
 * @property descriptionText the card's summary as plain text, for engines from
 *   [ExtraEngines]' manifest, which has no string resource; wins over [description].
 */
data class SttEngineDescriptor(
    val id: String,
    val displayName: String,
    @StringRes val description: Int,
    val family: EngineFamily,
    val languages: List<String>,
    val licenseId: String,
    val licenseKey: String,
    val files: List<ModelFile>,
    val developer: Boolean = false,
    val livePartials: Boolean = true,
    val languageOptions: LanguageOptions? = null,
    val chunkMs: Int? = null,
    val lowerCaseLatin: Boolean = false,
    val preSpeechPhantom: String? = null,
    val previousFiles: List<ModelFile> = emptyList(),
    val descriptionText: String? = null,
) {
    /** Total download size, which is also the installed size (files are stored as downloaded). */
    val sizeBytes: Long get() = files.sumOf { it.sizeBytes }

    fun file(role: FileRole): ModelFile = files.first { it.role == role }

    /**
     * [text] is what this model writes for the noise before speech: [preSpeechPhantom], or
     * its family's pre-speech phrases (#57: Whisper's distinctive phrases and 「おわり」,
     * Qwen3's 「システム」). A session holds it back when it comes first and drops it when
     * speech follows ([FinishedUtterances]).
     */
    fun isPreSpeechPhantom(text: String): Boolean =
        FinishedUtterances.isPhantom(text, preSpeechPhantom) || Phantoms.isPreSpeechPhantom(text, family.recognizer)
}

/**
 * Every engine the app can install (ARCHITECTURE.md D4, D5). The user
 * engines (Max, 2026-10-02, #45), in list order: Parakeet Ultra (the main
 * engine, 25 European languages), Parakeet TDT_CTC 110M (the small English
 * option, #25), Parakeet TDT_CTC 0.6B ja (Japanese, #60), Canary 180M
 * Flash (tiny; English, Spanish, German and French), Nemotron 3.5
 * Streaming (text as you speak; Hindi, Arabic and Vietnamese since #37),
 * and Whisper Large v3 Turbo (Whisper's ~100 languages, for those nothing
 * else covers), and Qwen3-ASR (its 30 languages since #88, Max 2026-10-17;
 * Chinese and Korean from #50). Granite is a developer engine (#13) for
 * benchmarking.
 * A new entry needs a model-ethics review (`docs/model-reviews/`) and a
 * license entry in NOTICE.md and [app.marmalade.stt.data.LicenseCatalog].
 *
 * Every file is a release asset of our `marmalade-stt-engines` GitHub repo
 * ([ENGINES_REPO], D5), pinned by tag and sha256: one release per engine, and a
 * published tag's assets are never replaced; new files get a new tag (#39).
 * Files we convert ourselves (Parakeet Ultra, `v1`) are made there; the others
 * (`v2` to `v8`, #75) are third-party int8 exports re-hosted byte for byte from
 * the Hugging Face commit NOTICE.md names for each, so no download goes to
 * Hugging Face (Max, 2026-10-08). Those exports are k2-fsa's (Fangjun Kuang
 * publishes them as `csukuangfj` and `csukuangfj2`), apart from Granite's
 * (qwertz92). Parakeet 110M's exist only as a tarball in sherpa-onnx's
 * `asr-models` GitHub release; its files came from a mirror holding the same
 * bytes (sha256 checked against the extracted tarball, #25).
 */
object SttEngineCatalog {

    /**
     * The base of Whisper turbo's language hints: auto-detect plus the major
     * languages. Short on purpose. Nemotron 3.5 and Qwen3-ASR (#88) list
     * their own.
     */
    private val MULTILINGUAL_HINTS = listOf(
        LanguageOptions.AUTO, "en", "de", "es", "fr", "it", "pt", "nl", "pl", "ru", "ja", "ko", "zh",
    )

    /**
     * Whisper turbo also takes the hint for the languages it shares only with
     * Nemotron 3.5 (#37): hinted, its FLEURS Hindi WER is 31 instead of 45 on
     * auto-detect; the others gain little but lose nothing. SherpaBackends
     * maps "nb" to Whisper's "no".
     */
    private val WHISPER_TURBO_HINTS = MULTILINGUAL_HINTS + listOf("hi", "ar", "tr", "vi", "nb")

    /**
     * Nemotron 3.5 takes a hint for each of its languages (prompt dictionary
     * keys, #27; `NEMOTRON_LANGUAGE_KEYS` in SherpaBackends maps each to its locale).
     */
    private val NEMOTRON_35_HINTS = listOf(LanguageOptions.AUTO) + EngineLanguages.NEMOTRON_35.sorted()

    /** Qwen3-ASR takes a hint for each of its 30 languages (#88; `QWEN3_LANGUAGE_NAMES` in SherpaBackends names each). */
    private val QWEN3_HINTS = listOf(LanguageOptions.AUTO) + EngineLanguages.QWEN3.sorted()

    /** Our engines repo (D5): every engine's files are release assets there, ours (#39) and re-hosted ones (#75). */
    private const val ENGINES_REPO = "maxwhipw/marmalade-stt-engines"

    private fun githubRelease(repo: String, tag: String) = "https://github.com/$repo/releases/download/$tag"

    /** A release asset of [ENGINES_REPO], named `<engine id>-<name>` there and installed as [name]. */
    private fun asset(role: FileRole, base: String, engineId: String, name: String, sha256: String, size: Long) =
        ModelFile(role, name, "$base/$engineId-$name", sha256, size)

    /**
     * Every engine the app can install: the built-in ones, then those from
     * [ExtraEngines]' manifest (norske-modeller.json in this fork's releases).
     */
    val engines: List<SttEngineDescriptor> get() = builtIn + ExtraEngines.engines.value

    /** Ids an [ExtraEngines] entry may not take. */
    internal val builtInIds: Set<String> by lazy { builtIn.map { it.id }.toSet() }

    private val builtIn: List<SttEngineDescriptor> = listOf(
        // ---- User engines, everyday first ----------------------------------------
        // Moondream's post-trained Parakeet v3 (#25): v3's graph and vocabulary with Moondream's
        // weights, converted by tools/parakeet-ultra/convert.py and hosted on ENGINES_REPO (#39).
        // The main engine (#45). v3 returned empty text when >= 0.3 s of silence came right before
        // an abrupt, mid-word start (#15); Ultra never did on the same clips (#25). Before changing
        // the offline pre-roll, run `python -m asrbench.regress` (tools/asr-bench).
        githubRelease(ENGINES_REPO, "v1").let { base ->
            SttEngineDescriptor(
                id = PARAKEET_ULTRA,
                displayName = "Parakeet Ultra",
                description = R.string.engine_desc_parakeet_ultra,
                family = EngineFamily.NEMO_TRANSDUCER,
                languages = listOf("mul"),
                licenseId = "CC-BY-4.0",
                licenseKey = "parakeet-ultra",
                files = listOf(
                    asset(FileRole.ENCODER, base, PARAKEET_ULTRA, "encoder.int8.onnx", "7ef3ca4ffb49d60d4faa757b2930c8cec9927e73c24f6155d5b23a00229a7ed6", 652_282_526L),
                    asset(FileRole.DECODER, base, PARAKEET_ULTRA, "decoder.int8.onnx", "1fab98fe6c12aded87d2da66272cc9e148d0a0044ce3850a12fe56302ec4a922", 11_845_274L),
                    asset(FileRole.JOINER, base, PARAKEET_ULTRA, "joiner.int8.onnx", "8ba94c6919c17a6bd27368fb89533628a72ffc81d4acedebf3cdcb2cae331dbb", 6_355_277L),
                    // Parakeet v3's vocabulary, unchanged.
                    asset(FileRole.TOKENS, base, PARAKEET_ULTRA, "tokens.txt", "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d", 93_939L),
                ),
            )
        },
        // The TDT head of NVIDIA's TDT_CTC 110M, the small option (#25, #27). Its CTC head (another
        // k2-fsa export) was behind on every host set at the same speed (#25). Its source: see the object's doc.
        githubRelease(ENGINES_REPO, "v2").let { base ->
            SttEngineDescriptor(
                id = "parakeet-tdt-110m-en",
                displayName = "Parakeet 110M",
                description = R.string.engine_desc_parakeet_110m,
                family = EngineFamily.NEMO_TRANSDUCER,
                languages = listOf("en"),
                licenseId = "CC-BY-4.0",
                licenseKey = "parakeet-tdt_ctc-110m",
                files = listOf(
                    asset(FileRole.ENCODER, base, "parakeet-tdt-110m-en", "encoder.int8.onnx", "0f35509ddeb9b39002fb077d979a9fe74f06eb0bc4dd5c34f512f82e5111d657", 131_113_202L),
                    asset(FileRole.DECODER, base, "parakeet-tdt-110m-en", "decoder.int8.onnx", "f7c331c5504c2e593c76ed22b728e3f554af6c4a383dde862e719ced08b1da19", 3_955_863L),
                    asset(FileRole.JOINER, base, "parakeet-tdt-110m-en", "joiner.int8.onnx", "bf7dff69e9f2cdbe9943d70da358f38b361c115ba0105bae7e908e0d6ec782f6", 1_411_403L),
                    asset(FileRole.TOKENS, base, "parakeet-tdt-110m-en", "tokens.txt", "450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10", 9_953L),
                ),
            )
        }.replacingFiles(
            // Pre-optimised ONNX Runtime files (#78): the same graphs, loading 31-37 % faster on a Pixel 6a.
            githubRelease(ENGINES_REPO, "v9").let { base ->
                listOf(
                    asset(FileRole.ENCODER, base, "parakeet-tdt-110m-en", "encoder.int8.ort", "fe0bab3304a93108bea8f5a63b64f4b422679a95da815ec400ecb4fd704a404f", 131_508_352L),
                    asset(FileRole.DECODER, base, "parakeet-tdt-110m-en", "decoder.int8.ort", "6486b1f9ba5555985e1dd5c1c61a17c7267a73054116676b367ca4d30a15fe5a", 3_962_088L),
                    asset(FileRole.JOINER, base, "parakeet-tdt-110m-en", "joiner.int8.ort", "87ac4bf5573c73887d33d1c1532451f328597d79b3e63c4874e2baae6d187bb2", 1_418_128L),
                    asset(FileRole.TOKENS, base, "parakeet-tdt-110m-en", "tokens.txt", "450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10", 9_953L),
                )
            },
        ),
        // NVIDIA's Japanese Parakeet, the Japanese engine (#60): the most accurate on real Japanese
        // speech at about Ultra's cost, a tenth of turbo's (docs/benchmarks/2026-10-japanese.md, #52).
        // k2-fsa exports only its CTC branch. Trained on ReazonSpeech (Japanese TV), which Max
        // accepted on 2026-10-03 (docs/model-reviews/reazonspeech-japanese.md).
        githubRelease(ENGINES_REPO, "v3").let { base ->
            SttEngineDescriptor(
                id = PARAKEET_JA,
                displayName = "Parakeet Japanese",
                description = R.string.engine_desc_parakeet_ja,
                family = EngineFamily.NEMO_CTC,
                languages = listOf("ja"),
                licenseId = "CC-BY-4.0",
                licenseKey = "parakeet-tdt_ctc-0.6b-ja",
                // Its vocabulary has Latin only as lower-case letters, and it writes 「うん」
                // for the noise before speech in 28 of 200 FLEURS-ja clips (#60).
                lowerCaseLatin = true,
                preSpeechPhantom = "うん",
                files = listOf(
                    asset(FileRole.ENCODER, base, PARAKEET_JA, "model.int8.onnx", "3addd00ef5bd1742078389e540b77394e4a508bdf2f4c9ad1b4a76d93e76598e", 655_542_604L),
                    asset(FileRole.TOKENS, base, PARAKEET_JA, "tokens.txt", "732f64c53909f2620c713f4106b487d92e6f54a6915b3cd3d1dbd32f9f4f392a", 28_557L),
                ),
            )
        },
        githubRelease(ENGINES_REPO, "v4").let { base ->
            SttEngineDescriptor(
                id = "canary-180m-flash",
                displayName = "Canary 180M Flash",
                description = R.string.engine_desc_canary_180m,
                family = EngineFamily.CANARY,
                languages = listOf("en", "es", "de", "fr"),
                licenseId = "CC-BY-4.0",
                licenseKey = "canary-180m-flash",
                languageOptions = LanguageOptions(listOf("en", "es", "de", "fr"), "en"),
                files = listOf(
                    asset(FileRole.ENCODER, base, "canary-180m-flash", "encoder.int8.onnx", "7a75b4e2a5857a6dcc0819503bbe3fad66943db4a3ccf21d3f27c633667d303f", 132_678_643L),
                    asset(FileRole.DECODER, base, "canary-180m-flash", "decoder.int8.onnx", "e41a2ab9c0c2fe81a1e8ade5a45fb02a74bc4db7d1f91b89a54a25e2cf79cba2", 74_437_848L),
                    asset(FileRole.TOKENS, base, "canary-180m-flash", "tokens.txt", "2dae6fc7815f9640645e0c765522b278ee0cef49b482d91f6913e334628d3e77", 53_555L),
                ),
            )
        },
        // NVIDIA's multilingual streaming model, kept for Hindi, Arabic and Vietnamese only (Max,
        // 2026-10-02, #37: docs/benchmarks/2026-10-nemotron-decision.md); Whisper turbo beats it on the
        // rest. sherpa-onnx fixes the chunk at export: this is the 1120 ms export, as accurate as the
        // 560 ms one or better on every set and about 40 % cheaper to run. The 320 ms (#45) and 560 ms
        // (#37) exports left the app.
        githubRelease(ENGINES_REPO, "v5").let { base ->
            SttEngineDescriptor(
                id = NEMOTRON_35_1120MS,
                displayName = "Nemotron 3.5",
                description = R.string.engine_desc_nemotron_35,
                family = EngineFamily.STREAMING_TRANSDUCER,
                languages = listOf("mul"),
                licenseId = "OpenMDW-1.1",
                licenseKey = "nemotron-3.5-asr-streaming",
                languageOptions = LanguageOptions(NEMOTRON_35_HINTS, LanguageOptions.AUTO),
                chunkMs = 1120,
                files = listOf(
                    asset(FileRole.ENCODER, base, NEMOTRON_35_1120MS, "encoder.int8.onnx", "2fff2166acaa535bd969fb223c1f0783d71029f143cb298bc54c2afe85abf772", 657_601_521L),
                    asset(FileRole.DECODER, base, NEMOTRON_35_1120MS, "decoder.int8.onnx", "19f9c98fc6d0a2c33a65a43b36fdb2e914c26c0aa9764be3aebc502a1e982fb0", 14_978_075L),
                    asset(FileRole.JOINER, base, NEMOTRON_35_1120MS, "joiner.int8.onnx", "4101c7c679a0bc30483794b27a059e34e79232aa2068d78d51231a22c8b0d7ce", 9_504_438L),
                    asset(FileRole.TOKENS, base, NEMOTRON_35_1120MS, "tokens.txt", "729cc103155bafa785f9cd45746cd41cabe97eab7182fc04d594129587958f8a", 131_440L),
                ),
            )
        }.replacingFiles(
            // Pre-optimised ONNX Runtime files (#78): the same graphs, loading 24-50 % faster on a Pixel 6a.
            githubRelease(ENGINES_REPO, "v10").let { base ->
                listOf(
                    asset(FileRole.ENCODER, base, NEMOTRON_35_1120MS, "encoder.int8.ort", "804929d57aa6a661fee7ff9ea6eeecb4c544b995cc40c1338b496c4931737eb6", 657_662_096L),
                    asset(FileRole.DECODER, base, NEMOTRON_35_1120MS, "decoder.int8.ort", "6159f5af06499469a32d47e57115a8de37e4708c5e9ed61bf694926df50b3571", 14_986_960L),
                    asset(FileRole.JOINER, base, NEMOTRON_35_1120MS, "joiner.int8.ort", "9cf237a2533855cb987b1d4a4d12379834cd1749c25e6d63bbac353c7a7bd3df", 9_511_160L),
                    asset(FileRole.TOKENS, base, NEMOTRON_35_1120MS, "tokens.txt", "729cc103155bafa785f9cd45746cd41cabe97eab7182fc04d594129587958f8a", 131_440L),
                )
            },
        ),
        // For the languages nothing above covers (#45).
        githubRelease(ENGINES_REPO, "v6").let { base ->
            SttEngineDescriptor(
                id = "whisper-turbo",
                displayName = "Whisper Large v3 Turbo",
                description = R.string.engine_desc_whisper_turbo,
                family = EngineFamily.WHISPER,
                languages = listOf("mul"),
                licenseId = "MIT",
                licenseKey = "openai-whisper",
                // Turbo's decoder is too slow to re-run five times a second.
                livePartials = false,
                languageOptions = LanguageOptions(WHISPER_TURBO_HINTS, LanguageOptions.AUTO),
                files = listOf(
                    asset(FileRole.ENCODER, base, "whisper-turbo", "turbo-encoder.int8.onnx", "b02dcdf54f348741e93fe732b67d933c8dcb6735655f710640143081db38878b", 674_716_297L),
                    asset(FileRole.DECODER, base, "whisper-turbo", "turbo-decoder.int8.onnx", "20accd02388482eb3a46bd615631adfdc85e1eb2c7db9ea3f02a40ffe6b81547", 361_080_764L),
                    asset(FileRole.TOKENS, base, "whisper-turbo", "turbo-tokens.txt", "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126", 816_730L),
                ),
            )
        },

        // ---- Developer engines: IBM ---------------------------------------------
        // The community int8 export (#26 checked its hash, WER and speed against our own export,
        // `tools/asr-bench/granite/export_onnx.py`): Apache-2.0, with its conversion scripts.
        githubRelease(ENGINES_REPO, "v7").let { base ->
            SttEngineDescriptor(
                id = "granite-speech-5-470m",
                displayName = "Granite Speech",
                description = R.string.engine_desc_granite,
                family = EngineFamily.GRANITE_CTC,
                languages = listOf("en"),
                licenseId = "Apache-2.0",
                licenseKey = "granite-speech-5.0-470m-turboctc",
                developer = true,
                files = listOf(
                    asset(FileRole.ENCODER, base, "granite-speech-5-470m", "model_int8.onnx", "8173a50ea67f864801971a40622a2e5c7d62fd230912bde94b745f92c74d60e9", 551_294_349L),
                    asset(FileRole.TOKENS, base, "granite-speech-5-470m", "tokenizer.json", "3ee80b02f0119a040a70eb909c20fac8271c173d7e71d195a3b35f77780061e6", 1_137_492L),
                ),
            )
        },

        // ---- Qwen: a user engine for its 30 languages (#88), recommended for Chinese and Korean ----
        githubRelease(ENGINES_REPO, "v8").let { base ->
            SttEngineDescriptor(
                id = QWEN3,
                displayName = "Qwen3-ASR",
                description = R.string.engine_desc_qwen3,
                family = EngineFamily.QWEN3_ASR,
                languages = listOf("mul"),
                licenseId = "Apache-2.0",
                licenseKey = "qwen3-asr-0.6b",
                livePartials = false,
                languageOptions = LanguageOptions(QWEN3_HINTS, LanguageOptions.AUTO),
                files = listOf(
                    asset(FileRole.CONV_FRONTEND, base, QWEN3, "conv_frontend.onnx", "d22dc4423e0940e49884e903d2ea2f7e5567c14fc1aed97e4e26d6b8f208ef9e", 44_148_281L),
                    asset(FileRole.ENCODER, base, QWEN3, "encoder.int8.onnx", "60748d3e6744a57c9c91e1b17424a6c2990567e8adceb0783940c03ed98fa9d9", 182_491_662L),
                    asset(FileRole.DECODER, base, QWEN3, "decoder.int8.onnx", "4f6885be5959ae26af3089d38ee7972c5fafbeeb1cf8d5e76eab6d8b61ca5771", 755_914_231L),
                    asset(FileRole.TOKENIZER_VOCAB, base, QWEN3, "vocab.json", "ca10d7e9fb3ed18575dd1e277a2579c16d108e32f27439684afa0e10b1440910", 2_776_833L),
                    asset(FileRole.TOKENIZER_MERGES, base, QWEN3, "merges.txt", "8831e4f1a044471340f7c0a83d7bd71306a5b867e95fd870f74d0c5308a904d5", 1_671_853L),
                    asset(FileRole.TOKENIZER_CONFIG, base, QWEN3, "tokenizer_config.json", "4942d005604266809309cabc9f4e9cb89ce855d59b14681fdc0e1cc62ea26c4c", 12_487L),
                ),
            )
        }.replacingFiles(
            // Pre-optimised ONNX Runtime files (#78): the same graphs, loading 20 % faster on a Pixel 6a.
            githubRelease(ENGINES_REPO, "v11").let { base ->
                listOf(
                    asset(FileRole.CONV_FRONTEND, base, QWEN3, "conv_frontend.ort", "a4e120496b7b66f452303121def15c3204c4042af2523e08dad7486fec216f71", 44_167_416L),
                    asset(FileRole.ENCODER, base, QWEN3, "encoder.int8.ort", "3cbaab59db7ec7900a57aee077fd9213fa48624ca8205bfba289ba76740bd750", 182_684_104L),
                    asset(FileRole.DECODER, base, QWEN3, "decoder.int8.ort", "2c56875df87054decd46e08eff5f46920960c989ddeb641becd6809d34392cff", 756_556_392L),
                    asset(FileRole.TOKENIZER_VOCAB, base, QWEN3, "vocab.json", "ca10d7e9fb3ed18575dd1e277a2579c16d108e32f27439684afa0e10b1440910", 2_776_833L),
                    asset(FileRole.TOKENIZER_MERGES, base, QWEN3, "merges.txt", "8831e4f1a044471340f7c0a83d7bd71306a5b867e95fd870f74d0c5308a904d5", 1_671_853L),
                    asset(FileRole.TOKENIZER_CONFIG, base, QWEN3, "tokenizer_config.json", "4942d005604266809309cabc9f4e9cb89ce855d59b14681fdc0e1cc62ea26c4c", 12_487L),
                )
            },
        ),
    )

    const val PARAKEET_ULTRA = "parakeet-ultra"
    const val PARAKEET_JA = "parakeet-ja"
    const val QWEN3 = "qwen3-asr-0-6b"

    /** Nemotron 3.5 Streaming, the 1120 ms export: the id stored settings use, so it stays. */
    const val NEMOTRON_35_1120MS = "nemotron-3-5-streaming-1120ms"

    /**
     * The engine that runs when nothing else can: no pick stored, or a pick
     * the user can no longer see. Parakeet Ultra, the main engine (#45). The
     * setup guide installs and picks the engine [EngineRecommender] chooses
     * for the phone's language and hardware, which is often not this one.
     */
    val default: SttEngineDescriptor get() = checkNotNull(byId(PARAKEET_ULTRA))

    fun byId(id: String): SttEngineDescriptor? = engines.firstOrNull { it.id == id }

    /**
     * This engine with [files] as its download, its files until now kept as
     * [SttEngineDescriptor.previousFiles] (#78), so phones holding them keep
     * running them without a re-download. Appended to an entry when its files
     * change format, the old list left as it was:
     * `githubRelease(ENGINES_REPO, "v1").let { … }.replacingFiles(githubRelease(ENGINES_REPO, "v9").let { base -> listOf(asset(…), …) })`.
     * The new files get a new tag (a published tag's assets are never
     * replaced). Only one previous set is kept: a second change drops the
     * oldest, whose installs then show as outdated and download again.
     */
    internal fun SttEngineDescriptor.replacingFiles(files: List<ModelFile>): SttEngineDescriptor =
        copy(files = files, previousFiles = this.files)

    /**
     * The engines to list: every user (non-developer) engine, plus the
     * developer engines when [showDeveloper] is on (Settings → Developer
     * engines). Catalog order.
     */
    fun visibleTo(showDeveloper: Boolean): List<SttEngineDescriptor> =
        engines.filter { runnable(it, showDeveloper) }

    /**
     * Whether [engine] runs when it is the stored choice: a user engine, or
     * any engine with Developer engines on. The recommender picks only user
     * engines, so no exception is needed for the recommended one (#89).
     */
    fun runnable(engine: SttEngineDescriptor, showDeveloper: Boolean): Boolean = showDeveloper || !engine.developer

    /**
     * The engine that actually runs for a stored choice: [selectedId] if it is
     * a known engine that is [runnable], otherwise the engine recommended for
     * this phone ([recommendedId]), otherwise [default]. So turning Developer
     * engines off falls back without forgetting the choice, and a phone
     * recommended Nemotron doesn't fall back to an uninstalled Parakeet when
     * the setup guide was left before the download finished.
     */
    fun effective(selectedId: String?, showDeveloper: Boolean, recommendedId: String? = null): SttEngineDescriptor =
        selectedId?.let(::byId)?.takeIf { runnable(it, showDeveloper) }
            ?: recommendedId?.let(::byId)?.takeIf { runnable(it, showDeveloper) }
            ?: default
}
