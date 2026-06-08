package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.repository.AttachmentRepository
import javax.inject.Inject

class DeleteAttachmentUseCase @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val fileManager: AttachmentFileManager
) {
    suspend operator fun invoke(attachment: Attachment) {
        fileManager.deleteFile(attachment.filePath)
        attachmentRepository.deleteAttachment(attachment)
    }
}
