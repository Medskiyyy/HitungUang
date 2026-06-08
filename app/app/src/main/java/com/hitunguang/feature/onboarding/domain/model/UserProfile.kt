package com.hitunguang.feature.onboarding.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val occupation: String?,
    val createdAt: Long,
    val updatedAt: Long
)
