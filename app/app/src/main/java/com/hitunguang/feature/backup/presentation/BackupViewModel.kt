package com.hitunguang.feature.backup.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hitunguang.core.backup.worker.DailyBackupWorker
import com.hitunguang.core.backup.worker.WeeklyBackupWorker
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.backup.domain.usecase.CreateBackupUseCase
import com.hitunguang.feature.backup.domain.usecase.GetBackupSettingsUseCase
import com.hitunguang.feature.backup.domain.usecase.RestoreBackupUseCase
import com.hitunguang.feature.backup.domain.usecase.UpdateBackupSettingsUseCase
import com.hitunguang.feature.settings.domain.model.BackupSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val getBackupSettingsUseCase: GetBackupSettingsUseCase,
    private val updateBackupSettingsUseCase: UpdateBackupSettingsUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState(isLoading = true))
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.backupSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        backupFolderUri = settings?.backupFolderUri,
                        lastBackupAt = settings?.lastBackupAt,
                        backupFrequency = settings?.backupFrequency ?: "WEEKLY",
                        autoBackupEnabled = settings?.autoBackupEnabled ?: false
                    )
                }
            }
        }
    }

    fun onFolderSelected(folderUri: String) {
        viewModelScope.launch {
            val existing = getBackupSettingsUseCase()
            val now = System.currentTimeMillis()
            val updated = existing?.copy(backupFolderUri = folderUri, updatedAt = now)
                ?: BackupSettings(
                    id = "backup_settings",
                    backupFolderUri = folderUri,
                    backupFrequency = "WEEKLY",
                    autoBackupEnabled = false,
                    lastBackupAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            updateBackupSettingsUseCase(updated)
        }
    }

    fun onBackupNow() {
        val folderUri = _uiState.value.backupFolderUri ?: run {
            _uiState.update { it.copy(errorMessage = "Pilih folder backup terlebih dahulu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, errorMessage = null, successMessage = null) }
            createBackupUseCase(folderUri).fold(
                onSuccess = { fileName ->
                    _uiState.update { it.copy(isBackingUp = false, successMessage = "Backup berhasil: $fileName") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isBackingUp = false, errorMessage = error.message ?: "Backup gagal.") }
                }
            )
        }
    }

    fun onRestoreFileSelected(zipUri: Uri) {
        _uiState.update {
            it.copy(showRestoreConfirmDialog = true, pendingRestoreUri = zipUri)
        }
    }

    fun onRestoreConfirmed() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update { it.copy(showRestoreConfirmDialog = false, pendingRestoreUri = null, isRestoring = true) }
        viewModelScope.launch {
            restoreBackupUseCase(uri).onFailure { error ->
                _uiState.update { it.copy(isRestoring = false, errorMessage = error.message ?: "Restore gagal.") }
            }
            // On success, app will restart automatically
        }
    }

    fun onRestoreDismissed() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false, pendingRestoreUri = null) }
    }

    fun onFrequencyChanged(frequency: String) {
        viewModelScope.launch {
            val existing = getBackupSettingsUseCase()
            val now = System.currentTimeMillis()
            val updated = existing?.copy(backupFrequency = frequency, updatedAt = now)
                ?: BackupSettings(
                    id = "backup_settings",
                    backupFolderUri = null,
                    backupFrequency = frequency,
                    autoBackupEnabled = false,
                    lastBackupAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            updateBackupSettingsUseCase(updated)
            rescheduleWorkers(frequency, updated.autoBackupEnabled)
        }
    }

    fun onAutoBackupToggled(enabled: Boolean) {
        viewModelScope.launch {
            val existing = getBackupSettingsUseCase()
            val now = System.currentTimeMillis()
            val updated = existing?.copy(autoBackupEnabled = enabled, updatedAt = now)
                ?: BackupSettings(
                    id = "backup_settings",
                    backupFolderUri = null,
                    backupFrequency = "WEEKLY",
                    autoBackupEnabled = enabled,
                    lastBackupAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            updateBackupSettingsUseCase(updated)
            rescheduleWorkers(updated.backupFrequency, enabled)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    private fun rescheduleWorkers(frequency: String, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()

        if (!enabled) {
            workManager.cancelUniqueWork("DailyBackupWork")
            workManager.cancelUniqueWork("WeeklyBackupWork")
            return
        }

        when (frequency) {
            "DAILY", "REALTIME" -> {
                workManager.cancelUniqueWork("WeeklyBackupWork")
                val dailyRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints).build()
                workManager.enqueueUniquePeriodicWork(
                    "DailyBackupWork",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    dailyRequest
                )
            }
            "WEEKLY" -> {
                workManager.cancelUniqueWork("DailyBackupWork")
                val weeklyRequest = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(7, TimeUnit.DAYS)
                    .setConstraints(constraints).build()
                workManager.enqueueUniquePeriodicWork(
                    "WeeklyBackupWork",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    weeklyRequest
                )
            }
        }
    }
}
