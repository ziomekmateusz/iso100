package io.github.ziomekmateusz.iso100.calc.camera

import kotlin.math.hypot

/** A sensor format in millimeters, landscape. */
data class SensorFormat(val widthMm: Double, val heightMm: Double) {
    val diagonalMm: Double get() = hypot(widthMm, heightMm)

    /** Full frame diagonal divided by this diagonal. */
    val cropFactor: Double get() = FULL_FRAME_DIAGONAL_MM / diagonalMm

    /** The diagonal over 1500, the usual criterion for sharpness. */
    val circleOfConfusionMm: Double get() = diagonalMm / 1500

    private companion object {
        val FULL_FRAME_DIAGONAL_MM = hypot(36.0, 24.0)
    }
}
