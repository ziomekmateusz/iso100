package io.github.ziomekmateusz.iso100.ui.camera

import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatPickerTest {
    @Test
    fun aFormatChosenAloneIsMarked() {
        val formats = formatRows(TEST_CATALOG, Settings(formatId = "aps-c"))
        assertEquals(listOf("aps-c"), formats.filter { it.selected }.map { it.id })
    }

    @Test
    fun withACameraChosenNoFormatIsChosenOnItsOwn() {
        assertTrue(
            formatRows(
                TEST_CATALOG,
                Settings(cameraId = "sony-alpha-7-iii")
            ).none { it.selected })
    }

    @Test
    fun anUnreadableCatalogHasNoFormats() {
        assertTrue(formatRows(CatalogState.Failed("broken"), Settings()).isEmpty())
    }
}
