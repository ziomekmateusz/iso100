package io.github.ziomekmateusz.iso100.calc.astro

import io.github.ziomekmateusz.iso100.calc.assertTimeClose
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SunTest {
    private val rome = Place(41.9028, 12.4964, ZoneId.of("Europe/Rome"))
    private val tromso = Place(69.6492, 18.9553, ZoneId.of("Europe/Oslo"))
    private val reykjavik = Place(64.1466, -21.9426, ZoneId.of("Atlantic/Reykjavik"))
    private val longyearbyen = Place(78.2232, 15.6267, ZoneId.of("Arctic/Longyearbyen"))
    private val sydney = Place(-33.8688, 151.2093, ZoneId.of("Australia/Sydney"))

    private fun assertAltitude(expected: Double, at: ZonedDateTime, place: Place, what: String) =
        assertEquals(expected, sunPosition(at, place).altitudeDeg, 0.2, what)

    @Test
    fun `twilights and photographic hours begin and end at their altitudes`() {
        for (date in listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))) {
            val day = sunDay(date, rome)
            for ((altitude, crossing) in listOf(-6.0 to day.civil, -12.0 to day.nautical, -18.0 to day.astronomical)) {
                assertAltitude(altitude, crossing.rise!!, rome, "$date rise at $altitude")
                assertAltitude(altitude, crossing.set!!, rome, "$date set at $altitude")
            }
            val (morningGolden, eveningGolden) = day.goldenHours
            assertAltitude(-4.0, morningGolden.start!!, rome, "$date golden morning start")
            assertAltitude(6.0, morningGolden.end!!, rome, "$date golden morning end")
            assertAltitude(6.0, eveningGolden.start!!, rome, "$date golden evening start")
            assertAltitude(-4.0, eveningGolden.end!!, rome, "$date golden evening end")
            val (morningBlue, eveningBlue) = day.blueHours
            assertAltitude(-8.0, morningBlue.start!!, rome, "$date blue morning start")
            assertAltitude(-4.0, morningBlue.end!!, rome, "$date blue morning end")
            assertAltitude(-4.0, eveningBlue.start!!, rome, "$date blue evening start")
            assertAltitude(-8.0, eveningBlue.end!!, rome, "$date blue evening end")
        }
    }

    @Test
    fun `full darkness runs from the end of the astronomical twilight into the next morning`() {
        val day = sunDay(LocalDate.of(2026, 12, 21), rome)
        val morning = day.darkness.first()
        val evening = day.darkness.last()
        assertNull(morning.start, "it began the day before")
        assertTimeClose(day.astronomical.rise!!.toOffsetDateTime().toString(), morning.end, "morning end")
        assertTimeClose(day.astronomical.set!!.toOffsetDateTime().toString(), evening.start, "evening start")
        assertEquals(LocalDate.of(2026, 12, 22), evening.end!!.toLocalDate())
    }

    @Test
    fun `the sky band covers the whole day without gaps`() {
        val dates = listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 6, 21), LocalDate.of(2026, 9, 23), LocalDate.of(2026, 12, 21))
        for (place in listOf(rome, reykjavik, tromso, longyearbyen, sydney)) for (date in dates) {
            val sky = sunDay(date, place).sky
            assertTrue(sky.first().start.isEqual(date.atStartOfDay(place.zone)), "$place $date starts at 00:00")
            assertTrue(sky.last().end.isEqual(date.plusDays(1).atStartOfDay(place.zone)), "$place $date ends at 24:00")
            sky.zipWithNext { a, b ->
                assertTrue(a.end.isEqual(b.start), "$place $date has no gaps")
                assertNotEquals(a.phase, b.phase, "$place $date segments are merged")
            }
        }
    }

    @Test
    fun `daylight runs from sunrise to sunset, or all day under the midnight sun`() {
        assertEquals(914.0, sunDay(LocalDate.of(2026, 6, 21), rome).daylight.toMinutes().toDouble(), 2.0)
        assertEquals(594.0, sunDay(LocalDate.of(2026, 6, 21), sydney).daylight.toMinutes().toDouble(), 2.0)
        assertEquals(Duration.ofDays(1), sunDay(LocalDate.of(2026, 6, 21), tromso).daylight)
    }

    @Test
    fun `sunrise, sunset and noon agree with the NOAA Solar Calculator`() {
        // Independent of suncalc: tables of gml.noaa.gov/grad/solcalc/table.php, read on 2026-10-25, to the minute.
        val romeJune = sunDay(LocalDate.of(2026, 6, 21), rome)
        assertTimeClose("2026-06-21T05:35+02:00", romeJune.sunriseSunset.rise, "Rome June sunrise")
        assertTimeClose("2026-06-21T20:49+02:00", romeJune.sunriseSunset.set, "Rome June sunset")
        assertTimeClose("2026-06-21T13:11:43+02:00", romeJune.noon, "Rome June noon")
        val romeDecember = sunDay(LocalDate.of(2026, 12, 21), rome)
        assertTimeClose("2026-12-21T07:34+01:00", romeDecember.sunriseSunset.rise, "Rome December sunrise")
        assertTimeClose("2026-12-21T16:42+01:00", romeDecember.sunriseSunset.set, "Rome December sunset")
        val sydneyJune = sunDay(LocalDate.of(2026, 6, 21), sydney)
        assertTimeClose("2026-06-21T07:00+10:00", sydneyJune.sunriseSunset.rise, "Sydney June sunrise")
        assertTimeClose("2026-06-21T16:54+10:00", sydneyJune.sunriseSunset.set, "Sydney June sunset")
    }

    @Test
    fun `civil twilight agrees with the US Naval Observatory`() {
        // Independent of suncalc: aa.usno.navy.mil/api/rstt/oneday, read on 2026-10-31, to the minute.
        val june = sunDay(LocalDate.of(2026, 6, 21), rome).civil
        assertTimeClose("2026-06-21T05:00+02:00", june.rise, "June dawn")
        assertTimeClose("2026-06-21T21:23+02:00", june.set, "June dusk")
        val december = sunDay(LocalDate.of(2026, 12, 21), rome).civil
        assertTimeClose("2026-12-21T07:03+01:00", december.rise, "December dawn")
        assertTimeClose("2026-12-21T17:14+01:00", december.set, "December dusk")
    }

    @Test
    fun `the requested date is used`() {
        val june = sunDay(LocalDate.of(2026, 6, 21), rome)
        val december = sunDay(LocalDate.of(2026, 12, 21), rome)
        assertEquals(LocalDate.of(2026, 6, 21), june.sunriseSunset.rise!!.toLocalDate())
        assertEquals(LocalDate.of(2026, 12, 21), december.sunriseSunset.rise!!.toLocalDate())
        assertTrue(Duration.between(june.sunriseSunset.rise.toLocalTime(), december.sunriseSunset.rise.toLocalTime()) > Duration.ofHours(1))
    }

    @Test
    fun `missing events are modelled, not a crash`() {
        val midsummer = sunDay(LocalDate.of(2026, 6, 21), tromso)
        assertNull(midsummer.sunriseSunset.rise)
        assertNull(midsummer.sunriseSunset.set)
        assertTrue(midsummer.sunriseSunset.alwaysAbove)
        val midwinter = sunDay(LocalDate.of(2026, 12, 21), tromso)
        assertTrue(midwinter.sunriseSunset.alwaysBelow)
        assertEquals(Duration.ZERO, midwinter.daylight)
        val iceland = sunDay(LocalDate.of(2026, 6, 21), reykjavik)
        assertTrue(iceland.astronomical.alwaysAbove, "no astronomical twilight in Reykjavik in June")
        assertTrue(iceland.darkness.isEmpty())
        val arctic = sunDay(LocalDate.of(2026, 3, 20), longyearbyen)
        assertTimeClose("2026-03-20T00:20+01:00", arctic.nautical.rise, "a nautical dawn without a dusk")
        assertNull(arctic.nautical.set)
    }

    @Test
    fun `the evening golden hour of the midnight sun ends the next day`() {
        val golden = sunDay(LocalDate.of(2026, 6, 21), tromso).goldenHours.last()
        assertEquals(LocalDate.of(2026, 6, 21), golden.start!!.toLocalDate())
        assertEquals(LocalDate.of(2026, 6, 22), golden.end!!.toLocalDate())
    }

    @Test
    fun `civil night runs from the end of the civil twilight to its start`() {
        val civil = sunDay(LocalDate.of(2026, 6, 21), rome).civil
        assertTrue(isCivilNight(civil.set!!.plusMinutes(10), rome))
        assertTrue(!isCivilNight(civil.set!!.minusMinutes(10), rome))
        assertTrue(isCivilNight(civil.rise!!.minusMinutes(10), rome))
        assertTrue(!isCivilNight(civil.rise!!.plusMinutes(10), rome))
        // at noon of the solstice the sun is 90 - 41.9 + 23.4 degrees high
        assertEquals(71.5, sunPosition(LocalDate.of(2026, 6, 21).atTime(13, 15).atZone(rome.zone), rome).altitudeDeg, 0.5)
        // the midnight sun never makes it night
        assertTrue(!isCivilNight(LocalDate.of(2026, 6, 21).atTime(0, 30).atZone(tromso.zone), tromso))
    }

    @Test
    fun `places are equal only with the same coordinates and zone`() {
        assertNotEquals(rome, reykjavik)
        assertEquals(Place(41.9028, 12.4964, ZoneId.of("Europe/Rome")), rome)
        assertEquals(Place(41.9028, 12.4964, ZoneId.of("Europe/Rome")).hashCode(), rome.hashCode())
    }
}
