package io.github.ziomekmateusz.iso100.calc.astro

import io.github.ziomekmateusz.iso100.calc.assertTimeClose
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Independent of suncalc: aa.usno.navy.mil/api, read on 2026-10-31. */
class MoonTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val romePlace = Place(41.9028, 12.4964, rome)
    private val tromso = Place(69.6492, 18.9553, ZoneId.of("Europe/Oslo"))

    private fun at(text: String): ZonedDateTime = OffsetDateTime.parse(text).atZoneSameInstant(rome)

    @Test
    fun `next phases agree with the US Naval Observatory`() {
        val next = moonState(at("2026-06-21T15:00+02:00")).nextPhases
        assertEquals(
            listOf(MoonPhaseName.FIRST_QUARTER, MoonPhaseName.FULL_MOON, MoonPhaseName.LAST_QUARTER, MoonPhaseName.NEW_MOON),
            next.map { it.phase },
        )
        val tolerance = Duration.ofMinutes(5)
        assertTimeClose("2026-06-21T21:55Z", next[0].time, "first quarter", tolerance)
        assertTimeClose("2026-06-29T23:56Z", next[1].time, "full moon", tolerance)
        assertTimeClose("2026-07-07T19:29Z", next[2].time, "last quarter", tolerance)
        assertTimeClose("2026-07-14T09:43Z", next[3].time, "new moon", tolerance)
    }

    @Test
    fun `illumination and phase follow the lunar month`() {
        fun moon(text: String) = moonState(at(text))
        assertEquals(0.45, moon("2026-06-21T12:00Z").illumination, 0.02)
        assertEquals(0.90, moon("2026-12-21T12:00Z").illumination, 0.02)
        assertEquals(1.0, moon("2026-06-29T23:56Z").illumination, 0.01)
        assertEquals(MoonPhaseName.FULL_MOON, moon("2026-06-29T23:56Z").phase)
        assertEquals(0.0, moon("2026-07-14T09:43Z").illumination, 0.01)
        assertEquals(MoonPhaseName.NEW_MOON, moon("2026-07-14T09:43Z").phase)
        assertEquals(MoonPhaseName.WAXING_GIBBOUS, moon("2026-12-21T12:00Z").phase)
        assertTrue(moon("2026-12-21T12:00Z").waxing)
        assertFalse(moon("2026-07-03T12:00Z").waxing)
    }

    @Test
    fun `moonrise and moonset agree with the US Naval Observatory`() {
        val june = moonDay(LocalDate.of(2026, 6, 21), romePlace)
        assertTimeClose("2026-06-21T12:48+02:00", june.rise, "June rise")
        assertTimeClose("2026-06-21T00:39+02:00", june.set, "June set")
        val december = moonDay(LocalDate.of(2026, 12, 21), romePlace)
        assertTimeClose("2026-12-21T13:59+01:00", december.rise, "December rise")
        assertTimeClose("2026-12-21T04:34+01:00", december.set, "December set")
    }

    @Test
    fun `in the far north the moon can stay up all day`() {
        val day = moonDay(LocalDate.of(2026, 12, 21), tromso)
        assertTrue(day.alwaysAbove)
        assertNull(day.rise)
        assertNull(day.set)
    }

    @Test
    fun `position agrees with the US Naval Observatory`() {
        val position = moonPosition(ZonedDateTime.of(2026, 6, 21, 20, 0, 0, 0, ZoneOffset.UTC), romePlace)
        assertEquals(30.85, position.altitudeDeg, 0.5)
        assertEquals(233.85, position.azimuthDeg, 0.5)
    }

    @Test
    fun `the moon at 03 00 and at 15 00 is not the same`() {
        val night = moonState(ZonedDateTime.of(2026, 6, 21, 3, 0, 0, 0, rome))
        val afternoon = moonState(ZonedDateTime.of(2026, 6, 21, 15, 0, 0, 0, rome))
        assertEquals(0.409, night.illumination, 0.01)
        assertEquals(0.461, afternoon.illumination, 0.01)
        assertTrue(afternoon.illumination - night.illumination > 0.04)
    }
}
