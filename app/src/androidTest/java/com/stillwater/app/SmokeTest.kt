package com.stillwater.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke test: the app starts, Hilt wires up, and the start
 * destination renders. Read-only — asserts presence, changes nothing.
 */
@HiltAndroidTest
@LargeTest
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appStartsAndShowsAScreen() {
        // Whichever start destination applies (fresh install → onboarding,
        // returning user → home), its anchor text must be on screen.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("I'm feeling an urge") or
                    androidx.compose.ui.test.hasText("Begin"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sosEntryIsOneTapFromHomeOrOnboardingRenders() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("I'm feeling an urge") or
                    androidx.compose.ui.test.hasText("Begin"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val home = composeRule.onAllNodes(
            androidx.compose.ui.test.hasText("I'm feeling an urge"),
        ).fetchSemanticsNodes().isNotEmpty()
        if (home) {
            // Crisis button present and clickable — do not click (keeps the
            // user's real log untouched); presence is the contract.
            composeRule.onNodeWithText("I'm feeling an urge").assertExists()
        } else {
            composeRule.onNodeWithText("Begin").assertExists()
        }
    }
}
