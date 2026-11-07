package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.log2
import kotlin.math.pow

private const val BAR_START_M = 0.5
private const val BAR_DOUBLING = 0.149
private const val BAR_LAST_FINITE = 0.9

/**
 * Where a distance sits on the bar, from 0 to 1: 0.5 m at the left edge, every doubling of the distance
 * 0.149 further right, infinity (null) at the right edge.
 * shortcut: below 0.5 m and beyond about 33 m everything sticks to the ends; rescale if macro or landscape need it.
 */
internal fun dofBarFraction(meters: Double?): Float = if (meters == null) {
    1f
} else {
    (log2(meters / BAR_START_M) * BAR_DOUBLING).coerceIn(
        0.0,
        BAR_LAST_FINITE,
    ).toFloat()
}

/** The distance under a point of the bar, the inverse of [dofBarFraction]; the infinity end gives the last finite distance. */
internal fun dofBarMeters(fraction: Float): Double =
    BAR_START_M * 2.0.pow(fraction.toDouble().coerceIn(0.0, BAR_LAST_FINITE) / BAR_DOUBLING)

/** A dragged distance kept to two digits (3.3, 12, 0.52), in the units shown to the user. */
fun twoDigits(value: Double): Double = BigDecimal(value).round(MathContext(2)).toDouble()

/**
 * The sharp zone from [nearM] to [farM] (null: infinity) on a log scale, the focus point and the hyperfocal mark.
 * [scale] are the labels under the bar with their distance in metres, null for infinity.
 * Dragging along the bar moves the focus: [onFocus] gets the distance in metres. The marks glide to new values.
 */
@Composable
fun DofBar(
    nearM: Double,
    focusM: Double,
    farM: Double?,
    hyperfocalM: Double,
    scale: List<Pair<String, Double?>>,
    description: String,
    onFocus: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Barlow, fontSize = 13.sp, color = colors.onSurfaceVariant, fontFeatureSettings = "tnum")
    var dragging by remember { mutableStateOf(false) }
    // under the finger the marks follow at once: a spring would trail behind it
    val motion: AnimationSpec<Float> =
        if (dragging) snap() else spring(stiffness = Spring.StiffnessMediumLow)
    val near by animateFloatAsState(dofBarFraction(nearM), motion, label = "near")
    val far by animateFloatAsState(dofBarFraction(farM), motion, label = "far")
    val focus by animateFloatAsState(dofBarFraction(focusM), motion, label = "focus")
    val hyperfocal by animateFloatAsState(dofBarFraction(hyperfocalM), motion, label = "hyperfocal")
    val currentOnFocus by rememberUpdatedState(onFocus)
    Column(
        modifier
            .semantics {
                contentDescription = description
                progressBarRangeInfo =
                    ProgressBarRangeInfo(dofBarFraction(focusM), 0f..dofBarFraction(null))
                setProgress { target ->
                    onFocus(dofBarMeters(target))
                    true
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ -> currentOnFocus(dofBarMeters(change.position.x / size.width)) }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(24.dp),
        ) {
            val barTop = 7.dp.toPx()
            val barHeight = 10.dp.toPx()
            val radius = CornerRadius(barHeight / 2)
            drawRoundRect(colors.outlineVariant, Offset(0f, barTop), Size(size.width, barHeight), radius)
            val start = near * size.width
            drawRoundRect(
                colors.primary,
                Offset(start, barTop),
                Size((far * size.width - start).coerceAtLeast(barHeight), barHeight),
                radius,
            )
            drawRect(
                colors.onSurfaceVariant,
                Offset(hyperfocal * size.width - 1.dp.toPx(), 3.dp.toPx()),
                Size(2.dp.toPx(), 18.dp.toPx()),
            )
            val focusPoint = Offset(focus * size.width, size.height / 2)
            drawCircle(colors.onSurface, 10.dp.toPx(), focusPoint)
            drawCircle(colors.surface, 8.5.dp.toPx(), focusPoint, style = Stroke(3.dp.toPx()))
        }
        // the labels grow with the font: the row takes the height of one of them
        val labelHeight = with(LocalDensity.current) { measurer.measure("0", labelStyle).size.height.toDp() }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(labelHeight),
        ) {
            for ((label, meters) in scale) {
                val text = measurer.measure(label, labelStyle)
                val x = if (meters == null) size.width - text.size.width else (dofBarFraction(meters) * size.width - text.size.width / 2f)
                drawText(text, topLeft = Offset(x.coerceIn(0f, size.width - text.size.width), 0f))
            }
        }
    }
}

private val previewScale = listOf("0,5" to 0.5, "1" to 1.0, "2" to 2.0, "5" to 5.0, "10" to 10.0, "∞ m" to null)

@Composable
private fun DofBarStates() {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DofBar(2.35, 3.0, 4.15, 10.72, previewScale, "", onFocus = {})
            DofBar(1.32, 5.0, null, 1.81, previewScale, "", onFocus = {})
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun DofBarPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { DofBarStates() }
}

@Preview(widthDp = 380, fontScale = 2f)
@Composable
private fun DofBarLargeTextPreview() {
    Iso100Theme { DofBarStates() }
}
