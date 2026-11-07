package io.github.ziomekmateusz.iso100.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.R
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.components.CameraSelector
import io.github.ziomekmateusz.iso100.ui.components.SegmentedChoice
import io.github.ziomekmateusz.iso100.ui.components.ToolScaffold
import io.github.ziomekmateusz.iso100.ui.place.PlaceChoice
import io.github.ziomekmateusz.iso100.ui.place.rememberPlace
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import io.github.ziomekmateusz.iso100.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onChooseCamera: () -> Unit, onSavedPlaces: () -> Unit, onLicenses: () -> Unit, app: AppContainer = LocalAppContainer.current) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf(SettingsInput()) }
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val selection by app.catalog.selection.collectAsStateWithLifecycle()
    val catalog by app.catalog.state.collectAsStateWithLifecycle()
    val saved by app.savedPlaces.places.collectAsStateWithLifecycle(emptyList())
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val languages = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val place = rememberPlace(app)
    val hasPlace = place.choice is PlaceChoice.Ready
    val state = remember(input, settings, selection, catalog, saved, languages, hasPlace) {
        SettingsUiState.from(input, settings, selection, catalog, saved.size, languages, version, hasPlace)
    }
    SettingsContent(
        state,
        onAction = { action ->
            when (action) {
                is SettingsAction.SetStopStep -> app.scope.launch { app.settings.setStopStep(action.step) }
                is SettingsAction.SetUnits -> app.scope.launch { app.settings.setUnits(action.units) }
                is SettingsAction.SetTheme -> app.scope.launch { app.settings.setTheme(action.theme) }
                is SettingsAction.SetAutoLocation -> app.scope.launch { app.settings.setAutoLocation(action.enabled) }
                is SettingsAction.SetAutoRedNight -> {
                    app.scope.launch { app.settings.setAutoRedNight(action.enabled) }
                    // without a place the red night cannot know when it is night: ask the position now
                    if (action.enabled && !hasPlace) place.locate()
                }
                is SettingsAction.SetLanguage -> AppCompatDelegate.setApplicationLocales(
                    if (action.tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(action.tag),
                )
                SettingsAction.ChooseCamera -> onChooseCamera()
                SettingsAction.OpenSavedPlaces -> onSavedPlaces()
                SettingsAction.OpenLicenses -> onLicenses()
                SettingsAction.OpenLanguage, SettingsAction.Dismiss -> Unit
            }
            input = input.reduce(action)
        },
        onBack = onBack,
    )
}

@Composable
fun SettingsContent(state: SettingsUiState, onAction: (SettingsAction) -> Unit, onBack: () -> Unit) {
    ToolScaffold(title = stringResource(R.string.settings), onBack = onBack) {
        CameraSelector(state.camera) { onAction(SettingsAction.ChooseCamera) }
        CalculationCard(state.settings, onAction)
        AppCard(state, onAction)
        Footer(state)
    }
    if (state.languageDialog) LanguageDialog(
        state.language,
        { onAction(SettingsAction.SetLanguage(it)) },
        { onAction(SettingsAction.Dismiss) })
}

/** Stop step, units and theme: how the tools count and look. */
@Composable
private fun CalculationCard(settings: Settings, onAction: (SettingsAction) -> Unit) {
    SettingsCard {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Choice(
                stringResource(R.string.stop_step_label),
                listOf(R.string.stop_full, R.string.stop_half, R.string.stop_third),
                STOP_STEPS.indexOf(settings.stopStep),
            ) { onAction(SettingsAction.SetStopStep(STOP_STEPS[it])) }
            Choice(
                stringResource(R.string.units_label),
                listOf(R.string.units_metric, R.string.units_imperial),
                UNIT_SYSTEMS.indexOf(settings.units),
            ) { onAction(SettingsAction.SetUnits(UNIT_SYSTEMS[it])) }
            Choice(
                stringResource(R.string.theme_label),
                listOf(
                    R.string.theme_system,
                    R.string.theme_dark,
                    R.string.theme_light,
                    R.string.theme_red_night
                ),
                THEMES.indexOf(settings.theme),
            ) { onAction(SettingsAction.SetTheme(THEMES[it])) }
        }
    }
}

