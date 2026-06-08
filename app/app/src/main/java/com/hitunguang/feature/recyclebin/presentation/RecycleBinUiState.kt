package com.hitunguang.feature.recyclebin.presentation

import com.hitunguang.feature.recyclebin.domain.model.RecycleBinItem

data class RecycleBinUiState(
    val isLoading: Boolean = false,
    val items: List<RecycleBinItem> = emptyList(),
    val error: String? = null
)
