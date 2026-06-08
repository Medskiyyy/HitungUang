package com.hitunguang.feature.transaction.domain.model

data class Attachment(
    val id: String,
    val transactionId: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long
)
