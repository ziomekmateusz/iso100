package io.github.ziomekmateusz.iso100.calc.startrails

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureScale
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureValue
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.firstInvalid
import kotlin.math.sqrt

enum class StarRule { RULE_500, RULE_300, NPF }

/**
 * Longest exposure in seconds before the stars start to trail. NPF is the simplified (35 N + 30 p) / f,
 * with p the pixel pitch in µm, and needs [aperture] and [resolutionMp].
 */
fun maxStarExposure(
    rule: StarRule,
    focalLengthMm: Double,
    format: SensorFormat,
    aperture: Double? = null,
    resolutionMp: Double? = null,
): Outcome<Double> {
    firstInvalid(Input.FOCAL_LENGTH to focalLengthMm)?.let { return it }
    return when (rule) {
        StarRule.RULE_500 -> Outcome.Ok(500 / (focalLengthMm * format.cropFactor))

        StarRule.RULE_300 -> Outcome.Ok(300 / (focalLengthMm * format.cropFactor))

        StarRule.NPF -> {
            val n = aperture ?: Double.NaN
            val mp = resolutionMp ?: Double.NaN
            firstInvalid(Input.APERTURE to n, Input.RESOLUTION to mp)
                ?: Outcome.Ok((35 * n + 30 * pixelPitchUm(format, mp)) / focalLengthMm)
        }
    }
}

fun pixelPitchUm(format: SensorFormat, resolutionMp: Double): Double = sqrt(format.widthMm * format.heightMm / (resolutionMp * 1e6)) * 1000

/** The shutter to suggest: the longest of the scale that is not above the limit. */
fun suggestedShutter(limitS: Double, step: StopStep): ExposureValue? = ExposureScale.of(ExposureSetting.SHUTTER, step).longestAtMost(limitS)
