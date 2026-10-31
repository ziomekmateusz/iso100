package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

/**
 * The date at the bottom of Sun and Moon: previous day, the date (it opens the date picker), next day.
 * [label] is "Oggi" or the weekday; [value] the date, with the hour for the Moon.
 */
@Composable
fun DateStepper(label: String, value: String, onPrevious: () -> Unit, onNext: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundStep(R.drawable.ic_back, stringResource(R.string.previous_day), onPrevious)
        Surface(onClick = onClick, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = RoundedCornerShape(28.dp), color = colors.surface) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(label, fontSize = 13.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, textAlign = TextAlign.Center)
            }
        }
        RoundStep(R.drawable.ic_forward, stringResource(R.string.next_day), onNext)
    }
}

@Composable
private fun RoundStep(icon: Int, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp).semantics { contentDescription = description },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Icon(painterResource(icon), null, Modifier.padding(17.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview
@Composable
private fun DateStepperPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { Surface(color = MaterialTheme.colorScheme.background) { DateStepper("Domenica", "21 giugno 2026", {}, {}, {}) } }
}

@Preview(fontScale = 2f)
@Composable
private fun DateStepperLargeTextPreview() {
    Iso100Theme { Surface(color = MaterialTheme.colorScheme.background) { DateStepper("Oggi", "sab 17 ottobre · 22:00", {}, {}, {}) } }
}
