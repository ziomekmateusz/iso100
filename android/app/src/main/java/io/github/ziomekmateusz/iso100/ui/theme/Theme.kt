package io.github.ziomekmateusz.iso100.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.github.ziomekmateusz.iso100.data.AppTheme

private val Iso100Shapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

fun AppTheme.isDark(systemDark: Boolean): Boolean = when (this) {
    AppTheme.SYSTEM -> systemDark
    AppTheme.LIGHT -> false
    AppTheme.DARK, AppTheme.RED_NIGHT -> true
}

@Composable
fun Iso100Theme(theme: AppTheme = AppTheme.DARK, content: @Composable () -> Unit) {
    val (colors, sky) = when {
        theme == AppTheme.RED_NIGHT -> RedNightColors to RedNightSky
        theme.isDark(isSystemInDarkTheme()) -> DarkColors to DarkSky
        else -> LightColors to LightSky
    }
    CompositionLocalProvider(LocalSkyColors provides sky) {
        MaterialTheme(colorScheme = colors, typography = Iso100Typography, shapes = Iso100Shapes, content = content)
    }
}

/** The three colour schemes, for @PreviewParameter. */
class ThemePreviews : PreviewParameterProvider<AppTheme> {
    override val values = sequenceOf(AppTheme.DARK, AppTheme.LIGHT, AppTheme.RED_NIGHT)
}
