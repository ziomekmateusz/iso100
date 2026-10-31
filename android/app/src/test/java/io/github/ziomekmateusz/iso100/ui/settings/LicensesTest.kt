package io.github.ziomekmateusz.iso100.ui.settings

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class LicensesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyLicenseHasItsTextInTheAssets() {
        val assets = ApplicationProvider.getApplicationContext<Application>().assets
        val expected = mapOf(
            OpenLicense.APACHE to "Apache License",
            OpenLicense.BSD to "Copyright 2008 Google Inc.",
            OpenLicense.CC_BY_SA to "Attribution-ShareAlike 3.0",
            OpenLicense.OFL to "SIL OPEN FONT LICENSE",
        )
        OpenLicense.entries.forEach { license ->
            val text = assets.open("licenses/${license.file}").bufferedReader().use { it.readText() }
            assertTrue(license.file, expected.getValue(license) in text)
        }
    }

    @Test
    fun aRowOpensItsLicense() {
        val opened = mutableListOf<OpenLicense>()
        compose.setContent { Iso100Theme { LicensesScreen({}, { opened += it }) } }
        compose.onNodeWithText("SIL Open Font License").performClick()
        assertEquals(listOf(OpenLicense.OFL), opened)
    }
}
