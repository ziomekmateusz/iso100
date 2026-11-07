package io.github.ziomekmateusz.iso100.calc

/** The result of a calculation: a value, or why there is none. Never an exception, infinity or NaN. */
sealed interface Outcome<out T> {
    data class Ok<out T>(val value: T) : Outcome<T>

    /** [input] must be greater than [minimum], in the unit the function takes it. */
    data class Invalid(val input: Input, val minimum: Double = 0.0) : Outcome<Nothing>

    /** No real setting gives this result: below the scale (f/0.25) or above it (ISO 400 000). */
    data class OutOfScale(val tooHigh: Boolean) : Outcome<Nothing>
}

enum class Input {
    FOCAL_LENGTH,
    FOCUS_DISTANCE,
    SUBJECT_DISTANCE,
    APERTURE,
    RESOLUTION,
    CLIP_LENGTH,
    INTERVAL,
    RECORDING_TIME,
    FPS,
    PHOTO_SIZE,
}

fun <T> Outcome<T>.valueOrNull(): T? = (this as? Outcome.Ok)?.value

/** The first input that is not a positive finite number. */
internal fun firstInvalid(vararg inputs: Pair<Input, Double>): Outcome.Invalid? =
    inputs.firstOrNull { (_, value) -> !(value > 0 && value.isFinite()) }?.let { Outcome.Invalid(it.first) }
