package io.github.ziomekmateusz.iso100.ui.stars

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureSetting
import io.github.ziomekmateusz.iso100.calc.exposure.ExposureValue
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.startrails.StarRule
import io.github.ziomekmateusz.iso100.calc.startrails.maxStarExposure
import io.github.ziomekmateusz.iso100.calc.startrails.pixelPitchUm
import io.github.ziomekmateusz.iso100.calc.startrails.suggestedShutter
import io.github.ziomekmateusz.iso100.calc.valueOrNull
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.components.Dial
import io.github.ziomekmateusz.iso100.ui.components.FOCAL_LENGTHS
import io.github.ziomekmateusz.iso100.ui.components.dial
import io.github.ziomekmateusz.iso100.ui.components.stepDown
import io.github.ziomekmateusz.iso100.ui.components.stepUp
import io.github.ziomekmateusz.iso100.ui.label
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** [rule] null: the default for the camera, NPF if it has the resolution. The aperture is exact, as for the DOF. */
@Serializable
@Parcelize
data class StarsInput(val rule: StarRule? = null, val focalLengthMm: Double = 24.0, val aperture: Double = 2.8) : Parcelable

sealed interface StarsAction {
    data class SetRule(val rule: StarRule) : StarsAction
    data class SetFocalLength(val mm: Double) : StarsAction
    data class StepFocalLength(val up: Boolean) : StarsAction
    data class SetAperture(val actual: Double) : StarsAction
}

fun StarsInput.reduce(action: StarsAction): StarsInput = when (action) {
    is StarsAction.SetRule -> copy(rule = action.rule)
    is StarsAction.SetFocalLength -> copy(focalLengthMm = action.mm)
    is StarsAction.StepFocalLength ->
        copy(focalLengthMm = if (action.up) stepUp(focalLengthMm, FOCAL_LENGTHS) else stepDown(focalLengthMm, FOCAL_LENGTHS))
    is StarsAction.SetAperture -> copy(aperture = action.actual)
}

/** A rule with its limit in seconds and the shutter to use, never above the limit; null when out of the domain. */
data class RuleResult(val rule: StarRule, val limitS: Double?, val shutter: ExposureValue?)

data class StarsUiState(
    val camera: CameraLabel?,
    val resolutionMp: Double?,
    /** The rules on offer: NPF only when the camera has the resolution. */
    val rules: List<StarRule>,
    val rule: StarRule,
    val step: StopStep,
    val focalLengthMm: Double,
    val aperture: Dial,
    val pixelPitchUm: Double?,
    /** The chosen rule first, then the others to compare. */
    val results: List<RuleResult>,
    val invalid: Outcome.Invalid?,
) {
    val chosen: RuleResult? get() = results.firstOrNull()

    companion object {
        fun from(input: StarsInput, settings: Settings, selection: Selection?): StarsUiState {
            // the resolution of the catalog is only an estimate, used for NPF alone
            val mp = selection?.camera?.specs?.resolutionMp
            val rules = if (mp != null) listOf(StarRule.RULE_500, StarRule.RULE_300, StarRule.NPF) else listOf(StarRule.RULE_500, StarRule.RULE_300)
            val rule = input.rule?.takeIf { it in rules } ?: if (mp != null) StarRule.NPF else StarRule.RULE_500
            val aperture = dial(ExposureSetting.APERTURE, settings.stopStep, input.aperture)
            val format = selection?.format?.sensor
            val outcomes = format?.let { f ->
                (listOf(rule) + (rules - rule)).associateWith { maxStarExposure(it, input.focalLengthMm, f, aperture.value.actual, mp) }
            }.orEmpty()
            return StarsUiState(
                camera = selection?.label(),
                resolutionMp = mp,
                rules = rules,
                rule = rule,
                step = settings.stopStep,
                focalLengthMm = input.focalLengthMm,
                aperture = aperture,
                pixelPitchUm = if (format != null && mp != null) pixelPitchUm(format, mp) else null,
                results = outcomes.map { (r, outcome) ->
                    val limit = outcome.valueOrNull()
                    RuleResult(r, limit, limit?.let { suggestedShutter(it, settings.stopStep) })
                },
                invalid = outcomes.values.firstNotNullOfOrNull { it as? Outcome.Invalid },
            )
        }
    }
}
