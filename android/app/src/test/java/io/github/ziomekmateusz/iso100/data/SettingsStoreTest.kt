package io.github.ziomekmateusz.iso100.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun dataStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") }

    @Test
    fun defaultsBeforeAnyChange() = runTest {
        assertEquals(Settings(), SettingsStore(dataStore(backgroundScope)).settings.first())
    }

    @Test
    fun cameraAndFormatExcludeEachOther() = runTest {
        val store = SettingsStore(dataStore(backgroundScope))
        store.setFormat("aps-c")
        store.setCamera("sony-alpha-7-iii")
        assertEquals(Settings(cameraId = "sony-alpha-7-iii"), store.settings.first())
        store.setFormat("four-thirds")
        assertEquals(Settings(formatId = "four-thirds"), store.settings.first())
    }

    @Test
    fun `the automatic red night is off until it is turned on`() = runTest {
        val store = SettingsStore(dataStore(backgroundScope))
        assertEquals(false, store.settings.first().autoRedNight)
        store.setAutoRedNight(true)
        assertEquals(true, store.settings.first().autoRedNight)
    }

    @Test
    fun aSavedPlaceIsChosenAndNullGoesBackToTheCurrentPosition() = runTest {
        val store = SettingsStore(dataStore(backgroundScope))
        store.setPlace("rome")
        assertEquals("rome", store.settings.first().placeId)
        store.setPlace(null)
        assertEquals(Settings(), store.settings.first())
    }

    @Test
    fun theScreenTurnsOffAsUsualUntilAskedToStayOn() = runTest {
        val store = SettingsStore(dataStore(backgroundScope))
        assertEquals(false, store.settings.first().keepScreenOn)
        store.setKeepScreenOn(true)
        assertEquals(true, store.settings.first().keepScreenOn)
    }

    @Test
    fun settingsSurviveARestart() = runTest {
        val firstRun = CoroutineScope(Dispatchers.IO + Job())
        SettingsStore(dataStore(firstRun)).apply {
            setUnits(UnitSystem.IMPERIAL)
            setStopStep(StopStep.HALF)
            setAutoLocation(false)
            setTheme(AppTheme.RED_NIGHT)
        }
        firstRun.coroutineContext.job.cancelAndJoin()

        val expected = Settings(units = UnitSystem.IMPERIAL, stopStep = StopStep.HALF, autoLocation = false, theme = AppTheme.RED_NIGHT)
        assertEquals(expected, SettingsStore(dataStore(backgroundScope)).settings.first())
    }

    @Test
    fun aStoredValueThatNoLongerExistsFallsBackToTheDefault() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("theme")] = "SEPIA" }
        assertEquals(AppTheme.DARK, SettingsStore(dataStore).settings.first().theme)
    }
}
