package app.marmalade.stt.install

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Engines listed in a manifest outside the app, on top of [SttEngineCatalog]'s
 * built-in ones: `norske-modeller.json` in this fork's `modeller` release,
 * made by `tools/norsk/` and `.github/workflows/norske-modeller.yml` (NB-Whisper
 * converted to sherpa-onnx int8). A new model there shows up in the app
 * without a new build.
 *
 * The last manifest that parsed is kept in `filesDir` and [load]ed at startup,
 * before anything looks an engine up: `ModelInstaller.removeUnknownModels`
 * deletes the files of engines it can't find. [refresh] fetches a new one
 * (the Engines screen does, when it opens); a manifest that can't be fetched
 * or read leaves the kept one as it is. Each entry goes through the same
 * per-file sha256 check as the built-in engines when it downloads.
 */
object ExtraEngines {

    const val MANIFEST_URL =
        "https://github.com/hakonamatata177/marmalade-stt-android/releases/download/modeller/norske-modeller.json"

    private const val TAG = "ExtraEngines"
    private const val FILE = "extra-engines.json"
    private const val MAX_MANIFEST_BYTES = 1 shl 20

    /** An id is the install directory's name: no path characters. */
    private val ID = Regex("^[a-z0-9][a-z0-9._-]{0,63}$")
    private val FILE_NAME = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
    private val SHA256 = Regex("^[0-9a-f]{64}$")

    /** The families a manifest may use, with the files each needs (see `offlineModelConfig`). */
    private val REQUIRED_ROLES = mapOf(
        EngineFamily.WHISPER to setOf(FileRole.ENCODER, FileRole.DECODER, FileRole.TOKENS),
        EngineFamily.NEMO_TRANSDUCER to setOf(FileRole.ENCODER, FileRole.DECODER, FileRole.JOINER, FileRole.TOKENS),
        EngineFamily.NEMO_CTC to setOf(FileRole.ENCODER, FileRole.TOKENS),
    )

    private val _engines = MutableStateFlow<List<SttEngineDescriptor>>(emptyList())

    /** The engines of the kept manifest, in its order. */
    val engines: StateFlow<List<SttEngineDescriptor>> = _engines.asStateFlow()

    /** Read the kept manifest. Cheap (one small file); called from `Application.onCreate`. */
    fun load(context: Context) {
        val file = File(context.filesDir, FILE)
        if (!file.isFile) return
        try {
            _engines.value = parse(file.readText())
        } catch (e: IOException) {
            Log.w(TAG, "Couldn't read the kept manifest", e)
        } catch (e: JSONException) {
            Log.w(TAG, "The kept manifest doesn't parse", e)
        }
    }

    /**
     * Fetch the manifest and keep it if it parses. True when the list of
     * engines changed.
     */
    suspend fun refresh(context: Context, fetcher: HttpFetcher = UrlHttpFetcher()): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = fetcher.openRange(MANIFEST_URL, 0L).stream.use { readCapped(it) }
            val parsed = parse(text)
            if (parsed == _engines.value) return@withContext false
            val file = File(context.filesDir, FILE)
            val tmp = File(context.filesDir, "$FILE.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) throw IOException("Couldn't replace $file")
            _engines.value = parsed
            Log.i(TAG, "Manifest: ${parsed.joinToString { it.id }}")
            true
        } catch (e: IOException) {
            Log.w(TAG, "Couldn't fetch the manifest", e)
            false
        } catch (e: JSONException) {
            Log.w(TAG, "The fetched manifest doesn't parse", e)
            false
        }
    }

    /**
     * The engines in [json]. An entry that is malformed, names an unsupported
     * family, lacks a file its family needs or takes a built-in id is skipped
     * (logged); a manifest that isn't JSON throws.
     */
    internal fun parse(json: String): List<SttEngineDescriptor> {
        val list = JSONObject(json).getJSONArray("engines")
        val seen = mutableSetOf<String>()
        return list.objects().mapNotNull { o ->
            try {
                descriptor(o).takeIf { it.id !in SttEngineCatalog.builtInIds && seen.add(it.id) }
            } catch (e: JSONException) {
                Log.w(TAG, "Skipping a manifest entry: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Skipping a manifest entry: ${e.message}")
                null
            }
        }
    }

    private fun descriptor(o: JSONObject): SttEngineDescriptor {
        val id = o.getString("id")
        require(ID.matches(id)) { "bad id $id" }
        val family = EngineFamily.valueOf(o.getString("family"))
        val required = requireNotNull(REQUIRED_ROLES[family]) { "$id: family $family isn't supported here" }
        val files = o.getJSONArray("files").objects().map { f ->
            val name = f.getString("name")
            val url = f.getString("url")
            val sha256 = f.getString("sha256").lowercase()
            val size = f.getLong("size")
            require(FILE_NAME.matches(name) && ".." !in name) { "$id: bad file name $name" }
            require(url.startsWith("https://")) { "$id: $name isn't https" }
            require(SHA256.matches(sha256)) { "$id: $name has no sha256" }
            require(size > 0) { "$id: $name has no size" }
            ModelFile(FileRole.valueOf(f.getString("role")), name, url, sha256, size)
        }
        require(files.map { it.role }.toSet().containsAll(required)) { "$id: missing one of $required" }
        require(files.map { it.name }.toSet().size == files.size) { "$id: two files share a name" }
        val languages = o.getJSONArray("languages").strings().map(EngineLanguages::normalize)
        val options = o.optJSONObject("languageOptions")?.let {
            LanguageOptions(it.getJSONArray("codes").strings(), it.getString("default"))
        }
        val name = o.getString("name")
        return SttEngineDescriptor(
            id = id,
            displayName = name,
            // Never shown: descriptionText wins. A real resource so lint has nothing to flag.
            description = android.R.string.unknownName,
            descriptionText = o.optString("description").ifEmpty { name },
            family = family,
            languages = languages,
            licenseId = o.optString("license").ifEmpty { "?" },
            licenseKey = o.optString("licenseKey"),
            files = files,
            livePartials = o.optBoolean("livePartials", true),
            languageOptions = options,
        )
    }

    private fun readCapped(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > MAX_MANIFEST_BYTES) throw IOException("Manifest over $MAX_MANIFEST_BYTES bytes")
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
