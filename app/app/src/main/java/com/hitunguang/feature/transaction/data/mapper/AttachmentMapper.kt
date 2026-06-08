package com.hitunguang.feature.transaction.data.mapper

import com.hitunguang.core.database.entity.AttachmentEntity
import com.hitunguang.feature.transaction.domain.model.Attachment

object AttachmentMapper {
    fun toDomain(entity: AttachmentEntity): Attachment {
        return Attachment(
            id = entity.id,
            transactionId = entity.transactionId,
            filePath = entity.filePath,
            mimeType = entity.mimeType,
            fileSize = entity.fileSize,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(domain: Attachment): AttachmentEntity {
        return AttachmentEntity(
            id = domain.id,
            transactionId = domain.transactionId,
            filePath = domain.filePath,
            mimeType = domain.mimeType,
            fileSize = domain.fileSize,
            createdAt = domain.createdAt
        )
    }
}
