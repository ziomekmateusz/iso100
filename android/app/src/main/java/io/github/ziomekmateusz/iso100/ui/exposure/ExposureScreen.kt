package io.github.ziomekmateusz.iso100.ui.exposure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.APERTURE
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.ISO
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.SHUTTER
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureValue
import io.github.ziomekmateusz.iso100.calc.exposure.NdFilter
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.components.AppBottomSheet
import io.github.ziomekmateusz.iso100.ui.components.ControlSheet
import io.github.ziomekmateusz.iso100.ui.components.Dial
import io.github.ziomekmateusz.iso100.ui.components.ErrorText
import io.github.ziomekmateusz.iso100.ui.components.FitText
import io.github.ziomekmateusz.iso100.ui.components.PrimaryButton
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.RollingText
import io.github.ziomekmateusz.iso100.ui.components.SegmentedChoice
import io.github.ziomekmateusz.iso100.ui.components.StopDial
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.rememberToolInput
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.shutterNumber
import io.github.ziomekmateusz.iso100.ui.shutterText
import io.github.ziomekmateusz.iso100.ui.stopStepText
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

@Composable
fun ExposureScreen(
    onBack: () -> Unit,
    onStartTimer: (seconds: Double, settings: String) -> Unit,
    app: AppContainer = LocalAppContainer.current
) {
    var input by rememberToolInput("exposure", ExposureInput())
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val selection by app.catalog.selection.collectAsStateWithLifecycle()
    val state = remember(input, settings, selection) { ExposureUiState.from(input, settings, selection) }
    ExposureContent(
        state,
        onAction = { input = input.reduce(it) },
        onBack = onBack,
        onStartTimer = onStartTimer
    )
}

@Composable
fun ExposureContent(
    state: ExposureUiState,
    onAction: (ExposureAction) -> Unit,
    onBack: () -> Unit,
    onStartTimer: (seconds: Double, settings: String) -> Unit
) {
    ToolScaffold(
        title = stringResource(R.string.title_exposure),
        onBack = onBack,
        footer = { padding ->
            ControlSheet(padding) {
                state.kept.forEach { (setting, dial) ->
                    SettingDial(setting, dial, state.step) { onAction(ExposureAction.SetNew(setting, it.actual)) }
                }
            }
        },
    ) {
        SegmentedChoice(
            options = listOf(stringResource(R.string.solve_iso), stringResource(R.string.aperture), stringResource(R.string.shutter)),
            selected = state.solve.ordinal,
            onSelect = { onAction(ExposureAction.Solve(ExposureSetting.entries[it])) },
            description = stringResource(R.string.solve_label),
        )
        Measured(state, onAction)
        ResultCard { Result(state, onStartTimer) }
        NdFilters(state.nd, onAction)
    }
    when (state.sheet) {
        ExposureSheet.MEASURED -> MeasuredSheet(state, onAction)
        ExposureSheet.ND_FILTERS -> NdSheet(state.nd, onAction)
        null -> Unit
    }
}

