package io.github.ziomekmateusz.iso100.calc.exposure

import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExposureTest {
    private val metered = Exposure(100.0, 8.0, 2.0.pow(-7))

    @Test
    fun `EV counts stops of aperture and shutter, LV normalizes it to ISO 100`() {
        assertClose(13.0, metered.ev, "EV")
        assertClose(13.0, metered.lv, "LV")
        val highIso = Exposure(400.0, 2.0.pow(3), 2.0.pow(-6))
        assertClose(12.0, highIso.ev, "EV at ISO 400")
        assertClose(10.0, highIso.lv, "LV at ISO 400")
    }

    @Test
    fun `equivalents trade stops one for one`() {
        val wider = Math.sqrt(2.0).pow(3)
        assertClose(1.0 / 1024, metered.equivalentShutter(100.0, wider), "three stops wider")
        assertClose(800.0, metered.equivalentIso(8.0, 1.0 / 1024), "three stops shorter")
        assertClose(8.0, metered.equivalentAperture(800.0, 1.0 / 1024), "back to the metered aperture")
    }

    @Test
    fun `an ND filter takes away its stops`() {
        assertClose(0.0625, metered.equivalentShutter(100.0, 8.0, NdFilter.ND8), "ND8")
        assertClose(7.8125, metered.equivalentShutter(100.0, 8.0, NdFilter.ND1000), "ND1000")
        assertClose(800.0, metered.equivalentIso(8.0, 2.0.pow(-7), NdFilter.ND8), "ND8 paid with ISO")
    }

    @Test
    fun `the equivalent ISO is computed at every stop step`() {
        val iso = metered.equivalentIso(4.0, 2.0.pow(-7))
        for (step in StopStep.entries) {
            val equivalent = ExposureScale.of(ExposureSetting.ISO, step).equivalent(iso).ok()
            assertNotNull(equivalent.nearest, "$step")
            assertEquals(25.0, equivalent.nearest!!.nominal, "$step")
        }
    }

    @Test
    fun `settings beyond the scale are out of scale, not its first or last value`() {
        val aperture = metered.equivalentAperture(100.0, 2.0.pow(-7), NdFilter.ND1024)
        val iso = metered.equivalentIso(16.0, 2.0.pow(-7), NdFilter.ND1024)
        val longShutter = metered.equivalentShutter(100.0, Math.sqrt(2.0).pow(9), NdFilter.ND1024)
        for (step in StopStep.entries) {
            assertEquals(Outcome.OutOfScale(tooHigh = false), ExposureScale.of(ExposureSetting.APERTURE, step).equivalent(aperture))
            assertEquals(Outcome.OutOfScale(tooHigh = true), ExposureScale.of(ExposureSetting.ISO, step).equivalent(iso))
            val shutter = ExposureScale.of(ExposureSetting.SHUTTER, step).equivalent(longShutter).ok()
            assertNull(shutter.nearest, "a shutter longer than 30 s is shown exactly")
            assertClose(64.0, shutter.exact, step)
        }
    }

    private fun Double.pow(n: Int) = Math.pow(this, n.toDouble())
}
