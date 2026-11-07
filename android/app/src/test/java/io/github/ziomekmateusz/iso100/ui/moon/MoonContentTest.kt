package io.github.ziomekmateusz.iso100.ui.moon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.sun.ROME
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class MoonContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val now = Instant.parse("2026-10-17T20:00:00Z")

    @Test
    fun theIlluminationThePhasesAndWhereTheMoonIs() {
        val state = MoonUiState.from(MoonInput(), ROME, now)
        val percent = (state.moon.illumination * 100).toInt()
        val actions = mutableListOf<MoonAction>()
        compose.setContent { Iso100Theme { MoonContent(state, { actions += it }, {}, {}) } }
        compose.onNodeWithText("$percent%").assertIsDisplayed()
        compose.onNodeWithText("Rises").assertIsDisplayed()
        compose.onNodeWithText("Next phases").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Next day").performClick()
        assertEquals(listOf<MoonAction>(MoonAction.SetDate(state.at.toLocalDate().plusDays(1))), actions)
    }

    @Test
    fun withoutAPlaceThePhaseStaysAndThePositionIsAsked() {
        compose.setContent {
            Iso100Theme { MoonContent(MoonUiState.from(MoonInput(), PlaceChoice.Unavailable(LocationState.Unknown), now), {}, {}, {}) }
        }
        compose.onNodeWithText("Use your position, or choose a place.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rises").assertDoesNotExist()
    }
}
