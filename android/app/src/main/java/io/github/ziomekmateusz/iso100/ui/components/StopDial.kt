package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureScale
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureValue
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlin.math.roundToInt

/** The values of a [StopDial]: the scale at the chosen step, which are full stops and which are outside the camera's range. */
data class Dial(val values: List<ExposureValue>, val selected: Int, val fullStops: List<Boolean>, val dimmed: List<Boolean>) {
    val value: ExposureValue get() = values[selected]
}

/** The dial of [setting] at [step], on the value closest to [value]; nominal values outside [range] are dimmed. */
fun dial(setting: ExposureSetting, step: StopStep, value: Double, range: ClosedFloatingPointRange<Double>? = null): Dial {
    val scale = ExposureScale.of(setting, step)
    val full = ExposureScale.of(setting, StopStep.FULL).values.map { it.actual }.toSet()
    return Dial(
        values = scale.values,
        selected = scale.values.indexOf(scale.nearest(value)),
        fullStops = scale.values.map { it.actual in full },
        dimmed = scale.values.map { range != null && it.nominal !in range },
    )
}

/**
 * The ruler dial of aperture, shutter and ISO: drag it, it clicks on every value of the scale; long
 * ticks are full stops; dimmed values are outside the camera's range. TalkBack reads it as a slider.
 */
@Composable
fun StopDial(
    label: String,
    dial: Dial,
    onSelect: (ExposureValue) -> Unit,
    valueText: @Composable (ExposureValue) -> String,
    tickLabel: (ExposureValue) -> String,
    trailing: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val tickStyle = TextStyle(fontFamily = Barlow, fontSize = 13.sp, fontFeatureSettings = "tnum")
    val count = dial.values.size
    // about 60 dp per stop, so that the labels of the full stops ("1/1000") never overlap
    val ticksPerStop = (count.toFloat() / dial.fullStops.count { it }.coerceAtLeast(1)).roundToInt()
    val spacing = with(LocalDensity.current) { (if (ticksPerStop <= 1) 48.dp else if (ticksPerStop == 2) 28.dp else 20.dp).toPx() }
    // 64 dp normally; taller when a large font makes the labels under the ticks grow
    val height = with(LocalDensity.current) { maxOf(64.dp, 42.dp + measurer.measure("0", tickStyle).size.height.toDp()) }
    // the dial moves on its own while dragged; the screen follows through onSelect
    var position by remember { mutableIntStateOf(dial.selected) }
    var drag by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(dial.selected) { position = dial.selected }
    val currentText = valueText(dial.values[position.coerceIn(0, count - 1)])

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Text(currentText, Modifier.alignByBaseline(), style = MaterialTheme.typography.headlineMedium, color = colors.primary)
            Text(trailing, Modifier.alignByBaseline(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal), color = colors.onSurfaceVariant)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(colors.background)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // dragging right brings the smaller values to the centre
                        var offset = drag + delta
                        var index = position
                        while (offset > spacing / 2 && index > 0) { index--; offset -= spacing }
                        while (offset < -spacing / 2 && index < count - 1) { index++; offset += spacing }
                        drag = offset.coerceIn(if (index == count - 1) 0f else -spacing, if (index == 0) 0f else spacing)
                        if (index != position) {
                            position = index
                            onSelect(dial.values[index])
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    },
                    onDragStopped = { animate(drag, 0f) { value, _ -> drag = value } },
                )
                .semantics {
                    contentDescription = label
                    stateDescription = currentText
                    progressBarRangeInfo = ProgressBarRangeInfo(position.toFloat(), 0f..(count - 1).toFloat(), steps = (count - 2).coerceAtLeast(0))
                    setProgress { target ->
                        onSelect(dial.values[target.roundToInt().coerceIn(0, count - 1)])
                        true
                    }
                },
        ) {
            val center = size.width / 2
            val top = 10.dp.toPx()
            for (i in 0 until count) {
                val x = center + (i - position) * spacing + drag
                if (x < -spacing || x > size.width + spacing) continue
                val full = dial.fullStops[i]
                val color = when {
                    i == position -> colors.primary
                    dial.dimmed[i] -> colors.outlineVariant
                    full -> colors.onSurface
                    else -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - 1.dp.toPx(), top),
                    size = Size(2.dp.toPx(), (if (full) 22 else 12).dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
                if (full && i != position) {
                    val text = measurer.measure(
                        tickLabel(dial.values[i]),
                        tickStyle.copy(color = if (dial.dimmed[i]) colors.onSurfaceVariant.copy(alpha = 0.5f) else colors.onSurfaceVariant),
                    )
                    drawText(text, topLeft = Offset(x - text.size.width / 2f, top + 28.dp.toPx()))
                }
            }
            drawRoundRect(
                color = colors.primary,
                topLeft = Offset(center - 2.dp.toPx(), -2.dp.toPx()),
                size = Size(4.dp.toPx(), 36.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

private val previewDial = dial(ExposureSetting.APERTURE, StopStep.THIRD, 8.0, 4.0..22.0)

@Preview
@Composable
private fun StopDialPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) {
        Surface { StopDial("Diaframma", previewDial, {}, { "f/${it.nominal}" }, { "${it.nominal}" }, "1/3 stop", Modifier.padding(16.dp)) }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun StopDialLargeTextPreview() {
    Iso100Theme {
        Surface { StopDial("Diaframma", dial(ExposureSetting.SHUTTER, StopStep.FULL, 1.0 / 125), {}, { "1/${(1 / it.nominal).toInt()} s" }, { "1/${(1 / it.nominal).toInt()}" }, "1 stop", Modifier.padding(16.dp)) }
    }
}
