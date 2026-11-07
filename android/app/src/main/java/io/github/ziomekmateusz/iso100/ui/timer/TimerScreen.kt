package io.github.ziomekmateusz.iso100.ui.timer

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.components.ControlSheet
import io.github.ziomekmateusz.iso100.ui.components.PrimaryButton
import io.github.ziomekmateusz.iso100.ui.components.RollingText
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlinx.coroutines.delay
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

/** A countdown for a bulb exposure of [seconds]; it runs only while the screen is open. */
@Composable
fun TimerScreen(seconds: Double, settings: String, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf(TimerInput(totalMs = (seconds * 1000).roundToLong())) }
    val context = LocalContext.current
    fun now() = SystemClock.elapsedRealtime()
    LaunchedEffect(input.endsAtMs) {
        if (!input.running) return@LaunchedEffect
        while (true) {
            input = input.reduce(TimerAction.Tick(now()))
            if (!input.running) break
            delay(input.untilNextSecondMs.milliseconds)
        }
        vibrateEnd(context)
    }
    TimerContent(
        TimerUiState.from(input),
        settings,
        onStart = { input = input.reduce(TimerAction.Start(now())) },
        onPause = { input = input.reduce(TimerAction.Pause(now())) },
        onReset = { input = input.reduce(TimerAction.Reset) },
        onBack = onBack,
    )
}

@Composable
fun TimerContent(
    state: TimerUiState,
    settings: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    ToolScaffold(
        title = stringResource(R.string.title_timer),
        onBack = onBack,
        modifier = if (state.phase == TimerPhase.RUNNING) Modifier.keepScreenOn() else Modifier,
        footer = { padding ->
            ControlSheet(padding) {
                TimerButtons(
                    state.phase,
                    onStart,
                    onPause,
                    onReset
                )
            }
        },
    ) {
        TimerRing(state)
        Text(
            settings,
            Modifier.fillMaxWidth(),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The time left inside a ring that fills as the exposure goes on. */
@Composable
private fun TimerRing(state: TimerUiState) {
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = if (state.phase == TimerPhase.RUNNING) state.nextProgress else state.progress,
        animationSpec = if (state.phase == TimerPhase.RUNNING) tween(
            state.untilNextSecondMs.toInt(),
            easing = LinearEasing
        ) else snap(),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(
                colors.outlineVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                colors.primary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(
                state.remainingText,
                MaterialTheme.typography.displayLarge,
                color = colors.onSurface
            )
            Text(
                stringResource(R.string.timer_of, clockText(state.totalMs)),
                fontSize = 17.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** Reset beside the main button, which starts, pauses, resumes or starts again. */
@Composable
private fun TimerButtons(
    phase: TimerPhase,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onReset,
            enabled = phase != TimerPhase.READY,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                stringResource(R.string.timer_reset),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        val (label, onClick) = when (phase) {
            TimerPhase.READY -> R.string.timer_start to onStart
            TimerPhase.RUNNING -> R.string.timer_pause to onPause
            TimerPhase.PAUSED -> R.string.timer_resume to onStart
            TimerPhase.FINISHED -> R.string.timer_again to onStart
        }
        PrimaryButton(stringResource(label), onClick, Modifier.weight(1f))
    }
}

/** Three long pulses: the exposure is over, close the shutter. */
private fun vibrateEnd(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    val pulses = VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 500), -1)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(
            pulses,
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(
            pulses,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        )
    }
}

@Preview
@Composable
private fun TimerPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) {
        TimerContent(
            TimerUiState.from(
                TimerInput(
                    totalMs = 128_000,
                    remainingMs = 64_000
                )
            ), "ISO 100 · f/8 · ND1000", {}, {}, {}, {})
    }
}

@Preview(fontScale = 2f)
@Composable
private fun TimerLargeTextPreview() {
    Iso100Theme {
        TimerContent(
            TimerUiState.from(TimerInput(totalMs = 3_725_000)),
            "ISO 100 · f/8 · ND1000",
            {},
            {},
            {},
            {})
    }
}
