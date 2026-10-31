package io.github.ziomekmateusz.iso100.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepsTest {
    @Test
    fun `decimals are parsed with a comma or a dot, anything else is an error`() {
        assertEquals(2.5, parseDecimal("2,5")!!, 0.0)
        assertEquals(2.5, parseDecimal("2.5")!!, 0.0)
        assertEquals(2.0, parseDecimal(" 2 ")!!, 0.0)
        assertEquals(0.5, parseDecimal(",5")!!, 0.0)
        assertEquals(3.0, parseDecimal("3.")!!, 0.0)
        assertNull(parseDecimal("2.5.0"))
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("-1"))
    }

    @Test
    fun theButtonsStepThroughTheLadderAndBeyond() {
        assertEquals(28.0, stepUp(24.0, FOCAL_LENGTHS), 0.0)
        assertEquals(20.0, stepDown(24.0, FOCAL_LENGTHS), 0.0)
        assertEquals(28.0, stepUp(25.0, FOCAL_LENGTHS), 0.0)
        assertEquals(24.0, stepDown(25.0, FOCAL_LENGTHS), 0.0)
        assertEquals(1600.0, stepUp(800.0, FOCAL_LENGTHS), 0.0)
        assertEquals(4.0, stepDown(8.0, FOCAL_LENGTHS), 0.0)
    }
}
