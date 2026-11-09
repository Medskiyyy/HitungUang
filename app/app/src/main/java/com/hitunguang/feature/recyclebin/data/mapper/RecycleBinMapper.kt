package com.hitunguang.feature.recyclebin.data.mapper

import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.database.entity.DeletedItemWithDetailsEntity
import com.hitunguang.feature.recyclebin.domain.model.RecycleBinItem
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.R

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
        var titleRes: Int? = null
        var subtitleRes: Int? = null
        var subtitleArgRes: Int? = null
        val amountText: String?
        val isExpense: Boolean

        when (entity.entityType) {
            "TRANSACTION" -> {
                title = entity.transaction?.title.orEmpty()
                if (entity.transaction?.title == null) titleRes = R.string.recycle_item_transaction_deleted
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
                amountText = "${if (isTxExpense) "-" else "+"}${CurrencyFormatter.format(amount)}"
            }
            "CATEGORY" -> {
                title = entity.category?.name.orEmpty()
                if (entity.category?.name == null) titleRes = R.string.recycle_item_category_deleted
                subtitleArgRes = when (entity.category?.categoryType) {
                    "INCOME" -> R.string.recycle_category_income
                    "EXPENSE" -> R.string.recycle_category_expense
                    else -> R.string.recycle_category_transfer
                }
                subtitleRes = R.string.recycle_subtitle_category
                subtitle = ""
                amountText = null
                isExpense = entity.category?.categoryType != "INCOME"
            }
            "WALLET" -> {
                title = entity.account?.name.orEmpty()
                if (entity.account?.name == null) titleRes = R.string.recycle_item_wallet_deleted
                subtitleArgRes = when (entity.account?.accountType) {
                    "CASH" -> R.string.recycle_wallet_cash
                    "BANK" -> R.string.recycle_wallet_bank
                    "E_WALLET" -> R.string.recycle_wallet_ewallet
                    else -> R.string.recycle_other
                }
                subtitleRes = R.string.recycle_subtitle_wallet
                subtitle = ""
                val balance = entity.account?.currentBalance ?: 0L
                amountText = CurrencyFormatter.format(balance)
                isExpense = false
            }
            "BUDGET" -> {
                title = entity.budgetCategoryName.orEmpty()
                titleRes = if (entity.budgetCategoryName != null) R.string.recycle_item_budget_named else R.string.recycle_item_budget
                subtitleArgRes = when (entity.budget?.budgetType) {
                    "MONTHLY" -> R.string.recycle_budget_monthly
                    "WEEKLY" -> R.string.recycle_budget_weekly
                    else -> R.string.recycle_other
                }
                subtitleRes = R.string.recycle_subtitle_budget
                subtitle = ""
                val amount = entity.budget?.amount ?: 0L
                amountText = CurrencyFormatter.format(amount)
                isExpense = true
            }
            else -> {
                title = ""
                titleRes = R.string.recycle_item_data_deleted
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
            titleRes = titleRes,
            subtitleRes = subtitleRes,
            subtitleArgRes = subtitleArgRes,
            amountText = amountText,
            isExpense = isExpense,
            transactionDetails = transactionDetails,
            isDefault = if (entity.entityType == "CATEGORY") entity.category?.isDefault ?: false else false
        )
    }
}
