package io.github.ziomekmateusz.iso100.calc.exposure

import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import kotlin.math.abs
import kotlin.math.log2

class ExposureScaleTest {
    private fun nominals(setting: ExposureSetting, step: StopStep) = ExposureScale.of(setting, step).values.map { it.nominal }

    @Test
    fun `full stops are the numbers printed on a camera`() {
        assertEquals(
            listOf(25, 50, 100, 200, 400, 800, 1600, 3200, 6400, 12800, 25600, 51200, 102400).map { it.toDouble() },
            nominals(ExposureSetting.ISO, StopStep.FULL),
        )
        assertEquals(
            listOf(0.7, 1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0),
            nominals(ExposureSetting.APERTURE, StopStep.FULL),
        )
        assertEquals(
            listOf(8000, 4000, 2000, 1000, 500, 250, 125, 60, 30, 15, 8, 4, 2).map { 1.0 / it } + listOf(1.0, 2.0, 4.0, 8.0, 15.0, 30.0),
            nominals(ExposureSetting.SHUTTER, StopStep.FULL),
        )
    }

    @Test
    fun `half and third stops sit between the full ones`() {
        fun assertRun(run: List<Double>, setting: ExposureSetting, step: StopStep) =
            assertTrue(Collections.indexOfSubList(nominals(setting, step), run) >= 0, "$setting $step $run")
        assertRun(listOf(100.0, 140.0, 200.0), ExposureSetting.ISO, StopStep.HALF)
        assertRun(listOf(5.6, 6.7, 8.0), ExposureSetting.APERTURE, StopStep.HALF)
        assertRun(listOf(1.0 / 125, 1.0 / 90, 1.0 / 60), ExposureSetting.SHUTTER, StopStep.HALF)
        assertRun(listOf(100.0, 125.0, 160.0, 200.0), ExposureSetting.ISO, StopStep.THIRD)
        assertRun(listOf(5.6, 6.3, 7.1, 8.0), ExposureSetting.APERTURE, StopStep.THIRD)
        assertRun(listOf(1.0 / 125, 1.0 / 100, 1.0 / 80, 1.0 / 60), ExposureSetting.SHUTTER, StopStep.THIRD)
    }

    @Test
    fun `actual values are the exact powers behind the printed ones`() {
        val aperture = ExposureScale.of(ExposureSetting.APERTURE, StopStep.FULL).values
        assertClose(Math.pow(2.0, 2.5), aperture.first { it.nominal == 5.6 }.actual, "f/5.6")
        val shutter = ExposureScale.of(ExposureSetting.SHUTTER, StopStep.FULL).values
        assertClose(1.0 / 128, shutter.first { it.nominal == 1.0 / 125 }.actual, "1/125 s")
        assertClose(32.0, shutter.last().actual, "30 s")
    }

    @Test
    fun `every value rises along its scale and stays close to what is printed`() {
        for (setting in ExposureSetting.entries) for (step in StopStep.entries) {
            val values = ExposureScale.of(setting, step).values
            values.zipWithNext { a, b -> assertTrue(a.actual < b.actual, "$setting $step $a $b") }
            val stopsPerUnit = if (setting == ExposureSetting.APERTURE) 2.0 else 1.0
            values.forEach { assertTrue(abs(log2(it.actual / it.nominal)) * stopsPerUnit < 0.2, "$setting $step $it") }
        }
    }

    @Test
    fun `a value is out of scale only more than half a step past the end`() {
        val iso = ExposureScale.of(ExposureSetting.ISO, StopStep.THIRD)
        assertEquals(25.0, iso.equivalent(23.0).ok().nearest!!.nominal)
        assertEquals(Outcome.OutOfScale(tooHigh = false), iso.equivalent(22.0))
        assertEquals(102400.0, iso.equivalent(114000.0).ok().nearest!!.nominal)
        assertEquals(Outcome.OutOfScale(tooHigh = true), iso.equivalent(116000.0))

        val aperture = ExposureScale.of(ExposureSetting.APERTURE, StopStep.THIRD)
        assertEquals(0.7, aperture.equivalent(0.68).ok().nearest!!.nominal)
        assertEquals(Outcome.OutOfScale(tooHigh = false), aperture.equivalent(0.66))
        assertEquals(32.0, aperture.equivalent(33.5).ok().nearest!!.nominal)
        assertEquals(Outcome.OutOfScale(tooHigh = true), aperture.equivalent(34.5))

        val shutter = ExposureScale.of(ExposureSetting.SHUTTER, StopStep.THIRD)
        assertEquals(1.0 / 8000, shutter.equivalent(1.0 / 9000).ok().nearest!!.nominal)
        assertEquals(Outcome.OutOfScale(tooHigh = false), shutter.equivalent(1.0 / 9500))
        assertEquals(30.0, shutter.equivalent(35.0).ok().nearest!!.nominal)
        assertNull(shutter.equivalent(36.0).ok().nearest, "a shutter longer than the scale is a result")
    }

    @Test
    fun `the shutter scale does not repeat its first stop`() {
        for (step in StopStep.entries) {
            val values = ExposureScale.of(ExposureSetting.SHUTTER, step).values
            assertEquals(1.0 / 8000, values[0].nominal)
            assertNotEquals(values[0].nominal, values[1].nominal, "$step")
        }
    }

    @Test
    fun `nearest is measured in stops`() {
        assertEquals(1.0 / 125, ExposureScale.of(ExposureSetting.SHUTTER, StopStep.FULL).nearest(0.0057).nominal)
    }

    @Test
    fun `the distance between two values is in stops on every scale`() {
        assertEquals(2.0, ExposureScale.of(ExposureSetting.APERTURE, StopStep.THIRD).stopsBetween(2.0, 4.0), 1e-9)
        assertEquals(1.0, ExposureScale.of(ExposureSetting.SHUTTER, StopStep.THIRD).stopsBetween(2.0, 4.0), 1e-9)
        assertEquals(1.0, ExposureScale.of(ExposureSetting.ISO, StopStep.THIRD).stopsBetween(400.0, 200.0), 1e-9)
    }

    @Test
    fun `defaults are found by value whatever the stop step`() {
        for (step in StopStep.entries) {
            assertEquals(8.0, ExposureScale.of(ExposureSetting.APERTURE, step).nearest(8.0).nominal, "$step")
            assertEquals(100.0, ExposureScale.of(ExposureSetting.ISO, step).nearest(100.0).nominal, "$step")
            assertEquals(1.0 / 125, ExposureScale.of(ExposureSetting.SHUTTER, step).nearest(1.0 / 125).nominal, "$step")
        }
    }

    @Test
    fun `ND stops and density come from the factor`() {
        assertEquals(1.0, NdFilter.ND2.stops, 1e-9)
        assertEquals(0.301, NdFilter.ND2.opticalDensity, 0.0005)
        assertEquals(3.0, NdFilter.ND8.stops, 1e-9)
        assertEquals(0.903, NdFilter.ND8.opticalDensity, 0.0005)
        assertEquals(6.644, NdFilter.ND100.stops, 0.0005)
        assertEquals(2.0, NdFilter.ND100.opticalDensity, 1e-9)
        assertEquals(8.644, NdFilter.ND400.stops, 0.0005)
        assertEquals(9.966, NdFilter.ND1000.stops, 0.0005)
        assertEquals(3.0, NdFilter.ND1000.opticalDensity, 1e-9)
        assertEquals(10.0, NdFilter.ND1024.stops, 1e-9)
    }
}
