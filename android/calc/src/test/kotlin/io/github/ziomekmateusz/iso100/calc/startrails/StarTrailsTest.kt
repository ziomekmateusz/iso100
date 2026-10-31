package io.github.ziomekmateusz.iso100.calc.startrails

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StarTrailsTest {
    private val fullFrame = SensorFormat(36.0, 24.0)
    private val apsC = SensorFormat(23.5, 15.6)

    @Test
    fun `the 500 and 300 rules divide by the focal length and the crop factor`() {
        assertClose(20.83, maxStarExposure(StarRule.RULE_500, 24.0, fullFrame).ok(), "500 rule", relative = 1e-3)
        assertClose(12.5, maxStarExposure(StarRule.RULE_300, 24.0, fullFrame).ok(), "300 rule", relative = 1e-3)
        assertClose(13.58, maxStarExposure(StarRule.RULE_500, 24.0, apsC).ok(), "500 rule on APS-C", relative = 1e-3)
    }

    @Test
    fun `NPF is 35 N plus 30 p over f`() {
        assertClose(6.0, pixelPitchUm(fullFrame, 24.0), "pixel pitch")
        assertClose(11.58, maxStarExposure(StarRule.NPF, 24.0, fullFrame, 2.8, 24.0).ok(), "24 MP", relative = 1e-3)
        assertClose(9.56, maxStarExposure(StarRule.NPF, 24.0, fullFrame, 2.8, 45.0).ok(), "45 MP", relative = 1e-3)
    }

    @Test
    fun `suggested shutter never exceeds the limit`() {
        for (focalLength in listOf(14.0, 24.0, 50.0)) for (format in listOf(fullFrame, apsC)) for (rule in StarRule.entries) {
            val limit = maxStarExposure(rule, focalLength, format, 2.8, 24.0).ok()
            for (step in StopStep.entries) assertTrue(suggestedShutter(limit, step)!!.nominal <= limit, "$rule $focalLength $step")
        }
        assertEquals(15.0, suggestedShutter(20.83, StopStep.FULL)!!.nominal)
        assertEquals(20.0, suggestedShutter(20.83, StopStep.THIRD)!!.nominal)
        assertEquals(10.0, suggestedShutter(12.5, StopStep.THIRD)!!.nominal)
        assertEquals(8.0, suggestedShutter(12.5, StopStep.FULL)!!.nominal)
        assertEquals(6.0, suggestedShutter(6.54, StopStep.THIRD)!!.nominal)
    }

    @Test
    fun `NPF needs aperture and resolution`() {
        assertEquals(Outcome.Invalid(Input.RESOLUTION), maxStarExposure(StarRule.NPF, 24.0, fullFrame, aperture = 2.8))
        assertEquals(Outcome.Invalid(Input.APERTURE), maxStarExposure(StarRule.NPF, 24.0, fullFrame, resolutionMp = 24.2))
    }

    @Test
    fun `a zero focal length is an error`() {
        assertEquals(Outcome.Invalid(Input.FOCAL_LENGTH), maxStarExposure(StarRule.RULE_500, 0.0, fullFrame))
    }
}
