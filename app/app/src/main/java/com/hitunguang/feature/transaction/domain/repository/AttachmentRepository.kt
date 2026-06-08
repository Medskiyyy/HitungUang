package com.hitunguang.feature.transaction.domain.repository

import com.hitunguang.feature.transaction.domain.model.Attachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun getAttachmentsByTransactionId(transactionId: String): Flow<List<Attachment>>
    suspend fun getAttachmentCount(transactionId: String): Int
    suspend fun addAttachment(attachment: Attachment)
    suspend fun deleteAttachment(attachment: Attachment)
}
