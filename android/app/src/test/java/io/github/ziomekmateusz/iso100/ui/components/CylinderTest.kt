package io.github.ziomekmateusz.iso100.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class CylinderTest {
    @Test
    fun theTickInTheCentreStaysWhereItIsAndFullyVisible() {
        val centre = onCylinder(0f, radius = 100f)
        assertEquals(0f, centre?.x ?: Float.NaN, 0.001f)
        assertEquals(1f, centre?.depth ?: Float.NaN, 0.001f)
    }

    @Test
    fun ticksTowardTheEdgesGetCloserAndFainterOnBothSides() {
        val right = onCylinder(60f, radius = 100f)
        val left = onCylinder(-60f, radius = 100f)
        assertTrue((right?.x ?: 999f) < 60f)
        assertTrue((right?.depth ?: 1f) < 1f)
        assertEquals(-(right?.x ?: 0f), left?.x ?: Float.NaN, 0.001f)
    }

    @Test
    fun aTickPastTheEdgeOfTheCylinderIsHidden() {
        assertNull(onCylinder((100 * PI / 2).toFloat() + 1f, radius = 100f))
    }

    @Test
    fun theValueSplitsAfterTheSlashSoTheSlashStaysInTheCentre() {
        assertEquals("f/" to "8", splitAtSlash("f/8"))
        assertEquals("1/" to "125 s", splitAtSlash("1/125 s"))
        assertEquals("" to "100", splitAtSlash("100"))
    }
}
