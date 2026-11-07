package io.github.ziomekmateusz.iso100.ui.place

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.data.SavedPlace
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SavedPlacesTest {
    @get:Rule
    val compose = createComposeRule()

    private val reykjavik = SavedPlace("r", "Reykjavík", 64.15, -21.94, "Europe/Rome")
    private val january = Instant.parse("2026-01-15T12:00:00Z")

    @Test
    fun `the zone of a place is searched by name and replaced`() {
        val editing = SavedPlacesInput().reduce(SavedPlacesAction.Edit(reykjavik)).reduce(SavedPlacesAction.OpenZones)
        val searched = SavedPlacesUiState.from(editing.reduce(SavedPlacesAction.SearchZone("reykj")), listOf(reykjavik), january)
        assertEquals(listOf(ZoneRow("Atlantic/Reykjavik", ZoneOffset.UTC)), searched.zones)
        assertEquals(
            "America/New_York",
            SavedPlacesUiState.from(editing.reduce(SavedPlacesAction.SearchZone("new york")), listOf(reykjavik), january).zones.single().id,
        )
        val picked = editing.reduce(SavedPlacesAction.PickZone("Atlantic/Reykjavik"))
        assertEquals("Atlantic/Reykjavik", picked.zoneId)
        assertFalse(picked.zonePicker)
    }

    @Test
    fun theInnermostDialogClosesFirstAndAnEmptyNameCannotBeSaved() {
        val editing = SavedPlacesInput().reduce(SavedPlacesAction.Edit(reykjavik))
        val asking = editing.reduce(SavedPlacesAction.AskDelete)
        assertEquals(editing, asking.reduce(SavedPlacesAction.Dismiss))
        assertEquals(SavedPlacesInput(), editing.reduce(SavedPlacesAction.Dismiss))
        assertFalse(SavedPlacesUiState.from(editing.reduce(SavedPlacesAction.SetName(" ")), listOf(reykjavik), january).canSave)
        assertTrue(SavedPlacesUiState.from(editing, listOf(reykjavik), january).canSave)
    }

    @Test
    fun aPlaceIsEditedInADialog() {
        var input by mutableStateOf(SavedPlacesInput())
        val actions = mutableListOf<SavedPlacesAction>()
        compose.setContent {
            Iso100Theme {
                SavedPlacesContent(SavedPlacesUiState.from(input, listOf(reykjavik), january), {
                    actions += it
                    input =
                        input.reduce(it)
                }, {})
            }
        }
        compose.onNodeWithText("Reykjavík").performClick()
        compose.onNodeWithText("Edit place").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        assertEquals(listOf(SavedPlacesAction.Edit(reykjavik), SavedPlacesAction.Save), actions)
    }

    @Test
    fun withoutPlacesTheScreenSaysHowToSaveOne() {
        compose.setContent { Iso100Theme { SavedPlacesContent(SavedPlacesUiState.from(SavedPlacesInput(), emptyList(), january), {}, {}) } }
        compose.onNodeWithText(
            "No saved places yet. In Sun or Moon, choose a place and search it by name: it is saved here.",
        ).assertIsDisplayed()
    }
}
