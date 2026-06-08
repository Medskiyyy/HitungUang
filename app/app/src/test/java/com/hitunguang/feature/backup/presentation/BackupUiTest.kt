package com.hitunguang.feature.backup.presentation

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BackupUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFrequencyOption_rendersAndTriggersClick() {
        var clicked = false
        composeTestRule.setContent {
            FrequencyOption(
                label = "Harian",
                selected = true,
                onClick = { clicked = true }
            )
        }

        // Verify label text
        composeTestRule.onNodeWithText("Harian").assertExists()

        // Perform click on the clickable node (the RadioButton)
        composeTestRule.onNode(hasClickAction()).performClick()
        assertTrue(clicked)
    }
}
