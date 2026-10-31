package io.github.ziomekmateusz.iso100.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.Selection

/** The chosen camera as the chips show it: "Sony Alpha 7 III" and "Full Frame (35 mm)", or only the format. */
data class CameraLabel(val title: String, val detail: String?)

fun Selection.label(): CameraLabel =
    if (camera != null) CameraLabel("${camera.maker} ${camera.model}", format.name) else CameraLabel(format.name, null)

/** "1/3 stop" next to the dials. */
@Composable
fun stopStepText(step: StopStep): String = stringResource(
    when (step) {
        StopStep.FULL -> R.string.stop_step_full
        StopStep.HALF -> R.string.stop_step_half
        StopStep.THIRD -> R.string.stop_step_third
    },
)

@Composable
fun distanceSymbol(units: UnitSystem): String =
    stringResource(if (units == UnitSystem.METRIC) R.string.symbol_m else R.string.symbol_ft)

/** A shutter time: "1/125 s", "1,3 s", "30 s"; from a minute on as [durationText]. */
@Composable
fun shutterText(seconds: Double): String {
    val locale = currentLocale()
    val s = stringResource(R.string.symbol_s)
    return when {
        seconds < 1 -> "1/" + shortNumber(1 / seconds, locale, if (1 / seconds >= 10) 0 else 1) + " " + s
        seconds < 60 -> shortNumber(seconds, locale, if (seconds >= 10) 0 else 1) + " " + s
        else -> durationText(seconds)
    }
}

/** A duration: "28,8 s", "2 min 8 s", "2 min", "1 h 5 min", "1 h" (no seconds from an hour on). */
@Composable
fun durationText(seconds: Double): String {
    if (seconds < 60) return shortNumber(seconds, currentLocale()) + " " + stringResource(R.string.symbol_s)
    val (h, m, sec) = hms(seconds)
    return when {
        h > 0 && m > 0 -> stringResource(R.string.duration_h_min, h, m)
        h > 0 -> stringResource(R.string.duration_h, h)
        sec > 0 -> stringResource(R.string.duration_min_s, m, sec)
        else -> stringResource(R.string.duration_min, m)
    }
}

/** "2,35 m" or "7,71 ft" from metres. */
@Composable
fun distanceText(meters: Double, units: UnitSystem): String =
    distanceNumber(units.fromMeters(meters), currentLocale()) + " " + distanceSymbol(units)

/** The message of an input out of the domain, e.g. "The interval must be greater than 0 s." */
@Composable
fun invalidMessage(invalid: Outcome.Invalid, units: UnitSystem): String {
    val locale = currentLocale()
    @Composable
    fun plain(symbol: Int) = shortNumber(invalid.minimum, locale, 2) + " " + stringResource(symbol)
    return when (invalid.input) {
        Input.FOCAL_LENGTH -> stringResource(R.string.error_focal_length, plain(R.string.symbol_mm))
        Input.FOCUS_DISTANCE -> stringResource(R.string.error_focus_distance, distanceText(invalid.minimum, units))
        Input.SUBJECT_DISTANCE -> stringResource(R.string.error_subject_distance, distanceText(invalid.minimum, units))
        Input.APERTURE -> stringResource(R.string.error_aperture, shortNumber(invalid.minimum, locale))
        Input.RESOLUTION -> stringResource(R.string.error_resolution, plain(R.string.symbol_mp))
        Input.CLIP_LENGTH -> stringResource(R.string.error_clip_length, plain(R.string.symbol_s))
        Input.INTERVAL -> stringResource(R.string.error_interval, plain(R.string.symbol_s))
        Input.RECORDING_TIME -> stringResource(R.string.error_recording_time, plain(R.string.symbol_s))
        Input.FPS -> stringResource(R.string.error_fps, shortNumber(invalid.minimum, locale))
        Input.PHOTO_SIZE -> stringResource(R.string.error_photo_size, plain(R.string.symbol_mb))
    }
}

/** The error of [input], if the outcome is about it. */
@Composable
fun Outcome.Invalid?.messageFor(input: Input, units: UnitSystem): String? =
    if (this != null && this.input == input) invalidMessage(this, units) else null
