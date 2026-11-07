package io.github.ziomekmateusz.iso100

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.Iso100App
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.choosePlace
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.effectiveTheme
import io.github.ziomekmateusz.iso100.ui.theme.isDark
import io.github.ziomekmateusz.iso100.ui.theme.minuteTicks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val container = (application as Iso100Application).container
        val settings =
            container.settings.settings.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
        installSplashScreen().setKeepOnScreenCondition { settings.value == null || !container.toolValues.isLoaded }
        super.onCreate(savedInstanceState)
        setContent {
            val current by settings.collectAsStateWithLifecycle()
            current?.let { AppContent(container, it) }
        }
    }

    @Composable
    private fun AppContent(container: AppContainer, settings: Settings) {
        val saved by container.savedPlaces.places.collectAsStateWithLifecycle(emptyList())
        val location by container.location.state.collectAsStateWithLifecycle()

        val now by remember { minuteTicks() }.collectAsStateWithLifecycle(Instant.now())
        val place = (choosePlace(settings.placeId, saved, location) as? PlaceChoice.Ready)?.place
        val theme = effectiveTheme(settings, place, now)
        val dark = theme.isDark(isSystemInDarkTheme())
        LaunchedEffect(dark) {
            val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
            enableEdgeToEdge(bars, bars)
        }
        CompositionLocalProvider(LocalAppContainer provides container) {
            Iso100Theme(theme) {
                Iso100App(if (settings.keepScreenOn) Modifier.keepScreenOn() else Modifier)
            }
        }
    }
}
