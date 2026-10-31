package io.github.ziomekmateusz.iso100.calc.exposure

import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/** A metered exposure. [ev] is at the given ISO, [lv] (light value) is normalized to ISO 100. */
data class Exposure(val iso: Double, val aperture: Double, val shutterS: Double) {
    val ev: Double get() = log2(aperture * aperture / shutterS)
    val lv: Double get() = ev - log2(iso / 100)

    fun equivalentIso(aperture: Double, shutterS: Double, nd: NdFilter? = null): Double =
        aperture * aperture * 100 / (shutterS * light(nd))

    fun equivalentAperture(iso: Double, shutterS: Double, nd: NdFilter? = null): Double =
        sqrt(light(nd) * shutterS * iso / 100)

    fun equivalentShutter(iso: Double, aperture: Double, nd: NdFilter? = null): Double =
        aperture * aperture * 100 / (light(nd) * iso)

    private fun light(nd: NdFilter?): Double = 2.0.pow(lv - (nd?.stops ?: 0.0))
}

enum class NdFilter(val factor: Int) {
    ND2(2), ND4(4), ND8(8), ND16(16), ND32(32), ND64(64), ND100(100), ND128(128),
    ND256(256), ND400(400), ND512(512), ND1000(1000), ND1024(1024);

    /** Light reduction in stops, computed from the factor. */
    val stops: Double get() = log2(factor.toDouble())
    val opticalDensity: Double get() = log10(factor.toDouble())
}
