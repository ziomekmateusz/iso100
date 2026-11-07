package io.github.ziomekmateusz.iso100.ui.timelapse

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.timelapse.Timelapse
import io.github.ziomekmateusz.iso100.calc.valueOrNull
import io.github.ziomekmateusz.iso100.ui.components.stepDown
import io.github.ziomekmateusz.iso100.ui.components.stepUp
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import io.github.ziomekmateusz.iso100.calc.timelapse.TimelapseInput as Solve

/** What to calculate. */
enum class TimelapseTarget { CLIP, INTERVAL, RECORDING }

val FPS_CHOICES = listOf(24, 25, 30, 60)

/** The values the ± buttons step through, in seconds. */
private val INTERVALS = listOf(0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 8.0, 10.0, 15.0, 20.0, 30.0, 60.0)

private val CLIP_LENGTHS = listOf(5.0, 10.0, 15.0, 20.0, 30.0, 45.0, 60.0, 90.0, 120.0, 180.0, 300.0)

/** Durations in seconds; only the two that are not [solve] are used. [photoSizeMb] null: no storage estimate. */
@Serializable
@Parcelize
data class TimelapseInput(
    val solve: TimelapseTarget = TimelapseTarget.CLIP,
    val clipS: Double = 30.0,
    val intervalS: Double = 5.0,
    val recordingS: Double = 3600.0,
    val fps: Int = 25,
    val photoSizeMb: Double? = 25.0,
) : Parcelable

sealed interface TimelapseAction {
    data class Solve(val target: TimelapseTarget) : TimelapseAction
    data class SetClip(val seconds: Double) : TimelapseAction
    data class StepClip(val up: Boolean) : TimelapseAction
    data class SetInterval(val seconds: Double) : TimelapseAction
    data class StepInterval(val up: Boolean) : TimelapseAction
    data class SetRecording(val seconds: Double) : TimelapseAction
    data class SetFps(val fps: Int) : TimelapseAction
    data class SetPhotoSize(val mb: Double?) : TimelapseAction
}

fun TimelapseInput.reduce(action: TimelapseAction): TimelapseInput = when (action) {
    is TimelapseAction.Solve -> copy(solve = action.target)
    is TimelapseAction.SetClip -> copy(clipS = action.seconds)
    is TimelapseAction.StepClip -> copy(clipS = if (action.up) stepUp(clipS, CLIP_LENGTHS) else stepDown(clipS, CLIP_LENGTHS))
    is TimelapseAction.SetInterval -> copy(intervalS = action.seconds)
    is TimelapseAction.StepInterval -> copy(intervalS = if (action.up) stepUp(intervalS, INTERVALS) else stepDown(intervalS, INTERVALS))
    is TimelapseAction.SetRecording -> copy(recordingS = action.seconds)
    is TimelapseAction.SetFps -> copy(fps = action.fps)
    is TimelapseAction.SetPhotoSize -> copy(photoSizeMb = action.mb)
}

data class TimelapseUiState(
    val solve: TimelapseTarget,
    val clipS: Double,
    val intervalS: Double,
    val recordingS: Double,
    val fps: Int,
    val photoSizeMb: Double?,
    val result: Timelapse?,
    /** An input out of the domain is a message, never infinity or NaN. */
    val invalid: Outcome.Invalid?,
) {
    companion object {
        fun from(input: TimelapseInput): TimelapseUiState {
            val fps = input.fps.toDouble()
            val outcome = Timelapse.solve(
                when (input.solve) {
                    TimelapseTarget.CLIP -> Solve.FindClip(input.intervalS, input.recordingS, fps, input.photoSizeMb)
                    TimelapseTarget.INTERVAL -> Solve.FindInterval(input.clipS, input.recordingS, fps, input.photoSizeMb)
                    TimelapseTarget.RECORDING -> Solve.FindRecording(input.clipS, input.intervalS, fps, input.photoSizeMb)
                },
            )
            return TimelapseUiState(
                solve = input.solve,
                clipS = input.clipS,
                intervalS = input.intervalS,
                recordingS = input.recordingS,
                fps = input.fps,
                photoSizeMb = input.photoSizeMb,
                result = outcome.valueOrNull(),
                invalid = outcome as? Outcome.Invalid,
            )
        }
    }
}
