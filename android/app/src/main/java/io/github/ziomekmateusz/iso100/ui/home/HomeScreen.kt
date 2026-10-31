package io.github.ziomekmateusz.iso100.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.CameraPicker
import io.github.ziomekmateusz.iso100.ui.Destination
import io.github.ziomekmateusz.iso100.ui.Dof
import io.github.ziomekmateusz.iso100.ui.Exposure
import io.github.ziomekmateusz.iso100.ui.Fov
import io.github.ziomekmateusz.iso100.ui.Moon
import io.github.ziomekmateusz.iso100.ui.Settings
import io.github.ziomekmateusz.iso100.ui.Stars
import io.github.ziomekmateusz.iso100.ui.Sun
import io.github.ziomekmateusz.iso100.ui.Timelapse
import io.github.ziomekmateusz.iso100.ui.components.BigTime
import io.github.ziomekmateusz.iso100.ui.components.RoundIconButton
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.dateFormatter
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.placeName
import io.github.ziomekmateusz.iso100.ui.place.rememberPlace
import io.github.ziomekmateusz.iso100.ui.timeFormatter
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import java.time.Instant
import java.time.ZoneId

private class Tool(val destination: Destination, @StringRes val name: Int, @StringRes val hint: Int, @DrawableRes val icon: Int)

private class ToolGroup(@StringRes val title: Int, val tools: List<Tool>)

// The tools of Home, under the "Oggi" card.
private val groups = listOf(
    ToolGroup(
        R.string.group_exposure,
        listOf(
            Tool(Exposure, R.string.tool_exposure, R.string.tool_exposure_hint, R.drawable.ic_exposure),
            Tool(Stars, R.string.tool_stars, R.string.tool_stars_hint, R.drawable.ic_stars),
        ),
    ),
    ToolGroup(
        R.string.group_optics,
        listOf(
            Tool(Dof, R.string.tool_dof, R.string.tool_dof_hint, R.drawable.ic_dof),
            Tool(Fov, R.string.tool_fov, R.string.tool_fov_hint, R.drawable.ic_fov),
        ),
    ),
    ToolGroup(
        R.string.group_sky,
        listOf(
            Tool(Sun, R.string.tool_sun, R.string.tool_sun_hint, R.drawable.ic_sun),
            Tool(Moon, R.string.tool_moon, R.string.tool_moon_hint, R.drawable.ic_moon),
        ),
    ),
    ToolGroup(R.string.group_time, listOf(Tool(Timelapse, R.string.tool_timelapse, R.string.tool_timelapse_hint, R.drawable.ic_timelapse))),
)

@Composable
fun HomeScreen(onOpen: (Destination) -> Unit, app: AppContainer = LocalAppContainer.current) {
    val place = rememberPlace(app)
    val selection by app.catalog.selection.collectAsStateWithLifecycle()
    val today = remember(place.choice, selection) { TodayUiState.from(place.choice, selection, Instant.now()) }
    HomeContent(today, onOpen, onLocate = place.locate)
}

