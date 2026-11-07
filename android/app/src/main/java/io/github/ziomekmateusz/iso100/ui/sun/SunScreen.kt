package io.github.ziomekmateusz.iso100.ui.sun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.astro.Crossing
import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.calc.astro.TimeRange
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.capitalized
import io.github.ziomekmateusz.iso100.ui.components.BigTime
import io.github.ziomekmateusz.iso100.ui.components.DateDialog
import io.github.ziomekmateusz.iso100.ui.components.DateStepper
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.SkyBand
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.dateFormatter
import io.github.ziomekmateusz.iso100.ui.durationText
import io.github.ziomekmateusz.iso100.ui.place.LocationMessage
import io.github.ziomekmateusz.iso100.ui.place.PlaceChip
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.PlacePickerSheet
import io.github.ziomekmateusz.iso100.ui.place.rememberPlace
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.LocalSkyColors
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import io.github.ziomekmateusz.iso100.ui.timeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SunScreen(onBack: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(SunInput()) }
    val place = rememberPlace(app)
    val state = remember(input, place.choice) { SunUiState.from(input, place.choice, Instant.now()) }
    SunContent(state, onAction = { input = input.reduce(it) }, onBack = onBack, onLocate = place.locate)
    if (state.placePicker) PlacePickerSheet(onDismiss = { input = input.reduce(SunAction.Dismiss) }, onLocate = place.locate)
}

@Composable
fun SunContent(state: SunUiState, onAction: (SunAction) -> Unit, onBack: () -> Unit, onLocate: () -> Unit) {
    val locale = currentLocale()
    ToolScaffold(
        title = stringResource(R.string.tool_sun),
        onBack = onBack,
        actions = { PlaceChip(state.place) { onAction(SunAction.OpenPlaces) } },
        bottomBar = {
            DateStepper(
                label = if (state.today) {
                    stringResource(
                        R.string.today,
                    )
                } else {
                    dateFormatter("EEEE", locale).format(state.date).capitalized(locale)
                },
                value = dateFormatter("dMMMMy", locale).format(state.date),
                onPrevious = { onAction(SunAction.SetDate(state.date.minusDays(1))) },
                onNext = { onAction(SunAction.SetDate(state.date.plusDays(1))) },
                onClick = { onAction(SunAction.OpenDatePicker) },
            )
        },
    ) {
        val day = state.day
        val place = state.place
        if (day == null || place !is PlaceChoice.Ready) {
            LocationMessage((place as? PlaceChoice.Unavailable)?.location ?: LocationState.Unknown, onLocate, {
                onAction(SunAction.OpenPlaces)
            })
        } else {
            val time = timeFormatter()
            ResultCard {
                RiseAndSet(day.sunriseSunset, time)
                SkyBand(state.band, state.now, stringResource(R.string.sky_band_description))
                val noon = day.noon?.let(time) ?: "—"
                Text(
                    stringResource(R.string.sun_daylight, durationText(day.daylight.seconds.toDouble()), noon),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PhotoTable(state, time)
            Twilights(day.astronomical, day.nautical, day.civil, time)
        }
    }
    if (state.datePicker) DateDialog(state.date, onPick = { onAction(SunAction.SetDate(it)) }, onDismiss = { onAction(SunAction.Dismiss) })
}

/** Sunrise and sunset, or that the sun does not set or does not rise today. */
@Composable
private fun RiseAndSet(crossing: Crossing, time: (ZonedDateTime) -> String) {
    val big = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp, lineHeight = 50.sp)
    when {
        crossing.alwaysAbove -> Text(
            stringResource(R.string.sun_always_up),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
        )

        crossing.alwaysBelow -> Text(
            stringResource(R.string.sun_always_down),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
        )

        else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.sun_sunrise),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BigTime(crossing.rise?.let(time) ?: "—", big)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.sun_sunset),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BigTime(crossing.set?.let(time) ?: "—", big)
            }
        }
    }
}

