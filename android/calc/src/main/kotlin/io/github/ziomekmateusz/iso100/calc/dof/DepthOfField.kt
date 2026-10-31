package io.github.ziomekmateusz.iso100.calc.dof

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import io.github.ziomekmateusz.iso100.calc.firstInvalid

/** Sharp zone in meters; [farM] null when it reaches infinity. */
data class DepthOfField(val nearM: Double, val farM: Double?, val hyperfocalM: Double) {
    val depthM: Double? get() = farM?.minus(nearM)
}

fun hyperfocalMm(focalLengthMm: Double, aperture: Double, circleOfConfusionMm: Double): Double =
    focalLengthMm * focalLengthMm / (aperture * circleOfConfusionMm) + focalLengthMm

fun depthOfField(focalLengthMm: Double, aperture: Double, focusM: Double, format: SensorFormat): Outcome<DepthOfField> {
    firstInvalid(Input.FOCAL_LENGTH to focalLengthMm, Input.APERTURE to aperture, Input.FOCUS_DISTANCE to focusM)
        ?.let { return it }
    val f = focalLengthMm
    val focus = focusM * 1000
    if (focus <= f) return Outcome.Invalid(Input.FOCUS_DISTANCE, minimum = f / 1000)
    val h = hyperfocalMm(f, aperture, format.circleOfConfusionMm)
    val near = focus * (h - f) / (h + focus - 2 * f)
    // h converted to metres and back can come out a hair smaller: still the hyperfocal, so the far limit is infinite
    val far = if (focus >= h * (1 - 1e-9)) null else focus * (h - f) / (h - focus)
    return Outcome.Ok(DepthOfField(near / 1000, far?.div(1000), h / 1000))
}
