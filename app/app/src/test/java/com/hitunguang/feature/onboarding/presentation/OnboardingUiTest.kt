package com.hitunguang.feature.onboarding.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hitunguang.feature.onboarding.presentation.components.ProfileStep
import com.hitunguang.feature.onboarding.presentation.components.WelcomeStep
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OnboardingUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWelcomeStep_rendersAndTriggersStart() {
        var startClicked = false
        composeTestRule.setContent {
            WelcomeStep(
                onStartClick = { startClicked = true }
            )
        }

        // Verify text exists
        composeTestRule.onNodeWithText("Selamat Datang di HitungUang").assertExists()
        composeTestRule.onNodeWithText("Mulai Sekarang").assertExists()

        // Perform click and verify callback
        composeTestRule.onNodeWithText("Mulai Sekarang").performClick()
        assertTrue(startClicked)
    }

    @Test
    fun testProfileStep_rendersInputAndTriggersNext() {
        var nextClicked = false
        var backClicked = false
        var enteredName = ""
        var enteredOccupation = ""

        composeTestRule.setContent {
            ProfileStep(
                name = enteredName,
                nameError = null,
                occupation = enteredOccupation,
                onNameChange = { enteredName = it },
                onOccupationChange = { enteredOccupation = it },
                onNextClick = { nextClicked = true },
                onBackClick = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Siapa Anda?").assertExists()
        composeTestRule.onNodeWithText("Lanjut").performClick()
        assertTrue(nextClicked)

        composeTestRule.onNodeWithText("Kembali").performClick()
        assertTrue(backClicked)
    }
}
