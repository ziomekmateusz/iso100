package io.github.ziomekmateusz.iso100.ui.fov

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.fov.FieldOfView
import io.github.ziomekmateusz.iso100.calc.fov.fieldOfView
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.calc.valueOrNull
import io.github.ziomekmateusz.iso100.data.CatalogFormat
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.components.DISTANCES
import io.github.ziomekmateusz.iso100.ui.components.FOCAL_LENGTHS
import io.github.ziomekmateusz.iso100.ui.components.stepDown
import io.github.ziomekmateusz.iso100.ui.components.stepUp
import io.github.ziomekmateusz.iso100.ui.label
import kotlinx.parcelize.Parcelize

/** What the user typed; the distance is kept in metres whatever the units. */
@Parcelize
data class FovInput(val focalLengthMm: Double = 24.0, val distanceM: Double = 10.0) : Parcelable

sealed interface FovAction {
    data class SetFocalLength(val mm: Double) : FovAction
    data class StepFocalLength(val up: Boolean) : FovAction

    /** [value] in the user's [units]. */
    data class SetDistance(val value: Double, val units: UnitSystem) : FovAction
    data class StepDistance(val up: Boolean, val units: UnitSystem) : FovAction
}

fun FovInput.reduce(action: FovAction): FovInput = when (action) {
    is FovAction.SetFocalLength -> copy(focalLengthMm = action.mm)
    is FovAction.StepFocalLength ->
        copy(focalLengthMm = if (action.up) stepUp(focalLengthMm, FOCAL_LENGTHS) else stepDown(focalLengthMm, FOCAL_LENGTHS))
    is FovAction.SetDistance -> copy(distanceM = action.units.toMeters(action.value))
    is FovAction.StepDistance -> {
        val shown = action.units.fromMeters(distanceM)
        copy(distanceM = action.units.toMeters(if (action.up) stepUp(shown, DISTANCES) else stepDown(shown, DISTANCES)))
    }
}

/** A smaller format drawn inside the frame: its id and its size relative to the chosen one. */
data class FovFrame(val formatId: String, val widthRatio: Double, val heightRatio: Double)

data class FovUiState(
    val camera: CameraLabel?,
    val focalLengthMm: Double,
    /** In [units]. */
    val distance: Double,
    val units: UnitSystem,
    val result: FieldOfView?,
    val invalid: Outcome.Invalid?,
    /** Width / height of the chosen format. */
    val aspectRatio: Double,
    val frames: List<FovFrame>,
) {
    companion object {
        fun from(input: FovInput, settings: Settings, selection: Selection?, formats: List<CatalogFormat>): FovUiState {
            val format = selection?.format
            val outcome = format?.let { fieldOfView(input.focalLengthMm, input.distanceM, it.sensor) }
            return FovUiState(
                camera = selection?.label(),
                focalLengthMm = input.focalLengthMm,
                distance = settings.units.fromMeters(input.distanceM),
                units = settings.units,
                result = outcome?.valueOrNull(),
                invalid = outcome as? Outcome.Invalid,
                aspectRatio = format?.let { it.widthMm / it.heightMm } ?: 1.5,
                frames = format?.let { comparisonFrames(it, formats) }.orEmpty(),
            )
        }
    }
}

private val COMPARED = listOf("full-frame", "aps-c", "four-thirds", "1-inch")

/** The first two of Full Frame, APS-C, Micro 4/3 and 1" smaller than [chosen], i.e. with a larger crop factor. */
internal fun comparisonFrames(chosen: CatalogFormat, formats: List<CatalogFormat>): List<FovFrame> =
    COMPARED.mapNotNull { id -> formats.firstOrNull { it.id == id } }
        .filter { it.sensor.cropFactor > chosen.sensor.cropFactor * 1.001 }
        .take(2)
        .map { FovFrame(it.id, it.widthMm / chosen.widthMm, it.heightMm / chosen.heightMm) }
