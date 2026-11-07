package io.github.ziomekmateusz.iso100.ui.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {
    private val ready = TimerInput(totalMs = 128_000)

    @Test
    fun aNewTimerWaitsWithTheWholeExposure() {
        val state = TimerUiState.from(ready)
        assertEquals("2:08", state.remainingText)
        assertEquals(0f, state.progress, 0f)
        assertEquals(TimerPhase.READY, state.phase)
    }

    @Test
    fun theRemainingTimeComesFromTheEndNotFromCountingTicks() {
        val running = ready.reduce(TimerAction.Start(nowMs = 1_000))
        val later = running.reduce(TimerAction.Tick(nowMs = 65_000))
        assertEquals(64_000, later.remainingMs)
        assertEquals("1:04", TimerUiState.from(later).remainingText)
        assertEquals(0.5f, TimerUiState.from(later).progress, 0.001f)
    }

    @Test
    fun aPartSecondStillLeftShowsTheWholeSecond() {
        val running =
            ready.reduce(TimerAction.Start(nowMs = 0)).reduce(TimerAction.Tick(nowMs = 127_500))
        assertEquals("0:01", TimerUiState.from(running).remainingText)
    }

    @Test
    fun aPauseKeepsTheTimeLeftAndStartGoesOnFromThere() {
        val paused =
            ready.reduce(TimerAction.Start(nowMs = 0)).reduce(TimerAction.Pause(nowMs = 28_000))
        assertEquals(100_000, paused.remainingMs)
        assertEquals(TimerPhase.PAUSED, TimerUiState.from(paused).phase)
        val resumed = paused.reduce(TimerAction.Start(nowMs = 500_000))
            .reduce(TimerAction.Tick(nowMs = 510_000))
        assertEquals(90_000, resumed.remainingMs)
    }

    @Test
    fun atTheEndItStopsAtZeroAndStartBeginsAgain() {
        val finished =
            ready.reduce(TimerAction.Start(nowMs = 0)).reduce(TimerAction.Tick(nowMs = 200_000))
        assertEquals(0, finished.remainingMs)
        assertFalse(finished.running)
        assertEquals(TimerPhase.FINISHED, TimerUiState.from(finished).phase)
        val again = finished.reduce(TimerAction.Start(nowMs = 300_000))
        assertTrue(again.running)
        assertEquals(128_000, again.reduce(TimerAction.Tick(nowMs = 300_000)).remainingMs)
    }

    @Test
    fun resetGoesBackToTheWholeExposureAndWaits() {
        val reset =
            ready.reduce(TimerAction.Start(nowMs = 0)).reduce(TimerAction.Tick(nowMs = 30_000))
                .reduce(TimerAction.Reset)
        assertEquals(ready, reset)
    }

    @Test
    fun theNextTickComesWhenTheShownSecondChanges() {
        assertEquals(1_000, ready.untilNextSecondMs)
        assertEquals(500, ready.copy(remainingMs = 127_500).untilNextSecondMs)
        assertEquals(1, ready.copy(remainingMs = 1).untilNextSecondMs)
    }

    @Test
    fun theRingHeadsForWhereItWillBeAtTheNextTick() {
        val running =
            ready.reduce(TimerAction.Start(nowMs = 0)).reduce(TimerAction.Tick(nowMs = 500))
        assertEquals(1f / 128, TimerUiState.from(running).nextProgress, 0.0001f)
        val lastSecond = running.reduce(TimerAction.Tick(nowMs = 127_600))
        assertEquals(1f, TimerUiState.from(lastSecond).nextProgress, 0f)
    }

    @Test
    fun longExposuresShowTheHours() {
        assertEquals("1:02:05", TimerUiState.from(TimerInput(totalMs = 3_725_000)).remainingText)
    }
}
