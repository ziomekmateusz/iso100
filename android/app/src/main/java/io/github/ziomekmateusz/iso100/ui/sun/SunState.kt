package io.github.ziomekmateusz.iso100.ui.sun

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.astro.SunDay
import io.github.ziomekmateusz.iso100.calc.astro.TimeRange
import io.github.ziomekmateusz.iso100.calc.astro.sunDay
import io.github.ziomekmateusz.iso100.ui.components.BandSegment
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** [epochDay] null: today in the zone of the place. */
@Parcelize
data class SunInput(val epochDay: Long? = null, val datePicker: Boolean = false, val placePicker: Boolean = false) : Parcelable

sealed interface SunAction {
    data class SetDate(val date: LocalDate) : SunAction
    data object OpenDatePicker : SunAction
    data object OpenPlaces : SunAction

    /** Closes the date picker or the place picker. */
    data object Dismiss : SunAction
}

fun SunInput.reduce(action: SunAction): SunInput = when (action) {
    is SunAction.SetDate -> copy(epochDay = action.date.toEpochDay(), datePicker = false)
    SunAction.OpenDatePicker -> copy(datePicker = true)
    SunAction.OpenPlaces -> copy(placePicker = true)
    SunAction.Dismiss -> copy(datePicker = false, placePicker = false)
}

/** A range of the morning and one of the evening, as the table "Per fotografare" shows them. */
data class DayParts(val morning: TimeRange?, val evening: TimeRange?)

data class SunUiState(
    val date: LocalDate,
    val today: Boolean,
    val place: PlaceChoice,
    /** Null until there is a place. */
    val day: SunDay?,
    val band: List<BandSegment>,
    /** Where "now" is on the band, only for today. */
    val now: Float?,
    val blueHours: DayParts,
    val goldenHours: DayParts,
    val darkness: DayParts,
    val datePicker: Boolean,
    val placePicker: Boolean,
) {
    companion object {
        fun from(input: SunInput, place: PlaceChoice, now: Instant): SunUiState {
            val ready = place as? PlaceChoice.Ready
            val zone = ready?.place?.zone ?: ZoneId.systemDefault()
            val today = now.atZone(zone).toLocalDate()
            // the chosen date, in the time zone of the place
            val date = input.epochDay?.let(LocalDate::ofEpochDay) ?: today
            val day = ready?.let { sunDay(date, it.place) }
            val start = date.atStartOfDay(zone)
            val length = Duration.between(start, date.plusDays(1).atStartOfDay(zone)).seconds.toFloat()
            fun fraction(t: ZonedDateTime) = (Duration.between(start, t).seconds / length).coerceIn(0f, 1f)
            val noon = day?.noon ?: date.atTime(12, 0).atZone(zone)
            return SunUiState(
                date = date,
                today = date == today,
                place = place,
                day = day,
                band = day?.sky.orEmpty().map { BandSegment(it.phase, fraction(it.start), fraction(it.end)) },
                now = if (day != null && date == today) fraction(now.atZone(zone)) else null,
                blueHours = parts(day?.blueHours, noon),
                goldenHours = parts(day?.goldenHours, noon),
                darkness = parts(day?.darkness, noon),
                datePicker = input.datePicker,
                placePicker = input.placePicker,
            )
        }

        /** Morning: begun the day before or before noon. Evening: from noon on, even when it ends after midnight. */
        fun parts(ranges: List<TimeRange>?, noon: ZonedDateTime): DayParts = DayParts(
            morning = ranges?.firstOrNull { range -> range.start.let { it == null || it < noon } },
            evening = ranges?.firstOrNull { range -> range.start.let { it != null && it >= noon } },
        )
    }
}
