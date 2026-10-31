package io.github.ziomekmateusz.iso100.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.ziomekmateusz.iso100.calc.astro.SkyPhase

/** The colours of the sky band and of the two halves of the moon, which have no Material role. */
@Immutable
data class SkyColors(
    val night: Color,
    val astronomical: Color,
    val nautical: Color,
    val civil: Color,
    val golden: Color,
    val day: Color,
    val moonLit: Color,
    val moonShadow: Color,
) {
    fun of(phase: SkyPhase): Color = when (phase) {
        SkyPhase.NIGHT -> night
        SkyPhase.ASTRONOMICAL -> astronomical
        SkyPhase.NAUTICAL -> nautical
        SkyPhase.CIVIL -> civil
        SkyPhase.GOLDEN -> golden
        SkyPhase.DAY -> day
    }
}

private val night = Color(0xFF0E1014)
private val astronomical = Color(0xFF1C2438)
private val nautical = Color(0xFF2C3E6B)
private val civil = Color(0xFF5577BD)
private val golden = Color(0xFFFF8A3D)
private val day = Color(0xFFE9E3D6)

val DarkSky = SkyColors(night, astronomical, nautical, civil, golden, day, moonLit = day, moonShadow = Color(0xFF2A2D32))

// the design has no light moon: the shadow takes onSurfaceVariant, so that the disc stands out on white
val LightSky = DarkSky.copy(moonShadow = Color(0xFF5E5C57))

val RedNightSky = SkyColors(
    night = Color(0xFF000000),
    astronomical = Color(0xFF160504),
    nautical = Color(0xFF2A0906),
    civil = Color(0xFF3A0E0A),
    golden = Color(0xFFE0402F),
    day = Color(0xFFFF5A40),
    moonLit = Color(0xFFE0402F),
    moonShadow = Color(0xFF2A0906),
)

val LocalSkyColors = staticCompositionLocalOf { DarkSky }
