package io.github.ziomekmateusz.iso100.calc.fov

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import io.github.ziomekmateusz.iso100.calc.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FieldOfViewTest {
    private fun assertFov(horizontal: Double, vertical: Double, diagonal: Double, width: Double, height: Double, fov: FieldOfView) {
        assertClose(horizontal, fov.horizontalDeg, "horizontal", relative = 1e-3)
        assertClose(vertical, fov.verticalDeg, "vertical", relative = 1e-3)
        assertClose(diagonal, fov.diagonalDeg, "diagonal", relative = 1e-3)
        assertClose(width, fov.widthM, "width", relative = 1e-3)
        assertClose(height, fov.heightM, "height", relative = 1e-3)
    }

    @Test
    fun `angles and framed area, by hand with 2 atan of size over 2f`() {
        assertFov(73.74, 53.13, 84.06, 15.0, 10.0, fieldOfView(24.0, 10.0, SensorFormat(36.0, 24.0)).ok())
        assertFov(52.17, 36.01, 60.88, 9.792, 6.5, fieldOfView(24.0, 10.0, SensorFormat(23.5, 15.6)).ok())
        assertFov(10.29, 6.867, 12.34, 0.54, 0.36, fieldOfView(200.0, 3.0, SensorFormat(36.0, 24.0)).ok())
    }

    @Test
    fun `a zero focal length is an error, not 180 degrees`() {
        val fullFrame = SensorFormat(36.0, 24.0)
        assertEquals(Outcome.Invalid(Input.FOCAL_LENGTH), fieldOfView(0.0, 10.0, fullFrame))
        assertEquals(Outcome.Invalid(Input.SUBJECT_DISTANCE), fieldOfView(24.0, 0.0, fullFrame))
    }
}
