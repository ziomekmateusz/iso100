package io.github.ziomekmateusz.iso100.ui.timelapse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.timelapse.Timelapse
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.components.ControlSheet
import io.github.ziomekmateusz.iso100.ui.components.DurationField
import io.github.ziomekmateusz.iso100.ui.components.ErrorText
import io.github.ziomekmateusz.iso100.ui.components.NumberField
import io.github.ziomekmateusz.iso100.ui.components.ResultCard
import io.github.ziomekmateusz.iso100.ui.components.ResultTile
import io.github.ziomekmateusz.iso100.ui.components.RollingText
import io.github.ziomekmateusz.iso100.ui.components.SegmentedChoice
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.components.ValueBox
import io.github.ziomekmateusz.iso100.ui.components.parseDecimal
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.durationText
import io.github.ziomekmateusz.iso100.ui.messageFor
import io.github.ziomekmateusz.iso100.ui.rememberToolInput
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

@Composable
fun TimelapseScreen(onBack: () -> Unit) {
    var input by rememberToolInput("timelapse", TimelapseInput())
    val state = remember(input) { TimelapseUiState.from(input) }
    TimelapseContent(state, onAction = { input = input.reduce(it) }, onBack = onBack)
}

@Composable
fun TimelapseContent(state: TimelapseUiState, onAction: (TimelapseAction) -> Unit, onBack: () -> Unit) {
    ToolScaffold(
        title = stringResource(R.string.tool_timelapse),
        onBack = onBack,
        footer = { padding -> ControlSheet(padding) { TimelapseControls(state, onAction) } },
    ) {
        SegmentedChoice(
            options = listOf(
                stringResource(R.string.timelapse_clip),
                stringResource(R.string.timelapse_interval),
                stringResource(R.string.timelapse_recording)
            ),
            selected = state.solve.ordinal,
            onSelect = { onAction(TimelapseAction.Solve(TimelapseTarget.entries[it])) },
            description = stringResource(R.string.solve_label),
        )
        ResultCard { Result(state) }
    }
}

/** The two values that are not being solved, then fps and photo size. */
@Composable
private fun TimelapseControls(state: TimelapseUiState, onAction: (TimelapseAction) -> Unit) {
    if (state.solve != TimelapseTarget.RECORDING) RecordingField(state, onAction)
    if (state.solve != TimelapseTarget.INTERVAL) IntervalField(state, onAction)
    if (state.solve != TimelapseTarget.CLIP) ClipField(state, onAction)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FpsChoice(state.fps, { onAction(TimelapseAction.SetFps(it)) }, Modifier.weight(1.4f))
        PhotoSizeField(
            state.photoSizeMb,
            { onAction(TimelapseAction.SetPhotoSize(it)) },
            state.errorFor(Input.PHOTO_SIZE),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun RecordingField(state: TimelapseUiState, onAction: (TimelapseAction) -> Unit) {
    DurationField(
        label = stringResource(R.string.recording_time),
        seconds = state.recordingS,
        onValueChange = { onAction(TimelapseAction.SetRecording(it)) },
        error = state.errorFor(Input.RECORDING_TIME),
    )
}

@Composable
private fun IntervalField(state: TimelapseUiState, onAction: (TimelapseAction) -> Unit) {
    NumberField(
        label = stringResource(R.string.interval_label),
        value = state.intervalS,
        unit = stringResource(R.string.symbol_s),
        onValueChange = { onAction(TimelapseAction.SetInterval(it)) },
        onDecrease = { onAction(TimelapseAction.StepInterval(up = false)) },
        onIncrease = { onAction(TimelapseAction.StepInterval(up = true)) },
        decreaseDescription = stringResource(R.string.interval_shorter),
        increaseDescription = stringResource(R.string.interval_longer),
        error = state.errorFor(Input.INTERVAL),
    )
}

@Composable
private fun ClipField(state: TimelapseUiState, onAction: (TimelapseAction) -> Unit) {
    NumberField(
        label = stringResource(R.string.clip_label),
        value = state.clipS,
        unit = stringResource(R.string.symbol_s),
        onValueChange = { onAction(TimelapseAction.SetClip(it)) },
        onDecrease = { onAction(TimelapseAction.StepClip(up = false)) },
        onIncrease = { onAction(TimelapseAction.StepClip(up = true)) },
        decreaseDescription = stringResource(R.string.clip_shorter),
        increaseDescription = stringResource(R.string.clip_longer),
        error = state.errorFor(Input.CLIP_LENGTH),
    )
}

/** In seconds: the messages of :calc speak in seconds whatever the units. */
@Composable
private fun TimelapseUiState.errorFor(input: Input): String? =
    invalid.messageFor(input, UnitSystem.METRIC)

@Composable
private fun Result(state: TimelapseUiState) {
    val colors = MaterialTheme.colorScheme
    val result = state.result
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(resultTitle(state.solve)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        RollingText(
            result?.let { durationText(solved(it, state.solve)) } ?: "—",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp, lineHeight = 64.sp),
            color = colors.primary,
        )
    }
    if (result != null) Bars(result)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultTile(stringResource(R.string.photos_to_take), result?.photos?.toString() ?: "—", Modifier.weight(1f))
        ResultTile(stringResource(R.string.card_space), result?.storageMb?.let { storageText(it) } ?: "—", Modifier.weight(1f))
    }
}

