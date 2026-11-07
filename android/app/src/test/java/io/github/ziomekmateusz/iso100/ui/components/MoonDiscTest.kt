package io.github.ziomekmateusz.iso100.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MoonDiscTest {
    @Test
    fun theCycleGoesFromNewMoonThroughFullMoonBackToNewMoon() {
        assertEquals(0f, moonCycle(0.0, waxing = true), 1e-4f)
        assertEquals(0.25f, moonCycle(0.5, waxing = true), 1e-4f)
        assertEquals(0.5f, moonCycle(1.0, waxing = true), 1e-4f)
        assertEquals(0.75f, moonCycle(0.5, waxing = false), 1e-4f)
    }

    @Test
    fun theDiscIsDrawnBackFromTheCycle() {
        assertEquals(0.3f, litFraction(moonCycle(0.3, waxing = false)), 1e-4f)
        assertEquals(0.5f, litFraction(2.25f), 1e-4f)
    }

    @Test
    fun theDiscTurnsTheShortWayAroundTheNewMoon() {
        assertEquals(1.02f, nearestTurn(from = 0.98f, to = 0.02f), 1e-4f)
        assertEquals(-0.02f, nearestTurn(from = 0.02f, to = 0.98f), 1e-4f)
        assertEquals(2.25f, nearestTurn(from = 2.3f, to = 0.25f), 1e-4f)
    }
}
