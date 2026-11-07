package io.github.ziomekmateusz.iso100.ui.exposure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.NdFilter
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class ExposureContentTest {
    @get:Rule
    val compose = createComposeRule()

    private var timer: Pair<Double, String>? = null

    private fun show(initial: ExposureInput = ExposureInput()): () -> ExposureInput {
        var input by mutableStateOf(initial)
        compose.setContent {
            Iso100Theme {
                ExposureContent(
                    ExposureUiState.from(input, Settings(), null),
                    { input = input.reduce(it) },
                    {},
                    { seconds, settings -> timer = seconds to settings },
                )
            }
        }
        return { input }
    }

    @Test
    fun theNewShutterNamesOnlyTheFilterTheDialsDoNotShow() {
        show(ExposureInput(nd = NdFilter.ND1000))
        compose.onNodeWithText("New shutter speed").assertIsDisplayed()
        compose.onNodeWithText("8 s").assertIsDisplayed()
        compose.onNodeWithText("with ND1000").assertIsDisplayed()
        compose.onNodeWithText("Metered · EV 13").assertIsDisplayed()
        compose.onNodeWithText("Start the timer").assertDoesNotExist()
    }

    @Test
    fun aShutterBeyond30SecondsOpensTheTimerWithTheSettings() {
        show(ExposureInput(measuredShutterS = 1.0, nd = NdFilter.ND1000))
        compose.onNodeWithText("with ND1000").assertIsDisplayed()
        compose.onNodeWithText("Start the timer").performScrollTo().performClick()
        assertEquals(1000.0, timer?.first ?: 0.0, 0.5)
        assertEquals("ISO 100 · f/8 · ND1000", timer?.second)
    }

    @Test
    fun theSegmentsChooseWhatToCalculate() {
        val input = show()
        compose.onNodeWithText("Shutter").assertIsSelected()
        compose.onNodeWithText("Calculate ISO").performClick()
        assertEquals(ExposureSetting.ISO, input().solve)
        compose.onNodeWithText("New ISO").assertIsDisplayed()
    }

    @Test
    fun `an aperture out of scale is a message`() {
        show(ExposureInput(solve = ExposureSetting.APERTURE, nd = NdFilter.ND1024))
        compose.onNodeWithText("Out of scale: it would take an aperture wider than f/0.7.").assertIsDisplayed()
    }

    @Test
    fun aFilterOfTheFullListTakesThePlaceOfMore() {
        val input = show()
        compose.onNodeWithText("More").performScrollTo().performClick()
        compose.onNodeWithText("ND16").performClick()
        assertEquals(NdFilter.ND16, input().nd)
        compose.onNodeWithText("ND16").assertIsSelected()
    }

    @Test
    fun theMeteredExposureIsEditedInASheet() {
        val input = show()
        compose.onNodeWithText("ISO 100").performClick()
        compose.onNodeWithText("Metered exposure").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(null, input().sheet) }
    }
}
