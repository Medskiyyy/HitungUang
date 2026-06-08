package com.hitunguang.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.core.notification.NotificationScheduler
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _settings = MutableStateFlow<NotificationSettings?>(null)
    val settings: StateFlow<NotificationSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getNotificationSettings().collect { currentSettings ->
                if (currentSettings == null) {
                    val defaultSettings = NotificationSettings(
                        id = "default",
                        dailyReminderEnabled = false,
                        dailyReminderTime = "20:00",
                        weeklyReviewEnabled = false,
                        monthlyReviewEnabled = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    settingsRepository.saveNotificationSettings(defaultSettings)
                    _settings.value = defaultSettings
                } else {
                    _settings.value = currentSettings
                }
            }
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        val current = _settings.value ?: return
        val updated = current.copy(
            dailyReminderEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            settingsRepository.saveNotificationSettings(updated)
            notificationScheduler.scheduleReminder(enabled, updated.dailyReminderTime)
        }
    }

    fun updateDailyReminderTime(timeStr: String) {
        val current = _settings.value ?: return
        val updated = current.copy(
            dailyReminderTime = timeStr,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            settingsRepository.saveNotificationSettings(updated)
            if (updated.dailyReminderEnabled) {
                notificationScheduler.scheduleReminder(true, timeStr)
            }
        }
    }

    fun toggleWeeklyReview(enabled: Boolean) {
        val current = _settings.value ?: return
        val updated = current.copy(
            weeklyReviewEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            settingsRepository.saveNotificationSettings(updated)
            notificationScheduler.scheduleWeeklyReview(enabled)
        }
    }

    fun toggleMonthlyReview(enabled: Boolean) {
        val current = _settings.value ?: return
        val updated = current.copy(
            monthlyReviewEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            settingsRepository.saveNotificationSettings(updated)
            notificationScheduler.scheduleMonthlyReview(enabled)
        }
    }
}
