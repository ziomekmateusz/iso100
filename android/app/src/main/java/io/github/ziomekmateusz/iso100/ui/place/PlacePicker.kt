package io.github.ziomekmateusz.iso100.ui.place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.data.PlaceCandidate
import io.github.ziomekmateusz.iso100.data.SavedPlace
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.components.AppBottomSheet
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.number
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId

/** The place picker: the current position, the saved places, and the search by name, which saves what it finds. */
@Composable
fun PlacePickerSheet(onDismiss: () -> Unit, onLocate: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val saved by app.savedPlaces.places.collectAsStateWithLifecycle(emptyList())
    val location by app.location.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    // null while the search runs; the Geocoder answers in a second or two
    val found by produceState<List<PlaceCandidate>?>(emptyList(), query) {
        if (query.isBlank()) {
            value = emptyList()
            return@produceState
        }
        value = null
        delay(400)
        value = app.geocoding.search(query.trim())
    }
    PlacePickerContent(
        state = PlacePickerUiState(settings.placeId, saved, location, app.geocoding.available, query, found),
        onAction = { action ->
            // the choice is saved on the app scope: the sheet closes at once
            when (action) {
                is PlacePickerAction.Search -> query = action.query
                PlacePickerAction.ChooseCurrent -> {
                    app.scope.launch { app.settings.setPlace(null) }
                    onLocate()
                    onDismiss()
                }
                is PlacePickerAction.ChooseSaved -> {
                    app.scope.launch { app.settings.setPlace(action.id) }
                    onDismiss()
                }
                is PlacePickerAction.ChooseFound -> {
                    // a new place takes the time zone of the phone; the user can change it among the saved places
                    app.scope.launch {
                        val place = app.savedPlaces.add(action.candidate.name, action.candidate.latitude, action.candidate.longitude, ZoneId.systemDefault())
                        app.settings.setPlace(place.id)
                    }
                    onDismiss()
                }
            }
        },
        onDismiss = onDismiss,
    )
}

/** [found] null while searching. */
data class PlacePickerUiState(
    val placeId: String?,
    val saved: List<SavedPlace>,
    val location: LocationState,
    val searchAvailable: Boolean,
    val query: String,
    val found: List<PlaceCandidate>?,
)

sealed interface PlacePickerAction {
    data class Search(val query: String) : PlacePickerAction
    data object ChooseCurrent : PlacePickerAction
    data class ChooseSaved(val id: String) : PlacePickerAction
    data class ChooseFound(val candidate: PlaceCandidate) : PlacePickerAction
}

@Composable
fun PlacePickerContent(state: PlacePickerUiState, onAction: (PlacePickerAction) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismiss = onDismiss) {
        LazyColumn(Modifier.padding(bottom = 16.dp)) {
            item { Text(stringResource(R.string.choose_place), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge) }
            item { CurrentPlace(state) { onAction(PlacePickerAction.ChooseCurrent) } }
            savedPlaces(state) { onAction(PlacePickerAction.ChooseSaved(it)) }
            placeSearch(state, onAction)
        }
    }
}

/** The phone's position, chosen when no saved place is. */
@Composable
private fun CurrentPlace(state: PlacePickerUiState, onChoose: () -> Unit) {
    val current = state.location
    PlaceRow(
        title = stringResource(R.string.current_position),
        detail = when (current) {
            is LocationState.Found -> placeName(current.name, current.place)
            LocationState.Locating -> stringResource(R.string.location_locating)
            else -> null
        },
        selected = state.placeId == null || state.saved.none { it.id == state.placeId },
        onClick = onChoose,
    )
}

private fun LazyListScope.savedPlaces(state: PlacePickerUiState, onChoose: (String) -> Unit) {
    if (state.saved.isEmpty()) return
    item { Header(stringResource(R.string.title_saved_places)) }
    items(state.saved, key = { it.id }) { place ->
        PlaceRow(place.name, place.zoneId, place.id == state.placeId, { onChoose(place.id) })
    }
}

/** The search field and what it found, or why there is no search. */
private fun LazyListScope.placeSearch(
    state: PlacePickerUiState,
    onAction: (PlacePickerAction) -> Unit
) {
    item { Header(stringResource(R.string.place_search)) }
    if (!state.searchAvailable) {
        item { Note(stringResource(R.string.place_search_unavailable)) }
        return
    }
    item { SearchField(state.query) { onAction(PlacePickerAction.Search(it)) } }
    val found = state.found
    when {
        found == null -> item { Searching() }
        found.isEmpty() && state.query.isNotBlank() -> item {
            Note(
                stringResource(
                    R.string.place_not_found,
                    state.query.trim()
                )
            )
        }

        else -> items(found) { candidate ->
            FoundPlace(candidate) {
                onAction(
                    PlacePickerAction.ChooseFound(
                        candidate
                    )
                )
            }
        }
    }
}

@Composable
private fun FoundPlace(candidate: PlaceCandidate, onChoose: () -> Unit) {
    val locale = currentLocale()
    PlaceRow(
        candidate.name,
        stringResource(
            R.string.coordinates,
            number(candidate.latitude, 2, locale),
            number(candidate.longitude, 2, locale)
        ),
        selected = false,
        onClick = onChoose,
    )
}

@Composable
private fun Searching() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator(Modifier.size(24.dp)) }
}

@Composable
private fun PlaceRow(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (detail != null) Text(detail, fontSize = 14.sp, color = colors.onSurfaceVariant)
        }
        if (selected) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(22.dp), tint = colors.primary) else Spacer(Modifier.size(22.dp))
    }
}

@Composable
private fun Header(text: String) {
    Text(text, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Note(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val description = stringResource(R.string.place_search)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = description },
        textStyle = TextStyle(fontSize = 18.sp, color = colors.onSurface, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        singleLine = true,
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            val shape = RoundedCornerShape(28.dp)
            Row(
                Modifier
                    .height(56.dp)
                    .background(colors.background, shape)
                    .border(1.dp, colors.outlineVariant, shape)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.place_search_hint), fontSize = 18.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            }
        },
    )
}
