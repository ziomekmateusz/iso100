package io.github.ziomekmateusz.iso100.ui.components

import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import org.junit.Assert.assertEquals
import org.junit.Test

class DialTest {
    @Test
    fun `the dial starts on the value closest to the one asked, not on an index`() {
        val third = dial(ExposureSetting.APERTURE, StopStep.THIRD, 8.0)
        assertEquals(8.0, third.value.nominal, 0.0)
        // f/6.3 does not exist in full stops: the closest is f/5.6
        val full = dial(ExposureSetting.APERTURE, StopStep.FULL, third.values[third.selected - 2].actual)
        assertEquals(5.6, full.value.nominal, 0.0)
    }

    @Test
    fun longTicksAreTheFullStopsAndValuesOutsideTheRangeAreDimmed() {
        val d = dial(ExposureSetting.ISO, StopStep.THIRD, 400.0, 100.0..51200.0)
        val full = d.values.filterIndexed { i, _ -> d.fullStops[i] }.map { it.nominal.toInt() }
        assertEquals(listOf(25, 50, 100, 200, 400, 800), full.take(6))
        assertEquals(listOf(25.0, 32.0, 40.0, 50.0, 64.0, 80.0), d.values.filterIndexed { i, _ -> d.dimmed[i] }.map { it.nominal }.take(6))
        assertEquals(false, d.dimmed[d.selected])
        assertEquals(true, d.dimmed.last())
    }

    @Test
    fun theBarPlacesDistancesAsTheBoard() {
        assertEquals(0f, dofBarFraction(0.5), 1e-6f)
        assertEquals(0.149f, dofBarFraction(1.0), 1e-6f)
        assertEquals(0.659f, dofBarFraction(10.72), 0.001f)
        assertEquals(0f, dofBarFraction(0.1), 0f)
        assertEquals(0.9f, dofBarFraction(500.0), 0f)
        assertEquals(1f, dofBarFraction(null), 0f)
    }
}
