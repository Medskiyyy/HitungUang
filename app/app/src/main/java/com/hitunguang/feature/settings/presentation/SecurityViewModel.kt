package com.hitunguang.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import com.hitunguang.feature.settings.domain.usecase.CheckSecurityStatusUseCase
import com.hitunguang.feature.settings.domain.usecase.DisablePinSecurityUseCase
import com.hitunguang.feature.settings.domain.usecase.SavePinUseCase
import com.hitunguang.feature.settings.domain.usecase.ValidatePinUseCase
import com.hitunguang.feature.settings.domain.usecase.ValidateRecoveryCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val isAppLocked: Boolean = false,
    val securitySettings: SecuritySettings? = null,
    val pinInput: String = "",
    val pinError: String? = null,
    val remainingAttempts: Int = 5,
    val isLockedOut: Boolean = false,
    
    // Recovery flow states
    val isRecoveryMode: Boolean = false,
    val recoveryCodeInput: String = "",
    val recoveryCodeError: String? = null,
    
    // PIN Setup / Change states inside settings
    val setupStep: SetupStep = SetupStep.INACTIVE,
    val newPinInput: String = "",
    val confirmPinInput: String = "",
    val setupError: String? = null,
    val generatedRecoveryCode: String? = null,
    
    // Verify current PIN before disable/change state
    val verifyCurrentPinOpen: Boolean = false,
    val verifyCurrentPinInput: String = "",
    val verifyCurrentPinError: String? = null,
    val pendingAction: PendingSecurityAction = PendingSecurityAction.NONE
)

enum class SetupStep {
    INACTIVE, ENTER_NEW_PIN, CONFIRM_NEW_PIN, SHOW_RECOVERY_CODE
}

