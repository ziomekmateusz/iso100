package io.github.ziomekmateusz.iso100.ui.place

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.data.SavedPlace
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** The place being edited ([editing] its id) with the name and the zone typed so far; null: just the list. */
@Parcelize
data class SavedPlacesInput(
    val editing: String? = null,
    val name: String = "",
    val zoneId: String = "",
    val zonePicker: Boolean = false,
    val zoneQuery: String = "",
    val confirmDelete: Boolean = false,
) : Parcelable

sealed interface SavedPlacesAction {
    data class Edit(val place: SavedPlace) : SavedPlacesAction
    data class SetName(val name: String) : SavedPlacesAction
    data object OpenZones : SavedPlacesAction
    data class SearchZone(val query: String) : SavedPlacesAction
    data class PickZone(val zoneId: String) : SavedPlacesAction
    data object AskDelete : SavedPlacesAction

    /** Saved, deleted or dismissed: the screen writes to the store, the input goes back to the list. */
    data object Save : SavedPlacesAction
    data object Delete : SavedPlacesAction
    data object Dismiss : SavedPlacesAction
}

fun SavedPlacesInput.reduce(action: SavedPlacesAction): SavedPlacesInput = when (action) {
    is SavedPlacesAction.Edit -> SavedPlacesInput(editing = action.place.id, name = action.place.name, zoneId = action.place.zoneId)

    is SavedPlacesAction.SetName -> copy(name = action.name)

    SavedPlacesAction.OpenZones -> copy(zonePicker = true, zoneQuery = "")

    is SavedPlacesAction.SearchZone -> copy(zoneQuery = action.query)

    is SavedPlacesAction.PickZone -> copy(zoneId = action.zoneId, zonePicker = false)

    SavedPlacesAction.AskDelete -> copy(confirmDelete = true)

    SavedPlacesAction.Save, SavedPlacesAction.Delete -> SavedPlacesInput()

    // the innermost dialog closes first
    SavedPlacesAction.Dismiss -> when {
        zonePicker -> copy(zonePicker = false)
        confirmDelete -> copy(confirmDelete = false)
        else -> SavedPlacesInput()
    }
}

/** A time zone with its offset now: "Europe/Rome", +01:00. */
data class ZoneRow(val id: String, val offset: ZoneOffset)

data class SavedPlacesUiState(
    val places: List<SavedPlace>,
    val editing: SavedPlace?,
    val name: String,
    val zoneId: String,
    val zonePicker: Boolean,
    val zoneQuery: String,
    val zones: List<ZoneRow>,
    val confirmDelete: Boolean,
) {
    val canSave: Boolean get() = name.isNotBlank()

    companion object {
        fun from(input: SavedPlacesInput, places: List<SavedPlace>, now: Instant): SavedPlacesUiState {
            val words = input.zoneQuery.lowercase().split(' ').filter { it.isNotBlank() }
            return SavedPlacesUiState(
                places = places,
                editing = places.firstOrNull { it.id == input.editing },
                name = input.name,
                zoneId = input.zoneId,
                zonePicker = input.zonePicker,
                zoneQuery = input.zoneQuery,
                // "rome", "new york": the ids write spaces as underscores
                zones = if (!input.zonePicker) {
                    emptyList()
                } else {
                    ZONES
                        .filter { id -> words.all { it in id.lowercase().replace('_', ' ') } }
                        .map { ZoneRow(it, ZoneId.of(it).rules.getOffset(now)) }
                },
                confirmDelete = input.confirmDelete,
            )
        }

        // the region ids only: no "Etc/GMT+3" or "SystemV/...", which nobody looks for by place
        private val ZONES = ZoneId.getAvailableZoneIds().filter {
            '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/")
        }.sorted()
    }
}
