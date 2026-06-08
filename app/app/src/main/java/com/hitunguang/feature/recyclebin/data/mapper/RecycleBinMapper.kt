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
        return RecycleBinItem(
            id = entity.recycleBinId,
            entityType = entity.entityType,
            entityId = entity.entityId,
            deletedAt = entity.deletedAt,
            expireAt = entity.expireAt,
            transactionDetails = transactionDetails
        )
    }
}
