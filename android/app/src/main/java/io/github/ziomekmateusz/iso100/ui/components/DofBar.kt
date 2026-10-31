package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.log2

/**
 * Where a distance sits on the bar, from 0 to 1: 0.5 m at the left edge, every doubling of the distance
 * 0.149 further right, infinity (null) at the right edge.
 * shortcut: below 0.5 m and beyond about 33 m everything sticks to the ends; rescale if macro or landscape need it.
 */
internal fun dofBarFraction(meters: Double?): Float =
    if (meters == null) 1f else (log2(meters / 0.5) * 0.149).coerceIn(0.0, 0.9).toFloat()

/**
 * The sharp zone from [nearM] to [farM] (null: infinity) on a log scale, the focus point and the hyperfocal mark.
 * [scale] are the labels under the bar with their distance in metres, null for infinity.
 */
@Composable
fun DofBar(
    nearM: Double,
    focusM: Double,
    farM: Double?,
    hyperfocalM: Double,
    scale: List<Pair<String, Double?>>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Barlow, fontSize = 13.sp, color = colors.onSurfaceVariant, fontFeatureSettings = "tnum")
    Column(modifier.semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            fun x(meters: Double?) = dofBarFraction(meters) * size.width
            val barTop = 7.dp.toPx()
            val barHeight = 10.dp.toPx()
            val radius = CornerRadius(barHeight / 2)
            drawRoundRect(colors.outlineVariant, Offset(0f, barTop), Size(size.width, barHeight), radius)
            val start = x(nearM)
            drawRoundRect(colors.primary, Offset(start, barTop), Size((x(farM) - start).coerceAtLeast(barHeight), barHeight), radius)
            drawRect(colors.onSurfaceVariant, Offset(x(hyperfocalM) - 1.dp.toPx(), 3.dp.toPx()), Size(2.dp.toPx(), 18.dp.toPx()))
            val focus = Offset(x(focusM), size.height / 2)
            drawCircle(colors.onSurface, 10.dp.toPx(), focus)
            drawCircle(colors.surface, 8.5.dp.toPx(), focus, style = Stroke(3.dp.toPx()))
        }
        // the labels grow with the font: the row takes the height of one of them
        val labelHeight = with(LocalDensity.current) { measurer.measure("0", labelStyle).size.height.toDp() }
        Canvas(Modifier.fillMaxWidth().height(labelHeight)) {
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
            DofBar(2.35, 3.0, 4.15, 10.72, previewScale, "")
            DofBar(1.32, 5.0, null, 1.81, previewScale, "")
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
