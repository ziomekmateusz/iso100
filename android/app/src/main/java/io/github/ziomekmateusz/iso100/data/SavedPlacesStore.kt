package io.github.ziomekmateusz.iso100.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import io.github.ziomekmateusz.iso100.calc.astro.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.ZoneId
import java.util.UUID

/** A place saved by the user, with the time zone used to show its times. */
@Serializable
data class SavedPlace(val id: String, val name: String, val latitude: Double, val longitude: Double, val zoneId: String) {
    val place: Place get() = Place(latitude, longitude, ZoneId.of(zoneId))
}

class SavedPlacesStore(private val dataStore: DataStore<List<SavedPlace>>) {
    val places: Flow<List<SavedPlace>> = dataStore.data.catch { if (it is IOException) emit(emptyList()) else throw it }

    suspend fun add(name: String, latitude: Double, longitude: Double, zone: ZoneId): SavedPlace {
        val place = SavedPlace(UUID.randomUUID().toString(), name, latitude, longitude, zone.id)
        dataStore.updateData { it + place }
        return place
    }

    suspend fun rename(id: String, name: String) {
        dataStore.updateData { places -> places.map { if (it.id == id) it.copy(name = name) else it } }
    }

    /** A new place takes the time zone of the phone, which is wrong for a faraway place: here the user corrects it. */
    suspend fun setZone(id: String, zone: ZoneId) {
        dataStore.updateData { places -> places.map { if (it.id == id) it.copy(zoneId = zone.id) else it } }
    }

    suspend fun delete(id: String) {
        dataStore.updateData { places -> places.filterNot { it.id == id } }
    }
}

object SavedPlacesSerializer : Serializer<List<SavedPlace>> {
    override val defaultValue: List<SavedPlace> = emptyList()

    override suspend fun readFrom(input: InputStream): List<SavedPlace> = try {
        Json.decodeFromString(input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("saved places are not valid JSON", e)
    }

    override suspend fun writeTo(t: List<SavedPlace>, output: OutputStream) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}
