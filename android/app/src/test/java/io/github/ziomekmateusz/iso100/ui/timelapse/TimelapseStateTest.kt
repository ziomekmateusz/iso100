package io.github.ziomekmateusz.iso100.ui.timelapse

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.ui.components.parseDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelapseStateTest {
    @Test
    fun theBoardExample() {
        val result = TimelapseUiState.from(TimelapseInput()).result!!
        assertEquals(28.8, result.clipS, 1e-9)
        assertEquals(720, result.photos)
        assertEquals(18000.0, result.storageMb!!, 1e-9)
    }

    @Test
    fun theTargetChoosesWhatIsCalculated() {
        assertEquals(4.8, TimelapseUiState.from(TimelapseInput(solve = TimelapseTarget.INTERVAL)).result!!.intervalS, 1e-9)
        assertEquals(3750.0, TimelapseUiState.from(TimelapseInput(solve = TimelapseTarget.RECORDING)).result!!.recordingS, 1e-9)
    }

    @Test
    fun `a zero interval is a message, not infinity`() {
        val state = TimelapseUiState.from(TimelapseInput(intervalS = 0.0))
        assertNull(state.result)
        assertEquals(Outcome.Invalid(Input.INTERVAL), state.invalid)
    }

    @Test
    fun withoutAPhotoSizeThereIsNoStorage() {
        assertNull(TimelapseUiState.from(TimelapseInput(photoSizeMb = null)).result!!.storageMb)
    }

    @Test
    fun theActionsChangeTheInput() {
        val input = TimelapseInput()
        assertEquals(TimelapseTarget.RECORDING, input.reduce(TimelapseAction.Solve(TimelapseTarget.RECORDING)).solve)
        assertEquals(8.0, input.reduce(TimelapseAction.StepInterval(up = true)).intervalS, 0.0)
        assertEquals(45.0, input.reduce(TimelapseAction.StepClip(up = true)).clipS, 0.0)
        assertEquals(30, input.reduce(TimelapseAction.SetFps(30)).fps)
        assertNull(input.reduce(TimelapseAction.SetPhotoSize(null)).photoSizeMb)
        assertEquals(5400.0, input.reduce(TimelapseAction.SetRecording(5400.0)).recordingS, 0.0)
    }

    @Test
    fun theDurationIsWholeHoursAndMinutes() {
        assertEquals(5400.0, parseDuration("1", "30")!!, 0.0)
        assertEquals(600.0, parseDuration("0", "10")!!, 0.0)
        assertNull(parseDuration("1,5", "0"))
        assertNull(parseDuration("", "10"))
    }
}
