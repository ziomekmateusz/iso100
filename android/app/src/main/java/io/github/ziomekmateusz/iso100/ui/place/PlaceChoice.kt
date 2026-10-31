package io.github.ziomekmateusz.iso100.ui.place

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.data.SavedPlace
import io.github.ziomekmateusz.iso100.data.Settings
import kotlinx.coroutines.flow.first

/** Where Sun and Moon are calculated: a saved place, the current position, or why there is none yet. */
sealed interface PlaceChoice {
    /** [name] is null without a Geocoder: show the coordinates. */
    data class Ready(val place: Place, val name: String?, val current: Boolean) : PlaceChoice

    /** The current position is chosen but not available: [location] says why. */
    data class Unavailable(val location: LocationState) : PlaceChoice
}

/** The saved place of the settings, if it still exists; otherwise the current position. */
fun choosePlace(placeId: String?, saved: List<SavedPlace>, location: LocationState): PlaceChoice {
    saved.firstOrNull { it.id == placeId }?.let { return PlaceChoice.Ready(it.place, it.name, current = false) }
    return if (location is LocationState.Found) PlaceChoice.Ready(location.place, location.name, current = true) else PlaceChoice.Unavailable(location)
}

/** The place of the screen and how to ask for the position: the permission first, then [LocationStore.refresh]. */
class PlaceHandle(val choice: PlaceChoice, val locate: () -> Unit)

/**
 * The place for Sun, Moon and Home. With the automatic position on (the default), the position is
 * asked when the screen opens, once per run of the app.
 */
@Composable
fun rememberPlace(app: AppContainer): PlaceHandle {
    val context = LocalContext.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val saved by app.savedPlaces.places.collectAsStateWithLifecycle(emptyList())
    val location by app.location.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { app.location.refresh() }
    val locate = remember(app) {
        {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (granted) app.location.refresh() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    LaunchedEffect(app) {
        val stored = app.settings.settings.first()
        if (stored.autoLocation && stored.placeId == null && app.location.state.value == LocationState.Unknown) locate()
    }
    val choice = remember(settings.placeId, saved, location) { choosePlace(settings.placeId, saved, location) }
    return PlaceHandle(choice, locate)
}
