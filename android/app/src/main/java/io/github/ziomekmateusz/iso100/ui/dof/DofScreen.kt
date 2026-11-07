package io.github.ziomekmateusz.iso100.ui.dof

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.dof.DepthOfField
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.components.ApertureDial
import io.github.ziomekmateusz.iso100.ui.components.CameraSelector
import io.github.ziomekmateusz.iso100.ui.components.ChooseCameraCard
import io.github.ziomekmateusz.iso100.ui.components.ControlSheet
import io.github.ziomekmateusz.iso100.ui.components.DofBar
import io.github.ziomekmateusz.iso100.ui.components.FocalLengthField
import io.github.ziomekmateusz.iso100.ui.components.NumberField
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.ResultTile
import io.github.ziomekmateusz.iso100.ui.components.RollingText
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.components.dial
import io.github.ziomekmateusz.iso100.ui.components.twoDigits
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.distanceNumber
import io.github.ziomekmateusz.iso100.ui.distanceSymbol
import io.github.ziomekmateusz.iso100.ui.distanceText
import io.github.ziomekmateusz.iso100.ui.messageFor
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.stopStepText
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

@Composable
fun DofScreen(onBack: () -> Unit, onChooseCamera: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(DofInput()) }
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val selection by app.catalog.selection.collectAsStateWithLifecycle()
    val state = remember(input, settings, selection) { DofUiState.from(input, settings, selection) }
    DofContent(state, onAction = { input = input.reduce(it) }, onBack = onBack, onChooseCamera = onChooseCamera)
}

@Composable
fun DofContent(state: DofUiState, onAction: (DofAction) -> Unit, onBack: () -> Unit, onChooseCamera: () -> Unit) {
    ToolScaffold(
        title = stringResource(R.string.tool_dof),
        onBack = onBack,
        footer = { padding -> ControlSheet(padding) { DofControls(state, onAction) } },
    ) {
        if (state.camera == null) {
            ChooseCameraCard(stringResource(R.string.empty_format_dof), onChooseCamera)
        } else {
            CameraSelector(camera = state.camera, onClick = onChooseCamera)
            ResultCard { DofResult(state, onAction) }
        }
    }
}

@Composable
private fun DofControls(state: DofUiState, onAction: (DofAction) -> Unit) {
    ApertureDial(
        state.aperture,
        trailing = stopStepText(state.step)
    ) { onAction(DofAction.SetAperture(it)) }
    FocalLengthField(
        state.focalLengthMm,
        state.invalid.messageFor(Input.FOCAL_LENGTH, state.units),
        onValueChange = { onAction(DofAction.SetFocalLength(it)) },
        onStep = { onAction(DofAction.StepFocalLength(it)) },
    )
    FocusField(state, onAction)
}

/** The focus distance, with "at the hyperfocal" once there is a result to take it from. */
@Composable
private fun FocusField(state: DofUiState, onAction: (DofAction) -> Unit) {
    val units = state.units
    NumberField(
        label = stringResource(R.string.focus_distance),
        value = state.focus,
        unit = distanceSymbol(units),
        onValueChange = { onAction(DofAction.SetFocus(it, units)) },
        onDecrease = { onAction(DofAction.StepFocus(up = false, units)) },
        onIncrease = { onAction(DofAction.StepFocus(up = true, units)) },
        decreaseDescription = stringResource(R.string.focus_closer),
        increaseDescription = stringResource(R.string.focus_farther),
        error = state.invalid.messageFor(Input.FOCUS_DISTANCE, units),
        labelAction = state.result?.let { result ->
            {
                HyperfocalButton {
                    onAction(
                        DofAction.FocusAt(
                            result.hyperfocalM
                        )
                    )
                }
            }
        },
    )
}

@Composable
private fun DofResult(state: DofUiState, onAction: (DofAction) -> Unit) {
    SharpZone(state.result, state.units)
    state.result?.let { result ->
        FocusBar(result, state, onAction)
        DepthTiles(result, state.units)
    }
}

