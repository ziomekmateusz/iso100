package io.github.ziomekmateusz.iso100.ui.theme

import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.sun.ROME
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AutoRedNightTest {
    // in Rome, 21 June: 22:00 is after the civil twilight, 12:00 is day
    private val night = Instant.parse("2026-06-21T20:00:00Z")
    private val day = Instant.parse("2026-06-21T10:00:00Z")
    private val on = Settings(theme = AppTheme.LIGHT, autoRedNight = true)

    @Test
    fun `off by default, the chosen theme stays at night too`() {
        assertEquals(AppTheme.LIGHT, effectiveTheme(Settings(theme = AppTheme.LIGHT), ROME.place, night))
    }

    @Test
    fun `when on, red night after the civil twilight and the chosen theme by day`() {
        assertEquals(AppTheme.RED_NIGHT, effectiveTheme(on, ROME.place, night))
        assertEquals(AppTheme.LIGHT, effectiveTheme(on, ROME.place, day))
    }

    @Test
    fun `without a place the chosen theme stays`() {
        assertEquals(AppTheme.LIGHT, effectiveTheme(on, null, night))
    }
}
