package com.hitunguang.feature.category.domain.model

data class Category(
    val id: String,
    val name: String,
    val categoryType: String,
    val icon: String?,
    val isDefault: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
