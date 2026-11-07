package io.github.ziomekmateusz.iso100.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.CameraSpecs
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.components.AppBottomSheet
import io.github.ziomekmateusz.iso100.ui.components.PrimaryButton
import io.github.ziomekmateusz.iso100.ui.components.RoundIconButton
import io.github.ziomekmateusz.iso100.ui.components.fadeUnder
import io.github.ziomekmateusz.iso100.ui.currentLocale
import io.github.ziomekmateusz.iso100.ui.shortNumber
import io.github.ziomekmateusz.iso100.ui.theme.Barlow
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

// lifts the search and the format button off the list scrolling under them;
// in the dark themes the lighter container and the border do it, the shadow barely shows
private val searchElevation = 6.dp

@Composable
fun CameraPickerScreen(onClose: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    var input by rememberSaveable { mutableStateOf(CameraPickerInput()) }
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val catalog by app.catalog.state.collectAsStateWithLifecycle()
    val state = remember(input, settings, catalog) { CameraPickerUiState.from(input, catalog, settings) }
    CameraPickerContent(
        state,
        onAction = { action ->
            when (action) {
                is CameraPickerAction.ChooseCamera -> app.scope.launch { app.settings.setCamera(action.id) }.also { onClose() }
                else -> input = input.reduce(action)
            }
        },
        onBack = onClose,
    )
    if (state.formatSheet) {
        FormatPickerSheet(onDismiss = {
            input = input.reduce(CameraPickerAction.HideFormats)
        }, onChosen = onClose)
    }
}

@Composable
fun CameraPickerContent(state: CameraPickerUiState, onAction: (CameraPickerAction) -> Unit, onBack: () -> Unit) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { PickerTopBar(onBack) },
        bottomBar = { PickerBottomBar(state, onAction) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { bars ->
        PickerList(state, onAction, bars)
    }
    state.details?.let { DetailsSheet(it, onAction) }
}

/** The cameras by maker: it scrolls under the bars and fades there. A new search starts from the top. */
@Composable
private fun PickerList(state: CameraPickerUiState, onAction: (CameraPickerAction) -> Unit, bars: PaddingValues) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.query) { listState.scrollToItem(0) }
    val top = bars.calculateTopPadding()
    val bottom = bars.calculateBottomPadding()
    LazyColumn(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .fadeUnder(top, bottom),
        listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = top + 14.dp,
            bottom = bottom + 16.dp,
        ),
    ) {
        when {
            state.failed -> item { Message(stringResource(R.string.catalog_error)) }

            state.groups.isEmpty() && state.query.isNotBlank() -> item {
                NoCameraFound(state.query) {
                    onAction(
                        CameraPickerAction.ShowFormats,
                    )
                }
            }

            else -> cameraGroups(state.groups, onAction)
        }
    }
}

private fun LazyListScope.cameraGroups(groups: List<CameraGroup>, onAction: (CameraPickerAction) -> Unit) {
    groups.forEachIndexed { groupIndex, group ->
        item(key = "maker " + group.maker) { MakerHeader(group, first = groupIndex == 0) }
        itemsIndexed(group.cameras, key = { _, camera -> camera.id }) { index, camera ->
            CameraItem(
                camera,
                first = index == 0,
                last = index == group.cameras.lastIndex,
                onChoose = { onAction(CameraPickerAction.ChooseCamera(camera.id)) },
                onDetails = { onAction(CameraPickerAction.ShowDetails(camera.id)) },
            )
        }
    }
}

@Composable
private fun MakerHeader(group: CameraGroup, first: Boolean) {
    Text(
        pluralStringResource(
            R.plurals.camera_group,
            group.cameras.size,
            group.maker,
            group.cameras.size,
        ),
        Modifier
            .padding(start = 4.dp, top = if (first) 0.dp else 16.dp, bottom = 6.dp)
            .semantics { heading() },
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CameraItem(camera: CameraRow, first: Boolean, last: Boolean, onChoose: () -> Unit, onDetails: () -> Unit) {
    Item(camera.selected, first, last, onChoose) {
        Lines(camera.model, camera.alias, camera.selected)
        FormatChip(camera.format)
        if (camera.hasSpecs) DetailsButton(camera.model, onDetails)
    }
}

@Composable
private fun DetailsButton(model: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.ic_info),
            stringResource(R.string.camera_details, model),
            Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "No camera for …" and the way out: the sensor format alone. */
@Composable
private fun NoCameraFound(query: String, onShowFormats: () -> Unit) {
    Column {
        Message(stringResource(R.string.no_camera_found, query.trim()))
        TextButton(onClick = onShowFormats) {
            Text(
                stringResource(R.string.choose_format_only),
                fontSize = 16.sp,
            )
        }
    }
}

/**
 * The search and the format button within reach of the thumb; the bar rides above the keyboard while
 * searching.
 */
@Composable
private fun PickerBottomBar(state: CameraPickerUiState, onAction: (CameraPickerAction) -> Unit) {
    Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                state.query,
                { onAction(CameraPickerAction.Search(it)) },
                Modifier.weight(1f),
            )
            FormatOnlyButton { onAction(CameraPickerAction.ShowFormats) }
        }
    }
}

