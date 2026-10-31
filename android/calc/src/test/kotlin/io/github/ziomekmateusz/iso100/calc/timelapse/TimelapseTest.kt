package io.github.ziomekmateusz.iso100.calc.timelapse

import io.github.ziomekmateusz.iso100.calc.Input
import io.github.ziomekmateusz.iso100.calc.Outcome
import io.github.ziomekmateusz.iso100.calc.assertClose
import io.github.ziomekmateusz.iso100.calc.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimelapseTest {
    @Test
    fun `the video length comes from the interval and the shooting time`() {
        val t = Timelapse.solve(TimelapseInput.FindClip(intervalS = 5.0, recordingS = 3600.0, fps = 25.0, photoSizeMb = 25.0)).ok()
        assertClose(28.8, t.clipS, "video")
        assertEquals(720, t.photos)
        assertClose(18000.0, t.storageMb!!, "card")
    }

    @Test
    fun `the interval comes from the video length and the shooting time`() {
        val t = Timelapse.solve(TimelapseInput.FindInterval(clipS = 30.0, recordingS = 3600.0, fps = 25.0)).ok()
        assertClose(4.8, t.intervalS, "interval")
        assertEquals(750, t.photos)
    }

    @Test
    fun `the shooting time comes from the video length and the interval`() {
        val t = Timelapse.solve(TimelapseInput.FindRecording(clipS = 30.0, intervalS = 5.0, fps = 25.0, photoSizeMb = 50.0)).ok()
        assertClose(3750.0, t.recordingS, "shooting")
        assertEquals(750, t.photos)
        assertClose(37500.0, t.storageMb!!, "card")
    }

    @Test
    fun `photos are rounded, not truncated`() {
        val t = Timelapse.solve(TimelapseInput.FindClip(intervalS = 8.88, recordingS = 37 * 60.0, fps = 25.0)).ok()
        assertEquals(250, t.photos)
        assertEquals(null, t.storageMb)
    }

    @Test
    fun `zero or negative inputs are errors, not infinity or NaN`() {
        assertEquals(Outcome.Invalid(Input.INTERVAL), Timelapse.solve(TimelapseInput.FindClip(0.0, 3600.0, 25.0)))
        assertEquals(Outcome.Invalid(Input.RECORDING_TIME), Timelapse.solve(TimelapseInput.FindClip(5.0, -1.0, 25.0)))
        assertEquals(Outcome.Invalid(Input.CLIP_LENGTH), Timelapse.solve(TimelapseInput.FindInterval(0.0, 0.0, 25.0)))
        assertEquals(Outcome.Invalid(Input.CLIP_LENGTH), Timelapse.solve(TimelapseInput.FindRecording(0.0, 5.0, 25.0)))
        assertEquals(Outcome.Invalid(Input.FPS), Timelapse.solve(TimelapseInput.FindClip(5.0, 3600.0, 0.0)))
        assertEquals(Outcome.Invalid(Input.PHOTO_SIZE), Timelapse.solve(TimelapseInput.FindClip(5.0, 3600.0, 25.0, photoSizeMb = 0.0)))
    }
}
