package io.github.ziomekmateusz.iso100.data

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
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
import java.time.ZoneId

class SavedPlacesStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file get() = File(folder.root, "saved_places.json")

    private fun store(scope: CoroutineScope) = SavedPlacesStore(
        DataStoreFactory.create(SavedPlacesSerializer, ReplaceFileCorruptionHandler { emptyList() }, scope = scope) { file },
    )

    private val rome = ZoneId.of("Europe/Rome")

    @Test
    fun placesAreSavedRenamedAndDeleted() = runTest {
        val store = store(backgroundScope)
        val colosseum = store.add("Colosseo", 41.8902, 12.4922, rome)
        val etna = store.add("Etna", 37.751, 14.9934, rome)
        store.rename(colosseum.id, "Roma")
        store.delete(etna.id)
        assertEquals(listOf(colosseum.copy(name = "Roma")), store.places.first())
        assertEquals(rome, store.places.first().single().place.zone)
    }

    @Test
    fun `the time zone of a place can be changed`() = runTest {
        val store = store(backgroundScope)
        // saved from a phone set to Rome, the place is in Reykjavik
        val reykjavik = store.add("Reykjavík", 64.1466, -21.9426, rome)
        store.setZone(reykjavik.id, ZoneId.of("Atlantic/Reykjavik"))
        assertEquals(ZoneId.of("Atlantic/Reykjavik"), store.places.first().single().place.zone)
    }

    @Test
    fun placesSurviveARestart() = runTest {
        val firstRun = CoroutineScope(Dispatchers.IO + Job())
        val tromso = store(firstRun).add("Tromsø", 69.6492, 18.9553, ZoneId.of("Europe/Oslo"))
        firstRun.coroutineContext.job.cancelAndJoin()
        assertEquals(listOf(tromso), store(backgroundScope).places.first())
    }

    @Test
    fun anUnreadableFileStartsAgainEmpty() = runTest {
        file.writeText("not json")
        assertEquals(emptyList<SavedPlace>(), store(backgroundScope).places.first())
    }
}
