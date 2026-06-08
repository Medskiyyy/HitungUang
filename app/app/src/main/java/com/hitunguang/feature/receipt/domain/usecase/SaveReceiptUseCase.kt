package com.hitunguang.feature.receipt.domain.usecase

import android.net.Uri
import com.hitunguang.core.filemanager.ReceiptFileManager
import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.model.ReceiptItem
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import java.util.UUID
import javax.inject.Inject

class SaveReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val receiptFileManager: ReceiptFileManager,
    private val createTransactionUseCase: CreateTransactionUseCase
) {
    suspend operator fun invoke(
        tempImageUri: Uri,
        merchantName: String?,
        receiptDate: Long,
        subtotal: Long?,
        tax: Long?,
        total: Long,
        ocrRawText: String?,
        accountId: String,
        categoryId: String?,
        items: List<ParsedItemInput>
    ) {
        val receiptId = UUID.randomUUID().toString()
        val savedFile = receiptFileManager.saveReceiptImage(tempImageUri)
        
        val receipt = Receipt(
            id = receiptId,
            imagePath = savedFile.absolutePath,
            merchantName = merchantName,
            receiptDate = receiptDate,
            subtotal = subtotal,
            tax = tax,
            total = total,
            ocrRawText = ocrRawText,
            createdAt = System.currentTimeMillis()
        )
        
        val receiptItems = items.map { item ->
            ReceiptItem(
                id = UUID.randomUUID().toString(),
                receiptId = receiptId,
                itemName = item.name,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                subtotal = item.subtotal,
                createdAt = System.currentTimeMillis()
            )
        }
        
        receiptRepository.saveReceipt(receipt, receiptItems)
        
        val noteSummary = receiptItems.joinToString("\n") { item ->
            val qtyStr = if (item.quantity != null) "${item.quantity}x " else ""
            "- ${item.itemName} (${qtyStr}Rp ${item.unitPrice})"
        }
        
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            categoryId = categoryId,
            receiptId = receiptId,
            transactionType = "EXPENSE",
            title = merchantName?.ifBlank { "Belanja Struk" } ?: "Belanja Struk",
            note = noteSummary.ifBlank { null },
            amount = total,
            transactionDate = receiptDate,
            isDeleted = false,
            deletedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        createTransactionUseCase(transaction)
    }
}

data class ParsedItemInput(
    val name: String,
    val quantity: Double?,
    val unitPrice: Long,
    val subtotal: Long
)
