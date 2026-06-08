package com.hitunguang.feature.transfer.domain.model

data class Transfer(
    val id: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Long,
    val adminFee: Long,
    val note: String?,
    val transferDate: Long,
    val createdAt: Long,
    val updatedAt: Long
)
