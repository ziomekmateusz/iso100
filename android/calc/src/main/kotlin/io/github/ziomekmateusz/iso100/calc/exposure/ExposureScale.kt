package io.github.ziomekmateusz.iso100.calc.exposure

import io.github.ziomekmateusz.iso100.calc.Outcome
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

enum class StopStep(val stops: Double) { FULL(1.0), HALF(0.5), THIRD(1.0 / 3) }

enum class ExposureSetting { ISO, APERTURE, SHUTTER }

/** A setting as the camera shows it ([nominal]: f/5.6, 1/125 s = 0.008) and its exact value ([actual]: 5.657, 1/128 s). */
data class ExposureValue(val nominal: Double, val actual: Double)

/** An equivalent setting: the calculated value and the nearest value of the scale, null for a shutter longer than the scale. */
data class Equivalent(val exact: Double, val nearest: ExposureValue?)

/** The ISO, aperture or shutter values a camera offers at the given stop step. */
class ExposureScale private constructor(val setting: ExposureSetting, val step: StopStep, val values: List<ExposureValue>) {

    /** What one stop multiplies a value by. */
    private val stopFactor = if (setting == ExposureSetting.APERTURE) sqrt(2.0) else 2.0

    /** The value closest to [value], measured in stops. */
    fun nearest(value: Double): ExposureValue = values.minBy { abs(ln(it.actual / value)) }

    fun stopsBetween(a: Double, b: Double): Double = abs(ln(a / b)) / ln(stopFactor)

    /** [exact] on this scale; more than half a step beyond an end is out of scale, except for shutters longer than the scale. */
    fun equivalent(exact: Double): Outcome<Equivalent> {
        val margin = stopFactor.pow(step.stops / 2)
        return when {
            exact < values.first().actual / margin -> Outcome.OutOfScale(tooHigh = false)
            exact > values.last().actual * margin ->
                if (setting == ExposureSetting.SHUTTER) Outcome.Ok(Equivalent(exact, null)) else Outcome.OutOfScale(tooHigh = true)
            else -> Outcome.Ok(Equivalent(exact, nearest(exact)))
        }
    }

    /** The longest shutter whose nominal value is at most [seconds], so never above the limit. */
    fun longestAtMost(seconds: Double): ExposureValue? = values.lastOrNull { it.nominal <= seconds }

    companion object {
        private val scales = ExposureSetting.entries.flatMap { setting ->
            StopStep.entries.map { step -> ExposureScale(setting, step, select(table(setting), step)) }
        }

        fun of(setting: ExposureSetting, step: StopStep): ExposureScale =
            scales.first { it.setting == setting && it.step == step }
    }
}

// Every group of four values is one stop: full, +1/3, +1/2, +2/3. The exponents are rounded like the old app, so the results match it.
private val OFFSETS = doubleArrayOf(0.0, 0.333, 0.5, 0.667)

private fun select(all: List<ExposureValue>, step: StopStep): List<ExposureValue> = all.filterIndexed { i, _ ->
    when (step) {
        StopStep.FULL -> i % 4 == 0
        StopStep.HALF -> i % 2 == 0
        StopStep.THIRD -> i % 4 != 2
    }
}

private fun table(setting: ExposureSetting): List<ExposureValue> = when (setting) {
    ExposureSetting.ISO -> values(2.0, 4.644, ISO)
    ExposureSetting.APERTURE -> values(sqrt(2.0), -1.0, APERTURES)
    ExposureSetting.SHUTTER -> values(2.0, -13.0, SHUTTERS)
}

private fun values(base: Double, firstExponent: Double, nominals: List<Double>) =
    nominals.mapIndexed { i, nominal -> ExposureValue(nominal, base.pow(firstExponent + i / 4 + OFFSETS[i % 4])) }

private val ISO = listOf(
    25, 32, 35, 40, 50, 64, 70, 80, 100, 125, 140, 160, 200, 250, 280, 320, 400, 500, 560, 640,
    800, 1000, 1100, 1250, 1600, 2000, 2200, 2500, 3200, 4000, 4400, 5000, 6400, 8000, 8800, 10000,
    12800, 16000, 17600, 20000, 25600, 32000, 35200, 40000, 51200, 64000, 70400, 80000, 102400,
).map { it.toDouble() }

private val APERTURES = listOf(
    0.7, 0.8, 0.8, 0.9, 1.0, 1.1, 1.2, 1.2, 1.4, 1.6, 1.7, 1.8, 2.0, 2.2, 2.4, 2.5, 2.8, 3.2, 3.3, 3.5,
    4.0, 4.5, 4.8, 5.0, 5.6, 6.3, 6.7, 7.1, 8.0, 9.0, 9.5, 10.0, 11.0, 13.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0,
    22.0, 25.0, 27.0, 28.0, 32.0,
)

private val SHUTTERS = listOf(
    8000, 6400, 6000, 5000, 4000, 3200, 3000, 2500, 2000, 1600, 1500, 1250, 1000, 800, 750, 640,
    500, 400, 350, 320, 250, 200, 180, 160, 125, 100, 90, 80, 60, 50, 45, 40, 30, 25, 20, 20,
    15, 13, 10, 10, 8, 6, 6, 5, 4, 3, 3, 2.5, 2, 1.6, 1.5, 1.3,
).map { 1.0 / it.toDouble() } + listOf(1.0, 1.3, 1.5, 1.6, 2.0, 2.5, 3.0, 3.0, 4.0, 5.0, 6.0, 6.0, 8.0, 10.0, 10.0, 13.0, 15.0, 20.0, 20.0, 25.0, 30.0)
