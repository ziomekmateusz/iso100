package io.github.ziomekmateusz.iso100.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class CatalogStoreTest {
    // the file the app ships, built by tools/ (unit tests run in the module directory)
    private val bundled = File("src/main/assets/catalog.json").readText()

    private val small = """
        {"schemaVersion": 1, "version": "test", "future": true,
         "formats": [{"id": "full-frame", "name": "Full Frame", "widthMm": 36, "heightMm": 24},
                     {"id": "aps-c", "name": "APS-C", "widthMm": 23.5, "heightMm": 15.6}],
         "cameras": [{"id": "sony-alpha-7-iii", "maker": "Sony", "model": "Alpha 7 III", "format": "full-frame",
                      "aliases": ["ILCE-7M3"], "specs": {"resolutionMp": 24.2}},
                     {"id": "sony-alpha-77-ii", "maker": "Sony", "model": "Alpha 77 II", "format": "aps-c"},
                     {"id": "nikon-z-6", "maker": "Nikon", "model": "Z 6", "format": "full-frame"}]}
    """.trimIndent()

    private fun catalog(text: String = small) = (parseCatalog(text) as CatalogState.Ready).catalog

    @Test
    fun atTheFirstStartTheStoreHasEveryCameraOfTheBundledCatalog() = runTest {
        val store = CatalogStore({ bundled }, MutableStateFlow(Settings()), backgroundScope)
        val ready = store.state.first { it != CatalogState.Loading } as CatalogState.Ready
        val expected = Json.parseToJsonElement(bundled).jsonObject.getValue("cameras").jsonArray.size
        assertEquals(expected, ready.catalog.cameras.size)
        assertTrue(ready.catalog.cameras.all { ready.catalog.format(it.format) != null })
    }

    @Test
    fun searchMatchesMakerModelAndAliasesIgnoringCase() {
        val c = catalog()
        assertEquals(listOf("sony-alpha-7-iii"), c.search("ilce-7m3").map { it.id })
        assertEquals(listOf("sony-alpha-7-iii", "sony-alpha-77-ii"), c.search("alpha 7").map { it.id })
        assertEquals(listOf("nikon-z-6"), c.search("nikon z 6").map { it.id })
        assertEquals(3, c.search(" ").size)
    }

    @Test
    fun searchWordsMatchTheStartOfAWord() {
        val c = catalog()
        // "alpha 7" must not find an Alpha 37: the 7 is inside a word there
        assertEquals(emptyList<String>(), c.search("lpha").map { it.id })
        assertEquals(listOf("sony-alpha-7-iii"), c.search("7m3").map { it.id })
    }

    @Test
    fun theSelectionFollowsTheSettings() {
        val c = catalog()
        val camera = c.selection(Settings(cameraId = "sony-alpha-7-iii"))!!
        assertEquals("full-frame", camera.format.id)
        assertEquals(24.2, camera.camera!!.specs!!.resolutionMp!!, 0.0)
        val formatOnly = c.selection(Settings(formatId = "aps-c"))!!
        assertNull(formatOnly.camera)
        assertEquals(23.5, formatOnly.format.sensor.widthMm, 0.0)
        assertNull(c.selection(Settings()))
        assertNull(c.selection(Settings(cameraId = "removed-camera")))
    }

    @Test
    fun theStoreDerivesTheSelection() = runTest {
        val settings = MutableStateFlow(Settings())
        val store = CatalogStore({ small }, settings, backgroundScope)
        settings.value = Settings(cameraId = "nikon-z-6")
        assertEquals("nikon-z-6", store.selection.first { it != null }!!.camera!!.id)
    }

    @Test
    fun anUnknownSchemaVersionIsAnErrorNotACrash() {
        val state = parseCatalog(small.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"))
        assertEquals(CatalogState.Failed("unsupported schemaVersion 2"), state)
    }

    @Test
    fun aMalformedOrMissingCatalogIsAnError() = runTest {
        assertTrue(parseCatalog("{\"schemaVersion\": 1, \"cameras\": [") is CatalogState.Failed)
        assertTrue(parseCatalog("{\"schemaVersion\": 1, \"version\": \"x\"}") is CatalogState.Failed)
        val missing = CatalogStore({ throw IOException("no asset") }, MutableStateFlow(Settings()), backgroundScope)
        assertTrue(missing.state.first { it != CatalogState.Loading } is CatalogState.Failed)
    }
}
