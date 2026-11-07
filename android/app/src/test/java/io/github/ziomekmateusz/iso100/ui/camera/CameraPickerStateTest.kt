package io.github.ziomekmateusz.iso100.ui.camera

import io.github.ziomekmateusz.iso100.data.CameraSpecs
import io.github.ziomekmateusz.iso100.data.Catalog
import io.github.ziomekmateusz.iso100.data.CatalogCamera
import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.fov.TEST_FORMATS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

val TEST_CATALOG = CatalogState.Ready(
    Catalog(
        schemaVersion = 1,
        version = "test",
        formats = TEST_FORMATS,
        cameras = listOf(
            CatalogCamera("canon-eos-r6", "Canon", "EOS R6", "full-frame"),
            CatalogCamera(
                "sony-alpha-7-iii",
                "Sony",
                "Alpha 7 III",
                "full-frame",
                mount = "Sony E",
                aliases = listOf("ILCE-7M3"),
                specs = CameraSpecs(isoMin = 100, isoMax = 51200, resolutionMp = 24.2, weightG = 650),
            ),
            CatalogCamera("sony-alpha-6400", "Sony", "Alpha 6400", "aps-c", aliases = listOf("ILCE-6400")),
        ),
    ),
)

class CameraPickerStateTest {
    @Test
    fun theSearchAndTheFormatSheetChangeTheInput() {
        val input = CameraPickerInput()
        assertEquals("alpha", input.reduce(CameraPickerAction.Search("alpha")).query)
        assertTrue(
            CameraPickerUiState.from(
                input.reduce(CameraPickerAction.ShowFormats),
                TEST_CATALOG,
                Settings(),
            ).formatSheet,
        )
        assertEquals(
            input,
            input.reduce(CameraPickerAction.ShowFormats).reduce(CameraPickerAction.HideFormats),
        )
        assertEquals(input, input.reduce(CameraPickerAction.ChooseCamera("canon-eos-r6")))
    }

    @Test
    fun theCamerasAreGroupedByMakerWithTheirExifNameAndFormat() {
        val state = CameraPickerUiState.from(CameraPickerInput(), TEST_CATALOG, Settings(cameraId = "sony-alpha-7-iii"))
        assertEquals(listOf("Canon", "Sony"), state.groups.map { it.maker })
        assertEquals(
            CameraRow("sony-alpha-7-iii", "Alpha 7 III", "ILCE-7M3", "Full Frame", selected = true, hasSpecs = true),
            state.groups[1].cameras[0],
        )
        assertEquals("APS-C", state.groups[1].cameras[1].format)
    }

    @Test
    fun theSearchFindsModelsAndExifNames() {
        fun ids(query: String) =
            CameraPickerUiState.from(CameraPickerInput(query), TEST_CATALOG, Settings()).groups.flatMap { g -> g.cameras.map { it.id } }
        assertEquals(listOf("sony-alpha-7-iii"), ids("alpha 7"))
        assertEquals(listOf("sony-alpha-6400"), ids("ilce-6400"))
        assertEquals(emptyList<String>(), ids("nikon"))
    }

    @Test
    fun theCharacteristicsOpenOnlyForTheCamerasThatHaveThem() {
        val open = CameraPickerInput().reduce(CameraPickerAction.ShowDetails("sony-alpha-7-iii"))
        val details = CameraPickerUiState.from(open, TEST_CATALOG, Settings()).details!!
        assertEquals("Sony Alpha 7 III", details.name)
        assertEquals("Full Frame (35 mm)", details.format)
        assertEquals("Sony E", details.mount)
        assertEquals(650, details.specs.weightG)
        assertEquals(null, CameraPickerUiState.from(open.reduce(CameraPickerAction.HideDetails), TEST_CATALOG, Settings()).details)
        val withoutSpecs = CameraPickerInput().reduce(CameraPickerAction.ShowDetails("canon-eos-r6"))
        assertEquals(null, CameraPickerUiState.from(withoutSpecs, TEST_CATALOG, Settings()).details)
        assertEquals(
            listOf(false, true, false),
            CameraPickerUiState.from(CameraPickerInput(), TEST_CATALOG, Settings()).groups.flatMap { g ->
                g.cameras.map { it.hasSpecs }
            },
        )
    }

    @Test
    fun anUnreadableCatalogIsShownAsAnError() {
        val state = CameraPickerUiState.from(CameraPickerInput(), CatalogState.Failed("broken"), Settings())
        assertTrue(state.failed)
        assertTrue(state.groups.isEmpty())
    }
}
