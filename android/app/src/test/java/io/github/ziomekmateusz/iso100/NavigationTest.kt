package io.github.ziomekmateusz.iso100

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ziomekmateusz.iso100.ui.Iso100App
import io.github.ziomekmateusz.iso100.ui.theme.Iso100Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun aToolOpensFromHomeAndBackClosesIt() {
        compose.onNodeWithText("Depth of field").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Equivalent").assertIsDisplayed()
    }

    @Test
    fun theOpenScreenSurvivesTheActivityRecreation() {
        compose.onNodeWithText("Stars").performClick()
        compose.onNodeWithText("Stars without trails").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Stars without trails").assertIsDisplayed()
    }

    @Test
    fun aLicenseOpensFromSettingsAndSurvivesTheActivityRecreation() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Licences").performScrollTo().performClick()
        compose.onNodeWithText("SIL Open Font License").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("SIL Open Font License").assertIsDisplayed()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Version 1.1", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}

@RunWith(AndroidJUnit4::class)
class StateRestorationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theBackStackIsSavedAndRestored() {
        val tester = StateRestorationTester(compose)
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        tester.setContent { CompositionLocalProvider(LocalAppContainer provides container) { Iso100Theme { Iso100App() } } }
        compose.onNodeWithContentDescription("Settings").performClick()
        tester.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }
}
