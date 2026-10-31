package io.github.ziomekmateusz.iso100.ui.exposure

import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.APERTURE
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.ISO
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.SHUTTER
import io.github.ziomekmateusz.iso100.calc.exposure.NdFilter
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.data.CameraSpecs
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.fov.ALPHA_7_III
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureStateTest {
    private fun state(input: ExposureInput = ExposureInput(), settings: Settings = Settings()) = ExposureUiState.from(input, settings, null)

    @Test
    fun `the defaults are ISO 100, f8 and 1 125 at every step`() {
        for (step in StopStep.entries) {
            val s = state(settings = Settings(stopStep = step))
            assertEquals("$step", listOf(100.0, 8.0, 1.0 / 125), s.measured.values.map { it.value.nominal })
        }
    }

    @Test
    fun `the equivalent ISO is calculated at every step`() {
        for (step in StopStep.entries) {
            val result = state(ExposureInput(solve = ISO, aperture = 4.0), Settings(stopStep = step)).result
            assertEquals("$step", 25.0, (result as Outcome.Ok).value.nearest!!.nominal, 0.0)
        }
    }

    @Test
    fun `an aperture beyond the scale is out of scale, not the first value of the scale`() {
        val s = state(ExposureInput(solve = APERTURE, nd = NdFilter.ND1024))
        assertEquals(Outcome.OutOfScale(tooHigh = false), s.result)
        assertEquals(0.7, s.limit!!.nominal, 0.0)
    }

    @Test
    fun theKeptSettingsAreTheTwoNotCalculated() {
        assertEquals(listOf(ISO, APERTURE), state().kept.keys.toList())
        assertEquals(listOf(ISO, SHUTTER), state(ExposureInput(solve = APERTURE)).kept.keys.toList())
    }

    @Test
    fun tenStopsOfNdTurn1125Into8Seconds() {
        val s = state(ExposureInput(nd = NdFilter.ND1000))
        assertEquals(8.0, (s.result as Outcome.Ok).value.nearest!!.nominal, 0.0)
        assertFalse(s.exactDiffers)
        assertEquals(13.0, s.ev, 1e-9)
    }

    @Test
    fun aShutterLongerThanTheScaleIsStillAResult() {
        val result = state(ExposureInput(measuredShutterS = 1.0 / 8, nd = NdFilter.ND1000)).result as Outcome.Ok
        assertNull(result.value.nearest)
        assertEquals(125.0, result.value.exact, 1.0)
    }

    @Test
    fun theCalculatedValueIsShownWhenTheScaleRoundsIt() {
        // ND100 is 6.64 stops: 0.78 s, a third of a stop from 1 s
        assertTrue(state(ExposureInput(nd = NdFilter.ND100), Settings(stopStep = StopStep.FULL)).exactDiffers)
        assertFalse(state(ExposureInput(nd = NdFilter.ND100)).exactDiffers)
    }

    @Test
    fun aCoarserStepMovesTheDialsToTheNearestValue() {
        val s = state(ExposureInput(aperture = 6.35), Settings(stopStep = StopStep.FULL))
        assertEquals(5.6, s.kept.getValue(APERTURE).value.nominal, 0.0)
    }

    @Test
    fun `ISO values the camera does not have are dimmed`() {
        val camera = ALPHA_7_III.copy(camera = ALPHA_7_III.camera!!.copy(specs = CameraSpecs(isoMin = 100, isoMax = 51200)))
        val iso = ExposureUiState.from(ExposureInput(), Settings(), camera).kept.getValue(ISO)
        assertEquals(listOf(25.0, 32.0, 40.0, 50.0, 64.0, 80.0, 102400.0), iso.values.filterIndexed { i, _ -> iso.dimmed[i] }.map { it.nominal }.let { it.take(6) + it.last() })
        assertFalse(iso.dimmed[iso.selected])
    }

    @Test
    fun theSheetsOpenAndChoosingAFilterClosesTheList() {
        val open = ExposureInput().reduce(ExposureAction.Open(ExposureSheet.ND_FILTERS))
        assertEquals(ExposureSheet.ND_FILTERS, open.sheet)
        val chosen = open.reduce(ExposureAction.SetNd(NdFilter.ND16))
        assertEquals(NdFilter.ND16, chosen.nd)
        assertNull(chosen.sheet)
        assertEquals(1.0 / 30, ExposureInput().reduce(ExposureAction.SetMeasured(SHUTTER, 1.0 / 30)).measuredShutterS, 0.0)
        assertEquals(400.0, ExposureInput().reduce(ExposureAction.SetNew(ISO, 400.0)).iso, 0.0)
    }
}
