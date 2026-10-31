package io.github.ziomekmateusz.iso100.ui.theme

import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.calc.astro.isCivilNight
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/**
 * The theme on screen: the chosen one, or red night when the automatic red night is on and at [place]
 * the civil twilight is over. Without a place there is no night to know of: the chosen theme stays.
 */
fun effectiveTheme(settings: Settings, place: Place?, now: Instant): AppTheme =
    if (settings.autoRedNight && place != null && isCivilNight(now.atZone(place.zone), place)) AppTheme.RED_NIGHT else settings.theme

/** The time at every new minute, for the theme to follow the end of the twilight. */
fun minuteTicks(): Flow<Instant> = flow {
    while (true) {
        val now = Instant.now()
        emit(now)
        delay(60_000 - now.toEpochMilli() % 60_000)
    }
}
