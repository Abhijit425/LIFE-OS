package com.lifeos.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lifeos.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Every primary destination opens a real screen. Runs on phone and tablet emulators. */
@HiltAndroidTest
class NavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun init() = hilt.inject()

    private fun waitFor(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun dashboardShowsOverview() {
        waitFor("Here is your life overview")
        compose.onNodeWithText("Here is your life overview", substring = true).assertIsDisplayed()
    }

    @Test fun primaryTabsNavigate() {
        waitFor("Here is your life overview")
        compose.onAllNodesWithText("Finance").onFirst().performClick()
        waitFor("never connects to your bank")
        compose.onAllNodesWithText("Tasks").onFirst().performClick()
        waitFor("Kanban")
        compose.onAllNodesWithText("LIFE AI").onFirst().performClick()
        waitFor("Command engine arrives")
        compose.onAllNodesWithText("More").onFirst().performClick()
        waitFor("Every part of your life")
    }

    @Test fun moreOpensSettingsAndThemeOptions() {
        waitFor("Here is your life overview")
        compose.onAllNodesWithText("More").onFirst().performClick()
        waitFor("Every part of your life")
        compose.onAllNodesWithText("Settings").onFirst().performClick()
        waitFor("Follow system")
        compose.onNodeWithText("Light").performClick()
        compose.onNodeWithText("Dark (default)").performClick()
    }
}
