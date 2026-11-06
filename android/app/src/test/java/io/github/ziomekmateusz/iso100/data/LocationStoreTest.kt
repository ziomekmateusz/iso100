package io.github.ziomekmateusz.iso100.data

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowGeocoder
import java.time.Duration
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class LocationStoreTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(LocationManager::class.java)
    private val store = LocationStore(app, Geocoding(app), CoroutineScope(Dispatchers.Unconfined))

    // the Geocoder would answer on another thread
    @Before
    fun noGeocoder() {
        ShadowGeocoder.setIsPresent(false)
    }

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

    @Test
    fun withoutAnyProviderTheStateSaysSo() {
        located()
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        assertEquals(LocationState.NotFound, resultAfterTheTimeout())
    }

    @Test
    fun withoutAnyPositionTheStateSaysSo() {
        located()
        assertEquals(LocationState.NotFound, resultAfterTheTimeout())
    }

    @Test
    fun anOldPositionIsUsedWhenNoFreshOneArrives() {
        located()
        simulate(ROME_LATITUDE)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(5))
        val found = resultAfterTheTimeout() as LocationState.Found
        assertEquals(ROME_LATITUDE, found.place.latitude, 1e-6)
    }

    @Test
    fun aNewPositionReplacesTheKnownOne() {
        located()
        simulate(ROME_LATITUDE)
        result()
        simulate(OSLO_LATITUDE)
        val found = result() as LocationState.Found
        assertEquals(OSLO_LATITUDE, found.place.latitude, 1e-6)
    }

    @Test
    fun aKnownPositionStaysWhenANewRequestFails() {
        located()
        simulate(ROME_LATITUDE)
        val known = result()
        shadowOf(manager).setLocationEnabled(false)
        assertEquals(known, resultAfterTheTimeout())
    }

    private fun located() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setLocationEnabled(true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
    }

    private fun simulate(latitude: Double) {
        shadowOf(manager).simulateLocation(
            Location(LocationManager.NETWORK_PROVIDER).apply {
                this.latitude = latitude
                longitude = 12.4964
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            },
        )
    }

    private fun resultAfterTheTimeout(): LocationState {
        store.refresh()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(2))
        return store.state.value
    }

    private companion object {
        const val ROME_LATITUDE = 41.9028
        const val OSLO_LATITUDE = 59.9139
    }
}
