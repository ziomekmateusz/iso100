package io.github.ziomekmateusz.iso100.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import io.github.ziomekmateusz.iso100.calc.astro.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.ZoneId
import kotlin.coroutines.resume

sealed interface LocationState {
    /** Not asked yet. */
    data object Unknown : LocationState

    data object Locating : LocationState

    data object PermissionDenied : LocationState

    data object LocationOff : LocationState

    /** Asked, but no position arrived. */
    data object NotFound : LocationState

    /** [name] is null when there is no Geocoder or it does not answer: the screens show the coordinates. */
    data class Found(val place: Place, val name: String?) : LocationState
}

/** The approximate position of the phone, in the time zone of the phone. */
class LocationStore(private val context: Context, private val geocoding: Geocoding, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<LocationState>(LocationState.Unknown)
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private var refreshing: Job? = null

    /** Asks for the position again, unless a request is running; a known position stays if no new one arrives. */
    fun refresh() {
        if (refreshing?.isActive == true) return
        if (_state.value !is LocationState.Found) _state.value = LocationState.Locating
        refreshing = scope.launch {
            val located = locate()
            if (located is LocationState.Found || _state.value !is LocationState.Found) _state.value = located
        }
    }

    private suspend fun locate(): LocationState {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (granted != PackageManager.PERMISSION_GRANTED) return LocationState.PermissionDenied
        val manager = context.getSystemService(LocationManager::class.java)
        if (!LocationManagerCompat.isLocationEnabled(manager)) return LocationState.LocationOff
        val providers = providers(manager)
        val location = providers.firstOrNull()?.let { currentLocation(manager, it) }
            ?: lastKnownLocation(manager, providers)
            ?: return LocationState.NotFound
        val place = Place(location.latitude, location.longitude, ZoneId.systemDefault())
        return LocationState.Found(place, geocoding.name(location.latitude, location.longitude))
    }

    @SuppressLint("InlinedApi")
    private fun providers(manager: LocationManager): List<String> = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { LocationManagerCompat.hasProvider(manager, it) && manager.isProviderEnabled(it) }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(manager: LocationManager, providers: List<String>): Location? =
        providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.elapsedRealtimeNanos }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancel = CancellationSignal()
            continuation.invokeOnCancellation { cancel.cancel() }
            LocationManagerCompat.getCurrentLocation(manager, provider, cancel, ContextCompat.getMainExecutor(context)) {
                continuation.resume(it)
            }
        }
}

/** A place found by name. */
data class PlaceCandidate(val name: String, val latitude: Double, val longitude: Double)

/**
 * Place names from the system Geocoder, which is missing on phones without Google services.
 * The blocking calls are deprecated from API 33 but work on every version, so one path is enough.
 */
@Suppress("DEPRECATION")
class Geocoding(private val context: Context) {

    /** False on phones without the Geocoder: no place names and no search by name. */
    val available: Boolean get() = Geocoder.isPresent()

    /** The town, else the region, else the country at the coordinates. */
    suspend fun name(latitude: Double, longitude: Double): String? =
        addresses { it.getFromLocation(latitude, longitude, 1) }.firstOrNull()?.label()

    suspend fun search(query: String): List<PlaceCandidate> = addresses { it.getFromLocationName(query, 5) }
        .mapNotNull { address -> address.label()?.let { PlaceCandidate(it, address.latitude, address.longitude) } }

    private fun Address.label(): String? = locality ?: subAdminArea ?: adminArea ?: countryName

    private suspend fun addresses(query: (Geocoder) -> List<Address>?): List<Address> {
        if (!Geocoder.isPresent()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                query(Geocoder(context)).orEmpty()
            } catch (e: IOException) {
                // offline or service down: no name, the screens show the coordinates
                emptyList()
            }
        }
    }
}