/** The sharp zone on the bar; dragging the focus sets it, kept to two digits in the user's units. */
@Composable
private fun FocusBar(result: DepthOfField, state: DofUiState, onAction: (DofAction) -> Unit) {
    val units = state.units
    DofBar(
        nearM = result.nearM,
        focusM = state.focusM,
        farM = result.farM,
        hyperfocalM = result.hyperfocalM,
        scale = barScale(units),
        description = stringResource(
            R.string.dof_bar_description,
            distanceText(result.nearM, units),
            farText(result.farM, units),
            distanceText(state.focusM, units),
            distanceText(result.hyperfocalM, units),
        ),
        onFocus = { meters ->
            onAction(
                DofAction.SetFocus(
                    twoDigits(units.fromMeters(meters)),
                    units
                )
            )
        },
    )
}

@Composable
private fun DepthTiles(result: DepthOfField, units: UnitSystem) {
    val depth = result.depthM?.let { distanceText(it, units) } ?: stringResource(R.string.infinity)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultTile(stringResource(R.string.dof_depth), depth, Modifier.weight(1f))
        ResultTile(
            stringResource(R.string.dof_hyperfocal),
            distanceText(result.hyperfocalM, units),
            Modifier.weight(1f)
        )
    }
}

/** "2,35 → 4,15 m", "1,32 → ∞", or a dash when the input is out of the domain. */
@Composable
private fun SharpZone(result: DepthOfField?, units: UnitSystem) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val symbol = distanceSymbol(units)
    val infinity = stringResource(R.string.infinity)
    val description = result?.let { stringResource(R.string.dof_sharp_description, distanceText(it.nearM, units), farText(it.farM, units)) }
    val unit = SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant)
    val text = buildAnnotatedString {
        if (result == null) {
            append("—")
            return@buildAnnotatedString
        }
        append(distanceNumber(units.fromMeters(result.nearM), locale))
        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant)) { append(" → ") }
        val far = result.farM
        if (far == null) {
            append(infinity)
        } else {
            append(distanceNumber(units.fromMeters(far), locale))
            withStyle(unit) { append(" $symbol") }
        }
    }
    Column(
        Modifier.clearAndSetSemantics { if (description != null) contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.dof_sharp_from), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        RollingText(text, style = MaterialTheme.typography.displayMedium, color = colors.onSurface)
    }
}

@Composable
private fun HyperfocalButton(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        border = BorderStroke(1.dp, primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = primary),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Text(stringResource(R.string.at_hyperfocal), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun farText(farM: Double?, units: UnitSystem): String =
    farM?.let { distanceText(it, units) } ?: stringResource(R.string.infinity_spoken)

/** The labels under the bar: 0,5 1 2 5 10 ∞ m, or 2 5 10 20 50 ∞ ft. */
@Composable
private fun barScale(units: UnitSystem): List<Pair<String, Double?>> {
    val locale = currentLocale()
    val marks = if (units == UnitSystem.METRIC) listOf(0.5, 1.0, 2.0, 5.0, 10.0) else listOf(2.0, 5.0, 10.0, 20.0, 50.0)
    return marks.map { shortNumber(it, locale) to units.toMeters(it) } +
        (stringResource(R.string.infinity) + " " + distanceSymbol(units) to null)
}

private val previewState = DofUiState(
    camera = CameraLabel("Sony Alpha 7 III", "Full Frame (35 mm)"),
    units = UnitSystem.METRIC,
    step = StopStep.THIRD,
    aperture = dial(ExposureSetting.APERTURE, StopStep.THIRD, 8.0),
    focalLengthMm = 50.0,
    focus = 3.0,
    focusM = 3.0,
    result = DepthOfField(2.35, 4.15, 10.72),
    invalid = null,
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun DofPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { DofContent(previewState, {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun DofInfinityPreview() {
    Iso100Theme { DofContent(previewState.copy(focalLengthMm = 24.0, focus = 5.0, focusM = 5.0, result = DepthOfField(1.32, null, 1.81)), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun DofEmptyPreview() {
    Iso100Theme { DofContent(previewState.copy(camera = null, result = null), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun DofErrorPreview() {
    Iso100Theme {
        DofContent(previewState.copy(focus = 0.03, focusM = 0.03, result = null, invalid = Outcome.Invalid(Input.FOCUS_DISTANCE, minimum = 0.05)), {}, {}, {})
    }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun DofLargeTextPreview() {
    Iso100Theme { DofContent(previewState, {}, {}, {}) }
}
