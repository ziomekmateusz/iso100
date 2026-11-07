package io.github.ziomekmateusz.iso100.ui.stars

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.calc.startrails.StarRule
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.fov.TEST_FORMATS
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class StarsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSuggestedShutterAndTheLimitAreShown() {
        val actions = mutableListOf<StarsAction>()
        compose.setContent {
            Iso100Theme { StarsContent(StarsUiState.from(StarsInput(), Settings(), ALPHA_7_III_24MP), { actions += it }, {}, {}) }
        }
        compose.onNodeWithText("Calculated limit 11.6 s, rounded down").assertIsDisplayed()
        compose.onNodeWithText("Sony Alpha 7 III · Full Frame (35 mm)").assertIsDisplayed()
        compose.onNode(hasText("300 rule") and hasClickAction()).performClick()
        assertEquals(listOf<StarsAction>(StarsAction.SetRule(StarRule.RULE_300)), actions)
    }

    @Test
    fun `without the resolution there is no NPF and no aperture`() {
        compose.setContent {
            Iso100Theme { StarsContent(StarsUiState.from(StarsInput(), Settings(), Selection(null, TEST_FORMATS[0])), {}, {}, {}) }
        }
        compose.onNodeWithText("NPF").assertDoesNotExist()
        compose.onNodeWithText("NPF only").assertDoesNotExist()
        compose.onNode(hasText("500 rule") and hasClickAction()).assertIsDisplayed()
    }
}
