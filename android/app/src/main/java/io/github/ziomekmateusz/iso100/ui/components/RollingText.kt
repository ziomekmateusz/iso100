package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme

private val rollingBlur = 6.dp
private const val ROLL_MILLIS = 220

/**
 * A number whose characters roll inside their line when they change, with a light blur while they
 * move: up when a character grows ("1/125" to "1/250"), down when it shrinks. The unit after the last
 * digit keeps its place when the number changes length. Read as one text by TalkBack and tests.
 */
@Composable
fun RollingText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val slots = rollingSlots(text.text)
    Row(modifier.clearAndSetSemantics { this.text = text }) {
        for (index in text.indices) {
            key(slots[index]) {
                RollingCharacter(
                    text.subSequence(index, index + 1),
                    style,
                    color,
                    Modifier.alignByBaseline()
                )
            }
        }
    }
}

@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) =
    RollingText(AnnotatedString(text), style, modifier, color)

/**
 * The new character sets the size and the baseline; the old one is only drawn while it leaves, so
 * the roll never moves or grows the surrounding line.
 */
@Composable
private fun RollingCharacter(
    character: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier
) {
    var shown by remember { mutableStateOf(character) }
    var leaving by remember { mutableStateOf<AnnotatedString?>(null) }
    val roll = remember { Animatable(1f) }
    LaunchedEffect(character) {
        if (character == shown) return@LaunchedEffect
        leaving = shown
        shown = character
        roll.snapTo(0f)
        roll.animateTo(1f, tween(ROLL_MILLIS))
        leaving = null
    }
    val progress = roll.value
    val direction = if (shown.text < (leaving?.text ?: "")) -1f else 1f
    Layout(
        content = {
            Text(
                shown,
                Modifier
                    .graphicsLayer {
                        translationY = (1f - progress) * direction * size.height
                        alpha = progress
                    }
                    .blur(rollingBlur * (1f - progress), BlurredEdgeTreatment.Unbounded),
                style = style,
                color = color,
            )
            leaving?.let { leavingCharacter ->
                Text(
                    leavingCharacter,
                    Modifier
                        .graphicsLayer {
                            translationY = -progress * direction * size.height
                            alpha = 1f - progress
                        }
                        .blur(rollingBlur * progress, BlurredEdgeTreatment.Unbounded),
                    style = style,
                    color = color,
                )
            }
        },
        modifier = modifier.clipToBounds(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val entering = placeables.first()
        layout(
            entering.width,
            entering.height,
            mapOf(FirstBaseline to entering[FirstBaseline], LastBaseline to entering[LastBaseline]),
        ) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

/**
 * A stable slot for each character, so that the same slot rolls from one value to the next: counted
 * from the start, except the unit after the last digit (" s", " m"), counted from the end.
 */
internal fun rollingSlots(text: String): List<String> {
    val unitStart = text.indexOfLast { it.isDigit() } + 1
    return text.indices.map { index -> if (index < unitStart) "start $index" else "end ${text.length - index}" }
}

@Preview
@Composable
private fun RollingTextPreview() {
    Iso100Theme { RollingText("1/125 s", MaterialTheme.typography.displayMedium) }
}
