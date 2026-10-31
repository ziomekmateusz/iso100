package io.github.ziomekmateusz.iso100.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

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
    Row(
        modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(24.dp))
            .padding(4.dp)
            .selectableGroup()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, option ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) colors.outlineVariant else Color.Transparent)
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
