package com.hitunguang.feature.receipt.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hitunguang.feature.receipt.domain.model.Receipt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OcrUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testReceiptCard_rendersAndTriggersCallbacks() {
        var clicked = false
        var deleted = false
        val mockReceipt = Receipt(
            id = "rc-1",
            imagePath = "/fake/receipt.jpg",
            merchantName = "Indomaret",
            receiptDate = 1717804800000L, // 8 June 2026
            subtotal = 45000L,
            tax = 4500L,
            total = 49500L,
            ocrRawText = "Raw",
            createdAt = System.currentTimeMillis()
        )
        val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))

        composeTestRule.setContent {
            ReceiptCard(
                receipt = mockReceipt,
                dateFormatter = dateFormatter,
                onClick = { clicked = true },
                onDelete = { deleted = true }
            )
        }

        // Verify content
        composeTestRule.onNodeWithText("Indomaret").assertExists()
        composeTestRule.onNodeWithText("Total: Rp 49500").assertExists()

        // Test click callback
        composeTestRule.onNodeWithText("Indomaret").performClick()
        assertTrue(clicked)

        // Test delete callback
        composeTestRule.onNodeWithContentDescription("Hapus Struk").performClick()
        assertTrue(deleted)
    }
}
