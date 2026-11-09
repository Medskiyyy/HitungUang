package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.feature.transaction.domain.model.UserStateException
import com.hitunguang.R
import android.net.Uri
import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.repository.AttachmentRepository
import java.util.UUID
import javax.inject.Inject

class AddAttachmentUseCase @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val fileManager: AttachmentFileManager
) {
    suspend operator fun invoke(transactionId: String, uri: Uri): Attachment {
        val currentCount = attachmentRepository.getAttachmentCount(transactionId)
        if (currentCount >= 5) {
            throw UserStateException(R.string.attachment_max_reached, fallbackMessage = "Maksimal 5 lampiran per transaksi.")
        }

        val savedFile = fileManager.saveAttachment(uri)
        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId,
            filePath = savedFile.absolutePath,
            mimeType = "image/jpeg",
            fileSize = savedFile.length(),
            createdAt = System.currentTimeMillis()
        )

        attachmentRepository.addAttachment(attachment)
        return attachment
    }
}
