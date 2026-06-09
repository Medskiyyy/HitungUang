package com.hitunguang.feature.recyclebin.data.mapper

import com.hitunguang.core.database.entity.DeletedItemWithDetailsEntity
import com.hitunguang.feature.recyclebin.domain.model.RecycleBinItem
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

object RecycleBinMapper {
    fun toDomain(entity: DeletedItemWithDetailsEntity): RecycleBinItem {
        val transactionDetails = entity.transaction?.let { tx ->
            TransactionWithDetails(
                id = tx.id,
                accountId = tx.accountId,
                accountName = entity.accountName.orEmpty(),
                categoryId = tx.categoryId,
                categoryName = entity.categoryName,
                categoryIcon = entity.category?.icon,
                receiptId = tx.receiptId,
                transactionType = tx.transactionType,
                title = tx.title,
                note = tx.note,
                amount = tx.amount,
                transactionDate = tx.transactionDate,
                isDeleted = tx.isDeleted,
                deletedAt = tx.deletedAt,
                createdAt = tx.createdAt,
                updatedAt = tx.updatedAt
            )
        }

        val title: String
        val subtitle: String
        val amountText: String?
        val isExpense: Boolean

        when (entity.entityType) {
            "TRANSACTION" -> {
                title = entity.transaction?.title ?: "Transaksi Terhapus"
                val accName = entity.accountName.orEmpty()
                val catName = entity.categoryName
                subtitle = if (accName.isNotEmpty() && !catName.isNullOrEmpty()) {
                    "$accName • $catName"
                } else {
                    accName.ifEmpty { catName.orEmpty() }
                }
                val isTxExpense = entity.transaction?.let {
                    it.transactionType == "EXPENSE" || it.transactionType == "TRANSFER_FEE"
                } ?: true
                isExpense = isTxExpense
                val amount = entity.transaction?.amount ?: 0L
                amountText = "${if (isTxExpense) "-" else "+"} Rp $amount"
            }
            "CATEGORY" -> {
                title = entity.category?.name ?: "Kategori Terhapus"
                val typeText = when (entity.category?.categoryType) {
                    "INCOME" -> "Pemasukan"
                    "EXPENSE" -> "Pengeluaran"
                    else -> "Transfer"
                }
                subtitle = "Kategori • $typeText"
                amountText = null
                isExpense = entity.category?.categoryType != "INCOME"
            }
            "WALLET" -> {
                title = entity.account?.name ?: "Dompet Terhapus"
                val walletType = when (entity.account?.accountType) {
                    "CASH" -> "Tunai"
                    "BANK" -> "Bank"
                    "E_WALLET" -> "E-Wallet"
                    else -> "Lainnya"
                }
                subtitle = "Dompet/Akun • $walletType"
                val balance = entity.account?.currentBalance ?: 0L
                amountText = "Rp $balance"
                isExpense = false
            }
            "BUDGET" -> {
                title = entity.budgetCategoryName?.let { "Anggaran $it" } ?: "Anggaran"
                val budgetType = when (entity.budget?.budgetType) {
                    "MONTHLY" -> "Bulanan"
                    "WEEKLY" -> "Mingguan"
                    else -> "Lainnya"
                }
                subtitle = "Anggaran • $budgetType"
                val amount = entity.budget?.amount ?: 0L
                amountText = "Rp $amount"
                isExpense = true
            }
            else -> {
                title = "Item Terhapus"
                subtitle = ""
                amountText = null
                isExpense = false
            }
        }

        return RecycleBinItem(
            id = entity.recycleBinId,
            entityType = entity.entityType,
            entityId = entity.entityId,
            deletedAt = entity.deletedAt,
            expireAt = entity.expireAt,
            title = title,
            subtitle = subtitle,
            amountText = amountText,
            isExpense = isExpense,
            transactionDetails = transactionDetails,
            isDefault = if (entity.entityType == "CATEGORY") entity.category?.isDefault ?: false else false
        )
    }
}