enum class PendingSecurityAction {
    NONE, DISABLE_PIN, CHANGE_PIN, VIEW_RECOVERY_CODE
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val checkSecurityStatusUseCase: CheckSecurityStatusUseCase,
    private val validatePinUseCase: ValidatePinUseCase,
    private val savePinUseCase: SavePinUseCase,
    private val disablePinSecurityUseCase: DisablePinSecurityUseCase,
    private val validateRecoveryCodeUseCase: ValidateRecoveryCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getSecuritySettings().collect { settings ->
                val hasPin = settings?.pinHash != null
                _uiState.update { 
                    it.copy(
                        securitySettings = settings,
                        isAppLocked = hasPin && !it.isAppLocked // Lock initially if PIN exists
                    )
                }
            }
        }
    }

    // App Lock screen actions
    fun onPinDigitEntered(digit: String) {
        val currentState = _uiState.value
        if (currentState.isLockedOut || currentState.pinInput.length >= 6) return

        val newInput = currentState.pinInput + digit
        _uiState.update { it.copy(pinInput = newInput, pinError = null) }

        if (newInput.length == 4 || newInput.length == 6) {
            // Auto-verify when 4 or 6 digits entered
            viewModelScope.launch {
                val isValid = validatePinUseCase(newInput)
                if (isValid) {
                    _uiState.update { 
                        it.copy(
                            isAppLocked = false, 
                            pinInput = "", 
                            remainingAttempts = 5, 
                            pinError = null
                        ) 
                    }
                } else {
                    val remaining = currentState.remainingAttempts - 1
                    _uiState.update { 
                        it.copy(
                            pinInput = "",
                            remainingAttempts = remaining,
                            pinError = "PIN salah",
                            isLockedOut = remaining <= 0
                        ) 
                    }
                }
            }
        }
    }

    fun onPinBackspace() {
        _uiState.update { 
            if (it.pinInput.isNotEmpty()) {
                it.copy(pinInput = it.pinInput.dropLast(1))
            } else it
        }
    }

    fun triggerBiometricUnlock() {
        _uiState.update { it.copy(isAppLocked = false, pinInput = "", remainingAttempts = 5, pinError = null) }
    }

    fun enterRecoveryMode(enabled: Boolean) {
        _uiState.update { 
            it.copy(
                isRecoveryMode = enabled, 
                recoveryCodeInput = "", 
                recoveryCodeError = null
            ) 
        }
    }

    fun onRecoveryCodeChanged(code: String) {
        _uiState.update { it.copy(recoveryCodeInput = code, recoveryCodeError = null) }
    }

    fun verifyRecoveryCode() {
        val code = _uiState.value.recoveryCodeInput
        if (code.isBlank()) {
            _uiState.update { it.copy(recoveryCodeError = "Kode tidak boleh kosong") }
            return
        }

        viewModelScope.launch {
            val isValid = validateRecoveryCodeUseCase(code)
            if (isValid) {
                _uiState.update { 
                    it.copy(
                        isAppLocked = false, 
                        isRecoveryMode = false,
                        recoveryCodeInput = "",
                        remainingAttempts = 5,
                        isLockedOut = false
                    ) 
                }
            } else {
                _uiState.update { it.copy(recoveryCodeError = "Kode pemulihan salah") }
            }
        }
    }

    // Settings Security setup actions
    fun startPinSetup() {
        _uiState.update { 
            it.copy(
                setupStep = SetupStep.ENTER_NEW_PIN, 
                newPinInput = "", 
                confirmPinInput = "", 
                setupError = null,
                generatedRecoveryCode = null
            ) 
        }
    }

    fun onSetupPinInputChanged(pin: String) {
        _uiState.update { 
            if (it.setupStep == SetupStep.ENTER_NEW_PIN) {
                it.copy(newPinInput = pin, setupError = null)
            } else {
                it.copy(confirmPinInput = pin, setupError = null)
            }
        }
    }

    fun submitSetupPin() {
        val state = _uiState.value
        if (state.setupStep == SetupStep.ENTER_NEW_PIN) {
            if (state.newPinInput.length < 4) {
                _uiState.update { it.copy(setupError = "PIN minimal 4 angka") }
                return
            }
            _uiState.update { it.copy(setupStep = SetupStep.CONFIRM_NEW_PIN, setupError = null) }
        } else if (state.setupStep == SetupStep.CONFIRM_NEW_PIN) {
            if (state.newPinInput != state.confirmPinInput) {
                _uiState.update { it.copy(setupError = "PIN konfirmasi tidak cocok") }
                return
            }
            // Save new PIN
            viewModelScope.launch {
                val recoveryCode = savePinUseCase(state.newPinInput)
                _uiState.update { 
                    it.copy(
                        setupStep = SetupStep.SHOW_RECOVERY_CODE, 
                        generatedRecoveryCode = recoveryCode,
                        setupError = null
                    ) 
                }
            }
        }
    }

    fun finishPinSetup() {
        _uiState.update { it.copy(setupStep = SetupStep.INACTIVE, generatedRecoveryCode = null) }
    }

    // Biometric toggle
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSecuritySettings().firstOrNull() ?: return@launch
            val updated = current.copy(biometricEnabled = enabled, updatedAt = System.currentTimeMillis())
            settingsRepository.saveSecuritySettings(updated)
        }
    }

    // Verification of current PIN before dangerous actions
    fun openVerifyCurrentPin(action: PendingSecurityAction) {
        _uiState.update { 
            it.copy(
                verifyCurrentPinOpen = true,
                verifyCurrentPinInput = "",
                verifyCurrentPinError = null,
                pendingAction = action
            ) 
        }
    }

    fun onVerifyCurrentPinInputChanged(pin: String) {
        _uiState.update { it.copy(verifyCurrentPinInput = pin, verifyCurrentPinError = null) }
    }

    fun submitVerifyCurrentPin() {
        val state = _uiState.value
        viewModelScope.launch {
            val isValid = validatePinUseCase(state.verifyCurrentPinInput)
            if (isValid) {
                _uiState.update { it.copy(verifyCurrentPinOpen = false, verifyCurrentPinError = null) }
                when (state.pendingAction) {
                    PendingSecurityAction.DISABLE_PIN -> {
                        disablePinSecurityUseCase()
                    }
                    PendingSecurityAction.CHANGE_PIN -> {
                        startPinSetup()
                    }
                    PendingSecurityAction.VIEW_RECOVERY_CODE -> {
                        // Open recovery code display (using a trick of showing setup step SHOW_RECOVERY_CODE with existing code)
                        val codeHash = state.securitySettings?.recoveryCodeHash
                        // Since we only store hash, we can't show raw recovery code unless they write it down.
                        // However, we can generate a new recovery code for them!
                        // Let's generate and save a new recovery code when they view it.
                        val newRecoveryCode = savePinUseCase(state.verifyCurrentPinInput)
                        _uiState.update {
                            it.copy(
                                setupStep = SetupStep.SHOW_RECOVERY_CODE,
                                generatedRecoveryCode = newRecoveryCode
                            )
                        }
                    }
                    else -> {}
                }
            } else {
                _uiState.update { it.copy(verifyCurrentPinError = "PIN saat ini salah") }
            }
        }
    }

    fun closeVerifyCurrentPin() {
        _uiState.update { it.copy(verifyCurrentPinOpen = false, pendingAction = PendingSecurityAction.NONE) }
    }
}