/** The shooting time against the length of the video, on the same scale. */
@Composable
private fun Bars(result: Timelapse) {
    val colors = MaterialTheme.colorScheme
    val recording = durationText(result.recordingS)
    val clip = durationText(result.clipS)
    val description = stringResource(R.string.timelapse_figure, recording, clip)
    Column(Modifier.clearAndSetSemantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Bar(stringResource(R.string.timelapse_recording), recording, 1f, colors.onSurface)
        // a video longer than the shooting (interval shorter than a frame) fills the bar
        Bar(stringResource(R.string.timelapse_video), clip, (result.clipS / result.recordingS).toFloat().coerceIn(0f, 1f), colors.primary)
    }
}

@Composable
private fun Bar(label: String, value: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.widthIn(min = 64.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier
            .weight(1f)
            .height(12.dp)) {
            Box(Modifier
                .fillMaxWidth(fraction)
                .widthIn(min = 8.dp)
                .height(12.dp)
                .background(color, RoundedCornerShape(6.dp)))
        }
        Text(value, Modifier.widthIn(min = 52.dp), fontFamily = Barlow, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}

@Composable
private fun FpsChoice(fps: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.fps_label), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FPS_CHOICES.forEach { choice ->
                val on = choice == fps
                val shape = RoundedCornerShape(14.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(shape)
                        .background(if (on) colors.outlineVariant else Color.Transparent)
                        .border(1.dp, if (on) colors.primary else colors.outlineVariant, shape)
                        .selectable(
                            selected = on,
                            onClick = { onSelect(choice) },
                            role = Role.RadioButton
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$choice", fontFamily = Barlow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (on) colors.onSurface else colors.onSurfaceVariant)
                }
            }
        }
    }
}

/** The size of a photo, optional: an empty box means no storage estimate. */
@Composable
private fun PhotoSizeField(value: Double?, onValueChange: (Double?) -> Unit, error: String?, modifier: Modifier) {
    val locale = currentLocale()
    var text by rememberSaveable { mutableStateOf(value?.let { shortNumber(it, locale, 2) }.orEmpty()) }
    val message = if (text.isNotBlank() && parseDecimal(text) == null) stringResource(R.string.error_not_a_number) else error
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.photo_size), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ValueBox(
            text = text,
            onTextChange = { new ->
                text = new
                if (new.isBlank()) onValueChange(null) else parseDecimal(new)?.let(onValueChange)
            },
            unit = stringResource(R.string.symbol_mb),
            description = stringResource(R.string.photo_size),
            error = message,
            keyboard = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )
        if (message != null) ErrorText(message)
    }
}

private fun solved(result: Timelapse, target: TimelapseTarget): Double = when (target) {
    TimelapseTarget.CLIP -> result.clipS
    TimelapseTarget.INTERVAL -> result.intervalS
    TimelapseTarget.RECORDING -> result.recordingS
}

private fun resultTitle(target: TimelapseTarget): Int = when (target) {
    TimelapseTarget.CLIP -> R.string.clip_label
    TimelapseTarget.INTERVAL -> R.string.interval_label
    TimelapseTarget.RECORDING -> R.string.recording_time
}

/** "18 GB" from a thousand megabytes on (1 GB = 1000 MB, as cards are sold), "750 MB" below. */
@Composable
private fun storageText(mb: Double): String {
    val locale = currentLocale()
    return if (mb >= 1000) shortNumber(mb / 1000, locale) + " " + stringResource(R.string.symbol_gb) else shortNumber(mb, locale, 0) + " " + stringResource(R.string.symbol_mb)
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TimelapsePreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { TimelapseContent(TimelapseUiState.from(TimelapseInput()), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TimelapseIntervalPreview() {
    Iso100Theme { TimelapseContent(TimelapseUiState.from(TimelapseInput(solve = TimelapseTarget.INTERVAL, photoSizeMb = null)), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TimelapseErrorPreview() {
    Iso100Theme { TimelapseContent(TimelapseUiState.from(TimelapseInput(intervalS = 0.0)), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun TimelapseLargeTextPreview() {
    Iso100Theme { TimelapseContent(TimelapseUiState.from(TimelapseInput()), {}, {}) }
}
