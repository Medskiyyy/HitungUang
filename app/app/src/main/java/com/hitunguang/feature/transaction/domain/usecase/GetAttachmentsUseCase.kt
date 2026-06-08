package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAttachmentsUseCase @Inject constructor(
    private val attachmentRepository: AttachmentRepository
) {
    operator fun invoke(transactionId: String): Flow<List<Attachment>> {
        return attachmentRepository.getAttachmentsByTransactionId(transactionId)
    }
}
