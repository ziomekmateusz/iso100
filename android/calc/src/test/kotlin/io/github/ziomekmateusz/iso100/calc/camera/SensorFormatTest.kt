package io.github.ziomekmateusz.iso100.calc.camera

import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SensorFormatTest {
    @Test
    fun `crop factor and circle of confusion come from the diagonal`() {
        // by hand: crop = 43.27 / diagonal, circle of confusion = diagonal / 1500
        val cases = listOf(
            Triple(SensorFormat(36.0, 24.0), 1.0, 0.02884),
            Triple(SensorFormat(23.5, 15.6), 1.534, 0.0188),
            Triple(SensorFormat(17.3, 13.0), 2.0, 0.01443),
            Triple(SensorFormat(43.8, 32.9), 0.79, 0.03652),
        )
        for ((format, crop, coc) in cases) {
            assertClose(crop, format.cropFactor, format, relative = 1e-3)
            assertClose(coc, format.circleOfConfusionMm, format, relative = 1e-3)
        }
    }

    @Test
    fun `imperial distances are converted to meters`() {
        assertEquals(3.048, UnitSystem.IMPERIAL.toMeters(10.0), 1e-12)
        assertEquals(10.0, UnitSystem.IMPERIAL.fromMeters(3.048), 1e-12)
        assertEquals(2.5, UnitSystem.METRIC.toMeters(2.5))
    }
}
