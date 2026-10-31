package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.LocalSkyColors
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlin.math.abs

/**
 * The moon drawn on a [Canvas]: [illumination] from 0 to 1, lit on the
 * right while [waxing], as seen from the northern hemisphere. [description] null: decoration only.
 */
@Composable
fun MoonDisc(illumination: Double, waxing: Boolean, size: Dp, description: String?, modifier: Modifier = Modifier, outlined: Boolean = false) {
    val sky = LocalSkyColors.current
    val outline = MaterialTheme.colorScheme.outlineVariant
    val semantics = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    Canvas(modifier.size(size).then(semantics)) {
        val r = this.size.minDimension / 2
        val k = illumination.coerceIn(0.0, 1.0).toFloat()
        drawCircle(sky.moonShadow, r)
        // the lit half, then the terminator: an ellipse of shadow before the quarter, of light after it
        drawArc(sky.moonLit, startAngle = if (waxing) -90f else 90f, sweepAngle = 180f, useCenter = true)
        val rx = abs(2 * k - 1) * r
        drawOval(if (k < 0.5f) sky.moonShadow else sky.moonLit, Offset(center.x - rx, center.y - r), Size(2 * rx, 2 * r))
        if (outlined) drawCircle(outline, r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
    }
}

@Preview
@Composable
private fun MoonDiscPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) {
        Surface {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoonDisc(0.42, waxing = true, size = 120.dp, description = null)
                MoonDisc(0.5, waxing = false, size = 22.dp, description = null, outlined = true)
                MoonDisc(0.85, waxing = false, size = 60.dp, description = null)
            }
        }
    }
}
