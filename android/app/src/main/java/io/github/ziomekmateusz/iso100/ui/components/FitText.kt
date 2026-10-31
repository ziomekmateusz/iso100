package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * A label for a narrow box: one line, made smaller only as much as needed, so that a large font
 * does not break a word in the middle ("Diaframm/a").
 */
@Composable
fun FitText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text,
        modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize),
    )
}

@Composable
fun FitText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = LocalContentColor.current, textAlign: TextAlign = TextAlign.Unspecified) =
    FitText(AnnotatedString(text), style, modifier, color, textAlign)

/** A time in large figures; the AM/PM of the 12-hour format is drawn smaller. */
@Composable
fun BigTime(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    val small = SpanStyle(fontSize = 0.5.em, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val annotated = buildAnnotatedString {
        text.forEach { c -> if (c.isLetter()) withStyle(small) { append(c) } else append(c) }
    }
    FitText(annotated, style, modifier, color)
}
