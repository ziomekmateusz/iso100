package io.github.ziomekmateusz.iso100.ui.camera

import android.os.Parcelable
import io.github.ziomekmateusz.iso100.data.CameraSpecs
import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Settings
import kotlinx.parcelize.Parcelize

/**
 * What the user typed, whether the sheet of the sensor formats is open, and the camera whose
 * characteristics are open ([details], its id).
 */
@Parcelize
data class CameraPickerInput(val query: String = "", val formatSheet: Boolean = false, val details: String? = null) : Parcelable

sealed interface CameraPickerAction {
    data class Search(val query: String) : CameraPickerAction
    data object ShowFormats : CameraPickerAction
    data object HideFormats : CameraPickerAction
    data class ShowDetails(val id: String) : CameraPickerAction
    data object HideDetails : CameraPickerAction

    /** Saved in the settings by the screen, which then closes. */
    data class ChooseCamera(val id: String) : CameraPickerAction
}

fun CameraPickerInput.reduce(action: CameraPickerAction): CameraPickerInput = when (action) {
    is CameraPickerAction.Search -> copy(query = action.query)
    CameraPickerAction.ShowFormats -> copy(formatSheet = true)
    CameraPickerAction.HideFormats -> copy(formatSheet = false)
    is CameraPickerAction.ShowDetails -> copy(details = action.id)
    CameraPickerAction.HideDetails -> copy(details = null)
    is CameraPickerAction.ChooseCamera -> this
}

/** [alias] is the first EXIF name, [format] the short name of the format: "Full Frame", "APS-C". */
data class CameraRow(
    val id: String,
    val model: String,
    val alias: String?,
    val format: String,
    val selected: Boolean,
    val hasSpecs: Boolean,
)

data class CameraGroup(val maker: String, val cameras: List<CameraRow>)

/** The characteristics of a camera that has them: only some models, from an old and unverified source. */
data class CameraDetails(
    val id: String,
    val name: String,
    val format: String,
    val mount: String?,
    val aliases: List<String>,
    val specs: CameraSpecs,
)

data class CameraPickerUiState(
    val query: String,
    val formatSheet: Boolean,
    val groups: List<CameraGroup>,
    val failed: Boolean,
    val details: CameraDetails?,
) {
    companion object {
        fun from(input: CameraPickerInput, catalogState: CatalogState, settings: Settings): CameraPickerUiState {
            val catalog = (catalogState as? CatalogState.Ready)?.catalog
            val selection = catalog?.selection(settings)
            val groups = catalog?.search(input.query).orEmpty()
                .groupBy { it.maker }
                .map { (maker, cameras) ->
                    CameraGroup(
                        maker,
                        cameras.map { camera ->
                            val format = catalog?.format(camera.format)?.name.orEmpty().substringBefore(" (")
                            CameraRow(
                                camera.id,
                                camera.model,
                                camera.aliases.firstOrNull(),
                                format,
                                camera.id == selection?.camera?.id,
                                camera.specs != null,
                            )
                        },
                    )
                }
            val details = catalog?.camera(input.details)?.let { camera ->
                camera.specs?.let { specs ->
                    CameraDetails(
                        camera.id,
                        "${camera.maker} ${camera.model}",
                        catalog.format(camera.format)?.name.orEmpty(),
                        camera.mount,
                        camera.aliases,
                        specs,
                    )
                }
            }
            return CameraPickerUiState(
                input.query,
                input.formatSheet,
                groups,
                catalogState is CatalogState.Failed,
                details,
            )
        }
    }
}
