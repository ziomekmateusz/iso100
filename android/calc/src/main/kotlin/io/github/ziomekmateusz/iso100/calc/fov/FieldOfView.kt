package io.github.ziomekmateusz.iso100.calc.fov

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import io.github.ziomekmateusz.iso100.calc.firstInvalid
import kotlin.math.atan

/** Angles in degrees and the framed area in meters at the subject distance. */
data class FieldOfView(val horizontalDeg: Double, val verticalDeg: Double, val diagonalDeg: Double, val widthM: Double, val heightM: Double)

fun fieldOfView(focalLengthMm: Double, distanceM: Double, format: SensorFormat): Outcome<FieldOfView> {
    firstInvalid(Input.FOCAL_LENGTH to focalLengthMm, Input.SUBJECT_DISTANCE to distanceM)?.let { return it }
    fun angle(sizeMm: Double) = Math.toDegrees(2 * atan(sizeMm / (2 * focalLengthMm)))
    return Outcome.Ok(
        FieldOfView(
            horizontalDeg = angle(format.widthMm),
            verticalDeg = angle(format.heightMm),
            diagonalDeg = angle(format.diagonalMm),
            widthM = format.widthMm * distanceM / focalLengthMm,
            heightM = format.heightMm * distanceM / focalLengthMm,
        ),
    )
}
