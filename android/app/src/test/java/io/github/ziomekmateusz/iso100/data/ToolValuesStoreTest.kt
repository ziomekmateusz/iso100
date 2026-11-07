package io.github.ziomekmateusz.iso100.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ToolValuesStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Serializable
    private data class LensValues(val focalLengthMm: Double = 50.0, val aperture: Double = 8.0)

    private val lens = ToolValues("lens", LensValues.serializer(), LensValues())

    private fun dataStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) {
            File(
                folder.root,
                "tool_values.preferences_pb"
            )
        }

    @Test
    fun aToolNeverChangedOpensWithItsDefaults() = runTest {
        val store = ToolValuesStore(dataStore(backgroundScope), backgroundScope)
        store.awaitLoaded()
        assertEquals(LensValues(), store.saved(lens))
    }

    @Test
    fun aChangeIsReadBackAtOnceBeforeItReachesTheDisk() = runTest {
        val store = ToolValuesStore(dataStore(backgroundScope), backgroundScope)
        store.awaitLoaded()
        store.save(lens, LensValues(focalLengthMm = 85.0))
        assertEquals(LensValues(focalLengthMm = 85.0), store.saved(lens))
    }

    @Test
    fun theLastValuesSurviveARestart() = runTest {
        val firstRun = CoroutineScope(Dispatchers.IO + Job())
        ToolValuesStore(dataStore(firstRun), backgroundScope).apply {
            awaitLoaded()
            save(lens, LensValues(focalLengthMm = 24.0))
            save(lens, LensValues(focalLengthMm = 35.0, aperture = 2.8)).join()
        }
        firstRun.coroutineContext.job.cancelAndJoin()

        val store = ToolValuesStore(dataStore(backgroundScope), backgroundScope)
        store.awaitLoaded()
        assertEquals(LensValues(focalLengthMm = 35.0, aperture = 2.8), store.saved(lens))
    }

    @Test
    fun valuesThatNoLongerDecodeFallBackToTheDefaults() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("lens")] = """{"focalLengthMm":"wide"}""" }
        val store = ToolValuesStore(dataStore, backgroundScope)
        store.awaitLoaded()
        assertEquals(LensValues(), store.saved(lens))
    }

    @Test
    fun aFieldAddedOrRemovedLaterKeepsTheOtherValues() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit {
            it[stringPreferencesKey("lens")] = """{"focalLengthMm":135.0,"filter":"ND8"}"""
        }
        val store = ToolValuesStore(dataStore, backgroundScope)
        store.awaitLoaded()
        assertEquals(LensValues(focalLengthMm = 135.0), store.saved(lens))
    }
}
