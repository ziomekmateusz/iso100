package io.github.ziomekmateusz.iso100.ui.moon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.astro.MoonPhaseName
import io.github.ziomekmateusz.iso100.calc.astro.MoonPhaseTime
import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.capitalized
import io.github.ziomekmateusz.iso100.ui.components.DateDialog
import io.github.ziomekmateusz.iso100.ui.components.DateStepper
import io.github.ziomekmateusz.iso100.ui.components.MoonDisc
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.ResultTile
import io.github.ziomekmateusz.iso100.ui.components.TimeDialog
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.dateFormatter
import io.github.ziomekmateusz.iso100.ui.number
import io.github.ziomekmateusz.iso100.ui.place.LocationMessage
import io.github.ziomekmateusz.iso100.ui.place.PlaceChip
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.PlacePickerSheet
import io.github.ziomekmateusz.iso100.ui.place.rememberPlace
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import io.github.ziomekmateusz.iso100.ui.timeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun MoonScreen(onBack: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(MoonInput()) }
    val place = rememberPlace(app)
    val state = remember(input, place.choice) { MoonUiState.from(input, place.choice, Instant.now()) }
    MoonContent(state, onAction = { input = input.reduce(it) }, onBack = onBack, onLocate = place.locate)
    if (state.placePicker) PlacePickerSheet(onDismiss = { input = input.reduce(MoonAction.Dismiss) }, onLocate = place.locate)
}

@Composable
fun MoonContent(state: MoonUiState, onAction: (MoonAction) -> Unit, onBack: () -> Unit, onLocate: () -> Unit) {
    val locale = currentLocale()
    val time = timeFormatter()
    val date = state.at.toLocalDate()
    ToolScaffold(
        title = stringResource(R.string.tool_moon),
        onBack = onBack,
        actions = { PlaceChip(state.place) { onAction(MoonAction.OpenPlaces) } },
        bottomBar = {
            DateStepper(
                label = if (state.today) stringResource(R.string.today) else dateFormatter("EEEE", locale).format(date).capitalized(locale),
                value = stringResource(R.string.date_and_time, dateFormatter("EEEdMMMM", locale).format(date), time(state.at)),
                onPrevious = { onAction(MoonAction.SetDate(date.minusDays(1))) },
                onNext = { onAction(MoonAction.SetDate(date.plusDays(1))) },
                onClick = { onAction(MoonAction.OpenDatePicker) },
            )
        },
    ) {
        Phase(state, time)
        // without a place the phase is still right: only rise, set and position are missing
        val place = state.place
        if (place is PlaceChoice.Unavailable) LocationMessage(place.location, onLocate, { onAction(MoonAction.OpenPlaces) })
        NextPhases(state.moon.nextPhases, time)
    }
    when (state.dialog) {
        MoonDialog.DATE -> DateDialog(date, onPick = { onAction(MoonAction.SetDate(it)) }, onDismiss = { onAction(MoonAction.Dismiss) })

        MoonDialog.TIME -> TimeDialog(state.at.toLocalTime(), onPick = {
            onAction(MoonAction.SetTime(it))
        }, onDismiss = { onAction(MoonAction.Dismiss) })

        null -> Unit
    }
}