/** Blue hour, golden hour and full darkness, morning and evening. */
@Composable
private fun PhotoTable(state: SunUiState, time: (ZonedDateTime) -> String) {
    val colors = MaterialTheme.colorScheme
    val sky = LocalSkyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            Text(stringResource(R.string.sun_for_photos), Modifier.weight(1.3f), fontSize = 14.sp, color = colors.onSurfaceVariant)
            Text(stringResource(R.string.morning), Modifier.weight(1f), fontSize = 14.sp, color = colors.onSurfaceVariant)
            Text(stringResource(R.string.evening), Modifier.weight(1f), fontSize = 14.sp, color = colors.onSurfaceVariant)
        }
        PhotoRow(
            stringResource(R.string.blue_hour),
            sky.civil,
            null,
            rangeText(state.blueHours.morning, time),
            rangeText(state.blueHours.evening, time),
        )
        PhotoRow(
            stringResource(R.string.golden_hour),
            sky.golden,
            null,
            rangeText(state.goldenHours.morning, time),
            rangeText(state.goldenHours.evening, time),
        )
        // darkness goes on into the night: only when it ends in the morning and when it starts in the evening
        PhotoRow(
            stringResource(R.string.full_darkness),
            sky.night,
            colors.onSurfaceVariant,
            state.darkness.morning?.end?.let { stringResource(R.string.range_until, time(it)) } ?: "—",
            state.darkness.evening?.start?.let { stringResource(R.string.range_from, time(it)) } ?: "—",
        )
    }
}

@Composable
private fun PhotoRow(label: String, dot: Color, dotBorder: Color?, morning: String, evening: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1.3f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(dot, CircleShape)
                        .then(
                            if (dotBorder != null) {
                                Modifier.border(
                                    2.dp,
                                    dotBorder,
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
                Text(label, fontSize = 16.sp)
            }
            Times(morning, evening, 17.sp, FontWeight.Medium)
        }
    }
}

@Composable
private fun Twilights(astronomical: Crossing, nautical: Crossing, civil: Crossing, time: (ZonedDateTime) -> String) {
    Column(Modifier.padding(horizontal = 4.dp)) {
        listOf(
            R.string.twilight_astronomical to astronomical,
            R.string.twilight_nautical to nautical,
            R.string.twilight_civil to civil,
        ).forEach { (label, crossing) ->
            Row(
                Modifier
                    .heightIn(min = 38.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(label), Modifier.weight(1.3f), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Times(crossing.rise?.let(time) ?: "—", crossing.set?.let(time) ?: "—", 16.sp, FontWeight.Normal)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

@Composable
private fun RowScope.Times(morning: String, evening: String, size: TextUnit, weight: FontWeight) {
    // titleMedium is Barlow with tabular figures
    val style = MaterialTheme.typography.titleMedium.copy(fontSize = size, lineHeight = size * 1.3f, fontWeight = weight)
    Text(morning, Modifier.weight(1f), style = style)
    Text(evening, Modifier.weight(1f), style = style)
}

/** "04:46–05:14"; "fino 02:56" when it began the day before; "da 22:35" when it does not end within a day. */
@Composable
private fun rangeText(range: TimeRange?, time: (ZonedDateTime) -> String): String {
    val start = range?.start
    val end = range?.end
    return when {
        range == null -> "—"
        start == null && end != null -> stringResource(R.string.range_until, time(end))
        start != null && end == null -> stringResource(R.string.range_from, time(start))
        start != null && end != null -> stringResource(R.string.range, time(start), time(end))
        else -> "—"
    }
}

private val rome = PlaceChoice.Ready(Place(41.9, 12.5, ZoneId.of("Europe/Rome")), "Roma", current = true)
private val june21 = Instant.parse("2026-06-21T10:00:00Z")

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SunPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { SunContent(SunUiState.from(SunInput(), rome, june21), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SunPolarPreview() {
    val tromso = PlaceChoice.Ready(Place(69.65, 18.96, ZoneId.of("Europe/Oslo")), "Tromsø", current = false)
    Iso100Theme { SunContent(SunUiState.from(SunInput(), tromso, june21), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SunNoPlacePreview() {
    Iso100Theme { SunContent(SunUiState.from(SunInput(), PlaceChoice.Unavailable(LocationState.PermissionDenied), june21), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun SunLargeTextPreview() {
    Iso100Theme { SunContent(SunUiState.from(SunInput(), rome, june21), {}, {}, {}) }
}
