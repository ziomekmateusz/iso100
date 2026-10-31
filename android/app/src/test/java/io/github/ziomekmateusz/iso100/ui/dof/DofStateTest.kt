package io.github.ziomekmateusz.iso100.ui.dof

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.fov.ALPHA_7_III
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DofStateTest {
    @Test
    fun theDialTheButtonsAndTheFieldsChangeTheInput() {
        val input = DofInput()
        assertEquals(5.657, input.reduce(DofAction.SetAperture(5.657)).aperture, 0.0)
        assertEquals(70.0, input.reduce(DofAction.StepFocalLength(up = true)).focalLengthMm, 0.0)
        assertEquals(5.0, input.reduce(DofAction.StepFocus(up = true, UnitSystem.METRIC)).focusM, 1e-9)
        assertEquals(3.048, input.reduce(DofAction.SetFocus(10.0, UnitSystem.IMPERIAL)).focusM, 1e-9)
        assertEquals(10.72, input.reduce(DofAction.FocusAt(10.72)).focusM, 0.0)
    }

    @Test
    fun theResultComesFromCalcWithTheExactAperture() {
        val state = DofUiState.from(DofInput(), Settings(), ALPHA_7_III)
        assertEquals(CameraLabel("Sony Alpha 7 III", "Full Frame (35 mm)"), state.camera)
        assertEquals(8.0, state.aperture.value.nominal, 0.0)
        // 50 mm, f/8, 3 m on full frame
        assertEquals(2.36, state.result!!.nearM, 0.005)
        assertEquals(4.12, state.result.farM!!, 0.005)
        assertEquals(10.88, state.result.hyperfocalM, 0.005)
    }

    @Test
    fun aCoarserStepMovesTheApertureToTheNearestStop() {
        val f63 = DofInput(aperture = 6.35)
        assertEquals(6.3, DofUiState.from(f63, Settings(), ALPHA_7_III).aperture.value.nominal, 0.0)
        assertEquals(5.6, DofUiState.from(f63, Settings(stopStep = StopStep.FULL), ALPHA_7_III).aperture.value.nominal, 0.0)
    }

    @Test
    fun focusingAtTheHyperfocalReachesInfinity() {
        val h = DofUiState.from(DofInput(), Settings(), ALPHA_7_III).result!!.hyperfocalM
        val state = DofUiState.from(DofInput().reduce(DofAction.FocusAt(h)), Settings(), ALPHA_7_III)
        assertNull(state.result!!.farM)
    }

    @Test
    fun `a focus closer than the focal length is an error for that field`() {
        val state = DofUiState.from(DofInput(focusM = 0.03), Settings(), ALPHA_7_III)
        assertNull(state.result)
        assertEquals(Outcome.Invalid(Input.FOCUS_DISTANCE, minimum = 0.05), state.invalid)
    }

    @Test
    fun withoutAFormatThereIsNoResult() {
        val state = DofUiState.from(DofInput(), Settings(), null)
        assertNull(state.camera)
        assertNull(state.result)
        assertNull(state.invalid)
    }

    @Test
    fun imperialUnitsShowTheFocusInFeet() {
        val state = DofUiState.from(DofInput(focusM = 3.048), Settings(units = UnitSystem.IMPERIAL), ALPHA_7_III)
        assertEquals(10.0, state.focus, 1e-9)
        assertEquals(3.048, state.focusM, 0.0)
    }
}
