package io.github.ziomekmateusz.iso100.ui.stars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.startrails.StarRule
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.CameraSpecs
import io.github.ziomekmateusz.iso100.data.CatalogCamera
import io.github.ziomekmateusz.iso100.data.CatalogFormat
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.components.CameraChip
import io.github.ziomekmateusz.iso100.ui.components.ChooseCameraCard
import io.github.ziomekmateusz.iso100.ui.components.ControlSheet
import io.github.ziomekmateusz.iso100.ui.components.NumberField
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.SegmentedChoice
import io.github.ziomekmateusz.iso100.ui.components.StopDial
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.durationText
import io.github.ziomekmateusz.iso100.ui.messageFor
import io.github.ziomekmateusz.iso100.ui.number
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.shutterText
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

@Composable
fun StarsScreen(onBack: () -> Unit, onChooseCamera: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(StarsInput()) }
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val selection by app.catalog.selection.collectAsStateWithLifecycle()
    val state = remember(input, settings, selection) { StarsUiState.from(input, settings, selection) }
    StarsContent(state, onAction = { input = input.reduce(it) }, onBack = onBack, onChooseCamera = onChooseCamera)
}

@Composable
fun StarsContent(state: StarsUiState, onAction: (StarsAction) -> Unit, onBack: () -> Unit, onChooseCamera: () -> Unit) {
    val locale = currentLocale()
    ToolScaffold(
        title = stringResource(R.string.title_stars),
        onBack = onBack,
        footer = { padding ->
            ControlSheet(padding) {
                NumberField(
                    label = stringResource(R.string.focal_length),
                    value = state.focalLengthMm,
                    unit = stringResource(R.string.symbol_mm),
                    onValueChange = { onAction(StarsAction.SetFocalLength(it)) },
                    onDecrease = { onAction(StarsAction.StepFocalLength(up = false)) },
                    onIncrease = { onAction(StarsAction.StepFocalLength(up = true)) },
                    decreaseDescription = stringResource(R.string.focal_length_shorter),
                    increaseDescription = stringResource(R.string.focal_length_longer),
                    error = state.invalid.messageFor(Input.FOCAL_LENGTH, UnitSystem.METRIC),
                )
                // the aperture counts only for NPF, which needs the resolution
                if (StarRule.NPF in state.rules) {
                    StopDial(
                        label = stringResource(R.string.aperture),
                        dial = state.aperture,
                        onSelect = { onAction(StarsAction.SetAperture(it.actual)) },
                        valueText = { stringResource(R.string.aperture_value, shortNumber(it.nominal, locale)) },
                        tickLabel = { shortNumber(it.nominal, locale) },
                        trailing = stringResource(R.string.npf_only),
                    )
                }
            }
        },
    ) {
        val camera = state.camera
        val mp = state.resolutionMp?.let { shortNumber(it, locale) + " " + stringResource(R.string.symbol_mp) }
        CameraChip(camera?.title, listOfNotNull(camera?.detail, mp).joinToString(" " + stringResource(R.string.separator)).ifEmpty { null }, onChooseCamera)
        if (camera == null) {
            ChooseCameraCard(stringResource(R.string.empty_format_stars), onChooseCamera)
        } else {
            ResultCard { Result(state) }
            SegmentedChoice(
                options = state.rules.map { stringResource(ruleName(it)) },
                selected = state.rules.indexOf(state.rule),
                onSelect = { onAction(StarsAction.SetRule(state.rules[it])) },
                description = stringResource(R.string.rule_label),
            )
            Text(
                ruleExplanation(state),
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Result(state: StarsUiState) {
    val colors = MaterialTheme.colorScheme
    val chosen = state.chosen
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.stars_max), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Text(
            chosen?.shutter?.let { shutterText(it.nominal) } ?: "—",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp, lineHeight = 64.sp),
            color = colors.primary,
        )
        chosen?.limitS?.let { Text(stringResource(R.string.stars_limit, durationText(it)), fontSize = 15.sp, color = colors.onSurfaceVariant) }
    }
    Column {
        state.results.forEach { result ->
            val on = result.rule == state.rule
            HorizontalDivider(color = colors.surfaceContainerHigh)
            Row(Modifier
                .heightIn(min = 40.dp)
                .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(ruleName(result.rule)),
                    Modifier.weight(1.4f),
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) colors.primary else colors.onSurface,
                )
                Text(result.limitS?.let { durationText(it) } ?: "—", Modifier.weight(1f), fontFamily = Barlow, fontSize = 15.sp, color = colors.onSurfaceVariant)
                Text(
                    result.shutter?.let { shutterText(it.nominal) } ?: "—",
                    Modifier.weight(1f),
                    fontFamily = Barlow,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) colors.primary else colors.onSurface,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ruleExplanation(state: StarsUiState): String = when (state.rule) {
    StarRule.RULE_500 -> stringResource(R.string.rule_500_explanation)
    StarRule.RULE_300 -> stringResource(R.string.rule_300_explanation)
    StarRule.NPF -> stringResource(R.string.rule_npf_explanation, number(state.pixelPitchUm ?: 0.0, 1, currentLocale()))
}

private fun ruleName(rule: StarRule): Int = when (rule) {
    StarRule.RULE_500 -> R.string.rule_500
    StarRule.RULE_300 -> R.string.rule_300
    StarRule.NPF -> R.string.rule_npf
}

private val previewFormat = CatalogFormat("full-frame", "Full Frame (35 mm)", 36.0, 24.0)
private val previewSelection = Selection(
    CatalogCamera("sony-alpha-7-iii", "Sony", "Alpha 7 III", "full-frame", specs = CameraSpecs(resolutionMp = 24.2)),
    previewFormat,
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun StarsPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { StarsContent(StarsUiState.from(StarsInput(), Settings(), previewSelection), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun StarsWithoutResolutionPreview() {
    Iso100Theme { StarsContent(StarsUiState.from(StarsInput(), Settings(), Selection(null, previewFormat)), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun StarsEmptyPreview() {
    Iso100Theme { StarsContent(StarsUiState.from(StarsInput(), Settings(), null), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun StarsErrorPreview() {
    Iso100Theme { StarsContent(StarsUiState.from(StarsInput(focalLengthMm = 0.0), Settings(), previewSelection), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun StarsLargeTextPreview() {
    Iso100Theme { StarsContent(StarsUiState.from(StarsInput(), Settings(), previewSelection), {}, {}, {}) }
}