@Composable
fun HomeContent(today: TodayUiState, onOpen: (Destination) -> Unit, onLocate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val name = stringResource(R.string.app_name)
            Text(
                buildAnnotatedString {
                    append(name.takeWhile { !it.isDigit() })
                    withStyle(SpanStyle(color = colors.primary)) { append(name.dropWhile { !it.isDigit() }) }
                },
                Modifier.semantics { heading() },
                style = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.02.em),
                color = colors.onSurface,
            )
            RoundIconButton(R.drawable.ic_settings, stringResource(R.string.settings), onClick = { onOpen(Settings) })
        }
        TodayCard(today, onOpen, onLocate)
        for (group in groups) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(group.title),
                    Modifier.padding(start = 4.dp).semantics { heading() },
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 0.04.em),
                    color = colors.onSurfaceVariant,
                )
                for (row in group.tools.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (tool in row) ToolTile(tool, onOpen, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTile(tool: Tool, onOpen: (Destination) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = { onOpen(tool.destination) },
        modifier = modifier.heightIn(min = 88.dp),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).background(colors.surfaceContainerHigh, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(tool.icon), contentDescription = null, Modifier.size(20.dp), tint = colors.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(tool.name),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
                Text(
                    stringResource(tool.hint),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Today where Sun and Moon are calculated: golden hour, sunset, moon; or what is missing to know it. */
@Composable
private fun TodayCard(today: TodayUiState, onOpen: (Destination) -> Unit, onLocate: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val time = timeFormatter()
    val place = today.place
    val date = dateFormatter("EEEEdMMMM", locale).format(today.date)
    Surface(shape = MaterialTheme.shapes.large, color = colors.surface) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (place is PlaceChoice.Ready) stringResource(R.string.today_in, placeName(place.name, place.place), date) else stringResource(R.string.today_plain, date),
                    Modifier.weight(1f).semantics { heading() },
                    fontSize = 16.sp,
                    color = colors.onSurfaceVariant,
                )
                Surface(
                    onClick = { onOpen(CameraPicker) },
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    modifier = Modifier.heightIn(min = 32.dp),
                ) {
                    Text(today.camera ?: stringResource(R.string.choose_camera), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, maxLines = 1)
                }
            }
            if (place is PlaceChoice.Unavailable) {
                TodayLocation(place.location, onLocate)
            } else {
                val value = MaterialTheme.typography.titleMedium
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodayValue(stringResource(R.string.golden_hour), Modifier.weight(1f)) {
                        BigTime(today.goldenHour?.let(time) ?: "—", value, color = colors.primary)
                    }
                    TodayValue(stringResource(R.string.sun_sunset), Modifier.weight(1f)) {
                        BigTime(today.sunset?.let(time) ?: "—", value)
                    }
                    val phase = stringResource(if (today.moonWaxing) R.string.moon_waxing else R.string.moon_waning)
                    TodayValue(stringResource(R.string.tool_moon), Modifier.weight(1f).semantics(mergeDescendants = true) { stateDescription = phase }) {
                        Text(
                            buildAnnotatedString {
                                append("${(today.moonIllumination * 100).toInt()}%")
                                withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant)) {
                                    append(if (today.moonWaxing) " ↑" else " ↓")
                                }
                            },
                            style = value,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayValue(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        value()
    }
}

/** A short line instead of the full message of Sun and Moon: Home is first of all the list of tools. */
@Composable
private fun TodayLocation(location: LocationState, onLocate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val (message, action) = when (location) {
            LocationState.Locating -> R.string.location_locating to null
            LocationState.PermissionDenied -> R.string.location_denied to R.string.allow
            LocationState.LocationOff -> R.string.location_off to R.string.retry
            LocationState.NotFound, is LocationState.Found -> R.string.location_not_found to R.string.retry
            LocationState.Unknown -> R.string.location_unknown to R.string.use_position
        }
        Text(stringResource(message), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (action != null) TextButton(onClick = onLocate) { Text(stringResource(action), fontSize = 16.sp) }
    }
}

private val previewToday = TodayUiState.from(
    PlaceChoice.Ready(Place(41.9, 12.5, ZoneId.of("Europe/Rome")), "Roma", current = true),
    null,
    Instant.parse("2026-10-17T10:00:00Z"),
).copy(camera = "Alpha 7 III")

@Preview(heightDp = 915, widthDp = 412)
@Composable
private fun HomePreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { HomeContent(previewToday, onOpen = {}, onLocate = {}) }
}

@Preview(heightDp = 915, widthDp = 412)
@Composable
private fun HomeWithoutPlacePreview() {
    Iso100Theme { HomeContent(previewToday.copy(place = PlaceChoice.Unavailable(LocationState.PermissionDenied), camera = null), onOpen = {}, onLocate = {}) }
}

@Preview(heightDp = 1600, widthDp = 412, fontScale = 2f)
@Composable
private fun HomeLargeTextPreview() {
    Iso100Theme { HomeContent(previewToday, onOpen = {}, onLocate = {}) }
}
