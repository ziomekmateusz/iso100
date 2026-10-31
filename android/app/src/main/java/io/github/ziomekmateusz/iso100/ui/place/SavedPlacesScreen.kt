package io.github.ziomekmateusz.iso100.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.SavedPlace
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@Composable
fun SavedPlacesScreen(onBack: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(SavedPlacesInput()) }
    val places by app.savedPlaces.places.collectAsStateWithLifecycle(emptyList())
    val state = remember(input, places) { SavedPlacesUiState.from(input, places, Instant.now()) }
    SavedPlacesContent(
        state,
        onAction = { action ->
            // the store writes on the app scope, so that they finish after the dialog closes
            val editing = state.editing
            when (action) {
                SavedPlacesAction.Save -> if (editing != null) app.scope.launch {
                    app.savedPlaces.rename(editing.id, state.name.trim())
                    app.savedPlaces.setZone(editing.id, ZoneId.of(state.zoneId))
                }
                SavedPlacesAction.Delete -> if (editing != null) app.scope.launch { app.savedPlaces.delete(editing.id) }
                else -> Unit
            }
            input = input.reduce(action)
        },
        onBack = onBack,
    )
}

@Composable
fun SavedPlacesContent(state: SavedPlacesUiState, onAction: (SavedPlacesAction) -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ToolScaffold(title = stringResource(R.string.title_saved_places), onBack = onBack) {
        if (state.places.isEmpty()) {
            Text(stringResource(R.string.saved_places_empty), Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        state.places.forEach { place ->
            Surface(onClick = { onAction(SavedPlacesAction.Edit(place)) }, shape = MaterialTheme.shapes.small, color = colors.surface) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(place.name, fontSize = 17.sp)
                        Text(
                            placeName(null, place.place) + " " + stringResource(R.string.separator) + place.zoneId,
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Icon(painterResource(R.drawable.ic_forward), null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                }
            }
        }
    }
    val editing = state.editing
    when {
        editing == null -> Unit
        state.zonePicker -> ZoneDialog(state, onAction)
        state.confirmDelete -> AlertDialog(
            onDismissRequest = { onAction(SavedPlacesAction.Dismiss) },
            text = { Text(stringResource(R.string.saved_place_delete_question, editing.name)) },
            confirmButton = { TextButton(onClick = { onAction(SavedPlacesAction.Delete) }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { onAction(SavedPlacesAction.Dismiss) }) { Text(stringResource(R.string.cancel)) } },
        )
        else -> EditDialog(state, onAction)
    }
}

/** The name and the time zone of a place, or its deletion. */
@Composable
private fun EditDialog(state: SavedPlacesUiState, onAction: (SavedPlacesAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(SavedPlacesAction.Dismiss) },
        title = { Text(stringResource(R.string.saved_place_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onAction(SavedPlacesAction.SetName(it)) },
                    label = { Text(stringResource(R.string.saved_place_name)) },
                    singleLine = true,
                    isError = !state.canSave,
                )
                Column {
                    Text(stringResource(R.string.time_zone), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onAction(SavedPlacesAction.OpenZones) }) { Text(state.zoneId, fontSize = 17.sp) }
                }
                TextButton(onClick = { onAction(SavedPlacesAction.AskDelete) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAction(SavedPlacesAction.Save) }, enabled = state.canSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = { onAction(SavedPlacesAction.Dismiss) }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ZoneDialog(state: SavedPlacesUiState, onAction: (SavedPlacesAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(SavedPlacesAction.Dismiss) },
        title = { Text(stringResource(R.string.time_zone)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.zoneQuery,
                    onValueChange = { onAction(SavedPlacesAction.SearchZone(it)) },
                    label = { Text(stringResource(R.string.time_zone_search)) },
                    singleLine = true,
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(state.zones, key = { it.id }) { zone ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(zone.id == state.zoneId, onClick = { onAction(SavedPlacesAction.PickZone(zone.id)) }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(zone.id.replace('_', ' '), Modifier.weight(1f), fontSize = 16.sp)
                            Text(stringResource(R.string.utc_offset, zone.offset.id.replace("Z", "+00:00")), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAction(SavedPlacesAction.Dismiss) }) { Text(stringResource(R.string.cancel)) } },
    )
}

private val previewPlaces = listOf(
    SavedPlace("rome", "Roma", 41.9, 12.5, "Europe/Rome"),
    SavedPlace("reykjavik", "Reykjavík", 64.15, -21.94, "Atlantic/Reykjavik"),
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SavedPlacesPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { SavedPlacesContent(SavedPlacesUiState.from(SavedPlacesInput(), previewPlaces, Instant.EPOCH), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SavedPlacesEmptyPreview() {
    Iso100Theme { SavedPlacesContent(SavedPlacesUiState.from(SavedPlacesInput(), emptyList(), Instant.EPOCH), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SavedPlacesEditPreview() {
    Iso100Theme {
        SavedPlacesContent(SavedPlacesUiState.from(SavedPlacesInput().reduce(SavedPlacesAction.Edit(previewPlaces[1])), previewPlaces, Instant.EPOCH), {}, {})
    }
}
