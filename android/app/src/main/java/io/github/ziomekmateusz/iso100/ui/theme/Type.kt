package io.github.ziomekmateusz.iso100.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R

/** For numbers, always with tabular figures. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

/** For text. */
val BarlowSemiCondensed = FontFamily(
    Font(R.font.barlow_semi_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_semi_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semi_condensed_semibold, FontWeight.SemiBold),
)

private fun numbers(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontFeatureSettings = "tnum",
)

private fun text(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = BarlowSemiCondensed,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private val base = Typography()

/** The type scale of the app; the styles it does not set keep Material's sizes in Barlow Semi Condensed. */
val Iso100Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = BarlowSemiCondensed),
    displayMedium = numbers(52, 55),
    displaySmall = base.displaySmall.copy(fontFamily = BarlowSemiCondensed),
    headlineLarge = base.headlineLarge.copy(fontFamily = BarlowSemiCondensed),
    headlineMedium = numbers(34, 40),
    headlineSmall = base.headlineSmall.copy(fontFamily = BarlowSemiCondensed),
    titleLarge = text(22, 28, FontWeight.SemiBold),
    titleMedium = numbers(24, 30),
    titleSmall = base.titleSmall.copy(fontFamily = BarlowSemiCondensed),
    bodyLarge = text(16, 22, FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontFamily = BarlowSemiCondensed),
    bodySmall = base.bodySmall.copy(fontFamily = BarlowSemiCondensed),
    labelLarge = text(14, 20, FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = BarlowSemiCondensed),
    labelSmall = base.labelSmall.copy(fontFamily = BarlowSemiCondensed),
)
