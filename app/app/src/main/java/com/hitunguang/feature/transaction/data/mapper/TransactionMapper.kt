package com.hitunguang.feature.transaction.data.mapper

import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.feature.transaction.domain.model.Transaction

object TransactionMapper {
    fun toDomain(entity: TransactionEntity): Transaction {
        return Transaction(
            id = entity.id,
            accountId = entity.accountId,
            categoryId = entity.categoryId,
            receiptId = entity.receiptId,
            transactionType = entity.transactionType,
            title = entity.title,
            note = entity.note,
            amount = entity.amount,
            transactionDate = entity.transactionDate,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Transaction): TransactionEntity {
        return TransactionEntity(
            id = domain.id,
            accountId = domain.accountId,
            categoryId = domain.categoryId,
            receiptId = domain.receiptId,
            transactionType = domain.transactionType,
            title = domain.title,
            note = domain.note,
            amount = domain.amount,
            transactionDate = domain.transactionDate,
            isDeleted = domain.isDeleted,
            deletedAt = domain.deletedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomainWithDetails(entity: com.hitunguang.core.database.entity.TransactionWithDetailsEntity): com.hitunguang.feature.transaction.domain.model.TransactionWithDetails {
        return com.hitunguang.feature.transaction.domain.model.TransactionWithDetails(
            id = entity.transaction.id,
            accountId = entity.transaction.accountId,
            accountName = entity.accountName,
            categoryId = entity.transaction.categoryId,
            categoryName = entity.categoryName,
            receiptId = entity.transaction.receiptId,
            transactionType = entity.transaction.transactionType,
            title = entity.transaction.title,
            note = entity.transaction.note,
            amount = entity.transaction.amount,
            transactionDate = entity.transaction.transactionDate,
            isDeleted = entity.transaction.isDeleted,
            deletedAt = entity.transaction.deletedAt,
            createdAt = entity.transaction.createdAt,
            updatedAt = entity.transaction.updatedAt
        )
    }
}
