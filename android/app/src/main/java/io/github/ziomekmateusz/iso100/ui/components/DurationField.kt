package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlin.math.roundToLong

/** Hours and minutes of [seconds], as the two boxes show them: "1" and "00". */
private fun split(seconds: Double): Pair<String, String> {
    val minutes = (seconds / 60).roundToLong()
    return (minutes / 60).toString() to "%02d".format(minutes % 60)
}

/** Seconds from the two boxes; null when one is not a whole number. */
internal fun parseDuration(hours: String, minutes: String): Double? {
    val h = hours.trim().takeIf { it.all(Char::isDigit) }?.toLongOrNull() ?: return null
    val m = minutes.trim().takeIf { it.all(Char::isDigit) }?.toLongOrNull() ?: return null
    return h * 3600.0 + m * 60.0
}

/**
 * A duration in hours and minutes, two boxes of whole numbers with the states of [NumberField]: it keeps
 * the typed text and passes on only valid durations; [error] is the message of the screen.
 */
@Composable
fun DurationField(label: String, seconds: Double, onValueChange: (Double) -> Unit, modifier: Modifier = Modifier, error: String? = null) {
    var hours by rememberSaveable { mutableStateOf(split(seconds).first) }
    var minutes by rememberSaveable { mutableStateOf(split(seconds).second) }
    // a duration changed from outside replaces the text; the typed one stays
    LaunchedEffect(seconds) {
        if (parseDuration(hours, minutes) != seconds) split(seconds).let { (h, m) -> hours = h; minutes = m }
    }
    val message = if (parseDuration(hours, minutes) == null) stringResource(R.string.error_not_a_whole_number) else error
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ValueBox(
                text = hours,
                onTextChange = { hours = it; parseDuration(it, minutes)?.let(onValueChange) },
                unit = stringResource(R.string.symbol_h),
                description = stringResource(R.string.hours),
                error = message,
                keyboard = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            ValueBox(
                text = minutes,
                onTextChange = { minutes = it; parseDuration(hours, it)?.let(onValueChange) },
                unit = stringResource(R.string.symbol_min),
                description = stringResource(R.string.minutes),
                error = message,
                keyboard = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        if (message != null) ErrorText(message)
    }
}

@Composable
private fun DurationFieldStates() {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DurationField("Tempo di ripresa", 3600.0, {})
            DurationField("Tempo di ripresa", 0.0, {}, error = "Il tempo di ripresa deve essere maggiore di 0 s.")
        }
    }
}

@Preview
@Composable
private fun DurationFieldPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { DurationFieldStates() }
}

@Preview(fontScale = 2f)
@Composable
private fun DurationFieldLargeTextPreview() {
    Iso100Theme { DurationFieldStates() }
}
