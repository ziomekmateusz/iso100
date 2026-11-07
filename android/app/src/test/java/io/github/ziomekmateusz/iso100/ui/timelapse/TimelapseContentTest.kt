package io.github.ziomekmateusz.iso100.ui.timelapse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class TimelapseContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(initial: TimelapseInput = TimelapseInput()): () -> TimelapseInput {
        var input by mutableStateOf(initial)
        compose.setContent { Iso100Theme { TimelapseContent(TimelapseUiState.from(input), { input = input.reduce(it) }, {}) } }
        return { input }
    }

    @Test
    fun anHourAtFiveSecondsMakesAlmostHalfAMinuteOfVideo() {
        show()
        compose.onNodeWithText("28.8 s").assertIsDisplayed()
        compose.onNodeWithText("720").assertIsDisplayed()
        compose.onNodeWithText("18 GB").assertIsDisplayed()
        compose.onNode(hasText("25") and isSelectable()).assertIsSelected()
    }

    @Test
    fun theHoursAndMinutesAreTypedApart() {
        val input = show()
        compose.onNode(hasContentDescription("Hours") and hasSetTextAction()).performScrollTo().performTextReplacement("2")
        compose.onNode(hasContentDescription("Minutes") and hasSetTextAction()).performTextReplacement("30")
        compose.runOnIdle { assertEquals(9000.0, input().recordingS, 0.0) }
        compose.onNodeWithText("1 min 12 s").assertIsDisplayed()
    }

    @Test
    fun `a zero interval shows a message and no result`() {
        show(TimelapseInput(intervalS = 0.0))
        compose.onNodeWithText("The interval must be greater than 0 s.").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("—").assertCountEquals(3)
    }

    @Test
    fun calculatingTheIntervalAsksForTheVideoLength() {
        val input = show()
        compose.onNodeWithText("Interval").performClick()
        assertEquals(TimelapseTarget.INTERVAL, input().solve)
        compose.onNodeWithText("4.8 s").assertIsDisplayed()
        compose.onNode(hasContentDescription("Video length") and hasSetTextAction()).performScrollTo().assertIsDisplayed()
    }
}
