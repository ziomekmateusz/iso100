package io.github.ziomekmateusz.iso100.ui.fov

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FovContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val focalField = hasContentDescription("Focal length") and hasSetTextAction()

    @Test
    fun theAnglesOfTheChosenCameraAreShown() {
        compose.setContent { Iso100Theme { FovContent(FovUiState.from(FovInput(), Settings(), ALPHA_7_III, TEST_FORMATS), {}, {}, {}) } }
        compose.onNodeWithText("73.7°").assertIsDisplayed()
        compose.onNodeWithText("Sony Alpha 7 III · Full Frame (35 mm)").assertIsDisplayed()
    }

    @Test
    fun theFieldSignalsTextThatIsNotANumberAndKeepsTheLastValidValue() {
        var input by mutableStateOf(FovInput())
        compose.setContent {
            Iso100Theme { FovContent(FovUiState.from(input, Settings(), ALPHA_7_III, TEST_FORMATS), { input = input.reduce(it) }, {}, {}) }
        }
        compose.onNode(focalField).performTextReplacement("35,5")
        compose.runOnIdle { assertEquals(35.5, input.focalLengthMm, 0.0) }
        compose.onNode(focalField).performTextReplacement("2.5.0")
        compose.onNode(focalField).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.runOnIdle { assertEquals(35.5, input.focalLengthMm, 0.0) }
    }

    @Test
    fun theStepButtonsSendTheirActions() {
        val actions = mutableListOf<FovAction>()
        compose.setContent {
            Iso100Theme { FovContent(FovUiState.from(FovInput(), Settings(), ALPHA_7_III, TEST_FORMATS), { actions += it }, {}, {}) }
        }
        compose.onNodeWithContentDescription("Longer focal length").performScrollTo().performClick()
        assertEquals(listOf<FovAction>(FovAction.StepFocalLength(up = true)), actions)
    }

    @Test
    fun withoutACameraTheEmptyStateAsksToChooseOne() {
        var chosen = false
        compose.setContent {
            Iso100Theme { FovContent(FovUiState.from(FovInput(), Settings(), null, TEST_FORMATS), {}, {}, { chosen = true }) }
        }
        compose.onNodeWithText("Choose the camera or the sensor format to calculate the field of view.").assertIsDisplayed()
        // the chip and the button both say "Choose camera": the button comes last
        compose.onAllNodes(hasText("Choose camera")).onLast().performClick()
        assertEquals(true, chosen)
    }

    @Test
    fun theTypedValuesSurviveTheRestorationOfTheState() {
        val tester = StateRestorationTester(compose)
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        tester.setContent { CompositionLocalProvider(LocalAppContainer provides container) { Iso100Theme { FovScreen({}, {}) } } }
        compose.onNode(focalField).performTextReplacement("35")
        tester.emulateSavedInstanceStateRestore()
        compose.onNode(focalField).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("35")))
    }
}
