package io.github.ziomekmateusz.iso100

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.ziomekmateusz.iso100.data.CatalogStore
import io.github.ziomekmateusz.iso100.data.Geocoding
import io.github.ziomekmateusz.iso100.data.LocationStore
import io.github.ziomekmateusz.iso100.data.SavedPlacesSerializer
import io.github.ziomekmateusz.iso100.data.SavedPlacesStore
import io.github.ziomekmateusz.iso100.data.SettingsStore
import io.github.ziomekmateusz.iso100.data.ToolValuesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/** Builds the app-wide stores; composables read it from [LocalAppContainer]. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsStore(
        PreferenceDataStoreFactory.create(ReplaceFileCorruptionHandler { emptyPreferences() }) {
            app.preferencesDataStoreFile("settings")
        },
    )

    val catalog = CatalogStore(
        read = { withContext(Dispatchers.IO) { app.assets.open("catalog.json").bufferedReader().use { it.readText() } } },
        settings = settings.settings,
        scope = scope,
    )

    val geocoding = Geocoding(app)

    val location = LocationStore(app, geocoding, scope)

    val toolValues = ToolValuesStore(
        PreferenceDataStoreFactory.create(ReplaceFileCorruptionHandler { emptyPreferences() }) {
            app.preferencesDataStoreFile("tool_values")
        },
        scope,
    )

    val savedPlaces = SavedPlacesStore(
        DataStoreFactory.create(SavedPlacesSerializer, ReplaceFileCorruptionHandler { emptyList() }) {
            app.dataStoreFile("saved_places.json")
        },
    )
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
