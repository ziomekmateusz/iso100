package io.github.ziomekmateusz.iso100.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.calc.astro.SkyPhase
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.LocalSkyColors
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews

/** A piece of the band: a phase from [start] to [end], as fractions of the day (0 = 00:00, 1 = 24:00). */
data class BandSegment(val phase: SkyPhase, val start: Float, val end: Float)

/**
 * The 24 hours split by phases of light, in the colours of the sky; [now] marks the current hour when the
 * day is today. A polar day is a single segment.
 */
@Composable
fun SkyBand(segments: List<BandSegment>, now: Float?, description: String, modifier: Modifier = Modifier) {
    val sky = LocalSkyColors.current
    val marker = MaterialTheme.colorScheme.onSurface
    Column(modifier.semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            val top = 4.dp.toPx()
            val height = 18.dp.toPx()
            val band = Path().apply { addRoundRect(RoundRect(0f, top, size.width, top + height, CornerRadius(height / 2))) }
            clipPath(band) {
                segments.forEach { s ->
                    drawRect(sky.of(s.phase), Offset(s.start * size.width, top), Size((s.end - s.start) * size.width + 0.5f, height))
                }
            }
            now?.let { drawRect(marker, Offset(it * size.width - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach {
                Text(it, fontFamily = Barlow, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val previewSegments = listOf(
    BandSegment(
        SkyPhase.NIGHT,
        0f,
        0.14f,
    ),
    BandSegment(SkyPhase.ASTRONOMICAL, 0.14f, 0.178f), BandSegment(SkyPhase.NAUTICAL, 0.178f, 0.208f),
    BandSegment(SkyPhase.CIVIL, 0.208f, 0.232f), BandSegment(SkyPhase.GOLDEN, 0.232f, 0.262f), BandSegment(SkyPhase.DAY, 0.262f, 0.837f),
    BandSegment(
        SkyPhase.GOLDEN,
        0.837f,
        0.866f,
    ),
    BandSegment(SkyPhase.CIVIL, 0.866f, 0.891f), BandSegment(SkyPhase.NAUTICAL, 0.891f, 0.921f),
    BandSegment(SkyPhase.ASTRONOMICAL, 0.921f, 0.959f), BandSegment(SkyPhase.NIGHT, 0.959f, 1f),
)

@Preview
@Composable
private fun SkyBandPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { Surface { SkyBand(previewSegments, 0.55f, "", Modifier.padding(16.dp)) } }
}

@Preview(fontScale = 2f)
@Composable
private fun SkyBandLargeTextPreview() {
    Iso100Theme { Surface { SkyBand(previewSegments, null, "", Modifier.padding(16.dp)) } }
}

@Preview
@Composable
private fun SkyBandPolarPreview() {
    Iso100Theme { Surface { SkyBand(listOf(BandSegment(SkyPhase.DAY, 0f, 1f)), null, "", Modifier.padding(16.dp)) } }
}
