package com.hitunguang.feature.onboarding.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.onboarding.domain.model.UserProfile
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Reduced from 8 steps → 6 steps (removed NOTIFICATION and BACKUP from onboarding).
 * These settings are better discovered contextually in-app rather than forced up-front.
 */
enum class OnboardingStep {
    WELCOME,
    PROFILE,
    ACCOUNT,
    BUDGET,
    SECURITY,
    TUTORIAL,
}

data class AccountDraft(
    val name: String,
    val type: String, // CASH, BANK, E_WALLET, etc.
    val icon: String,
    val initialBalance: Long
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val totalSteps: Int = 6,
    val name: String = "",
    val nameError: String? = null,
    val occupation: String = "",
    val accounts: List<AccountDraft> = listOf(AccountDraft("Dompet Utama", "CASH", "wallet", 0L)),
    val budgetAmount: String = "",
    val pin: String = "",
    val pinError: String? = null,
    val confirmPin: String = "",
    val confirmPinError: String? = null,
    val isPinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val recoveryCode: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileRepository: UserProfileRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Current 1-based step index for progress indicators */
    val currentStepIndex: Int
        get() = _uiState.value.currentStep.ordinal + 1

    fun nextStep() {
        val currentState = _uiState.value
        when (currentState.currentStep) {
            OnboardingStep.WELCOME -> {
                updateStep(OnboardingStep.PROFILE)
            }
            OnboardingStep.PROFILE -> {
                if (currentState.name.isBlank()) {
                    _uiState.update { it.copy(nameError = "Nama tidak boleh kosong") }
                } else {
                    _uiState.update { it.copy(nameError = null) }
                    updateStep(OnboardingStep.ACCOUNT)
                }
            }
            OnboardingStep.ACCOUNT -> {
                updateStep(OnboardingStep.BUDGET)
            }
            OnboardingStep.BUDGET -> {
                updateStep(OnboardingStep.SECURITY)
            }
            OnboardingStep.SECURITY -> {
                if (currentState.isPinEnabled) {
                    if (currentState.pin.length < 4) {
                        _uiState.update { it.copy(pinError = "PIN minimal 4 angka") }
                    } else if (currentState.pin != currentState.confirmPin) {
                        _uiState.update { it.copy(confirmPinError = "PIN konfirmasi tidak cocok", pinError = null) }
                    } else {
                        _uiState.update { it.copy(pinError = null, confirmPinError = null) }
                        updateStep(OnboardingStep.TUTORIAL)
                    }
                } else {
                    updateStep(OnboardingStep.TUTORIAL)
                }
            }
            OnboardingStep.TUTORIAL -> {
                completeOnboarding()
            }
        }
    }

    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        val prev = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.PROFILE -> OnboardingStep.WELCOME
            OnboardingStep.ACCOUNT -> OnboardingStep.PROFILE
            OnboardingStep.BUDGET -> OnboardingStep.ACCOUNT
            OnboardingStep.SECURITY -> OnboardingStep.BUDGET
            OnboardingStep.TUTORIAL -> OnboardingStep.SECURITY
        }
        updateStep(prev)
    }

    private fun updateStep(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun skipBudget() {
        _uiState.update { it.copy(budgetAmount = "") }
        updateStep(OnboardingStep.SECURITY)
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = if (name.isBlank()) "Nama tidak boleh kosong" else null) }
    }

    fun updateOccupation(occupation: String) {
        _uiState.update { it.copy(occupation = occupation) }
    }

    fun updateBudgetAmount(amount: String) {
        _uiState.update { it.copy(budgetAmount = amount) }
    }

    fun updatePin(pin: String) {
        _uiState.update { it.copy(pin = pin, pinError = null) }
    }

    fun updateConfirmPin(confirmPin: String) {
        _uiState.update { it.copy(confirmPin = confirmPin, confirmPinError = null) }
    }

    fun setPinEnabled(enabled: Boolean) {
        _uiState.update {
            val recovery = if (enabled && it.recoveryCode == null) generateRecoveryCode() else it.recoveryCode
            it.copy(isPinEnabled = enabled, recoveryCode = recovery)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    fun addAccount(account: AccountDraft) {
        _uiState.update { it.copy(accounts = it.accounts + account) }
    }

    fun removeAccount(index: Int) {
        _uiState.update {
            val list = it.accounts.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            it.copy(accounts = list)
        }
    }

    private fun generateRecoveryCode(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
    }

    private fun completeOnboarding() {
        val state = _uiState.value
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // 1. Save User Profile
            val profile = UserProfile(
                id = UUID.randomUUID().toString(),
                name = state.name,
                occupation = state.occupation.ifBlank { null },
                createdAt = now,
                updatedAt = now
            )
            userProfileRepository.saveUserProfile(profile)

            // 2. Save Accounts (if empty, save default Dompet Cash account)
            val accountsToSave = state.accounts.ifEmpty {
                listOf(AccountDraft("Dompet Utama", "CASH", "wallet", 0L))
            }
            accountsToSave.forEach { draft ->
                val account = Account(
                    id = UUID.randomUUID().toString(),
                    name = draft.name,
                    accountType = draft.type,
                    icon = draft.icon,
                    initialBalance = draft.initialBalance,
                    currentBalance = draft.initialBalance,
                    createdAt = now,
                    updatedAt = now
                )
                accountRepository.insertAccount(account)
            }

            // 3. Save Budget if configured
            state.budgetAmount.toLongOrNull()?.let { amount ->
                if (amount > 0L) {
                    val budget = Budget(
                        id = UUID.randomUUID().toString(),
                        categoryId = null,
                        budgetType = "GLOBAL",
                        amount = amount,
                        thresholdPercent = 80,
                        startDate = now,
                        endDate = now + 30L * 24 * 60 * 60 * 1000, // 30 days
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                    budgetRepository.insertBudget(budget)
                }
            }

            // 4. Save Security settings
            val securitySettings = SecuritySettings(
                id = "security_settings",
                pinHash = if (state.isPinEnabled && state.pin.isNotBlank()) hashPin(state.pin) else null,
                biometricEnabled = state.biometricEnabled,
                recoveryCodeHash = if (state.isPinEnabled && state.recoveryCode != null) hashPin(state.recoveryCode) else null,
                createdAt = now,
                updatedAt = now
            )
            settingsRepository.saveSecuritySettings(securitySettings)

            // 5. Save default Notifications settings (opt-out, can change later in Settings)
            val notificationSettings = NotificationSettings(
                id = "notification_settings",
                dailyReminderEnabled = false,
                dailyReminderTime = "20:00",
                weeklyReviewEnabled = false,
                monthlyReviewEnabled = false,
                createdAt = now,
                updatedAt = now
            )
            settingsRepository.saveNotificationSettings(notificationSettings)

            // 6. Save App settings
            val appSettings = AppSettings(
                id = "app_settings",
                themeMode = "SYSTEM",
                hideBalance = false,
                receiptAutoDeleteDays = 30,
                dashboardPeriod = "MONTHLY",
                createdAt = now,
                updatedAt = now
            )
            settingsRepository.saveAppSettings(appSettings)
        }
    }

    private fun hashPin(pin: String): String {
        return com.hitunguang.core.security.PinHasher.hash(pin)
    }
}
