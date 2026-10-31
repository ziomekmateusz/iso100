package io.github.ziomekmateusz.iso100.ui.sun

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class SunContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun todayInRomeWithThePlaceAndTheDate() {
        val actions = mutableListOf<SunAction>()
        compose.setContent { Iso100Theme { SunContent(SunUiState.from(SunInput(), ROME, JUNE_21), { actions += it }, {}, {}) } }
        compose.onNodeWithText("Roma").assertIsDisplayed()
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("June 21, 2026").assertIsDisplayed()
        compose.onNodeWithText("Golden hour").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous day").performClick()
        assertEquals(listOf<SunAction>(SunAction.SetDate(LocalDate.of(2026, 6, 20))), actions)
    }

    @Test
    fun `the midnight sun is said, not shown as missing times`() {
        compose.setContent { Iso100Theme { SunContent(SunUiState.from(SunInput(), TROMSO, JUNE_21), {}, {}, {}) } }
        compose.onNodeWithText("The sun does not set today").assertIsDisplayed()
        compose.onNodeWithText("Sunrise").assertDoesNotExist()
    }

    @Test
    fun aDeniedPermissionAsksAgainOrOffersAPlace() {
        var located = false
        val actions = mutableListOf<SunAction>()
        compose.setContent {
            Iso100Theme { SunContent(SunUiState.from(SunInput(), PlaceChoice.Unavailable(LocationState.PermissionDenied), JUNE_21), { actions += it }, {}, { located = true }) }
        }
        compose.onNodeWithText("Allow the approximate position, or choose a place.").assertIsDisplayed()
        compose.onNodeWithText("Allow").performClick()
        assertEquals(true, located)
        // the chip of the header and the button of the message
        compose.onAllNodesWithText("Choose place").assertCountEquals(2)
    }
}
