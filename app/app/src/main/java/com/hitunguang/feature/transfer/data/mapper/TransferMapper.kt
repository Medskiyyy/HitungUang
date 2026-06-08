package com.hitunguang.feature.transfer.data.mapper

import com.hitunguang.core.database.entity.TransferEntity
import com.hitunguang.feature.transfer.domain.model.Transfer

object TransferMapper {
    fun toDomain(entity: TransferEntity): Transfer {
        return Transfer(
            id = entity.id,
            fromAccountId = entity.fromAccountId,
            toAccountId = entity.toAccountId,
            amount = entity.amount,
            adminFee = entity.adminFee,
            note = entity.note,
            transferDate = entity.transferDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Transfer): TransferEntity {
        return TransferEntity(
            id = domain.id,
            fromAccountId = domain.fromAccountId,
            toAccountId = domain.toAccountId,
            amount = domain.amount,
            adminFee = domain.adminFee,
            note = domain.note,
            transferDate = domain.transferDate,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
