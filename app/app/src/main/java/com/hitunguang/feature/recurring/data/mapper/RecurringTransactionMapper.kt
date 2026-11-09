package com.hitunguang.feature.recurring.data.mapper

import com.hitunguang.core.database.entity.RecurringTransactionEntity
import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction

object RecurringTransactionMapper {
    fun toDomain(entity: RecurringTransactionEntity): RecurringTransaction {
        return RecurringTransaction(
            id = entity.id,
            accountId = entity.accountId,
            categoryId = entity.categoryId,
            transactionType = entity.transactionType,
            title = entity.title,
            note = entity.note,
            amount = entity.amount,
            recurrenceRule = RecurrenceRule.parse(entity.recurrenceRule),
            nextRunAt = entity.nextRunAt,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: RecurringTransaction): RecurringTransactionEntity {
        return RecurringTransactionEntity(
            id = domain.id,
            accountId = domain.accountId,
            categoryId = domain.categoryId,
            transactionType = domain.transactionType,
            title = domain.title,
            note = domain.note,
            amount = domain.amount,
            recurrenceRule = domain.recurrenceRule.encode(),
            nextRunAt = domain.nextRunAt,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
