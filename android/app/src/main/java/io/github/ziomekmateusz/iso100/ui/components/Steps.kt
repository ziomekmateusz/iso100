package io.github.ziomekmateusz.iso100.ui.components

// The values the ± buttons step through; past the ends they double or halve.

val FOCAL_LENGTHS = listOf(
    8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0, 28.0, 35.0, 40.0,
    50.0, 70.0, 85.0, 105.0, 135.0, 200.0, 300.0, 400.0, 600.0, 800.0,
)

/** In the unit the user reads, metres or feet. */
val DISTANCES = listOf(0.25, 0.3, 0.4, 0.5, 0.7, 1.0, 1.5, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 20.0, 30.0, 50.0, 100.0)

fun stepUp(value: Double, ladder: List<Double>): Double = ladder.firstOrNull { it > value * (1 + 1e-9) } ?: (value * 2)

fun stepDown(value: Double, ladder: List<Double>): Double = ladder.lastOrNull { it < value * (1 - 1e-9) } ?: (value / 2)

/**
 * The number in a field, written with a comma or a dot; null for anything else, so the field shows an
 * error instead of reading "2.5x" as 2.
 */
fun parseDecimal(text: String): Double? {
    val normalized = text.trim().replace(',', '.')
    return if (Regex("""\d+(\.\d*)?|\.\d+""").matches(normalized)) normalized.toDouble() else null
}
