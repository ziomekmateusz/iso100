package io.github.ziomekmateusz.iso100.ui.place

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PlacePickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theCurrentPositionIsAskedAgainEvenWhenKnown() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val manager = app.getSystemService(LocationManager::class.java)
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setLocationEnabled(true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).simulateLocation(
            Location(LocationManager.NETWORK_PROVIDER).apply {
                latitude = 41.9028
                longitude = 12.4964
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            },
        )
        val container = AppContainer(app)
        container.location.refresh()
        compose.waitUntil {
            shadowOf(Looper.getMainLooper()).idle()
            container.location.state.value is LocationState.Found
        }

        var located = false
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                Iso100Theme { PlacePickerSheet(onDismiss = {}, onLocate = { located = true }) }
            }
        }
        compose.onNodeWithText("Current position").performClick()
        assertTrue(located)
    }
}
