package io.github.ziomekmateusz.iso100.ui.camera

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.components.AppBottomSheet
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

data class FormatRow(
    val id: String,
    val name: String,
    val widthMm: Double,
    val heightMm: Double,
    val selected: Boolean
)

/** The sensor formats of the catalog; one is [FormatRow.selected] only when it was chosen without a camera. */
internal fun formatRows(catalogState: CatalogState, settings: Settings): List<FormatRow> {
    val catalog = (catalogState as? CatalogState.Ready)?.catalog ?: return emptyList()
    val selection = catalog.selection(settings)
    return catalog.formats.map { format ->
        FormatRow(
            format.id,
            format.name,
            format.widthMm,
            format.heightMm,
            selection?.camera == null && format.id == selection?.format?.id
        )
    }
}

/** For a camera that is not in the catalog: the choice is saved on the app scope, then [onChosen] closes the picker. */
@Composable
fun FormatPickerSheet(
    onDismiss: () -> Unit,
    onChosen: () -> Unit,
    app: AppContainer = LocalAppContainer.current
) {
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val catalog by app.catalog.state.collectAsStateWithLifecycle()
    FormatPickerContent(
        formatRows(catalog, settings),
        onChoose = { id ->
            app.scope.launch { app.settings.setFormat(id) }
            onChosen()
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun FormatPickerContent(
    formats: List<FormatRow>,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss) {
        LazyColumn(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item { SheetTitle() }
            formatItems(formats, onChoose)
        }
    }
}

@Composable
private fun SheetTitle() {
    Text(
        stringResource(R.string.title_format),
        Modifier
            .padding(bottom = 12.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
}

private fun LazyListScope.formatItems(formats: List<FormatRow>, onChoose: (String) -> Unit) {
    itemsIndexed(formats, key = { _, format -> format.id }) { index, format ->
        FormatItem(
            format,
            first = index == 0,
            last = index == formats.lastIndex
        ) { onChoose(format.id) }
    }
}

@Composable
private fun FormatItem(format: FormatRow, first: Boolean, last: Boolean, onChoose: () -> Unit) {
    val locale = currentLocale()
    Item(format.selected, first, last, onChoose) {
        Lines(
            format.name,
            stringResource(
                R.string.format_size,
                shortNumber(format.widthMm, locale, 2),
                shortNumber(format.heightMm, locale, 2)
            ),
            format.selected,
        )
    }
}

private val previewFormats = listOf(
    FormatRow("full-frame", "Full Frame (35 mm)", 36.0, 24.0, selected = false),
    FormatRow("aps-c", "APS-C", 23.5, 15.6, selected = true),
    FormatRow("four-thirds", "Four Thirds / Micro 4/3", 17.3, 13.0, selected = false),
)

@Preview
@Composable
private fun FormatPickerPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { FormatPickerContent(previewFormats, {}, {}) }
}