/** Red night, location, places, language and licenses: the app around the tools. */
@Composable
private fun AppCard(state: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    val settings = state.settings
    SettingsCard {
        Column(Modifier.padding(horizontal = 16.dp)) {
            SwitchRow(
                stringResource(R.string.auto_red_night),
                stringResource(if (settings.autoRedNight && !state.hasPlace) R.string.auto_red_night_needs_place else R.string.auto_red_night_hint),
                settings.autoRedNight,
            ) { onAction(SettingsAction.SetAutoRedNight(it)) }
            RowDivider()
            SwitchRow(
                stringResource(R.string.auto_location),
                stringResource(R.string.auto_location_hint),
                settings.autoLocation
            ) {
                onAction(SettingsAction.SetAutoLocation(it))
            }
            RowDivider()
            LinkRow(
                stringResource(R.string.title_saved_places),
                state.savedPlaces.toString()
            ) { onAction(SettingsAction.OpenSavedPlaces) }
            RowDivider()
            LinkRow(stringResource(R.string.language), languageName(state.language)) {
                onAction(
                    SettingsAction.OpenLanguage
                )
            }
            RowDivider()
            LinkRow(
                stringResource(R.string.title_licenses),
                ""
            ) { onAction(SettingsAction.OpenLicenses) }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
}

private val STOP_STEPS = listOf(StopStep.FULL, StopStep.HALF, StopStep.THIRD)
private val UNIT_SYSTEMS = listOf(UnitSystem.METRIC, UnitSystem.IMPERIAL)
private val THEMES = listOf(AppTheme.SYSTEM, AppTheme.DARK, AppTheme.LIGHT, AppTheme.RED_NIGHT)

@Composable
private fun Choice(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SegmentedChoice(options.map { stringResource(it) }, selected, onSelect, label, containerColor = MaterialTheme.colorScheme.background)
    }
}

@Composable
private fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp)
            Text(hint, fontSize = 14.sp, color = colors.onSurfaceVariant)
        }
        // the row takes the click: the switch only shows the state
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.primary,
                checkedThumbColor = colors.background,
                uncheckedTrackColor = colors.outlineVariant,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = false, onClick = onClick, role = Role.Button)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), fontSize = 17.sp)
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(painterResource(R.drawable.ic_forward), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The version of the app and of the catalog, and the attribution Lensfun asks for (CC BY-SA 3.0). */
@Composable
private fun Footer(state: SettingsUiState) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val catalog = state.catalogVersion
        Text(
            if (catalog == null) stringResource(R.string.settings_version, state.appVersion)
            else pluralStringResource(R.plurals.settings_catalog, state.cameras, state.appVersion, catalog, state.cameras),
            fontSize = 14.sp,
            color = colors.onSurfaceVariant,
        )
        val lensfun = stringResource(R.string.lensfun)
        val sentence = stringResource(R.string.settings_lensfun, lensfun)
        Text(
            buildAnnotatedString {
                append(sentence.substringBefore(lensfun))
                withLink(LinkAnnotation.Url("https://lensfun.github.io", TextLinkStyles(SpanStyle(color = colors.primary)))) { append(lensfun) }
                append(sentence.substringAfter(lensfun))
            },
            fontSize = 14.sp,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun languageName(tag: String?): String = when (tag) {
    "en" -> stringResource(R.string.language_en)
    "it" -> stringResource(R.string.language_it)
    "pl" -> stringResource(R.string.language_pl)
    else -> stringResource(R.string.language_system)
}

@Composable
private fun LanguageDialog(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                LANGUAGES.forEach { tag ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                tag == current,
                                onClick = { onPick(tag) },
                                role = Role.RadioButton
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == current, onClick = null)
                        Text(languageName(tag), fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val previewState = SettingsUiState(
    settings = Settings(),
    camera = CameraLabel("Sony Alpha 7 III", "Full Frame (35 mm)"),
    savedPlaces = 3,
    language = "it",
    appVersion = "2.0",
    catalogVersion = "f4a4cfa96f83",
    cameras = 579,
    languageDialog = false,
    hasPlace = true,
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SettingsPreview(@PreviewParameter(ThemePreviews::class) theme: AppTheme) {
    Iso100Theme(theme) { SettingsContent(previewState, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SettingsLanguagePreview() {
    Iso100Theme { SettingsContent(previewState.copy(languageDialog = true), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 1800, fontScale = 2f)
@Composable
private fun SettingsLargeTextPreview() {
    Iso100Theme { SettingsContent(previewState.copy(camera = null, catalogVersion = null), {}, {}) }
}
