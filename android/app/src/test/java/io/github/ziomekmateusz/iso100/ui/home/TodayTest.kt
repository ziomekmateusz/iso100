package io.github.ziomekmateusz.iso100.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.Destination
import io.github.ziomekmateusz.iso100.ui.Moon
import io.github.ziomekmateusz.iso100.ui.Sun
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
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class TodayTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theCardTakesTheEveningGoldenHourAndTheSunsetFromTheSun() {
        val today = TodayUiState.from(ROME, JUNE_21)
        val sun = SunUiState.from(SunInput(), ROME, JUNE_21)
        assertEquals(sun.goldenHours.evening?.start, today.goldenHour)
        assertEquals(sun.day?.sunriseSunset?.set, today.sunset)
        assertNull(
            TodayUiState.from(
                PlaceChoice.Unavailable(LocationState.Unknown),
                JUNE_21,
            ).goldenHour,
        )
    }

    @Test
    fun minutesUntilAnEventAreNegativeOnceItHasPassed() {
        val now = Instant.parse("2026-11-05T13:00:00Z")
        assertEquals(140L, minutesBetween(now, Instant.parse("2026-11-05T15:20:00Z")))
        assertEquals(-62L, minutesBetween(now, Instant.parse("2026-11-05T11:58:00Z")))
        assertEquals(0L, minutesBetween(now, Instant.parse("2026-11-05T13:00:40Z")))
    }

    @Test
    fun theCardSaysWhereAndWhenAndOpensSunAndMoon() {
        val opened = mutableListOf<Destination>()
        val today = TodayUiState.from(ROME, JUNE_21)
        compose.setContent { Iso100Theme { HomeContent(today, { opened += it }, {}) } }
        compose.onNodeWithText("Today in Roma · Sunday, June 21").assertIsDisplayed()
        compose.onNodeWithText("Golden hour").performClick()
        compose.onNodeWithText(if (today.moonWaxing) "waxing" else "waning").performClick()
        assertEquals(listOf<Destination>(Sun, Moon), opened)
    }

    @Test
    fun withoutAPlaceTheCardAsksForThePosition() {
        var located = false
        compose.setContent {
            Iso100Theme {
                HomeContent(
                    TodayUiState.from(
                        PlaceChoice.Unavailable(LocationState.PermissionDenied),
                        JUNE_21,
                    ),
                    {},
                    { located = true },
                )
            }
        }
        compose.onNodeWithText("Allow").performClick()
        assertEquals(true, located)
    }
}
