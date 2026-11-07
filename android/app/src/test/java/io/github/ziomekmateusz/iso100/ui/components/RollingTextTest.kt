package io.github.ziomekmateusz.iso100.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RollingTextTest {
    @Test
    fun theUnitAfterTheLastDigitKeepsItsSlotWhenTheNumberChangesLength() {
        val before = rollingSlots("1/125 s")
        val after = rollingSlots("1/50 s")
        assertEquals(before.takeLast(2), after.takeLast(2))
        assertEquals(before.take(4), after.take(4))
    }

    @Test
    fun withoutAUnitTheSlotsCountFromTheStart() {
        assertEquals(rollingSlots("f/8"), rollingSlots("f/11").take(3))
    }
}
