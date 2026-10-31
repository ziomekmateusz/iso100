package io.github.ziomekmateusz.iso100.data

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class LocationStoreTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(LocationManager::class.java)
    private val store = LocationStore(app, Geocoding(app), CoroutineScope(Dispatchers.Unconfined))

    private fun result(): LocationState = runBlocking {
        withTimeout(5_000) {
            store.refresh()
            shadowOf(Looper.getMainLooper()).idle()
            store.state.first { it != LocationState.Locating }
        }
    }

    @Test
    fun withoutPermissionTheStateSaysSo() {
        assertEquals(LocationState.PermissionDenied, result())
    }

    @Test
    fun withLocationOffTheStateSaysSo() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setLocationEnabled(false)
        assertEquals(LocationState.LocationOff, result())
    }

    @Test
    fun aPositionIsFoundInThePhoneTimeZone() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setLocationEnabled(true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).simulateLocation(
            Location(LocationManager.NETWORK_PROVIDER).apply {
                latitude = 41.9028
                longitude = 12.4964
                time = System.currentTimeMillis()
            },
        )
        val found = result() as LocationState.Found
        assertEquals(41.9028, found.place.latitude, 1e-6)
        assertEquals(ZoneId.systemDefault(), found.place.zone)
    }
}
