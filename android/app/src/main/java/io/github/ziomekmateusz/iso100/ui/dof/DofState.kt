package io.github.ziomekmateusz.iso100.ui.dof

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.dof.DepthOfField
import io.github.ziomekmateusz.iso100.calc.dof.depthOfField
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.calc.valueOrNull
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.components.DISTANCES
import io.github.ziomekmateusz.iso100.ui.components.Dial
import io.github.ziomekmateusz.iso100.ui.components.FOCAL_LENGTHS
import io.github.ziomekmateusz.iso100.ui.components.dial
import io.github.ziomekmateusz.iso100.ui.components.stepDown
import io.github.ziomekmateusz.iso100.ui.components.stepUp
import io.github.ziomekmateusz.iso100.ui.label
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** What the user chose: the exact aperture (f/8 = 8.0, f/5.6 = 5.657), the focus in metres whatever the units. */
@Serializable
@Parcelize
data class DofInput(val focalLengthMm: Double = 50.0, val aperture: Double = 8.0, val focusM: Double = 3.0) : Parcelable

sealed interface DofAction {
    data class SetAperture(val actual: Double) : DofAction
    data class SetFocalLength(val mm: Double) : DofAction
    data class StepFocalLength(val up: Boolean) : DofAction

    /** [value] in the user's [units]. */
    data class SetFocus(val value: Double, val units: UnitSystem) : DofAction
    data class StepFocus(val up: Boolean, val units: UnitSystem) : DofAction

    /** "At the hyperfocal": the distance comes from the result on screen. */
    data class FocusAt(val meters: Double) : DofAction
}

fun DofInput.reduce(action: DofAction): DofInput = when (action) {
    is DofAction.SetAperture -> copy(aperture = action.actual)
    is DofAction.SetFocalLength -> copy(focalLengthMm = action.mm)
    is DofAction.StepFocalLength ->
        copy(focalLengthMm = if (action.up) stepUp(focalLengthMm, FOCAL_LENGTHS) else stepDown(focalLengthMm, FOCAL_LENGTHS))
    is DofAction.SetFocus -> copy(focusM = action.units.toMeters(action.value))
    is DofAction.StepFocus -> {
        val shown = action.units.fromMeters(focusM)
        copy(focusM = action.units.toMeters(if (action.up) stepUp(shown, DISTANCES) else stepDown(shown, DISTANCES)))
    }
    is DofAction.FocusAt -> copy(focusM = action.meters)
}

data class DofUiState(
    val camera: CameraLabel?,
    val units: UnitSystem,
    val step: StopStep,
    val aperture: Dial,
    val focalLengthMm: Double,
    /** In [units]. */
    val focus: Double,
    val focusM: Double,
    val result: DepthOfField?,
    val invalid: Outcome.Invalid?,
) {
    companion object {
        fun from(input: DofInput, settings: Settings, selection: Selection?): DofUiState {
            // the aperture on the scale of the chosen step: changing the step in the settings moves it to the nearest value
            val aperture = dial(ExposureSetting.APERTURE, settings.stopStep, input.aperture)
            val outcome = selection?.let { depthOfField(input.focalLengthMm, aperture.value.actual, input.focusM, it.format.sensor) }
            return DofUiState(
                camera = selection?.label(),
                units = settings.units,
                step = settings.stopStep,
                aperture = aperture,
                focalLengthMm = input.focalLengthMm,
                focus = settings.units.fromMeters(input.focusM),
                focusM = input.focusM,
                result = outcome?.valueOrNull(),
                invalid = outcome as? Outcome.Invalid,
            )
        }
    }
}
