package com.hitunguang.feature.account.data.mapper

import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.feature.account.domain.model.Account

object AccountMapper {
    fun toDomain(entity: AccountEntity): Account {
        return Account(
            id = entity.id,
            name = entity.name,
            accountType = entity.accountType,
            icon = entity.icon,
            initialBalance = entity.initialBalance,
            currentBalance = entity.currentBalance,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Account): AccountEntity {
        return AccountEntity(
            id = domain.id,
            name = domain.name,
            accountType = domain.accountType,
            icon = domain.icon,
            initialBalance = domain.initialBalance,
            currentBalance = domain.currentBalance,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
