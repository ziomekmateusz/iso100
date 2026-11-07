package io.github.ziomekmateusz.iso100.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/** The values of one tool, saved under [key] as JSON; [default] when nothing readable was saved. */
class ToolValues<T>(val key: String, val serializer: KSerializer<T>, val default: T)

/**
 * The last values of each tool, so that a tool opens where the user left it. Values only: sheets and
 * dates start over. Read once at launch, then [saved] answers at once, without suspending; [save] keeps
 * the value in memory and writes it a moment later, once the user stops changing it.
 * Called from the main thread.
 */
class ToolValuesStore(private val dataStore: DataStore<Preferences>, private val scope: CoroutineScope) {
    private val stored = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .stateIn(scope, SharingStarted.Eagerly, null)
    private val latest = mutableMapOf<String, String>()
    private val writes = mutableMapOf<String, Job>()

    /** False until the values on disk have been read: before that, [saved] gives the defaults. */
    val isLoaded: Boolean get() = stored.value != null

    suspend fun awaitLoaded() {
        stored.first { it != null }
    }

    fun <T> saved(tool: ToolValues<T>): T {
        val text = latest[tool.key] ?: stored.value?.get(stringPreferencesKey(tool.key))
        ?: return tool.default
        // SerializationException is an IllegalArgumentException: values from an older or broken save
        return try {
            json.decodeFromString(tool.serializer, text)
        } catch (_: IllegalArgumentException) {
            tool.default
        }
    }

    /** Returns the pending write, which a newer value of the same tool replaces. */
    fun <T> save(tool: ToolValues<T>, value: T): Job {
        val text = json.encodeToString(tool.serializer, value)
        latest[tool.key] = text
        writes[tool.key]?.cancel()
        return scope.launch {
            delay(WRITE_DELAY_MS.milliseconds)
            dataStore.edit { it[stringPreferencesKey(tool.key)] = text }
        }.also { writes[tool.key] = it }
    }

    private companion object {
        /** A dial drag changes the value many times a second: only where it stops is written. */
        const val WRITE_DELAY_MS = 500L
        val json = Json { ignoreUnknownKeys = true }
    }
}
