package io.github.ziomekmateusz.iso100.data

import io.github.ziomekmateusz.iso100.calc.camera.SensorFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/** The camera catalog read from catalog.json; fields the app does not know are ignored. */
@Serializable
data class Catalog(
    val schemaVersion: Int,
    val version: String,
    val attribution: String? = null,
    val formats: List<CatalogFormat>,
    val cameras: List<CatalogCamera>,
) {
    fun format(id: String?): CatalogFormat? = formats.firstOrNull { it.id == id }

    fun camera(id: String?): CatalogCamera? = cameras.firstOrNull { it.id == id }

    /**
     * Cameras where every word of [query] starts a word of the maker, the model or an alias, ignoring case:
     * "alpha 7" finds the Alpha 7 and the Alpha 77, not the Alpha 37; "7m3" finds the ILCE-7M3.
     */
    fun search(query: String): List<CatalogCamera> {
        val words = words(query)
        return cameras.filter { camera ->
            val text = (listOf(camera.maker, camera.model) + camera.aliases).flatMap(::words)
            words.all { word -> text.any { it.startsWith(word) } }
        }
    }

    private fun words(text: String) = text.lowercase().split(NOT_ALPHANUMERIC).filter { it.isNotEmpty() }

    /** The camera and format chosen in [settings]; null when nothing valid is chosen. */
    fun selection(settings: Settings): Selection? {
        val camera = camera(settings.cameraId)
        val format = format(camera?.format ?: settings.formatId) ?: return null
        return Selection(camera, format)
    }
}

@Serializable
data class CatalogFormat(val id: String, val name: String, val widthMm: Double, val heightMm: Double) {
    val sensor: SensorFormat get() = SensorFormat(widthMm, heightMm)
}

@Serializable
data class CatalogCamera(
    val id: String,
    val maker: String,
    val model: String,
    val format: String,
    val mount: String? = null,
    val aliases: List<String> = emptyList(),
    val specs: CameraSpecs? = null,
)

/** Characteristics from an old, unverified source: the calculations use only the resolution (NPF rule) and the ISO range, to dim the dial. */
@Serializable
data class CameraSpecs(
    val isoMin: Int? = null,
    val isoMax: Int? = null,
    val resolutionMp: Double? = null,
    val afPoints: Int? = null,
    val fps: Double? = null,
    val stabilization: Boolean? = null,
    val weatherSealed: Boolean? = null,
    val wifi: Boolean? = null,
    val weightG: Int? = null,
)

/** [camera] is null when only a sensor format was chosen. */
data class Selection(val camera: CatalogCamera?, val format: CatalogFormat)

sealed interface CatalogState {
    data object Loading : CatalogState

    data class Ready(val catalog: Catalog) : CatalogState

    /** The catalog could not be read: an error to show, never a crash. */
    data class Failed(val reason: String) : CatalogState
}

/** The formats of a ready catalog; none while it loads or after an error. */
val CatalogState.formats: List<CatalogFormat> get() = (this as? CatalogState.Ready)?.catalog?.formats.orEmpty()

/** Reads the catalog once, keeps it in memory and derives the chosen camera from the settings. */
class CatalogStore(read: suspend () -> String, settings: Flow<Settings>, scope: CoroutineScope) {
    val state: StateFlow<CatalogState> = flow {
        emit(
            try {
                parseCatalog(read())
            } catch (e: IOException) {
                CatalogState.Failed("cannot read the catalog: ${e.message}")
            },
        )
    }.stateIn(scope, SharingStarted.Eagerly, CatalogState.Loading)

    val selection: StateFlow<Selection?> = combine(state, settings) { state, settings ->
        (state as? CatalogState.Ready)?.catalog?.selection(settings)
    }.stateIn(scope, SharingStarted.Eagerly, null)
}

private const val CATALOG_SCHEMA_VERSION = 1

private val NOT_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

private val json = Json { ignoreUnknownKeys = true }

fun parseCatalog(text: String): CatalogState = try {
    val element = json.parseToJsonElement(text)
    val schema = element.jsonObject["schemaVersion"]?.jsonPrimitive?.int
    if (schema != CATALOG_SCHEMA_VERSION) {
        CatalogState.Failed("unsupported schemaVersion $schema")
    } else {
        CatalogState.Ready(json.decodeFromJsonElement(Catalog.serializer(), element))
    }
} catch (e: IllegalArgumentException) {
    // SerializationException and malformed numbers are both IllegalArgumentException
    CatalogState.Failed("invalid catalog: ${e.message}")
}
