package io.github.ziomekmateusz.iso100.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The colours of the design; every other Material role is derived from them,
// so the red night scheme has no colour other than red.
private fun scheme(
    dark: Boolean,
    background: Color,
    surface: Color,
    surfaceContainerHigh: Color,
    outlineVariant: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    primary: Color,
    error: Color,
): ColorScheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = primary,
    onPrimary = background,
    primaryContainer = surfaceContainerHigh,
    onPrimaryContainer = onSurface,
    inversePrimary = primary,
    secondary = primary,
    onSecondary = background,
    secondaryContainer = outlineVariant,
    onSecondaryContainer = onSurface,
    tertiary = primary,
    onTertiary = background,
    tertiaryContainer = surfaceContainerHigh,
    onTertiaryContainer = onSurface,
    background = background,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceContainerHigh,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = primary,
    inverseSurface = onSurface,
    inverseOnSurface = surface,
    error = error,
    onError = background,
    errorContainer = surfaceContainerHigh,
    onErrorContainer = error,
    outline = onSurfaceVariant,
    outlineVariant = outlineVariant,
    scrim = Color.Black,
    surfaceBright = surfaceContainerHigh,
    surfaceDim = background,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = outlineVariant,
)

val DarkColors = scheme(
    dark = true,
    background = Color(0xFF16171A),
    surface = Color(0xFF202226),
    surfaceContainerHigh = Color(0xFF2A2D32),
    outlineVariant = Color(0xFF34373D),
    onSurface = Color(0xFFEDEBE6),
    onSurfaceVariant = Color(0xFFA3A19B),
    primary = Color(0xFFFF8A3D),
    error = Color(0xFFFF8F8F),
)

val LightColors = scheme(
    dark = false,
    background = Color(0xFFF4F2EE),
    surface = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECE9E3),
    outlineVariant = Color(0xFFD9D5CD),
    onSurface = Color(0xFF1A1B1E),
    onSurfaceVariant = Color(0xFF5E5C57),
    primary = Color(0xFFC2550F),
    error = lightColorScheme().error,
)

val RedNightColors = scheme(
    dark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF160504),
    surfaceContainerHigh = Color(0xFF2A0906),
    outlineVariant = Color(0xFF3A0E0A),
    onSurface = Color(0xFFE0402F),
    onSurfaceVariant = Color(0xFFE0402F),
    primary = Color(0xFFFF5A40),
    error = Color(0xFFFF5A40),
)
