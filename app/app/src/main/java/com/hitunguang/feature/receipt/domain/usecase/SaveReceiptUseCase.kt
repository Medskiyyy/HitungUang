package com.hitunguang.feature.receipt.domain.usecase

import com.hitunguang.core.common.util.CurrencyFormatter
import android.net.Uri
import androidx.room.withTransaction
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.dao.AccountDao
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
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val accountDao: AccountDao,
    private val database: HitungUangDatabase
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
        // Same rule as CreateTransactionUseCase, checked before any file or row is written
        // so a rejected save leaves nothing behind and can simply be retried.
        val currentBalance = accountDao.getAccountBalance(accountId)
        if (currentBalance < total) {
            throw InsufficientBalanceException(currentBalance, total)
        }

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
        
        val noteSummary = receiptItems.joinToString("\n") { item ->
            val qtyStr = if (item.quantity != null) "${item.quantity}x " else ""
            "- ${item.itemName} (${qtyStr}${CurrencyFormatter.format(item.unitPrice)})"
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
        
        // Receipt, items and transaction are committed together or not at all.
        try {
            database.withTransaction {
                receiptRepository.saveReceipt(receipt, receiptItems)
                createTransactionUseCase(transaction)
            }
        } catch (e: Exception) {
            receiptFileManager.deleteFile(savedFile.absolutePath)
            throw e
        }
        receiptFileManager.deleteTempFileIfNeeded(tempImageUri)
    }
}

class InsufficientBalanceException(
    val currentBalance: Long,
    val required: Long
) : IllegalStateException("Saldo nggak cukup. Saldo sekarang: ${CurrencyFormatter.format(currentBalance)}, butuh: ${CurrencyFormatter.format(required)}.")

data class ParsedItemInput(
    val name: String,
    val quantity: Double?,
    val unitPrice: Long,
    val subtotal: Long
)