@Composable
private fun Measured(state: ExposureUiState, onAction: (ExposureAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.measured_ev, shortNumber(state.ev, currentLocale())),
            Modifier.padding(start = 4.dp),
            fontSize = 15.sp,
            color = colors.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.measured.forEach { (setting, dial) ->
                OutlinedButton(
                    onClick = { onAction(ExposureAction.Open(ExposureSheet.MEASURED)) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    FitText(
                        settingText(setting, dial.value.nominal),
                        MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Result(
    state: ExposureUiState,
    onStartTimer: (seconds: Double, settings: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val big = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp, lineHeight = 64.sp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(resultTitle(state.solve)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        when (val result = state.result) {
            is Outcome.Ok -> {
                RollingText(
                    settingText(
                        state.solve,
                        result.value.nearest?.nominal ?: result.value.exact,
                        plain = true
                    ), style = big, color = colors.primary
                )
                if (state.exactDiffers) {
                    Text(stringResource(R.string.exact_value, settingText(state.solve, result.value.exact, plain = true)), fontSize = 15.sp, color = colors.onSurfaceVariant)
                }
            }
            is Outcome.OutOfScale -> {
                Text("—", style = big, color = colors.primary)
                ErrorText(outOfScaleMessage(state.solve, result.tooHigh, state.limit!!))
            }
            is Outcome.Invalid -> Text("—", style = big, color = colors.primary)
        }
        state.nd?.let {
            Text(
                stringResource(R.string.with_nd, it.name),
                fontSize = 15.sp,
                color = colors.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = state.bulb) {
            val settings = shotSettings(state)
            PrimaryButton(
                text = stringResource(R.string.start_timer),
                onClick = {
                    (state.result as? Outcome.Ok)?.let {
                        onStartTimer(
                            it.value.exact,
                            settings
                        )
                    }
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            )

        }
        (state.result as? Outcome.Ok)?.takeIf { state.bulb }?.let { bulb ->

        }
    }
}

/** "ISO 100 · f/8 · ND1000": the settings to shoot with, for the timer. */
@Composable
private fun shotSettings(state: ExposureUiState): String =
    (state.kept.map { (setting, dial) -> settingText(setting, dial.value.nominal) } + listOfNotNull(
        state.nd?.name
    ))
        .joinToString(" " + stringResource(R.string.separator))

@Composable
private fun NdFilters(nd: NdFilter?, onAction: (ExposureAction) -> Unit) {
    val locale = currentLocale()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.nd_filter), Modifier.padding(start = 4.dp), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NdChip(stringResource(R.string.nd_none), stringResource(R.string.nd_stops, "0"), nd == null, Modifier.weight(1f)) {
                onAction(ExposureAction.SetNd(null))
            }
            ND_CHIPS.forEach { filter ->
                NdChip(filter.name, stringResource(R.string.nd_stops, shortNumber(filter.stops, locale)), nd == filter, Modifier.weight(1f)) {
                    onAction(ExposureAction.SetNd(filter))
                }
            }
            val other = nd?.takeIf { it !in ND_CHIPS }
            NdChip(
                other?.name ?: stringResource(R.string.nd_more),
                other?.let { stringResource(R.string.nd_stops, shortNumber(it.stops, locale)) } ?: stringResource(R.string.nd_more_detail),
                other != null,
                Modifier.weight(1f),
            ) { onAction(ExposureAction.Open(ExposureSheet.ND_FILTERS)) }
        }
    }
}

@Composable
private fun NdChip(name: String, detail: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.outlineVariant else colors.background,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 6.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val color = if (selected) colors.onSurface else colors.onSurfaceVariant
            FitText(name, MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = color, textAlign = TextAlign.Center)
            FitText(detail, MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal), color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MeasuredSheet(state: ExposureUiState, onAction: (ExposureAction) -> Unit) {
    AppBottomSheet(onDismiss = { onAction(ExposureAction.CloseSheet) }) {
        Column(Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.measured_title), style = MaterialTheme.typography.titleLarge)
            state.measured.forEach { (setting, dial) ->
                SettingDial(setting, dial, state.step) { onAction(ExposureAction.SetMeasured(setting, it.actual)) }
            }
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.done),
                onClick = { onAction(ExposureAction.CloseSheet) }
            )
        }
    }
}

@Composable
private fun NdSheet(nd: NdFilter?, onAction: (ExposureAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    AppBottomSheet(onDismiss = { onAction(ExposureAction.CloseSheet) }) {
        Text(stringResource(R.string.nd_filters), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.padding(bottom = 16.dp)) {
            items(listOf<NdFilter?>(null) + NdFilter.entries) { filter ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = filter == nd,
                            onClick = { onAction(ExposureAction.SetNd(filter)) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(filter?.name ?: stringResource(R.string.nd_none), Modifier.weight(1f), fontSize = 18.sp)
                    if (filter != null) {
                        Text(
                            stringResource(R.string.nd_detail, filter.factor, shortNumber(filter.stops, locale), shortNumber(filter.opticalDensity, locale)),
                            fontFamily = Barlow,
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    if (filter == nd) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(22.dp), tint = colors.primary)
                    } else {
                        Spacer(Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingDial(setting: ExposureSetting, dial: Dial, step: StopStep, onSelect: (ExposureValue) -> Unit) {
    val locale = currentLocale()
    StopDial(
        label = stringResource(settingLabel(setting)),
        dial = dial,
        onSelect = onSelect,
        valueText = { settingText(setting, it.nominal, plain = true) },
        tickLabel = { if (setting == SHUTTER) shutterNumber(it.nominal, locale) else shortNumber(it.nominal, locale) },
        trailing = stopStepText(step),
    )
}

/** "ISO 100", "f/8", "1/125 s"; [plain] ISO without its name, as on the dial and in the result. */
@Composable
private fun settingText(setting: ExposureSetting, value: Double, plain: Boolean = false): String {
    val locale = currentLocale()
    return when (setting) {
        ISO -> shortNumber(value, locale, 0).let { if (plain) it else stringResource(R.string.iso_value, it) }
        APERTURE -> stringResource(R.string.aperture_value, shortNumber(value, locale))
        SHUTTER -> shutterText(value)
    }
}

private fun settingLabel(setting: ExposureSetting): Int = when (setting) {
    ISO -> R.string.iso
    APERTURE -> R.string.aperture
    SHUTTER -> R.string.shutter
}

private fun resultTitle(setting: ExposureSetting): Int = when (setting) {
    ISO -> R.string.result_iso
    APERTURE -> R.string.result_aperture
    SHUTTER -> R.string.result_shutter
}

/** The message of a result out of the scale: no value of the scale, but the end it goes past. */
@Composable
private fun outOfScaleMessage(setting: ExposureSetting, tooHigh: Boolean, limit: ExposureValue): String {
    val locale = currentLocale()
    return when (setting) {
        ISO -> stringResource(if (tooHigh) R.string.out_of_scale_iso_high else R.string.out_of_scale_iso_low, shortNumber(limit.nominal, locale, 0))
        APERTURE -> stringResource(if (tooHigh) R.string.out_of_scale_aperture_high else R.string.out_of_scale_aperture_low, shortNumber(limit.nominal, locale))
        // a shutter longer than the scale is still a result: only "too short" is out of scale
        SHUTTER -> stringResource(R.string.out_of_scale_shutter_low, shutterText(limit.nominal))
    }
}

private val previewState = ExposureUiState.from(ExposureInput(nd = NdFilter.ND1000), Settings(), null)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun ExposurePreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { ExposureContent(previewState, {}, {}, { _, _ -> }) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun ExposureOutOfScalePreview() {
    Iso100Theme {
        ExposureContent(
            ExposureUiState.from(
                ExposureInput(
                    solve = APERTURE,
                    nd = NdFilter.ND1024
                ), Settings(), null
            ), {}, {}, { _, _ -> })
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun ExposureLongShutterPreview() {
    Iso100Theme {
        ExposureContent(
            ExposureUiState.from(
                ExposureInput(
                    measuredShutterS = 1.0 / 8,
                    nd = NdFilter.ND1000
                ), Settings(), null
            ), {}, {}, { _, _ -> })
    }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun ExposureLargeTextPreview() {
    Iso100Theme { ExposureContent(previewState, {}, {}, { _, _ -> }) }
}
