package io.github.ziomekmateusz.iso100.ui.moon

import io.github.ziomekmateusz.iso100.calc.astro.moonState
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.sun.ROME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class MoonStateTest {
    private val now = Instant.parse("2026-10-17T20:00:00Z")

    @Test
    fun `the moon is calculated at the chosen instant in the zone of the place`() {
        val input = MoonInput(epochDay = LocalDate.of(2026, 10, 17).toEpochDay(), minuteOfDay = 15 * 60)
        val state = MoonUiState.from(input, ROME, now)
        val at = ZonedDateTime.of(LocalDate.of(2026, 10, 17), LocalTime.of(15, 0), ROME.place.zone)
        assertEquals(at, state.at)
        assertEquals(moonState(at), state.moon)
        // 20:00 UTC is 22:00 in Rome: the same day
        assertTrue(state.today)
        assertFalse(MoonUiState.from(input.copy(epochDay = input.epochDay!! + 1), ROME, now).today)
    }

    @Test
    fun nowIsTheDefaultAndRiseSetNeedAPlace() {
        val here = MoonUiState.from(MoonInput(), ROME, now)
        assertTrue(here.today)
        assertEquals(22, here.at.hour)
        assertNotNull(here.riseSet)
        val nowhere = MoonUiState.from(MoonInput(), PlaceChoice.Unavailable(LocationState.Unknown), now)
        assertNull(nowhere.riseSet)
        assertNull(nowhere.position)
        assertEquals(4, nowhere.moon.nextPhases.size)
    }

    @Test
    fun theDatePickerGoesOnToTheTimeAndTheArrowsDoNot() {
        val date = LocalDate.of(2026, 11, 1)
        val picking = MoonInput().reduce(MoonAction.OpenDatePicker).reduce(MoonAction.SetDate(date))
        assertEquals(MoonDialog.TIME, picking.dialog)
        val done = picking.reduce(MoonAction.SetTime(LocalTime.of(21, 30)))
        assertNull(done.dialog)
        assertEquals(21 * 60 + 30, done.minuteOfDay)
        assertNull(MoonInput().reduce(MoonAction.SetDate(date)).dialog)
    }

    @Test
    fun theAzimuthBecomesAPointOfTheCompass() {
        assertEquals(listOf(0, 1, 4, 5, 0, 0), listOf(0.0, 44.0, 180.0, 225.0, 350.0, -10.0).map(::compassPoint))
    }
}
