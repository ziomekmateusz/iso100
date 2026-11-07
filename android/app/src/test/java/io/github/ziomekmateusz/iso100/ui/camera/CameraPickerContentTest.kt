package io.github.ziomekmateusz.iso100.ui.camera

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.AppContainer
import io.github.ziomekmateusz.iso100.LocalAppContainer
import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class CameraPickerContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(settings: Settings = Settings(), onClose: () -> Unit = {}, actions: MutableList<CameraPickerAction>? = null) {
        var input by mutableStateOf(CameraPickerInput())
        compose.setContent {
            Iso100Theme {
                CameraPickerContent(
                    CameraPickerUiState.from(input, TEST_CATALOG, settings),
                    { actions?.add(it); input = input.reduce(it) },
                    onClose,
                )
            }
        }
    }

    @Test
    fun typingFiltersTheListAndTheGroupSaysHowManyResults() {
        show()
        compose.onNodeWithContentDescription("Search by model or EXIF name").performTextInput("ilce")
        compose.onNodeWithText("Sony · 2 results").assertIsDisplayed()
        compose.onNodeWithText("EOS R6").assertDoesNotExist()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("EOS R6").assertIsDisplayed()
    }

    @Test
    fun theChosenCameraIsSelectedAndTappingACameraChoosesIt() {
        val actions = mutableListOf<CameraPickerAction>()
        show(Settings(cameraId = "sony-alpha-7-iii"), actions = actions)
        compose.onNodeWithText("Alpha 7 III").assertIsSelected()
        compose.onNodeWithText("Alpha 6400").performClick()
        assertEquals(CameraPickerAction.ChooseCamera("sony-alpha-6400"), actions.last())
    }

    @Test
    fun nothingFoundSaysSoAndLeavesTheWayToTheFormats() {
        val actions = mutableListOf<CameraPickerAction>()
        show(actions = actions)
        compose.onNodeWithContentDescription("Search by model or EXIF name").performTextInput("nikon")
        compose.onNodeWithText("No camera matches “nikon”.").assertIsDisplayed()
        compose.onNodeWithText("Can’t find it? Choose only the sensor format").performClick()
        assertEquals(CameraPickerAction.ShowFormats, actions.last())
    }

    @Test
    fun theCharacteristicsOfACameraOpenFromItsInfoButton() {
        val actions = mutableListOf<CameraPickerAction>()
        show(actions = actions)
        compose.onNodeWithContentDescription("Characteristics of EOS R6").assertDoesNotExist()
        compose.onNodeWithContentDescription("Characteristics of Alpha 7 III").performClick()
        compose.onNodeWithText("Sony Alpha 7 III").assertIsDisplayed()
        compose.onNodeWithText("650 g").assertIsDisplayed()
        compose.onNodeWithText("ISO 100–51200").assertIsDisplayed()
        compose.onNodeWithText("Choose this camera").performScrollTo().performClick()
        assertEquals(CameraPickerAction.ChooseCamera("sony-alpha-7-iii"), actions.last())
    }

    @Test
    fun anUnreadableCatalogShowsAnError() {
        compose.setContent { Iso100Theme { CameraPickerContent(CameraPickerUiState.from(CameraPickerInput(), CatalogState.Failed("x"), Settings()), {}, {}) } }
        compose.onNodeWithText("The camera catalog cannot be read.").assertIsDisplayed()
    }

    @Test
    fun choosingACameraSavesItAndCloses() {
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        var closed = false
        compose.setContent { CompositionLocalProvider(LocalAppContainer provides container) { Iso100Theme { CameraPickerScreen({ closed = true }) } } }
        compose.waitUntil(5_000) { container.catalog.state.value is CatalogState.Ready }
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("sony-alpha-7-iii")
        compose.onNodeWithText("Alpha 7 III").performClick()
        compose.runOnIdle { assertEquals(true, closed) }
        val saved = runBlocking { withTimeout(5_000) { container.settings.settings.first { it.cameraId != null } } }
        assertEquals("sony-alpha-7-iii", saved.cameraId)
    }
}
