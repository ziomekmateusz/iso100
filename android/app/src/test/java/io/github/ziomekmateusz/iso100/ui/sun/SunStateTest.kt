package io.github.ziomekmateusz.iso100.ui.sun

import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.calc.astro.SkyPhase
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.data.SavedPlace
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.choosePlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val ROME = PlaceChoice.Ready(Place(41.9028, 12.4964, ZoneId.of("Europe/Rome")), "Roma", current = true)
val TROMSO = PlaceChoice.Ready(Place(69.6496, 18.956, ZoneId.of("Europe/Oslo")), "Tromsø", current = false)
val JUNE_21: Instant = Instant.parse("2026-06-21T10:00:00Z")

class SunStateTest {
    private fun on(date: LocalDate, place: PlaceChoice = ROME) = SunUiState.from(SunInput(epochDay = date.toEpochDay()), place, JUNE_21)

    @Test
    fun `the chosen date is the one calculated`() {
        val june = on(LocalDate.of(2026, 6, 21)).day!!.sunriseSunset.rise!!
        val december = on(LocalDate.of(2026, 12, 21)).day!!.sunriseSunset.rise!!
        assertEquals(LocalDate.of(2026, 6, 21), june.toLocalDate())
        assertEquals(LocalDate.of(2026, 12, 21), december.toLocalDate())
        assertEquals(5, june.hour)
        assertEquals(7, december.hour)
    }

    @Test
    fun `in Tromso in June the sun does not set and the golden hour crosses midnight`() {
        val state = on(LocalDate.of(2026, 6, 21), TROMSO)
        val day = state.day!!
        assertTrue(day.sunriseSunset.alwaysAbove)
        assertNull(day.sunriseSunset.set)
        assertTrue(state.band.all { it.phase == SkyPhase.DAY || it.phase == SkyPhase.GOLDEN })
        val evening = state.goldenHours.evening!!
        assertEquals(LocalDate.of(2026, 6, 22), evening.end!!.toLocalDate())
    }

    @Test
    fun `the golden hour of the morning contains the sunrise, the evening one the sunset`() {
        val state = on(LocalDate.of(2026, 6, 21))
        val day = state.day!!
        val morning = state.goldenHours.morning!!
        val evening = state.goldenHours.evening!!
        assertTrue(morning.start!! < day.sunriseSunset.rise!! && day.sunriseSunset.rise!! < morning.end!!)
        assertTrue(evening.start!! < day.sunriseSunset.set!! && day.sunriseSunset.set!! < evening.end!!)
        assertTrue(state.blueHours.morning!!.end!! <= morning.start!!.plusMinutes(1))
        assertNull(state.darkness.morning!!.start)
    }

    @Test
    fun theBandCoversTheDayAndNowIsMarkedOnlyToday() {
        val today = SunUiState.from(SunInput(), ROME, JUNE_21)
        assertTrue(today.today)
        assertEquals(0f, today.band.first().start, 0f)
        assertEquals(1f, today.band.last().end, 0f)
        // 10:00 UTC is 12:00 in Rome in summer
        assertEquals(0.5f, today.now!!, 0.001f)
        assertNull(on(LocalDate.of(2026, 6, 22)).now)
    }

    @Test
    fun withoutAPlaceThereIsNoDay() {
        val state = SunUiState.from(SunInput(), PlaceChoice.Unavailable(LocationState.Locating), JUNE_21)
        assertNull(state.day)
        assertTrue(state.band.isEmpty())
    }

    @Test
    fun theActionsChangeTheInput() {
        val picking = SunInput().reduce(SunAction.OpenDatePicker)
        assertTrue(picking.datePicker)
        val picked = picking.reduce(SunAction.SetDate(LocalDate.of(2026, 12, 21)))
        assertEquals(LocalDate.of(2026, 12, 21).toEpochDay(), picked.epochDay)
        assertFalse(picked.datePicker)
        assertFalse(SunInput().reduce(SunAction.OpenPlaces).reduce(SunAction.Dismiss).placePicker)
    }

    @Test
    fun `a saved place wins over the position, and a deleted one falls back to it`() {
        val oslo = SavedPlace("oslo", "Oslo", 59.91, 10.75, "Europe/Oslo")
        val found = LocationState.Found(ROME.place, "Roma")
        assertEquals(PlaceChoice.Ready(oslo.place, "Oslo", current = false), choosePlace("oslo", listOf(oslo), found))
        assertEquals(ROME, choosePlace("deleted", listOf(oslo), found))
        assertEquals(PlaceChoice.Unavailable(LocationState.NotFound), choosePlace(null, listOf(oslo), LocationState.NotFound))
        assertNotNull(oslo.place.zone)
    }
}
