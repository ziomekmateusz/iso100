package io.github.ziomekmateusz.iso100.calc.dof

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import io.github.ziomekmateusz.iso100.calc.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DepthOfFieldTest {
    private val fullFrame = SensorFormat(36.0, 24.0)

    private fun assertDof(hyperfocalM: Double, nearM: Double, farM: Double, dof: DepthOfField) {
        assertClose(hyperfocalM, dof.hyperfocalM, "hyperfocal", relative = 1e-3)
        assertClose(nearM, dof.nearM, "near", relative = 1e-3)
        assertClose(farM, dof.farM!!, "far", relative = 1e-3)
    }

    @Test
    fun `limits and hyperfocal, by hand with H = f2 over N c plus f`() {
        assertDof(10.884, 2.358, 4.123, depthOfField(50.0, 8.0, 3.0, fullFrame).ok())
        assertDof(5.467, 1.942, 6.587, depthOfField(50.0, 16.0, 3.0, fullFrame).ok())
        assertDof(192.2, 4.875, 5.131, depthOfField(85.0, 2.0, 5.0, SensorFormat(23.5, 15.6)).ok())
        assertDof(2.440, 0.4166, 0.6253, depthOfField(14.0, 5.6, 0.5, SensorFormat(17.3, 13.0)).ok())
    }

    @Test
    fun `at the hyperfocal everything is sharp from half of it to infinity`() {
        val h = depthOfField(24.0, 8.0, 3.0, fullFrame).ok().hyperfocalM
        assertClose(2.520, h, "hyperfocal", relative = 1e-3)
        assertClose(h / 2, depthOfField(24.0, 8.0, h, fullFrame).ok().nearM, "near", relative = 1e-6)
    }

    @Test
    fun `focusing beyond the hyperfocal gives an infinite far limit without exceptions`() {
        val dof = depthOfField(24.0, 11.0, 5.0, fullFrame).ok()
        assertNull(dof.farM)
        assertNull(dof.depthM)
        assertClose(1.34, dof.nearM, "near", relative = 0.01)
        assertClose(1.84, dof.hyperfocalM, "hyperfocal", relative = 0.01)
    }

    @Test
    fun `focusing at the hyperfocal reaches infinity`() {
        val h = depthOfField(35.0, 5.6, 3.0, fullFrame).ok().hyperfocalM
        assertNull(depthOfField(35.0, 5.6, h, fullFrame).ok().farM)
    }

    @Test
    fun `inputs out of the domain are typed errors`() {
        assertEquals(Outcome.Invalid(Input.FOCAL_LENGTH), depthOfField(0.0, 8.0, 3.0, fullFrame))
        assertEquals(Outcome.Invalid(Input.APERTURE), depthOfField(50.0, -1.0, 3.0, fullFrame))
        assertEquals(Outcome.Invalid(Input.FOCUS_DISTANCE), depthOfField(50.0, 8.0, Double.NaN, fullFrame))
        assertEquals(Outcome.Invalid(Input.FOCUS_DISTANCE, minimum = 0.2), depthOfField(200.0, 8.0, 0.1, fullFrame))
    }
}
