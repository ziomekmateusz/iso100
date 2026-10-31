package io.github.ziomekmateusz.iso100.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// Numbers as the user's language writes them (2,35 in Italian, 2.35 in English); units come from strings.xml.

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

fun number(value: Double, decimals: Int, locale: Locale): String = String.format(locale, "%.${decimals}f", value)

/** At most [maxDecimals] decimals, without trailing zeros: 8, 5,6, 2,35. */
fun shortNumber(value: Double, locale: Locale, maxDecimals: Int = 1): String {
    var text = number(value, maxDecimals, locale)
    val separator = number(0.5, 1, locale)[1]
    if (separator in text) text = text.trimEnd('0').trimEnd(separator)
    return text
}

/** A shutter time as cameras write it: 1/125, 1/1,6, 1,3, 30. */
fun shutterNumber(seconds: Double, locale: Locale): String =
    if (seconds < 1) "1/" + shortNumber(1 / seconds, locale) else shortNumber(seconds, locale)

/** Distances: two decimals below 100, none above. */
fun distanceNumber(value: Double, locale: Locale): String = number(value, if (abs(value) < 100) 2 else 0, locale)

/** Hours, minutes and seconds of a duration rounded to the second. */
fun hms(seconds: Double): Triple<Long, Long, Long> {
    val total = seconds.roundToLong()
    return Triple(total / 3600, total % 3600 / 60, total % 60)
}

/** A date formatter in the language of the user, from an Android skeleton: "21 giugno 2026", "domenica", "dom 18 ott". */
fun dateFormatter(skeleton: String, locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

/**
 * Hours and minutes, 24 or 12 hours as the phone is set: "05:35" or "5:35 AM". The spaces do not break,
 * so that "4:54 AM–5:16 AM" wraps only after the dash.
 */
@Composable
fun timeFormatter(): (TemporalAccessor) -> String {
    val locale = currentLocale()
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24) {
        val formatter = dateFormatter(if (is24) "HHmm" else "hmm", locale)
        // every kind of space, as Android writes "5:35 AM" with U+202F, which the line breaker may break
        val format: (TemporalAccessor) -> String = { time ->
            formatter.format(time).map { if (Character.isSpaceChar(it)) ' ' else it }.joinToString("")
        }
        format
    }
}

fun String.capitalized(locale: Locale): String = replaceFirstChar { it.titlecase(locale) }
