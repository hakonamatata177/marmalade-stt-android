package app.marmalade.stt.install

import android.app.Application
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** [ExtraEngines.parse] on the published manifest (a copy) and on entries it must skip. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ExtraEnginesTest {

    private val published: String =
        checkNotNull(javaClass.getResource("/extra-engines/norske-modeller.json")).readText()

    @Test
    fun readsThePublishedManifest() {
        val engines = ExtraEngines.parse(published)
        assertEquals(listOf("nb-whisper-tiny", "nb-whisper-base", "nb-whisper-small"), engines.map { it.id })
        val small = engines.last()
        assertEquals("NB-Whisper Small", small.displayName)
        assertEquals(EngineFamily.WHISPER, small.family)
        assertEquals(listOf("nb", "nn"), small.languages)
        assertEquals(LanguageOptions(listOf("nb", "nn"), "nb"), small.languageOptions)
        assertEquals("nb-whisper", small.licenseKey)
        assertTrue(small.descriptionText!!.isNotBlank())
        assertEquals(setOf(FileRole.ENCODER, FileRole.DECODER, FileRole.TOKENS), small.files.map { it.role }.toSet())
        assertTrue(small.files.all { it.url.startsWith("https://github.com/") && it.sha256.length == 64 })
        // Norwegian requests are routed to it.
        assertTrue(EngineLanguages.supports(small, "nb-NO"))
        assertTrue(EngineLanguages.supports(small, "no"))
    }

    @Test
    fun skipsBadEntriesAndKeepsTheRest() {
        val root = JSONObject(published)
        val list = root.getJSONArray("engines")
        fun copyOf(i: Int) = JSONObject(list.getJSONObject(i).toString())
        list.put(copyOf(0).put("id", "../evil"))
        list.put(copyOf(0).put("id", SttEngineCatalog.PARAKEET_ULTRA))
        list.put(copyOf(0).put("id", "granite-like").put("family", "GRANITE_CTC"))
        list.put(copyOf(0).put("id", "no-decoder").also { o -> o.getJSONArray("files").remove(1) })
        list.put(copyOf(0).put("id", "http-file").also { o ->
            o.getJSONArray("files").getJSONObject(0).put("url", "http://example.com/encoder.onnx")
        })
        list.put(copyOf(0).put("id", "bad-name").also { o ->
            o.getJSONArray("files").getJSONObject(0).put("name", "../encoder.onnx")
        })
        list.put(copyOf(0)) // duplicate id
        val engines = ExtraEngines.parse(root.toString())
        assertEquals(listOf("nb-whisper-tiny", "nb-whisper-base", "nb-whisper-small"), engines.map { it.id })
    }

    @Test(expected = JSONException::class)
    fun notJsonThrows() {
        ExtraEngines.parse("<html>captive portal</html>")
    }
}
