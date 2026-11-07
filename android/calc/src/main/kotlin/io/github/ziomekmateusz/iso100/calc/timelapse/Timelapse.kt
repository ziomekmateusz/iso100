package io.github.ziomekmateusz.iso100.calc.timelapse

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.firstInvalid
import kotlin.math.roundToInt

/** What to calculate, with the other two durations (seconds). */
sealed interface TimelapseInput {
    val fps: Double
    val photoSizeMb: Double?

    data class FindClip(val intervalS: Double, val recordingS: Double, override val fps: Double, override val photoSizeMb: Double? = null) :
        TimelapseInput

    data class FindInterval(val clipS: Double, val recordingS: Double, override val fps: Double, override val photoSizeMb: Double? = null) :
        TimelapseInput

    data class FindRecording(val clipS: Double, val intervalS: Double, override val fps: Double, override val photoSizeMb: Double? = null) :
        TimelapseInput
}

/** Durations in seconds; [storageMb] in decimal megabytes (1 GB = 1000 MB), null without a photo size. */
data class Timelapse(val clipS: Double, val intervalS: Double, val recordingS: Double, val photos: Int, val storageMb: Double?) {
    companion object {
        fun solve(input: TimelapseInput): Outcome<Timelapse> {
            val fps = input.fps
            firstInvalid(Input.FPS to fps)?.let { return it }
            input.photoSizeMb?.let { size -> firstInvalid(Input.PHOTO_SIZE to size)?.let { return it } }
            val (clip, interval, recording) = when (input) {
                is TimelapseInput.FindClip -> {
                    firstInvalid(Input.INTERVAL to input.intervalS, Input.RECORDING_TIME to input.recordingS)?.let { return it }
                    Triple(input.recordingS / (input.intervalS * fps), input.intervalS, input.recordingS)
                }

                is TimelapseInput.FindInterval -> {
                    firstInvalid(Input.CLIP_LENGTH to input.clipS, Input.RECORDING_TIME to input.recordingS)?.let { return it }
                    Triple(input.clipS, input.recordingS / (fps * input.clipS), input.recordingS)
                }

                is TimelapseInput.FindRecording -> {
                    firstInvalid(Input.CLIP_LENGTH to input.clipS, Input.INTERVAL to input.intervalS)?.let { return it }
                    Triple(input.clipS, input.intervalS, input.intervalS * fps * input.clipS)
                }
            }
            val photos = (fps * clip).roundToInt()
            return Outcome.Ok(Timelapse(clip, interval, recording, photos, input.photoSizeMb?.let { photos * it }))
        }
    }
}
