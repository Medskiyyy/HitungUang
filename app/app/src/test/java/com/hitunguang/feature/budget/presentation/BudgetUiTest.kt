package com.hitunguang.feature.budget.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hitunguang.feature.budget.domain.model.Budget
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BudgetUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBudgetProgressCard_rendersAndTriggersCallbacks() {
        var editClicked = false
        var deleteClicked = false
        val mockBudget = Budget(
            id = "bg-1",
            categoryId = null,
            budgetType = "GLOBAL",
            amount = 1000000L,
            thresholdPercent = 80,
            startDate = 1717804800000L, // 8 June 2026
            endDate = 1720396800000L,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val mockProgress = BudgetWithProgress(
            budget = mockBudget,
            categoryName = null,
            spentAmount = 500000L,
            remainingAmount = 500000L,
            progressPercent = 50f,
            isOverBudget = false,
            isThresholdReached = false
        )

        composeTestRule.setContent {
            BudgetProgressCard(
                budgetProgress = mockProgress,
                onEditClick = { editClicked = true },
                onDeleteClick = { deleteClicked = true }
            )
        }

        // Verify content
        composeTestRule.onNodeWithText("Anggaran Global").assertExists()
        composeTestRule.onNodeWithText("Terpakai: Rp 500.000 / Rp 1.000.000").assertExists()
        composeTestRule.onNodeWithText("Sisa limit: Rp 500.000").assertExists()

        // Test buttons
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        assertTrue(editClicked)

        composeTestRule.onNodeWithContentDescription("Hapus").performClick()
        assertTrue(deleteClicked)
    }
}
