package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos

private const val MOON_TURN_MILLIS = 600

/**
 * Where the moon is in its cycle, from 0 to 1: 0 new moon, 0.25 first quarter, 0.5 full moon, 0.75 last quarter.
 */
internal fun moonCycle(illumination: Double, waxing: Boolean): Float {
    val waxingPart = (acos(1 - 2 * illumination.coerceIn(0.0, 1.0)) / (2 * PI)).toFloat()
    return if (waxing) waxingPart else 1f - waxingPart
}

/** The lit part of the disc at any point of the cycle, also past the first turn. */
internal fun litFraction(cycle: Float): Float = (1 - cos(2 * PI * cycle).toFloat()) / 2

/** The value equal to [to] in the cycle that is closest to [from]: the disc turns the short way, past the new moon too. */
internal fun nearestTurn(from: Float, to: Float): Float {
    val ahead = (to - from).mod(1f)
    return from + if (ahead > 0.5f) ahead - 1f else ahead
}

/**
 * The moon drawn on a [Canvas]: [illumination] from 0 to 1, lit on the
 * right while [waxing], as seen from the northern hemisphere. [description] null: decoration only.
 * A new phase turns the disc through the cycle instead of jumping to it.
 */
@Composable
fun MoonDisc(illumination: Double, waxing: Boolean, size: Dp, description: String?, modifier: Modifier = Modifier, outlined: Boolean = false) {
    val sky = LocalSkyColors.current
    val outline = MaterialTheme.colorScheme.outlineVariant
    val semantics = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    val target = moonCycle(illumination, waxing)
    val cycle = remember { Animatable(target) }
    LaunchedEffect(target) {
        cycle.animateTo(
            nearestTurn(cycle.value, target),
            tween(MOON_TURN_MILLIS)
        )
    }
    Canvas(modifier
        .size(size)
        .then(semantics)) {
        val r = this.size.minDimension / 2
        val k = litFraction(cycle.value)
        val litOnTheRight = cycle.value.mod(1f) < 0.5f
        drawCircle(sky.moonShadow, r)
        // the lit half, then the terminator: an ellipse of shadow before the quarter, of light after it
        drawArc(
            sky.moonLit,
            startAngle = if (litOnTheRight) -90f else 90f,
            sweepAngle = 180f,
            useCenter = true
        )
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
