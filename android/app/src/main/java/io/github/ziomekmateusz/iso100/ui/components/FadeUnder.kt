package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val fadeLength = 48.dp

/**
 * Makes the content transparent under the bars [top] and [bottom] high, with a short fade at their
 * edges: it works on any background.
 */
internal fun Modifier.fadeUnder(top: Dp, bottom: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val topEdge = top.toPx()
        val bottomEdge = size.height - bottom.toPx()
        val fade = fadeLength.toPx()
        drawMask(
            topEdge - fade,
            topEdge,
            Color.Transparent,
            Color.Black,
            rectTop = 0f,
            rectBottom = topEdge,
        )
        if (bottomEdge < size.height) {
            drawMask(
                bottomEdge,
                bottomEdge + fade,
                Color.Black,
                Color.Transparent,
                rectTop = bottomEdge,
                rectBottom = size.height,
            )
        }
    }

private fun DrawScope.drawMask(startY: Float, endY: Float, startColor: Color, endColor: Color, rectTop: Float, rectBottom: Float) =
    drawRect(
        brush = Brush.verticalGradient(listOf(startColor, endColor), startY = startY, endY = endY),
        topLeft = Offset(0f, rectTop),
        size = Size(size.width, rectBottom - rectTop),
        blendMode = BlendMode.DstIn,
    )
