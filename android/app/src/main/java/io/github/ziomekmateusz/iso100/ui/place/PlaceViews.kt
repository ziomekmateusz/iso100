package io.github.ziomekmateusz.iso100.ui.place

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.astro.Place
import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.ui.components.PrimaryButton
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.number
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import java.time.ZoneId

/** "Roma", or the coordinates when the Geocoder gave no name. */
@Composable
fun placeName(name: String?, place: Place): String {
    val locale = currentLocale()
    return name ?: stringResource(R.string.coordinates, number(place.latitude, 2, locale), number(place.longitude, 2, locale))
}

/** The place in the header of Sun and Moon; it opens the place picker. */
@Composable
fun PlaceChip(choice: PlaceChoice, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val text = when (choice) {
        is PlaceChoice.Ready -> placeName(choice.name, choice.place)
        is PlaceChoice.Unavailable -> stringResource(R.string.choose_place)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.background,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_pin), null, Modifier.size(16.dp), tint = colors.onSurface)
            Text(text, fontSize = 15.sp, color = colors.onSurface, maxLines = 1)
        }
    }
}

/** Why there is no place yet, with what the user can do: locate, allow the position, try again, or choose a place. */
@Composable
fun LocationMessage(location: LocationState, onLocate: () -> Unit, onChoosePlace: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = colors.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val (message, action) = when (location) {
                LocationState.Unknown -> R.string.location_unknown to R.string.use_position
                LocationState.Locating -> R.string.location_locating to null
                LocationState.PermissionDenied -> R.string.location_denied to R.string.allow
                LocationState.LocationOff -> R.string.location_off to R.string.retry
                LocationState.NotFound, is LocationState.Found -> R.string.location_not_found to R.string.retry
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (location == LocationState.Locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(stringResource(message), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (action != null) PrimaryButton(stringResource(action), onLocate)
                OutlinedButton(
                    onClick = onChoosePlace,
                    modifier = Modifier.heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) { Text(stringResource(R.string.choose_place), fontSize = 16.sp, color = colors.onSurface) }
            }
        }
    }
}

@Preview
@Composable
private fun LocationMessagePreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LocationMessage(LocationState.PermissionDenied, {}, {})
                LocationMessage(LocationState.Locating, {}, {})
            }
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun LocationMessageLargeTextPreview() {
    Iso100Theme { LocationMessage(LocationState.LocationOff, {}, {}, Modifier.padding(16.dp)) }
}

@Preview
@Composable
private fun PlaceChipPreview() {
    Iso100Theme { Surface { PlaceChip(PlaceChoice.Ready(Place(41.9, 12.5, ZoneId.of("Europe/Rome")), "Roma", current = true)) {} } }
}
