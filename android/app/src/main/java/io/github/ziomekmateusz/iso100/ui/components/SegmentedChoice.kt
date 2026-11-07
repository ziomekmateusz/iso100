package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

private val segmentGap = 4.dp
private val segmentRadius = 20.dp

/** A choice between a few options (what to calculate, rule, units, theme); TalkBack reads them as radio buttons. */
@Composable
fun SegmentedChoice(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val colors = MaterialTheme.colorScheme
    val indicator by animateFloatAsState(selected.toFloat(), label = "segment indicator")
    Row(
        modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(24.dp))
            .padding(4.dp)
            .drawBehind { drawIndicator(indicator, options.size, colors.outlineVariant) }
            .selectableGroup()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(segmentGap),
    ) {
        options.forEachIndexed { i, option ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(segmentRadius))
                    .selectable(selected = on, onClick = { onSelect(i) }, role = Role.RadioButton)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                FitText(
                    option,
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (on) colors.onSurface else colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** One rounded segment behind the options, at [position] (0 the first option, fractions while it slides). */
private fun DrawScope.drawIndicator(position: Float, count: Int, color: Color) {
    val gap = segmentGap.toPx()
    val width = (size.width - gap * (count - 1)) / count
    val start = position * (width + gap)
    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - start - width else start
    drawRoundRect(
        color,
        Offset(left, 0f),
        Size(width, size.height),
        CornerRadius(segmentRadius.toPx()),
    )
}

@Preview
@Composable
private fun SegmentedChoicePreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SegmentedChoice(listOf("Calcola ISO", "Diaframma", "Tempo"), 2, {}, "Valore da calcolare", Modifier.padding(16.dp))
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun SegmentedChoiceLargeTextPreview() {
    Iso100Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SegmentedChoice(listOf("Sistema", "Scuro", "Chiaro", "Notte"), 1, {}, "Tema", Modifier.padding(16.dp))
        }
    }
}
