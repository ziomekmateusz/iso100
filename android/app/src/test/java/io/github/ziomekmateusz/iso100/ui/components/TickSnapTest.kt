package io.github.ziomekmateusz.iso100.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TickSnapTest {
    private val spacing = 80f
    private val maxScroll = 800f

    @Test
    fun atRestTheDialSettlesOnTheNearestTick() {
        assertEquals(-30f, snapOffset(scroll = 190f, travel = 0f, spacing, maxScroll), 0.001f)
        assertEquals(30f, snapOffset(scroll = 210f, travel = 0f, spacing, maxScroll), 0.001f)
    }

    @Test
    fun aSmallMovementBackwardsKeepsTheTickUnderTheCentre() {
        assertEquals(1f, snapOffset(scroll = 399f, travel = -5f, spacing, maxScroll), 0.001f)
    }

    @Test
    fun aFlungDialSettlesOnTheTickNearestToWhereItWouldStop() {
        assertEquals(230f, snapOffset(scroll = 170f, travel = 250f, spacing, maxScroll), 0.001f)
        assertEquals(-170f, snapOffset(scroll = 170f, travel = -150f, spacing, maxScroll), 0.001f)
    }

    @Test
    fun theDialNeverSettlesPastItsEnds() {
        assertEquals(0f, snapOffset(scroll = 800f, travel = 500f, spacing, maxScroll), 0.001f)
        assertEquals(0f, snapOffset(scroll = 0f, travel = -500f, spacing, maxScroll), 0.001f)
    }
}
