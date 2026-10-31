package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

/**
 * A number with − and + buttons. It keeps the typed text itself, accepts a comma or a dot and passes
 * on only valid numbers; [error] is the message of the screen, e.g. a value out of the domain.
 */
@Composable
fun NumberField(
    label: String,
    value: Double,
    unit: String,
    onValueChange: (Double) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    labelAction: (@Composable () -> Unit)? = null,
) {
    val locale = currentLocale()
    var text by rememberSaveable { mutableStateOf(shortNumber(value, locale, 2)) }
    // a value changed from outside (± buttons, other units) replaces the text; the typed one stays
    LaunchedEffect(value) { if (parseDecimal(text) != value) text = shortNumber(value, locale, 2) }
    val message = if (parseDecimal(text) == null) stringResource(R.string.error_not_a_number) else error
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            labelAction?.invoke()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("−", decreaseDescription, onDecrease)
            ValueBox(
                text = text,
                onTextChange = { new ->
                    text = new
                    parseDecimal(new)?.let(onValueChange)
                },
                unit = unit,
                description = label,
                error = message,
                keyboard = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            StepButton("+", increaseDescription, onIncrease)
        }
        if (message != null) ErrorText(message)
    }
}

/** The text box of [NumberField] and [DurationField]: normal, active (focused) and error states. */
@Composable
internal fun ValueBox(
    text: String,
    onTextChange: (String) -> Unit,
    unit: String,
    description: String,
    error: String?,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val border = when {
        error != null -> colors.error
        focused -> colors.primary
        else -> Color.Transparent
    }
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .heightIn(min = 56.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = description
                if (error != null) error(error)
            },
        textStyle = TextStyle(
            fontFamily = Barlow,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            fontFeatureSettings = "tnum",
            color = colors.onSurface,
            textAlign = TextAlign.End,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
        singleLine = true,
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .background(colors.background, shape)
                    .border(2.dp, border, shape)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f, fill = false)) { inner() }
                Text(unit, fontSize = 18.sp, color = colors.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun StepButton(sign: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(sign, fontFamily = Barlow, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(message, modifier, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun NumberFieldStates() {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberField("Focale", 50.0, "mm", {}, {}, {}, "−", "+")
            NumberField("Messa a fuoco", 0.1, "m", {}, {}, {}, "−", "+", error = "La messa a fuoco deve essere maggiore di 0,20 m.")
        }
    }
}

@Preview
@Composable
private fun NumberFieldPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { NumberFieldStates() }
}

@Preview(fontScale = 2f)
@Composable
private fun NumberFieldLargeTextPreview() {
    Iso100Theme { NumberFieldStates() }
}
