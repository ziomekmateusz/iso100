package io.github.ziomekmateusz.iso100.calc.units

/** Unit of the distances the user types and reads; the calculations take meters. */
enum class UnitSystem(private val metersPerUnit: Double) {
    METRIC(1.0),
    IMPERIAL(0.3048);

    fun toMeters(value: Double): Double = value * metersPerUnit

    fun fromMeters(meters: Double): Double = meters / metersPerUnit
}
