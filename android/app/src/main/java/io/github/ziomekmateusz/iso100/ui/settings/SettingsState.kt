package io.github.ziomekmateusz.iso100.ui.settings

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.AppTheme
import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Selection
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.CameraLabel
import io.github.ziomekmateusz.iso100.ui.label
import kotlinx.parcelize.Parcelize

/** The languages of the app: null follows the phone. */
val LANGUAGES = listOf(null, "en", "it", "pl")

@Parcelize
data class SettingsInput(val languageDialog: Boolean = false) : Parcelable

/** The choices go to the stores (the settings, and AppCompat for the language); the screen only opens and closes the dialog. */
sealed interface SettingsAction {
    data class SetStopStep(val step: StopStep) : SettingsAction
    data class SetUnits(val units: UnitSystem) : SettingsAction
    data class SetTheme(val theme: AppTheme) : SettingsAction
    data class SetAutoLocation(val enabled: Boolean) : SettingsAction
    data class SetAutoRedNight(val enabled: Boolean) : SettingsAction
    data class SetKeepScreenOn(val enabled: Boolean) : SettingsAction
    data class SetLanguage(val tag: String?) : SettingsAction
    data object OpenLanguage : SettingsAction
    data object Dismiss : SettingsAction
    data object ChooseCamera : SettingsAction
    data object OpenSavedPlaces : SettingsAction
    data object OpenLicenses : SettingsAction
}

fun SettingsInput.reduce(action: SettingsAction): SettingsInput = when (action) {
    SettingsAction.OpenLanguage -> copy(languageDialog = true)
    SettingsAction.Dismiss, is SettingsAction.SetLanguage -> copy(languageDialog = false)
    else -> this
}

data class SettingsUiState(
    val settings: Settings,
    val camera: CameraLabel?,
    val savedPlaces: Int,
    /** "it", or null for the language of the phone. */
    val language: String?,
    val appVersion: String,
    /** Null while the catalog loads or when it cannot be read. */
    val catalogVersion: String?,
    val cameras: Int,
    val languageDialog: Boolean,
    /** The automatic red night needs a place where Sun and Moon are calculated. */
    val hasPlace: Boolean,
) {
    companion object {
        fun from(
            input: SettingsInput,
            settings: Settings,
            selection: Selection?,
            catalog: CatalogState,
            savedPlaces: Int,
            languageTags: String,
            appVersion: String,
            hasPlace: Boolean,
        ): SettingsUiState {
            val ready = (catalog as? CatalogState.Ready)?.catalog
            // AppCompat keeps a list of tags, "it" or "pl-PL"; only the language counts
            val language = languageTags.substringBefore(',').substringBefore('-').ifEmpty { null }?.takeIf { it in LANGUAGES }
            return SettingsUiState(
                settings = settings,
                camera = selection?.label(),
                savedPlaces = savedPlaces,
                language = language,
                appVersion = appVersion,
                catalogVersion = ready?.version,
                cameras = ready?.cameras?.size ?: 0,
                languageDialog = input.languageDialog,
                hasPlace = hasPlace,
            )
        }
    }
}