/** The disc, the illumination, the phase with the next quarter, rise and set and where the moon is (with a place). */
@Composable
private fun Phase(state: MoonUiState, time: (ZonedDateTime) -> String) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val moon = state.moon
    // rounded down: 99.6% is not yet the full moon of tomorrow
    val percent = (moon.illumination * 100).toInt()
    val phase = stringResource(phaseName(moon.phase))
    ResultCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MoonDisc(moon.illumination, moon.waxing, 200.dp, pluralStringResource(R.plurals.moon_disc_description, percent, percent, phase))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$percent%", style = MaterialTheme.typography.displayMedium.copy(fontSize = 56.sp, lineHeight = 58.sp))
                Text(
                    // "Luna piena · luna piena domani" says it twice: the next phase alone is enough
                    listOfNotNull(
                        phase.takeIf {
                            moon.phase != moon.nextPhases.firstOrNull()?.phase
                        },
                        moon.nextPhases.firstOrNull()?.let { nextQuarter(it, state.at) },
                    )
                        .joinToString(" " + stringResource(R.string.separator)),
                    fontSize = 17.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        val riseSet = state.riseSet
        val position = state.position
        if (riseSet != null && position != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultTile(stringResource(R.string.moon_rise), riseSet.rise?.let(time) ?: "—", Modifier.weight(1f))
                ResultTile(stringResource(R.string.moon_set), riseSet.set?.let(time) ?: "—", Modifier.weight(1f))
            }
            val direction = stringArrayResource(R.array.compass_points)[compassPoint(position.azimuthDeg)]
            val altitude = number(position.altitudeDeg, 0, locale)
            Text(
                stringResource(
                    if (position.altitudeDeg >=
                        0
                    ) {
                        R.string.moon_position_above
                    } else {
                        R.string.moon_position_below
                    },
                    time(state.at),
                    altitude,
                    direction,
                ),
                fontSize = 15.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** "primo quarto domani", "luna piena oggi", "ultimo quarto dom 1 nov". */
@Composable
private fun nextQuarter(next: MoonPhaseTime, at: ZonedDateTime): String {
    val locale = currentLocale()
    val name = stringResource(phaseName(next.phase)).lowercase(locale)
    val day = next.time.toLocalDate()
    return when (day) {
        at.toLocalDate() -> stringResource(R.string.moon_next_today, name)
        at.toLocalDate().plusDays(1) -> stringResource(R.string.moon_next_tomorrow, name)
        else -> stringResource(R.string.moon_next_on, name, dateFormatter("EEEdMMM", locale).format(day))
    }
}

@Composable
private fun NextPhases(phases: List<MoonPhaseTime>, time: (ZonedDateTime) -> String) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.moon_next_phases), Modifier.padding(start = 4.dp), fontSize = 15.sp, color = colors.onSurfaceVariant)
        phases.forEach { phase ->
            val name = stringResource(phaseName(phase.phase))
            val whenText = stringResource(R.string.date_and_time, dateFormatter("EEEdMMM", locale).format(phase.time), time(phase.time))
            Surface(shape = MaterialTheme.shapes.small, color = colors.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clearAndSetSemantics { contentDescription = "$name, $whenText" },
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MoonDisc(
                        quarterIllumination(phase.phase),
                        waxing = phase.phase == MoonPhaseName.FIRST_QUARTER,
                        size = 22.dp,
                        description = null,
                        outlined = true,
                    )
                    Text(name, Modifier.weight(1f), fontSize = 17.sp)
                    Text(
                        whenText,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun quarterIllumination(phase: MoonPhaseName): Double = when (phase) {
    MoonPhaseName.NEW_MOON -> 0.0
    MoonPhaseName.FULL_MOON -> 1.0
    else -> 0.5
}

private fun phaseName(phase: MoonPhaseName): Int = when (phase) {
    MoonPhaseName.NEW_MOON -> R.string.moon_new
    MoonPhaseName.WAXING_CRESCENT -> R.string.moon_waxing_crescent
    MoonPhaseName.FIRST_QUARTER -> R.string.moon_first_quarter
    MoonPhaseName.WAXING_GIBBOUS -> R.string.moon_waxing_gibbous
    MoonPhaseName.FULL_MOON -> R.string.moon_full
    MoonPhaseName.WANING_GIBBOUS -> R.string.moon_waning_gibbous
    MoonPhaseName.LAST_QUARTER -> R.string.moon_last_quarter
    MoonPhaseName.WANING_CRESCENT -> R.string.moon_waning_crescent
}

private val rome = PlaceChoice.Ready(Place(41.9, 12.5, ZoneId.of("Europe/Rome")), "Roma", current = true)
private val evening = Instant.parse("2026-10-17T20:00:00Z")

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MoonPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { MoonContent(MoonUiState.from(MoonInput(), rome, evening), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MoonNoPlacePreview() {
    Iso100Theme { MoonContent(MoonUiState.from(MoonInput(), PlaceChoice.Unavailable(LocationState.NotFound), evening), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun MoonLargeTextPreview() {
    Iso100Theme { MoonContent(MoonUiState.from(MoonInput(), rome, evening), {}, {}, {}) }
}
