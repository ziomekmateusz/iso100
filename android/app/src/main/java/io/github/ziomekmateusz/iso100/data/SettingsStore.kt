package io.github.ziomekmateusz.iso100.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class AppTheme { SYSTEM, DARK, LIGHT, RED_NIGHT }

/**
 * What the user chose. [cameraId] and [formatId] exclude each other: a camera from the catalog,
 * or only a sensor format. [placeId] is a saved place for Sun and Moon, null for the current position.
 * The language is not here: AppCompat keeps it.
 */
data class Settings(
    val units: UnitSystem = UnitSystem.METRIC,
    val stopStep: StopStep = StopStep.THIRD,
    val cameraId: String? = null,
    val formatId: String? = null,
    val autoLocation: Boolean = true,
    val theme: AppTheme = AppTheme.DARK,
    val placeId: String? = null,
    /** Red night by itself after the civil twilight, at the place of Sun and Moon. */
    val autoRedNight: Boolean = false,
)

class SettingsStore(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            Settings(
                units = p[UNITS].toEnum(UnitSystem.METRIC),
                stopStep = p[STOP_STEP].toEnum(StopStep.THIRD),
                cameraId = p[CAMERA],
                formatId = p[FORMAT],
                autoLocation = p[AUTO_LOCATION] ?: true,
                theme = p[THEME].toEnum(AppTheme.DARK),
                placeId = p[PLACE],
                autoRedNight = p[AUTO_RED_NIGHT] ?: false,
            )
        }

    suspend fun setUnits(units: UnitSystem) = edit { it[UNITS] = units.name }

    suspend fun setStopStep(step: StopStep) = edit { it[STOP_STEP] = step.name }

    /** A camera of the catalog; the format comes with it. */
    suspend fun setCamera(id: String) = edit {
        it[CAMERA] = id
        it.remove(FORMAT)
    }

    /** Only a sensor format, for a camera missing from the catalog. */
    suspend fun setFormat(id: String) = edit {
        it[FORMAT] = id
        it.remove(CAMERA)
    }

    suspend fun setAutoLocation(enabled: Boolean) = edit { it[AUTO_LOCATION] = enabled }

    suspend fun setTheme(theme: AppTheme) = edit { it[THEME] = theme.name }

    suspend fun setAutoRedNight(enabled: Boolean) = edit { it[AUTO_RED_NIGHT] = enabled }

    /** A saved place, or null for the current position. */
    suspend fun setPlace(id: String?) = edit { if (id == null) it.remove(PLACE) else it[PLACE] = id }

    private suspend fun edit(change: (MutablePreferences) -> Unit) {
        dataStore.edit(change)
    }

    private companion object {
        val UNITS = stringPreferencesKey("units")
        val STOP_STEP = stringPreferencesKey("stop_step")
        val CAMERA = stringPreferencesKey("camera")
        val FORMAT = stringPreferencesKey("format")
        val AUTO_LOCATION = booleanPreferencesKey("auto_location")
        val THEME = stringPreferencesKey("theme")
        val PLACE = stringPreferencesKey("place")
        val AUTO_RED_NIGHT = booleanPreferencesKey("auto_red_night")
    }
}

/** A stored name back to its enum; a name that no longer exists falls back to [default]. */
private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E = enumValues<E>().firstOrNull { it.name == this } ?: default
