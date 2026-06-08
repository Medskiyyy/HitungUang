package com.hitunguang.feature.transaction.data.repository

import com.hitunguang.core.database.dao.AttachmentDao
import com.hitunguang.feature.transaction.data.mapper.AttachmentMapper
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AttachmentRepositoryImpl @Inject constructor(
    private val attachmentDao: AttachmentDao
) : AttachmentRepository {
    override fun getAttachmentsByTransactionId(transactionId: String): Flow<List<Attachment>> {
        return attachmentDao.getAttachmentsByTransactionId(transactionId).map { list ->
            list.map { AttachmentMapper.toDomain(it) }
        }
    }

    override suspend fun getAttachmentCount(transactionId: String): Int {
        return attachmentDao.getAttachmentCountForTransaction(transactionId)
    }

    override suspend fun addAttachment(attachment: Attachment) {
        attachmentDao.insertAttachment(AttachmentMapper.toEntity(attachment))
    }

    override suspend fun deleteAttachment(attachment: Attachment) {
        attachmentDao.deleteAttachment(AttachmentMapper.toEntity(attachment))
    }
}
