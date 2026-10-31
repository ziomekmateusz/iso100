package io.github.ziomekmateusz.iso100.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.CameraPicker
import io.github.ziomekmateusz.iso100.ui.Destination
import io.github.ziomekmateusz.iso100.ui.fov.ALPHA_7_III
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.sun.JUNE_21
import io.github.ziomekmateusz.iso100.ui.sun.ROME
import io.github.ziomekmateusz.iso100.ui.sun.SunInput
import io.github.ziomekmateusz.iso100.ui.sun.SunUiState
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class TodayTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theCardTakesTheEveningGoldenHourAndTheSunsetFromTheSun() {
        val today = TodayUiState.from(ROME, ALPHA_7_III, JUNE_21)
        val sun = SunUiState.from(SunInput(), ROME, JUNE_21)
        assertEquals(sun.goldenHours.evening!!.start, today.goldenHour)
        assertEquals(sun.day!!.sunriseSunset.set, today.sunset)
        assertEquals("Alpha 7 III", today.camera)
        assertNull(TodayUiState.from(PlaceChoice.Unavailable(LocationState.Unknown), null, JUNE_21).goldenHour)
    }

    @Test
    fun theCardSaysWhereAndWhenAndOpensTheCamera() {
        val opened = mutableListOf<Destination>()
        compose.setContent { Iso100Theme { HomeContent(TodayUiState.from(ROME, ALPHA_7_III, JUNE_21), { opened += it }, {}) } }
        compose.onNodeWithText("Today in Roma · Sunday, June 21").assertIsDisplayed()
        compose.onNodeWithText("Golden hour").assertIsDisplayed()
        compose.onNodeWithText("Alpha 7 III").performClick()
        assertEquals(listOf<Destination>(CameraPicker), opened)
    }

    @Test
    fun withoutAPlaceTheCardAsksForThePosition() {
        var located = false
        compose.setContent {
            Iso100Theme { HomeContent(TodayUiState.from(PlaceChoice.Unavailable(LocationState.PermissionDenied), null, JUNE_21), {}, { located = true }) }
        }
        compose.onNodeWithText("Allow").performClick()
        assertEquals(true, located)
    }
}
