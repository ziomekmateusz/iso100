package io.github.ziomekmateusz.iso100.ui.fov

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import io.github.ziomekmateusz.iso100.calc.fov.FieldOfView
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.data.formats
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.components.CameraChip
import io.github.ziomekmateusz.iso100.ui.components.ChooseCameraCard
import io.github.ziomekmateusz.iso100.ui.components.ControlSheet
import io.github.ziomekmateusz.iso100.ui.components.NumberField
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.RollingText
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.distanceSymbol
import io.github.ziomekmateusz.iso100.ui.messageFor
import io.github.ziomekmateusz.iso100.ui.number
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

@Composable
fun FovScreen(onBack: () -> Unit, onChooseCamera: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(FovInput()) }
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val selection by app.catalog.selection.collectAsStateWithLifecycle()
    val catalog by app.catalog.state.collectAsStateWithLifecycle()
    val state = remember(input, settings, selection, catalog) { FovUiState.from(input, settings, selection, catalog.formats) }
    FovContent(state, onAction = { input = input.reduce(it) }, onBack = onBack, onChooseCamera = onChooseCamera)
}

@Composable
fun FovContent(state: FovUiState, onAction: (FovAction) -> Unit, onBack: () -> Unit, onChooseCamera: () -> Unit) {
    val units = state.units
    ToolScaffold(
        title = stringResource(R.string.tool_fov),
        onBack = onBack,
        footer = { padding ->
            ControlSheet(padding) {
                NumberField(
                    label = stringResource(R.string.focal_length),
                    value = state.focalLengthMm,
                    unit = stringResource(R.string.symbol_mm),
                    onValueChange = { onAction(FovAction.SetFocalLength(it)) },
                    onDecrease = { onAction(FovAction.StepFocalLength(up = false)) },
                    onIncrease = { onAction(FovAction.StepFocalLength(up = true)) },
                    decreaseDescription = stringResource(R.string.focal_length_shorter),
                    increaseDescription = stringResource(R.string.focal_length_longer),
                    error = state.invalid.messageFor(Input.FOCAL_LENGTH, units),
                )
                NumberField(
                    label = stringResource(R.string.subject_distance),
                    value = state.distance,
                    unit = distanceSymbol(units),
                    onValueChange = { onAction(FovAction.SetDistance(it, units)) },
                    onDecrease = { onAction(FovAction.StepDistance(up = false, units)) },
                    onIncrease = { onAction(FovAction.StepDistance(up = true, units)) },
                    decreaseDescription = stringResource(R.string.distance_closer),
                    increaseDescription = stringResource(R.string.distance_farther),
                    error = state.invalid.messageFor(Input.SUBJECT_DISTANCE, units),
                )
            }
        },
    ) {
        CameraChip(state.camera?.title, state.camera?.detail, onChooseCamera)
        if (state.camera == null) {
            ChooseCameraCard(stringResource(R.string.empty_format_fov), onChooseCamera)
        } else {
            ResultCard {
                Angles(state.result)
                state.result?.let { Frame(it, state) }
            }
        }
    }
}

@Composable
private fun Angles(result: FieldOfView?) {
    val locale = currentLocale()
    fun angle(value: Double?) = value?.let { number(it, 1, locale) + "°" } ?: "—"
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Angle(stringResource(R.string.fov_horizontal), angle(result?.horizontalDeg), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        Angle(stringResource(R.string.fov_vertical), angle(result?.verticalDeg), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        Angle(stringResource(R.string.fov_diagonal), angle(result?.diagonalDeg), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
    }
}

@Composable
private fun Angle(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurfaceVariant)
        RollingText(value, style = MaterialTheme.typography.headlineMedium, color = color)
    }
}

/** The framed area at the subject distance, with the same lens on smaller sensors inside. */
@Composable
private fun Frame(result: FieldOfView, state: FovUiState) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val symbol = distanceSymbol(state.units)
    fun size(meters: Double) = number(state.units.fromMeters(meters), 1, locale) + " " + symbol
    val distance = number(state.distance, if (state.distance < 10) 1 else 0, locale) + " " + symbol
    val width = size(result.widthM)
    val height = size(result.heightM)
    val description = stringResource(R.string.fov_frame_description, distance, width, height)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(state.aspectRatio.toFloat())
                .border(2.dp, colors.primary, RoundedCornerShape(6.dp))
                .semantics { contentDescription = description },
        ) {
            val label = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
            Text(width, Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp), style = label, color = colors.primary)
            Text(height, Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp), style = label, color = colors.primary)
            state.frames.forEachIndexed { i, frame ->
                val stroke = colors.onSurfaceVariant.copy(alpha = if (i == 0) 1f else 0.6f)
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(frame.widthRatio.toFloat())
                        .fillMaxHeight(frame.heightRatio.toFloat())
                        .drawBehind {
                            val dash = if (i == 0) floatArrayOf(
                                6.dp.toPx(),
                                4.dp.toPx()
                            ) else floatArrayOf(2.dp.toPx(), 3.dp.toPx())
                            drawRoundRect(
                                color = stroke,
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(dash)
                                ),
                            )
                        },
                ) {
                    Text(
                        stringResource(formatShortName(frame.formatId)),
                        Modifier
                            .align(if (i == 0) Alignment.TopEnd else Alignment.BottomStart)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            if (state.frames.isEmpty()) stringResource(R.string.fov_caption, distance) else stringResource(R.string.fov_caption_compared, distance),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatShortName(id: String): Int = when (id) {
    "full-frame" -> R.string.format_short_full_frame
    "aps-c" -> R.string.format_short_aps_c
    "four-thirds" -> R.string.format_short_four_thirds
    else -> R.string.format_short_one_inch
}

private val previewState = FovUiState(
    camera = CameraLabel("Sony Alpha 7 III", "Full Frame (35 mm)"),
    focalLengthMm = 24.0,
    distance = 10.0,
    units = UnitSystem.METRIC,
    result = FieldOfView(73.7, 53.1, 84.1, 15.0, 10.0),
    invalid = null,
    aspectRatio = 1.5,
    frames = listOf(FovFrame("aps-c", 0.653, 0.65), FovFrame("four-thirds", 0.48, 0.54)),
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun FovPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { FovContent(previewState, {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun FovEmptyPreview() {
    Iso100Theme { FovContent(previewState.copy(camera = null, result = null, frames = emptyList()), {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun FovErrorPreview() {
    Iso100Theme {
        FovContent(
            previewState.copy(result = null, invalid = io.github.ziomekmateusz.iso100.calc.Outcome.Invalid(Input.FOCAL_LENGTH), focalLengthMm = 0.0),
            {}, {}, {},
        )
    }
}

@Preview(widthDp = 412, heightDp = 1600, fontScale = 2f)
@Composable
private fun FovLargeTextPreview() {
    Iso100Theme { FovContent(previewState, {}, {}, {}) }
}
