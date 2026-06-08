package com.hitunguang.feature.budget.domain.model

data class Budget(
    val id: String,
    val categoryId: String?,
    val budgetType: String,
    val amount: Long,
    val thresholdPercent: Int,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
