package io.github.ziomekmateusz.iso100.calc.astro

import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import java.time.LocalDate
import java.time.ZonedDateTime

enum class MoonPhaseName {
    NEW_MOON, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS, FULL_MOON, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT,
}

data class MoonPhaseTime(val phase: MoonPhaseName, val time: ZonedDateTime)

/** [illumination] goes from 0 (new moon) to 1 (full moon); [nextPhases] are the next new moon, first quarter, full moon and last quarter, in time order. */
data class MoonState(val illumination: Double, val phase: MoonPhaseName, val waxing: Boolean, val nextPhases: List<MoonPhaseTime>)

/** The moon at an exact instant; the phase times are in the time zone of [at]. */
fun moonState(at: ZonedDateTime): MoonState {
    val illumination = MoonIllumination.compute().on(at).execute()
    val next = listOf(MoonPhase.Phase.NEW_MOON, MoonPhase.Phase.FIRST_QUARTER, MoonPhase.Phase.FULL_MOON, MoonPhase.Phase.LAST_QUARTER)
        .map { MoonPhaseTime(MoonPhaseName.valueOf(it.name), MoonPhase.compute().on(at).phase(it).execute().time) }
        .sortedBy { it.time.toInstant() }
    return MoonState(
        illumination = illumination.fraction,
        phase = MoonPhaseName.valueOf(illumination.closestPhase.name),
        waxing = illumination.phase < 0,
        nextPhases = next,
    )
}

/** Moonrise and moonset on [date], searched from 00:00 within the day. */
fun moonDay(date: LocalDate, place: Place): Crossing {
    val t = MoonTimes.compute().on(date.atStartOfDay(place.zone)).at(place.latitude, place.longitude).oneDay().execute()
    return Crossing(t.rise, t.set, t.isAlwaysUp, t.isAlwaysDown)
}

fun moonPosition(at: ZonedDateTime, place: Place): SkyPosition {
    val p = MoonPosition.compute().on(at).at(place.latitude, place.longitude).execute()
    return SkyPosition(p.altitude, p.azimuth)
}
