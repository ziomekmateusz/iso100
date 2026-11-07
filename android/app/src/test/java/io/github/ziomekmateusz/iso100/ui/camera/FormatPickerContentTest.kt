package io.github.ziomekmateusz.iso100.ui.camera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.data.Settings
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class FormatPickerContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSheetMarksTheFormatChosenAloneAndTappingAFormatChoosesIt() {
        val chosen = mutableListOf<String>()
        compose.setContent {
            Iso100Theme {
                FormatPickerContent(
                    formatRows(
                        TEST_CATALOG,
                        Settings(formatId = "full-frame")
                    ), onChoose = { chosen += it }, onDismiss = {})
            }
        }
        compose.onNodeWithText("Sensor format").assertIsDisplayed()
        compose.onNodeWithText("Full Frame (35 mm)").assertIsSelected()
        compose.onNodeWithText("APS-C").performClick()
        assertEquals(listOf("aps-c"), chosen)
    }
}