/** Back, as in the tools, and the title. */
@Composable
private fun PickerTopBar(onBack: () -> Unit) {
    Row(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(R.drawable.ic_back, stringResource(R.string.back), onBack)
        Text(
            stringResource(R.string.title_camera),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** As tall as the search field beside it; TalkBack reads the whole "can't find it?" sentence. */
@Composable
private fun FormatOnlyButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .shadow(searchElevation, CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .size(56.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
        ),
    ) {
        Icon(
            painterResource(R.drawable.ic_sensor),
            stringResource(R.string.choose_format_only),
            Modifier.size(24.dp),
        )
    }
}

/** The characteristics of a camera, with a button to choose it. */
@Composable
private fun DetailsSheet(details: CameraDetails, onAction: (CameraPickerAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val specs = details.specs
    val yes = stringResource(R.string.yes)
    val no = stringResource(R.string.no)
    fun flag(value: Boolean?) = value?.let { if (it) yes else no }
    val rows = listOf(
        R.string.spec_format to details.format,
        R.string.spec_mount to details.mount,
        R.string.spec_resolution to specs.resolutionMp?.let { shortNumber(it, locale) + " " + stringResource(R.string.symbol_mp) },
        R.string.spec_iso to
                if (specs.isoMin != null && specs.isoMax != null) stringResource(
                    R.string.spec_iso_range,
                    specs.isoMin,
                    specs.isoMax
                ) else null,
        R.string.spec_af_points to specs.afPoints?.toString(),
        R.string.spec_fps to specs.fps?.let { stringResource(R.string.spec_fps_value, shortNumber(it, locale)) },
        R.string.spec_stabilization to flag(specs.stabilization),
        R.string.spec_weather_sealed to flag(specs.weatherSealed),
        R.string.spec_wifi to flag(specs.wifi),
        R.string.spec_weight to specs.weightG?.let { stringResource(R.string.spec_weight_value, it) },
        R.string.spec_exif to details.aliases.joinToString(", ").ifEmpty { null },
    )
    AppBottomSheet(onDismiss = { onAction(CameraPickerAction.HideDetails) }) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(details.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            Column {
                rows.forEach { (label, value) ->
                    if (value != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(label), Modifier.weight(1f), fontSize = 16.sp, color = colors.onSurfaceVariant)
                            Text(value, fontSize = 16.sp)
                        }
                        HorizontalDivider(color = colors.surfaceContainerHigh)
                    }
                }
            }
            Text(stringResource(R.string.specs_note), fontSize = 14.sp, color = colors.onSurfaceVariant)
            PrimaryButton(stringResource(R.string.choose_this_camera), {
                onAction(CameraPickerAction.ChooseCamera(details.id))
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val description = stringResource(R.string.camera_search)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
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
                    .shadow(searchElevation, shape)
                    .background(colors.surfaceContainerHigh, shape)
                    .border(
                        if (focused) 2.dp else 1.dp,
                        if (focused) colors.primary else colors.outlineVariant,
                        shape,
                    )
                    .padding(start = 16.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.camera_search_hint),
                            fontSize = 18.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            stringResource(R.string.clear_search),
                            Modifier.size(18.dp),
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

/** A row of a list in a rounded card: the card is cut by the first and the last row of the group. */
@Composable
internal fun Item(selected: Boolean, first: Boolean, last: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val top = if (first) 20.dp else 0.dp
    val bottom = if (last) 20.dp else 0.dp
    Column(
        Modifier
            .clip(RoundedCornerShape(top, top, bottom, bottom))
            .background(colors.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .background(if (selected) colors.surfaceContainerHigh else Color.Transparent)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(22.dp), tint = colors.primary)
            } else {
                Spacer(Modifier.size(22.dp))
            }
        }
        if (!last) HorizontalDivider(color = colors.surfaceContainerHigh)
    }
}

@Composable
internal fun RowScope.Lines(title: String, detail: String?, selected: Boolean) {
    Column(Modifier.weight(1f)) {
        Text(title, fontSize = 18.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        if (detail != null) Text(detail, fontFamily = Barlow, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormatChip(name: String) {
    Box(
        Modifier
            .heightIn(min = 26.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val previewState = CameraPickerUiState(
    query = "alpha 7",
    formatSheet = false,
    groups = listOf(
        CameraGroup(
            "Sony",
            listOf(
                CameraRow("sony-alpha-7-iii", "Alpha 7 III", "ILCE-7M3", "Full Frame", selected = true, hasSpecs = true),
                CameraRow("sony-alpha-7-iv", "Alpha 7 IV", "ILCE-7M4", "Full Frame", selected = false, hasSpecs = false),
                CameraRow("sony-alpha-77-ii", "Alpha 77 II", "ILCA-77M2", "APS-C", selected = false, hasSpecs = false),
            ),
        ),
    ),
    failed = false,
    details = null,
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun CameraPickerPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { CameraPickerContent(previewState, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun CameraPickerNoResultPreview() {
    Iso100Theme { CameraPickerContent(previewState.copy(query = "xyz", groups = emptyList()), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun CameraPickerDetailsPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    val specs = CameraSpecs(
        isoMin = 100,
        isoMax = 51200,
        resolutionMp = 24.2,
        afPoints = 693,
        fps = 10.0,
        stabilization = true,
        weatherSealed = true,
        wifi = true,
        weightG = 650,
    )
    val details = CameraDetails("sony-alpha-7-iii", "Sony Alpha 7 III", "Full Frame (35 mm)", "Sony E", listOf("ILCE-7M3"), specs)
    Iso100Theme(theme) { CameraPickerContent(previewState.copy(details = details), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915, fontScale = 2f)
@Composable
private fun CameraPickerLargeTextPreview() {
    Iso100Theme { CameraPickerContent(previewState, {}, {}) }
}
