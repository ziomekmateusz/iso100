package io.github.ziomekmateusz.iso100.ui.stars

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.startrails.StarRule
import io.github.ziomekmateusz.iso100.data.CameraSpecs
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.fov.ALPHA_7_III
import io.github.ziomekmateusz.iso100.ui.fov.TEST_FORMATS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

val ALPHA_7_III_24MP = ALPHA_7_III.copy(camera = ALPHA_7_III.camera!!.copy(specs = CameraSpecs(resolutionMp = 24.2)))

class StarsStateTest {
    @Test
    fun `NPF is offered and chosen by default only when the camera has the resolution`() {
        val withMp = StarsUiState.from(StarsInput(), Settings(), ALPHA_7_III_24MP)
        assertEquals(StarRule.NPF, withMp.rule)
        assertEquals(listOf(StarRule.RULE_500, StarRule.RULE_300, StarRule.NPF), withMp.rules)
        val without = StarsUiState.from(StarsInput(rule = StarRule.NPF), Settings(), Selection(null, TEST_FORMATS[0]))
        assertEquals(StarRule.RULE_500, without.rule)
        assertEquals(listOf(StarRule.RULE_500, StarRule.RULE_300), without.rules)
    }

    @Test
    fun theBoardExampleWithTheChosenRuleFirst() {
        val state = StarsUiState.from(StarsInput(), Settings(), ALPHA_7_III_24MP)
        assertEquals(listOf(StarRule.NPF, StarRule.RULE_500, StarRule.RULE_300), state.results.map { it.rule })
        assertEquals(11.6, state.chosen!!.limitS!!, 0.05)
        assertEquals(listOf(10.0, 20.0, 10.0), state.results.map { it.shutter!!.nominal })
        assertEquals(5.98, state.pixelPitchUm!!, 0.01)
    }

    @Test
    fun `the suggested shutter is never above the limit`() {
        for (step in StopStep.entries) {
            for (focal in listOf(14.0, 24.0, 35.0, 50.0, 85.0, 200.0)) {
                for (rule in StarRule.entries) {
                    val result = StarsUiState.from(StarsInput(rule, focal), Settings(stopStep = step), ALPHA_7_III_24MP).chosen!!
                    assertTrue("$step $focal $rule", result.shutter!!.nominal <= result.limitS!!)
                }
            }
        }
    }

    @Test
    fun aZeroFocalLengthIsAnErrorForThatField() {
        val state = StarsUiState.from(StarsInput(focalLengthMm = 0.0), Settings(), ALPHA_7_III_24MP)
        assertEquals(Outcome.Invalid(Input.FOCAL_LENGTH), state.invalid)
        assertNull(state.chosen!!.shutter)
    }

    @Test
    fun theActionsChangeTheInput() {
        val input = StarsInput()
        assertEquals(StarRule.RULE_300, input.reduce(StarsAction.SetRule(StarRule.RULE_300)).rule)
        assertEquals(28.0, input.reduce(StarsAction.StepFocalLength(up = true)).focalLengthMm, 0.0)
        assertEquals(1.414, input.reduce(StarsAction.SetAperture(1.414)).aperture, 0.0)
    }
}
