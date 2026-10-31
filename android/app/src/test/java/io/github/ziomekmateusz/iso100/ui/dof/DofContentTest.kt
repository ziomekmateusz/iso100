package io.github.ziomekmateusz.iso100.ui.dof

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.calc.dof.DepthOfField
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.fov.ALPHA_7_III
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DofContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = DofUiState.from(DofInput(), Settings(), ALPHA_7_III)

    @Test
    fun theSharpZoneAndTheHyperfocalAreShown() {
        compose.setContent { Iso100Theme { DofContent(state.copy(result = DepthOfField(2.35, 4.15, 10.72)), {}, {}, {}) } }
        compose.onNodeWithContentDescription("Sharp from 2.35 m to 4.15 m").assertIsDisplayed()
        compose.onNodeWithText("1.80 m").assertIsDisplayed()
        compose.onNodeWithText("10.72 m").assertIsDisplayed()
    }

    @Test
    fun aFarLimitAtInfinityIsSaid() {
        compose.setContent { Iso100Theme { DofContent(state.copy(result = DepthOfField(1.32, null, 1.81)), {}, {}, {}) } }
        compose.onNodeWithContentDescription("Sharp from 1.32 m to infinity").assertIsDisplayed()
        compose.onNodeWithText("∞").assertIsDisplayed()
    }

    @Test
    fun theHyperfocalButtonFocusesAtTheHyperfocal() {
        val actions = mutableListOf<DofAction>()
        compose.setContent { Iso100Theme { DofContent(state, { actions += it }, {}, {}) } }
        compose.onNodeWithText("At hyperfocal").performScrollTo().performClick()
        assertEquals(listOf<DofAction>(DofAction.FocusAt(state.result!!.hyperfocalM)), actions)
    }

    @Test
    fun theDialIsASliderOnTheStopsOfTheScale() {
        var input by mutableStateOf(DofInput())
        compose.setContent { Iso100Theme { DofContent(DofUiState.from(input, Settings(), ALPHA_7_III), { input = input.reduce(it) }, {}, {}) } }
        val dial = compose.onNodeWithContentDescription("Aperture")
        dial.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "f/8"))
        dial.performSemanticsAction(SemanticsActions.SetProgress) { it(state.aperture.selected + 3f) }
        compose.runOnIdle { assertEquals(11.0, DofUiState.from(input, Settings(), ALPHA_7_III).aperture.value.nominal, 0.0) }
        dial.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "f/11"))
    }

    @Test
    fun draggingTheDialRightOpensTheAperture() {
        var input by mutableStateOf(DofInput())
        compose.setContent { Iso100Theme { DofContent(DofUiState.from(input, Settings(), ALPHA_7_III), { input = input.reduce(it) }, {}, {}) } }
        compose.onNodeWithContentDescription("Aperture").performScrollTo().performTouchInput { swipeRight() }
        compose.runOnIdle { assertTrue("f/${input.aperture}", input.aperture < 8.0) }
    }

    @Test
    fun `a focus closer than the focal length shows a message`() {
        compose.setContent { Iso100Theme { DofContent(DofUiState.from(DofInput(focusM = 0.03), Settings(), ALPHA_7_III), {}, {}, {}) } }
        compose.onNodeWithText("The focus distance must be greater than 0.05 m.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withoutACameraTheEmptyStateAsksToChooseOne() {
        compose.setContent { Iso100Theme { DofContent(DofUiState.from(DofInput(), Settings(), null), {}, {}, {}) } }
        compose.onNodeWithText("Choose the camera or the sensor format to calculate the depth of field.").assertIsDisplayed()
    }
}
