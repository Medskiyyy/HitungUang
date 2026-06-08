package com.hitunguang.feature.backup.presentation

data class BackupUiState(
    val isLoading: Boolean = false,
    val backupFolderUri: String? = null,
    val lastBackupAt: Long? = null,
    val backupFrequency: String = "WEEKLY",
    val autoBackupEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val showRestoreConfirmDialog: Boolean = false,
    val pendingRestoreUri: android.net.Uri? = null
)
