package io.github.ziomekmateusz.iso100.ui.home

import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.ui.moon.MoonInput
import io.github.ziomekmateusz.iso100.ui.moon.MoonUiState
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.sun.SunInput
import io.github.ziomekmateusz.iso100.ui.sun.SunUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** The "Oggi" card of Home: the evening golden hour, the sunset and the moon, from the states of Sun and Moon. */
data class TodayUiState(
    val place: PlaceChoice,
    val date: LocalDate,
    /** The start of the evening golden hour; null when there is none (polar days). */
    val goldenHour: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val moonIllumination: Double,
    val moonWaxing: Boolean,
    /** "Alpha 7 III", or the format alone; null when nothing is chosen. */
    val camera: String?,
) {
    companion object {
        fun from(place: PlaceChoice, selection: Selection?, now: Instant): TodayUiState {
            val sun = SunUiState.from(SunInput(), place, now)
            val moon = MoonUiState.from(MoonInput(), place, now)
            return TodayUiState(
                place = place,
                date = sun.date,
                goldenHour = sun.goldenHours.evening?.start,
                sunset = sun.day?.sunriseSunset?.set,
                moonIllumination = moon.moon.illumination,
                moonWaxing = moon.moon.waxing,
                camera = selection?.let { it.camera?.model ?: it.format.name },
            )
        }
    }
}
