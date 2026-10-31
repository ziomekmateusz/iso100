package io.github.ziomekmateusz.iso100.ui.fov

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.CatalogCamera
import io.github.ziomekmateusz.iso100.data.CatalogFormat
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

val TEST_FORMATS = listOf(
    CatalogFormat("full-frame", "Full Frame (35 mm)", 36.0, 24.0),
    CatalogFormat("aps-c", "APS-C", 23.5, 15.6),
    CatalogFormat("four-thirds", "Four Thirds / Micro 4/3", 17.3, 13.0),
    CatalogFormat("1-inch", "1\"", 13.2, 8.8),
)

val ALPHA_7_III = Selection(CatalogCamera("sony-alpha-7-iii", "Sony", "Alpha 7 III", "full-frame"), TEST_FORMATS[0])

class FovStateTest {
    @Test
    fun theButtonsAndTheFieldsChangeTheInput() {
        val input = FovInput(focalLengthMm = 24.0, distanceM = 10.0)
        assertEquals(28.0, input.reduce(FovAction.StepFocalLength(up = true)).focalLengthMm, 0.0)
        assertEquals(35.0, input.reduce(FovAction.SetFocalLength(35.0)).focalLengthMm, 0.0)
        assertEquals(15.0, input.reduce(FovAction.StepDistance(up = true, UnitSystem.METRIC)).distanceM, 1e-9)
        assertEquals(3.048, input.reduce(FovAction.SetDistance(10.0, UnitSystem.IMPERIAL)).distanceM, 1e-9)
    }

    @Test
    fun theDistanceStepsInTheUnitTheUserReads() {
        val tenFeet = FovInput(distanceM = 3.048)
        assertEquals(15 * 0.3048, tenFeet.reduce(FovAction.StepDistance(up = true, UnitSystem.IMPERIAL)).distanceM, 1e-9)
    }

    @Test
    fun theResultComesFromCalcWithTheChosenFormat() {
        val state = FovUiState.from(FovInput(), Settings(), ALPHA_7_III, TEST_FORMATS)
        assertEquals(CameraLabel("Sony Alpha 7 III", "Full Frame (35 mm)"), state.camera)
        assertEquals(73.7, state.result!!.horizontalDeg, 0.05)
        assertEquals(15.0, state.result.widthM, 1e-9)
        assertEquals(1.5, state.aspectRatio, 1e-9)
    }

    @Test
    fun theFramesAreTheFirstTwoSmallerFormats() {
        fun frames(id: String) = comparisonFrames(TEST_FORMATS.first { it.id == id }, TEST_FORMATS).map { it.formatId }
        assertEquals(listOf("aps-c", "four-thirds"), frames("full-frame"))
        assertEquals(listOf("four-thirds", "1-inch"), frames("aps-c"))
        assertEquals(emptyList<String>(), frames("1-inch"))
        val apsC = comparisonFrames(TEST_FORMATS[0], TEST_FORMATS).first()
        assertEquals(23.5 / 36, apsC.widthRatio, 1e-9)
    }

    @Test
    fun withoutAFormatThereIsNoResult() {
        val state = FovUiState.from(FovInput(), Settings(), null, TEST_FORMATS)
        assertNull(state.camera)
        assertNull(state.result)
    }

    @Test
    fun aZeroFocalLengthIsAnErrorForThatField() {
        val state = FovUiState.from(FovInput(focalLengthMm = 0.0), Settings(), ALPHA_7_III, TEST_FORMATS)
        assertNull(state.result)
        assertEquals(Outcome.Invalid(Input.FOCAL_LENGTH), state.invalid)
    }

    @Test
    fun imperialUnitsShowTheDistanceInFeet() {
        val state = FovUiState.from(FovInput(distanceM = 3.048), Settings(units = UnitSystem.IMPERIAL), ALPHA_7_III, TEST_FORMATS)
        assertEquals(10.0, state.distance, 1e-9)
    }
}
