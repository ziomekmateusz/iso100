package io.github.ziomekmateusz.iso100.ui.place

import io.github.ziomekmateusz.iso100.data.LocationState
import io.github.ziomekmateusz.iso100.data.SavedPlace
import io.github.ziomekmateusz.iso100.data.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceChoiceTest {
    private val oslo = SavedPlace("oslo", "Oslo", 59.9139, 10.7522, "Europe/Oslo")

    @Test
    fun theFirstTimeThePositionIsAsked() {
        assertTrue(shouldLocate(Settings(), emptyList(), LocationState.Unknown))
    }

    @Test
    fun aSavedPlaceNeedsNoPosition() {
        assertFalse(shouldLocate(Settings(placeId = "oslo"), listOf(oslo), LocationState.Unknown))
    }

    @Test
    fun aDeletedPlaceFallsBackToThePosition() {
        assertTrue(shouldLocate(Settings(placeId = "deleted"), listOf(oslo), LocationState.Unknown))
    }

    @Test
    fun withTheAutomaticPositionOffNothingIsAsked() {
        assertFalse(
            shouldLocate(
                Settings(autoLocation = false),
                emptyList(),
                LocationState.Unknown,
            ),
        )
    }

    @Test
    fun anErrorWaitsForAnExplicitRetry() {
        listOf(
            LocationState.NotFound,
            LocationState.LocationOff,
            LocationState.PermissionDenied,
        ).forEach { error ->
            assertFalse(shouldLocate(Settings(), emptyList(), error))
        }
    }

    @Test
    fun aRunningOrKnownPositionIsNotAskedAgain() {
        listOf(
            LocationState.Locating,
            LocationState.Found(oslo.place, oslo.name),
        ).forEach { state ->
            assertFalse(shouldLocate(Settings(), emptyList(), state))
        }
    }
}
