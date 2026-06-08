package com.hitunguang.feature.onboarding.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.feature.onboarding.presentation.components.AccountStep
import com.hitunguang.feature.onboarding.presentation.components.BackupStep
import com.hitunguang.feature.onboarding.presentation.components.BudgetStep
import com.hitunguang.feature.onboarding.presentation.components.NotificationStep
import com.hitunguang.feature.onboarding.presentation.components.ProfileStep
import com.hitunguang.feature.onboarding.presentation.components.SecurityStep
import com.hitunguang.feature.onboarding.presentation.components.TutorialStep
import com.hitunguang.feature.onboarding.presentation.components.WelcomeStep

@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState.currentStep) {
            OnboardingStep.WELCOME -> {
                WelcomeStep(
                    onStartClick = { viewModel.nextStep() }
                )
            }
            OnboardingStep.PROFILE -> {
                ProfileStep(
                    name = uiState.name,
                    nameError = uiState.nameError,
                    occupation = uiState.occupation,
                    onNameChange = { viewModel.updateName(it) },
                    onOccupationChange = { viewModel.updateOccupation(it) },
                    onNextClick = { viewModel.nextStep() },
                    onBackClick = { viewModel.previousStep() }
                )
            }
            OnboardingStep.ACCOUNT -> {
                AccountStep(
                    accounts = uiState.accounts,
                    onAddAccount = { viewModel.addAccount(it) },
                    onRemoveAccount = { viewModel.removeAccount(it) },
                    onNextClick = { viewModel.nextStep() },
                    onBackClick = { viewModel.previousStep() }
                )
            }
            OnboardingStep.BUDGET -> {
                BudgetStep(
                    budgetAmount = uiState.budgetAmount,
                    onBudgetAmountChange = { viewModel.updateBudgetAmount(it) },
                    onNextClick = { viewModel.nextStep() },
                    onBackClick = { viewModel.previousStep() }
                )
            }
            OnboardingStep.NOTIFICATION -> {
                NotificationStep(
                    dailyReminderEnabled = uiState.dailyReminderEnabled,
                    onDailyReminderToggled = { viewModel.setNotificationEnabled(it) },
                    onNextClick = { viewModel.nextStep() },
                    onBackClick = { viewModel.previousStep() }
                )
            }
            OnboardingStep.SECURITY -> {
                SecurityStep(
                    isPinEnabled = uiState.isPinEnabled,
                    pin = uiState.pin,
                    pinError = uiState.pinError,
                    confirmPin = uiState.confirmPin,
                    confirmPinError = uiState.confirmPinError,
                    biometricEnabled = uiState.biometricEnabled,
                    recoveryCode = uiState.recoveryCode,
                    onPinEnabledToggled = { viewModel.setPinEnabled(it) },
                    onPinChange = { viewModel.updatePin(it) },
                    onConfirmPinChange = { viewModel.updateConfirmPin(it) },
                    onBiometricToggled = { viewModel.setBiometricEnabled(it) },
                    onNextClick = { viewModel.nextStep() },
                    onBackClick = { viewModel.previousStep() }
                )
            }
            OnboardingStep.BACKUP -> {
                BackupStep(
                    backupUri = uiState.backupUri,
                    onBackupUriSelected = { viewModel.setBackupUri(it) },
                    onNextClick = { viewModel.nextStep() },
                    onBackClick = { viewModel.previousStep() }
                )
            }
            OnboardingStep.TUTORIAL -> {
                TutorialStep(
                    onFinishClick = {
                        viewModel.nextStep()
                        onOnboardingFinished()
                    },
                    onBackClick = { viewModel.previousStep() }
                )
            }
        }
    }
}
