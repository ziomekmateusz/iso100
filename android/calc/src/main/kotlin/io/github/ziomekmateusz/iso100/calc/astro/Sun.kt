package io.github.ziomekmateusz.iso100.calc.astro

import org.shredzone.commons.suncalc.SunPosition
import org.shredzone.commons.suncalc.SunTimes
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** The phases of daylight shown in the sky band, from the altitude of the sun. */
enum class SkyPhase { NIGHT, ASTRONOMICAL, NAUTICAL, CIVIL, GOLDEN, DAY }

data class SkySegment(val phase: SkyPhase, val start: ZonedDateTime, val end: ZonedDateTime)

/**
 * The sun on [date] in the time zone of the place. Events are searched from 00:00 for one day;
 * golden hour is [-4° +6°], blue hour [-8° -4°], full darkness below -18°.
 */
data class SunDay(
    val date: LocalDate,
    val sunriseSunset: Crossing,
    val civil: Crossing,
    val nautical: Crossing,
    val astronomical: Crossing,
    val noon: ZonedDateTime?,
    val daylight: Duration,
    val sky: List<SkySegment>,
    val goldenHours: List<TimeRange>,
    val blueHours: List<TimeRange>,
    val darkness: List<TimeRange>,
)

fun sunDay(date: LocalDate, place: Place): SunDay {
    val start = date.atStartOfDay(place.zone)
    val end = date.plusDays(1).atStartOfDay(place.zone)
    val day = Duration.between(start, end)
    val visual = sunTimes(place, start, day, SunTimes.Twilight.VISUAL)
    val bands = bands(place, start, end)
    return SunDay(
        date = date,
        sunriseSunset = visual.crossing(),
        civil = sunTimes(place, start, day, SunTimes.Twilight.CIVIL).crossing(),
        nautical = sunTimes(place, start, day, SunTimes.Twilight.NAUTICAL).crossing(),
        astronomical = sunTimes(place, start, day, SunTimes.Twilight.ASTRONOMICAL).crossing(),
        noon = visual.noon,
        daylight = daylight(visual, start, end),
        sky = bands.fold(mutableListOf()) { sky, band ->
            val phase = PHASES[band.index]
            if (sky.lastOrNull()?.phase == phase) sky[sky.lastIndex] = sky.last().copy(end = band.end)
            else sky += SkySegment(phase, band.start, band.end)
            sky
        },
        goldenHours = ranges(place, bands, 5..5, start, end),
        blueHours = ranges(place, bands, 3..4, start, end),
        darkness = ranges(place, bands, 0..0, start, end),
    )
}

/** Altitude (without refraction, like the sky bands) and azimuth of the sun. */
fun sunPosition(at: ZonedDateTime, place: Place): SkyPosition {
    val p = SunPosition.compute().on(at).at(place.latitude, place.longitude).execute()
    return SkyPosition(p.trueAltitude, p.azimuth)
}

/** True from the end of the civil twilight to its start the next morning, that is with the sun below -6°. */
fun isCivilNight(at: ZonedDateTime, place: Place): Boolean = sunPosition(at, place).altitudeDeg < EDGES[3]

// Band i lies between EDGES[i - 1] and EDGES[i]: night, astronomical, nautical, nautical and blue, civil and blue, golden, day.
private val EDGES = doubleArrayOf(-18.0, -12.0, -8.0, -6.0, -4.0, 6.0)
private val PHASES = listOf(
    SkyPhase.NIGHT, SkyPhase.ASTRONOMICAL, SkyPhase.NAUTICAL, SkyPhase.NAUTICAL, SkyPhase.CIVIL, SkyPhase.GOLDEN, SkyPhase.DAY,
)

private class Band(val index: Int, val start: ZonedDateTime, val end: ZonedDateTime)

private fun sunTimes(place: Place, from: ZonedDateTime, window: Duration, twilight: SunTimes.Twilight): SunTimes =
    SunTimes.compute().on(from).at(place.latitude, place.longitude).twilight(twilight).limit(window).execute()

private fun sunTimes(place: Place, from: ZonedDateTime, window: Duration, angle: Double): SunTimes =
    SunTimes.compute().on(from).at(place.latitude, place.longitude).twilight(angle).limit(window).execute()

private fun SunTimes.crossing() = Crossing(rise, set, isAlwaysUp, isAlwaysDown)

private fun daylight(t: SunTimes, start: ZonedDateTime, end: ZonedDateTime): Duration {
    val rise = t.rise
    val set = t.set
    return when {
        t.isAlwaysUp -> Duration.between(start, end)
        rise != null && set != null ->
            if (rise < set) Duration.between(rise, set) else Duration.between(rise, end) + Duration.between(start, set)
        rise != null -> Duration.between(rise, end)
        set != null -> Duration.between(start, set)
        else -> Duration.ZERO
    }
}

/** The day split at every crossing of the edges, each piece classified by the sun altitude in its middle. */
private fun bands(place: Place, start: ZonedDateTime, end: ZonedDateTime): List<Band> {
    val window = Duration.between(start, end)
    val cuts = (
        listOf(start, end) + EDGES.flatMap { edge ->
            sunTimes(place, start, window, edge).let { listOfNotNull(it.rise, it.set) }
        }.filter { it in start..<end }
        ).distinctBy { it.toInstant() }.sortedBy { it.toInstant() }
    return cuts.zipWithNext { a, b ->
        val middle = a.plusSeconds(Duration.between(a, b).seconds / 2)
        val altitude = SunPosition.compute().on(middle).at(place.latitude, place.longitude).execute().trueAltitude
        Band(EDGES.count { altitude >= it }, a, b)
    }.fold(mutableListOf()) { merged, band ->
        val last = merged.lastOrNull()
        if (last?.index == band.index) merged[merged.lastIndex] = Band(band.index, last.start, band.end) else merged += band
        merged
    }
}

/** Runs of bands in [indices]; a run reaching the end of the day continues until the sun leaves those bands. */
private fun ranges(place: Place, bands: List<Band>, indices: IntRange, start: ZonedDateTime, end: ZonedDateTime): List<TimeRange> {
    val runs = mutableListOf<TimeRange>()
    var i = 0
    while (i < bands.size) {
        if (bands[i].index !in indices) {
            i++
            continue
        }
        var j = i
        while (j + 1 < bands.size && bands[j + 1].index in indices) j++
        val from = bands[i].start.takeUnless { it.isEqual(start) }
        val to = if (bands[j].end.isEqual(end)) exit(place, indices, end) else bands[j].end
        runs += TimeRange(from, to)
        i = j + 1
    }
    return runs
}

/** First time within a day after [from] when the sun goes below or above the bands [indices]. */
private fun exit(place: Place, indices: IntRange, from: ZonedDateTime): ZonedDateTime? {
    val day = Duration.ofDays(1)
    val down = if (indices.first > 0) sunTimes(place, from, day, EDGES[indices.first - 1]).set else null
    val up = if (indices.last < EDGES.size) sunTimes(place, from, day, EDGES[indices.last]).rise else null
    return listOfNotNull(down, up).minByOrNull { it.toInstant() }
}
