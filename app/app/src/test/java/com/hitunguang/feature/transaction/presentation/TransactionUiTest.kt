package com.hitunguang.feature.transaction.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.presentation.components.DeleteTransactionDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TransactionUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDeleteTransactionDialog_rendersAndTriggersCallbacks() {
        var dismissClicked = false
        var confirmClicked = false
        val mockTx = TransactionWithDetails(
            id = "tx-1",
            accountId = "acc-1",
            accountName = "Dompet Tunai",
            categoryId = "cat-1",
            categoryName = "Makanan",
            categoryIcon = "restaurant",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Nasi Goreng",
            note = "Pedas",
            amount = 15000L,
            transactionDate = System.currentTimeMillis(),
            isDeleted = false,
            deletedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            DeleteTransactionDialog(
                transaction = mockTx,
                onDismiss = { dismissClicked = true },
                onConfirm = { confirmClicked = true }
            )
        }

        // Verify text content
        composeTestRule.onNodeWithText("Hapus transaksi?").assertExists()
        composeTestRule.onNodeWithText("Hapus \"Nasi Goreng\"? Saldo dompet akan dikembalikan. Transaksinya masuk Tempat Sampah dan bisa dipulihkan dalam 30 hari.").assertExists()

        // Test buttons
        composeTestRule.onNodeWithText("Batal").performClick()
        assertTrue(dismissClicked)

        composeTestRule.onNodeWithText("Hapus").performClick()
        assertTrue(confirmClicked)
    }
}
