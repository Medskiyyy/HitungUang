package com.hitunguang.feature.account.domain.model

data class Account(
    val id: String,
    val name: String,
    val accountType: String,
    val icon: String?,
    val initialBalance: Long,
    val currentBalance: Long,
    val createdAt: Long,
    val updatedAt: Long
)
