package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

/** How quickly a flung dial slows down: higher stops it sooner. */
private const val DIAL_FRICTION = 2f
private val dialDecay = exponentialDecay<Float>(DIAL_FRICTION)

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
    val ticksPerStop = (count.toFloat() / dial.fullStops.count { it }.coerceAtLeast(1)).roundToInt()
    val spacing = with(LocalDensity.current) {
        (
                if (ticksPerStop <= 1) {
                    48.dp
                } else if (ticksPerStop == 2) {
                    28.dp
                } else {
                    20.dp
                }
                ).toPx()
    }
    val tickLabels = remember(dial, measurer, tickStyle) {
        dial.values.mapIndexed { index, value ->
            if (dial.fullStops[index]) {
                measurer.measure(
                    tickLabel(value),
                    tickStyle,
                )
            } else {
                null
            }
        }
    }
    val labelHeight = remember(measurer, tickStyle) { measurer.measure("0", tickStyle).size.height }
    val height = with(LocalDensity.current) { maxOf(64.dp, 42.dp + labelHeight.toDp()) }
    val maxScroll = (count - 1) * spacing
    var scroll by remember { mutableFloatStateOf(dial.selected * spacing) }
    var position by remember { mutableIntStateOf(dial.selected) }
    val scrollState = rememberScrollableState { delta ->
        val previous = scroll
        scroll = (scroll + delta).coerceIn(0f, maxScroll)
        val index = (scroll / spacing).roundToInt()
        if (index != position) {
            position = index
            onSelect(dial.values[index])
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        scroll - previous
    }
    val ticks = remember(spacing, maxScroll) {
        object : SnapLayoutInfoProvider {
            override fun calculateApproachOffset(velocity: Float, decayOffset: Float) = 0f
            override fun calculateSnapOffset(velocity: Float) =
                snapOffset(scroll, dialDecay.calculateTargetValue(0f, velocity), spacing, maxScroll)
        }
    }
    LaunchedEffect(dial.selected, spacing) {
        if (scrollState.isScrollInProgress) return@LaunchedEffect
        position = dial.selected
        scroll = dial.selected * spacing
    }
    val currentText = valueText(dial.values[position.coerceIn(0, count - 1)])

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DialHeader(label, currentText, trailing)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(colors.background)
                .scrollable(
                    scrollState,
                    Orientation.Horizontal,
                    reverseDirection = true,
                    flingBehavior = remember(ticks) {
                        snapFlingBehavior(
                            ticks,
                            dialDecay,
                            spring(stiffness = Spring.StiffnessMediumLow),
                        )
                    },
                )
                .semantics {
                    contentDescription = label
                    stateDescription = currentText
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        position.toFloat(),
                        0f..(count - 1).toFloat(),
                        steps = (count - 2).coerceAtLeast(0),
                    )
                    setProgress { target ->
                        onSelect(dial.values[target.roundToInt().coerceIn(0, count - 1)])
                        true
                    }
                },
        ) {
            val center = size.width / 2
            val tickMiddle = 21.dp.toPx()
            for (index in 0 until count) {
                val point = onCylinder(index * spacing - scroll, radius = center) ?: continue
                val x = center + point.x
                val full = dial.fullStops[index]
                val color = when {
                    index == position -> colors.primary
                    dial.dimmed[index] -> colors.outlineVariant
                    full -> colors.onSurface
                    else -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                }
                val tickHeight = (if (full) 22 else 12).dp.toPx() * (0.7f + 0.3f * point.depth)
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * point.depth),
                    topLeft = Offset(x - 1.dp.toPx(), tickMiddle - tickHeight / 2),
                    size = Size(2.dp.toPx(), tickHeight),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
                val text = tickLabels[index]
                if (text != null && index != position) {
                    val labelColor =
                        if (dial.dimmed[index]) colors.onSurfaceVariant.copy(alpha = 0.5f) else colors.onSurfaceVariant
                    val labelTop = tickMiddle + 17.dp.toPx()
                    scale(scaleX = point.depth, scaleY = 1f, pivot = Offset(x, labelTop)) {
                        drawText(
                            text,
                            color = labelColor.copy(alpha = labelColor.alpha * point.depth),
                            topLeft = Offset(x - text.size.width / 2f, labelTop),
                        )
                    }
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

/**
 * The label at the start, [trailing] at the end and the value in the middle, on one baseline. The
 * value stays still while it changes: "f/" ends on the centre line, so the slash never moves; a value
 * without a slash ("100", "2 min") is centred.
 */
@Composable
private fun DialHeader(label: String, value: String, trailing: String) {
    val colors = MaterialTheme.colorScheme
    val (beforeSlash, afterSlash) = splitAtSlash(value)
    val valueStyle = MaterialTheme.typography.headlineMedium
    Layout(
        content = {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            RollingText(beforeSlash, valueStyle, color = colors.primary)
            RollingText(afterSlash, valueStyle, color = colors.primary)
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = colors.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label, $value, $trailing" },
    ) { measurables, constraints ->
        val placeables =
            measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val baselines = placeables.map { placeable ->
            placeable[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: placeable.height
        }
        val baseline = baselines.max()
        val height = placeables.indices.maxOf { baseline - baselines[it] + placeables[it].height }
        val width = constraints.maxWidth
        val (labelText, before, after, trailingText) = placeables
        val centre = if (before.width == 0) width / 2 - after.width / 2 else width / 2
        layout(width, height) {
            fun Placeable.placeOnBaseline(x: Int, index: Int) = place(x, baseline - baselines[index])
            labelText.placeOnBaseline(0, 0)
            before.placeOnBaseline(centre - before.width, 1)
            after.placeOnBaseline(centre, 2)
            trailingText.placeOnBaseline(width - trailingText.width, 3)
        }
    }
}

/** Where a tick lands on the cylinder: [x] px from the centre, and how much it faces the viewer ([depth] 1 in the centre, 0 at the edge). */
internal data class CylinderPoint(val x: Float, val depth: Float)

/** A tick [offset] px from the centre of the flat ruler, wrapped on a cylinder of [radius]; null past its edge. */
internal fun onCylinder(offset: Float, radius: Float): CylinderPoint? {
    val angle = offset / radius
    if (abs(angle) >= PI / 2) return null
    return CylinderPoint(radius * sin(angle), cos(angle))
}

/**
 * How far to scroll from [scroll] to settle on the tick nearest to where the dial would stop after
 * [travel] more pixels; never past the ends.
 */
internal fun snapOffset(scroll: Float, travel: Float, spacing: Float, maxScroll: Float): Float =
    (round((scroll + travel) / spacing) * spacing).coerceIn(0f, maxScroll) - scroll

/** "f/8" to "f/" and "8"; without a slash everything goes after it. */
internal fun splitAtSlash(text: String): Pair<String, String> {
    val afterSlash = text.indexOf('/') + 1
    return text.take(afterSlash) to text.drop(afterSlash)
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
        Surface {
            StopDial("Diaframma", dial(ExposureSetting.SHUTTER, StopStep.FULL, 1.0 / 125), {
            }, { "1/${(1 / it.nominal).toInt()} s" }, { "1/${(1 / it.nominal).toInt()}" }, "1 stop", Modifier.padding(16.dp))
        }
    }
}
