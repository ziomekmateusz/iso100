package io.github.ziomekmateusz.iso100.ui.exposure

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.Equivalent
import io.github.ziomekmateusz.iso100.calc.exposure.Exposure
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureScale
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.APERTURE
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.ISO
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting.SHUTTER
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureValue
import io.github.ziomekmateusz.iso100.calc.exposure.NdFilter
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.components.Dial
import io.github.ziomekmateusz.iso100.ui.components.dial
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ExposureSheet { MEASURED, ND_FILTERS }

/**
 * The metered exposure and the new settings, kept by value: the dials show the nearest value of the
 * scale at the step of the settings. Only the two settings that are not [solve] are used.
 */
@Serializable
@Parcelize
data class ExposureInput(
    val solve: ExposureSetting = SHUTTER,
    val measuredIso: Double = 100.0,
    val measuredAperture: Double = 8.0,
    val measuredShutterS: Double = 1.0 / 125,
    val iso: Double = 100.0,
    val aperture: Double = 8.0,
    val shutterS: Double = 1.0 / 125,
    val nd: NdFilter? = null,
    // a sheet left open is not reopened next time
    @Transient val sheet: ExposureSheet? = null,
) : Parcelable

sealed interface ExposureAction {
    data class Solve(val setting: ExposureSetting) : ExposureAction
    data class SetMeasured(val setting: ExposureSetting, val value: Double) : ExposureAction
    data class SetNew(val setting: ExposureSetting, val value: Double) : ExposureAction

    /** Null: no filter. Choosing one closes the list. */
    data class SetNd(val filter: NdFilter?) : ExposureAction
    data class Open(val sheet: ExposureSheet) : ExposureAction
    data object CloseSheet : ExposureAction
}

fun ExposureInput.reduce(action: ExposureAction): ExposureInput = when (action) {
    is ExposureAction.Solve -> copy(solve = action.setting)
    is ExposureAction.SetMeasured -> when (action.setting) {
        ISO -> copy(measuredIso = action.value)
        APERTURE -> copy(measuredAperture = action.value)
        SHUTTER -> copy(measuredShutterS = action.value)
    }
    is ExposureAction.SetNew -> when (action.setting) {
        ISO -> copy(iso = action.value)
        APERTURE -> copy(aperture = action.value)
        SHUTTER -> copy(shutterS = action.value)
    }
    is ExposureAction.SetNd -> copy(nd = action.filter, sheet = null)
    is ExposureAction.Open -> copy(sheet = action.sheet)
    ExposureAction.CloseSheet -> copy(sheet = null)
}

data class ExposureUiState(
    val solve: ExposureSetting,
    val step: StopStep,
    /** ISO, aperture and shutter as metered. */
    val measured: Map<ExposureSetting, Dial>,
    val ev: Double,
    /** The two settings the user keeps, in the order ISO, aperture, shutter. */
    val kept: Map<ExposureSetting, Dial>,
    val result: Outcome<Equivalent>,
    /** The calculated value is far enough from the value of the scale to be shown too. */
    val exactDiffers: Boolean,
    /** The end of the scale that an out-of-scale result goes past. */
    val limit: ExposureValue?,
    val nd: NdFilter?,
    val sheet: ExposureSheet?,
) {
    companion object {
        fun from(input: ExposureInput, settings: Settings, selection: Selection?): ExposureUiState {
            val step = settings.stopStep
            val specs = selection?.camera?.specs
            // ISO values the camera does not have are only dimmed, never blocked
            val isoRange = if (specs?.isoMin != null && specs.isoMax != null) specs.isoMin.toDouble()..specs.isoMax.toDouble() else null
            fun dials(iso: Double, aperture: Double, shutterS: Double) = linkedMapOf(
                ISO to dial(ISO, step, iso, isoRange),
                APERTURE to dial(APERTURE, step, aperture),
                SHUTTER to dial(SHUTTER, step, shutterS),
            )
            val measured = dials(input.measuredIso, input.measuredAperture, input.measuredShutterS)
            val new = dials(input.iso, input.aperture, input.shutterS)
            val metered = Exposure(measured.getValue(ISO).value.actual, measured.getValue(APERTURE).value.actual, measured.getValue(SHUTTER).value.actual)
            val iso = new.getValue(ISO).value.actual
            val aperture = new.getValue(APERTURE).value.actual
            val shutter = new.getValue(SHUTTER).value.actual
            val exact = when (input.solve) {
                ISO -> metered.equivalentIso(aperture, shutter, input.nd)
                APERTURE -> metered.equivalentAperture(iso, shutter, input.nd)
                SHUTTER -> metered.equivalentShutter(iso, aperture, input.nd)
            }
            val scale = ExposureScale.of(input.solve, step)
            val result = scale.equivalent(exact)
            val nearest = (result as? Outcome.Ok)?.value?.nearest
            return ExposureUiState(
                solve = input.solve,
                step = step,
                measured = measured,
                ev = metered.ev,
                kept = LinkedHashMap(new.filterKeys { it != input.solve }),
                result = result,
                exactDiffers = nearest != null && scale.stopsBetween(exact, nearest.actual) > 0.05,
                limit = (result as? Outcome.OutOfScale)?.let { if (it.tooHigh) scale.values.last() else scale.values.first() },
                nd = input.nd,
                sheet = input.sheet,
            )
        }
    }
}

/** The filters with their own chip; the others are in the full list. */
val ND_CHIPS = listOf(NdFilter.ND8, NdFilter.ND64, NdFilter.ND1000)
