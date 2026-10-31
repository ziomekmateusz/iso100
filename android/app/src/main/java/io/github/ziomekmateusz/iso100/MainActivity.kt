package io.github.ziomekmateusz.iso100

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.Iso100App
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.choosePlace
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.effectiveTheme
import io.github.ziomekmateusz.iso100.ui.theme.isDark
import io.github.ziomekmateusz.iso100.ui.theme.minuteTicks
import java.time.Instant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as Iso100Application).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
            val saved by container.savedPlaces.places.collectAsStateWithLifecycle(emptyList())
            val location by container.location.state.collectAsStateWithLifecycle()

            val now by remember { minuteTicks() }.collectAsStateWithLifecycle(Instant.now())
            val place = (choosePlace(settings.placeId, saved, location) as? PlaceChoice.Ready)?.place
            val theme = effectiveTheme(settings, place, now)
            val dark = theme.isDark(isSystemInDarkTheme())
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(bars, bars)
            }
            CompositionLocalProvider(LocalAppContainer provides container) {
                Iso100Theme(theme) {
                    Iso100App()
                }
            }
        }
    }
}
