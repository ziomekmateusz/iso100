package io.github.ziomekmateusz.iso100.ui.moon

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.astro.Crossing
import io.github.ziomekmateusz.iso100.calc.astro.MoonState
import io.github.ziomekmateusz.iso100.calc.astro.SkyPosition
import io.github.ziomekmateusz.iso100.calc.astro.moonDay
import io.github.ziomekmateusz.iso100.calc.astro.moonPosition
import io.github.ziomekmateusz.iso100.calc.astro.moonState
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class MoonDialog { DATE, TIME }

/** [epochDay] and [minuteOfDay] null: now, in the zone of the place. The date picker is followed by the time picker. */
@Parcelize
data class MoonInput(
    val epochDay: Long? = null,
    val minuteOfDay: Int? = null,
    val dialog: MoonDialog? = null,
    val placePicker: Boolean = false,
) : Parcelable

sealed interface MoonAction {
    data class SetDate(val date: LocalDate) : MoonAction
    data class SetTime(val time: LocalTime) : MoonAction
    data object OpenDatePicker : MoonAction
    data object OpenPlaces : MoonAction

    /** Closes a picker. */
    data object Dismiss : MoonAction
}

fun MoonInput.reduce(action: MoonAction): MoonInput = when (action) {
    // a date chosen in the picker goes on to the time; the arrows change only the day
    is MoonAction.SetDate -> copy(epochDay = action.date.toEpochDay(), dialog = if (dialog == MoonDialog.DATE) MoonDialog.TIME else null)

    is MoonAction.SetTime -> copy(minuteOfDay = action.time.hour * 60 + action.time.minute, dialog = null)

    MoonAction.OpenDatePicker -> copy(dialog = MoonDialog.DATE)

    MoonAction.OpenPlaces -> copy(placePicker = true)

    MoonAction.Dismiss -> copy(dialog = null, placePicker = false)
}

data class MoonUiState(
    val at: ZonedDateTime,
    val today: Boolean,
    val place: PlaceChoice,
    /** Illumination and phases do not depend on the place, only on the instant. */
    val moon: MoonState,
    /** Null until there is a place. */
    val riseSet: Crossing?,
    val position: SkyPosition?,
    val dialog: MoonDialog?,
    val placePicker: Boolean,
) {
    companion object {
        fun from(input: MoonInput, place: PlaceChoice, now: Instant): MoonUiState {
            val ready = place as? PlaceChoice.Ready
            val zone = ready?.place?.zone ?: ZoneId.systemDefault()
            val local = now.atZone(zone).truncatedTo(ChronoUnit.MINUTES)
            val date = input.epochDay?.let(LocalDate::ofEpochDay) ?: local.toLocalDate()
            val time = input.minuteOfDay?.let { LocalTime.of(it / 60, it % 60) } ?: local.toLocalTime()
            val at = ZonedDateTime.of(date, time, zone)
            return MoonUiState(
                at = at,
                today = date == local.toLocalDate(),
                place = place,
                moon = moonState(at),
                riseSet = ready?.let { moonDay(date, it.place) },
                position = ready?.let { moonPosition(at, it.place) },
                dialog = input.dialog,
                placePicker = input.placePicker,
            )
        }
    }
}

/** The eight points of the compass, 0 = north, from an azimuth measured from north towards east. */
fun compassPoint(azimuthDeg: Double): Int = (((azimuthDeg % 360 + 360) % 360 + 22.5) / 45).toInt() % 8
