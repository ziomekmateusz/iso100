package io.github.ziomekmateusz.iso100.ui.timer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A countdown of [totalMs]. While it runs, [endsAtMs] is when it reaches zero on the clock of the
 * device and the time left is worked out from it, so a late frame never makes it drift.
 */
@Parcelize
data class TimerInput(val totalMs: Long, val remainingMs: Long = totalMs, val endsAtMs: Long? = null) : Parcelable {
    val running: Boolean get() = endsAtMs != null

    /** How long until the shown second changes, which is also exactly at zero on the last one. */
    val untilNextSecondMs: Long get() = (remainingMs - 1) % 1000 + 1
}

sealed interface TimerAction {
    /** Starts, goes on after a pause, or begins again once at zero. */
    data class Start(val nowMs: Long) : TimerAction
    data class Pause(val nowMs: Long) : TimerAction
    data class Tick(val nowMs: Long) : TimerAction
    data object Reset : TimerAction
}

fun TimerInput.reduce(action: TimerAction): TimerInput = when (action) {
    is TimerAction.Start -> when {
        running -> this
        remainingMs == 0L -> copy(remainingMs = totalMs, endsAtMs = action.nowMs + totalMs)
        else -> copy(endsAtMs = action.nowMs + remainingMs)
    }

    is TimerAction.Pause -> endsAtMs?.let {
        copy(
            remainingMs = (it - action.nowMs).coerceAtLeast(0),
            endsAtMs = null,
        )
    } ?: this

    is TimerAction.Tick -> endsAtMs?.let { endsAt ->
        if (action.nowMs >= endsAt) {
            copy(
                remainingMs = 0,
                endsAtMs = null,
            )
        } else {
            copy(remainingMs = endsAt - action.nowMs)
        }
    } ?: this

    TimerAction.Reset -> TimerInput(totalMs)
}

enum class TimerPhase { READY, RUNNING, PAUSED, FINISHED }

/**
 * [progress] from 0 at the start to 1 at zero; [remainingText] "2:08", "1:02:05". While it runs the
 * ring moves at a steady pace towards [nextProgress], where it is due in [untilNextSecondMs].
 */
data class TimerUiState(
    val remainingText: String,
    val progress: Float,
    val nextProgress: Float,
    val untilNextSecondMs: Long,
    val phase: TimerPhase,
    val totalMs: Long,
) {
    companion object {
        fun from(input: TimerInput): TimerUiState = TimerUiState(
            remainingText = clockText(input.remainingMs),
            progress = progress(input.remainingMs, input.totalMs),
            nextProgress = progress(input.remainingMs - input.untilNextSecondMs, input.totalMs),
            untilNextSecondMs = input.untilNextSecondMs,
            phase = when {
                input.running -> TimerPhase.RUNNING
                input.remainingMs == 0L -> TimerPhase.FINISHED
                input.remainingMs == input.totalMs -> TimerPhase.READY
                else -> TimerPhase.PAUSED
            },
            totalMs = input.totalMs,
        )
    }
}

private fun progress(remainingMs: Long, totalMs: Long): Float = 1f - remainingMs.coerceAtLeast(0).toFloat() / totalMs

/** Whole seconds rounded up: "0:01" until the very end, never "0:00" while time is left. */
internal fun clockText(milliseconds: Long): String {
    val seconds = (milliseconds + 999) / 1000
    val hours = seconds / 3600
    val minutes = seconds / 60 % 60
    val rest = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, rest)
    } else {
        "%d:%02d".format(
            minutes,
            rest,
        )
    }
}
