package io.github.ziomekmateusz.iso100.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.calc.exposure.StopStep
import io.github.ziomekmateusz.iso100.calc.units.UnitSystem
import io.github.ziomekmateusz.iso100.data.CatalogState
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.camera.TEST_CATALOG
import io.github.ziomekmateusz.iso100.ui.fov.ALPHA_7_III
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class SettingsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun state(settings: Settings = Settings(), languages: String = "", catalog: CatalogState = TEST_CATALOG) =
        SettingsUiState.from(SettingsInput(), settings, ALPHA_7_III, catalog, 2, languages, "2.0", hasPlace = false)

    @Test
    fun `only English, Italian and Polish, whatever AppCompat keeps`() {
        assertEquals("it", state(languages = "it").language)
        assertEquals("pl", state(languages = "pl-PL").language)
        assertNull(state(languages = "").language)
        assertNull(state(languages = "de").language)
    }

    @Test
    fun theCatalogVersionAndSizeComeFromTheCatalog() {
        assertEquals("test", state().catalogVersion)
        assertEquals(3, state().cameras)
        assertNull(state(catalog = CatalogState.Loading).catalogVersion)
        assertTrue(SettingsInput().reduce(SettingsAction.OpenLanguage).languageDialog)
        assertEquals(SettingsInput(), SettingsInput(languageDialog = true).reduce(SettingsAction.SetLanguage("pl")))
    }

    @Test
    fun theChoicesShowTheSettingsAndSendTheirActions() {
        val actions = mutableListOf<SettingsAction>()
        compose.setContent { Iso100Theme { SettingsContent(state(Settings(autoLocation = false)), { actions += it }, {}) } }
        compose.onNodeWithText("Thirds").assertIsSelected()
        compose.onNodeWithText("Metres").assertIsSelected()
        compose.onNodeWithText("Dark").assertIsSelected()
        compose.onNodeWithText("Feet").performClick()
        compose.onNodeWithText("Full").performClick()
        compose.onNode(hasText("Approximate position") and isToggleable()).assertIsOff().performClick()
        assertEquals(
            listOf(
                SettingsAction.SetUnits(UnitSystem.IMPERIAL),
                SettingsAction.SetStopStep(StopStep.FULL),
                SettingsAction.SetAutoLocation(true),
            ),
            actions,
        )
    }

    @Test
    fun `the automatic red night is off and says when it needs a place`() {
        val actions = mutableListOf<SettingsAction>()
        compose.setContent { Iso100Theme { SettingsContent(state(), { actions += it }, {}) } }
        compose.onNode(hasText("Red night after sunset") and isToggleable()).assertIsOff().performClick()
        assertEquals(listOf<SettingsAction>(SettingsAction.SetAutoRedNight(true)), actions)
        compose.onNodeWithText("Turns on by itself at the end of the civil twilight").assertIsDisplayed()
    }

    @Test
    fun `without a place the switch on says that one is needed`() {
        compose.setContent { Iso100Theme { SettingsContent(state(Settings(autoRedNight = true)), {}, {}) } }
        compose.onNodeWithText("Needs a place: your position or a saved place").assertIsDisplayed()
    }

    @Test
    fun theFooterHasTheVersionsAndTheAttributionOfLensfun() {
        compose.setContent { Iso100Theme { SettingsContent(state(), {}, {}) } }
        compose.onNodeWithText("ISO100 2.0 · catalogue test · 3 cameras").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Camera data partly from Lensfun, licence CC BY-SA 3.0.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theLicensesOpenFromTheirRow() {
        val actions = mutableListOf<SettingsAction>()
        compose.setContent { Iso100Theme { SettingsContent(state(), { actions += it }, {}) } }
        compose.onNodeWithText("Licences").performScrollTo().performClick()
        assertEquals(listOf<SettingsAction>(SettingsAction.OpenLicenses), actions)
    }

    @Test
    fun theLanguageIsChosenInADialog() {
        val actions = mutableListOf<SettingsAction>()
        compose.setContent { Iso100Theme { SettingsContent(state(languages = "it").copy(languageDialog = true), { actions += it }, {}) } }
        compose.onNodeWithText("Polski").performClick()
        assertEquals(listOf<SettingsAction>(SettingsAction.SetLanguage("pl")), actions)
    }
}
