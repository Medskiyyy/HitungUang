package com.hitunguang.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.onboarding.presentation.components.AccountStep
import com.hitunguang.feature.onboarding.presentation.components.BudgetStep
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
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top stepper progress bar (hidden on welcome)
            if (uiState.currentStep != OnboardingStep.WELCOME) {
                OnboardingStepper(
                    currentStep = uiState.currentStep.ordinal,
                    totalSteps = uiState.totalSteps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.doubleLarge)
                        .padding(top = Spacing.large)
                )
            }

            // Animated content between steps
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(animationSpec = tween(250)) { it / 4 * direction })
                        .togetherWith(
                            fadeOut(animationSpec = tween(220)) +
                                    slideOutHorizontally(animationSpec = tween(250)) { -it / 4 * direction }
                        )
                },
                label = "onboarding_step_transition"
            ) { step ->
                when (step) {
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
                            onSkipClick = { viewModel.skipBudget() },
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
    }
}

@Composable
fun OnboardingStepper(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    // currentStep is 0-based (0=Welcome, but we don't show stepper on Welcome)
    // We want 1-based display: step 1/5, 2/5, etc. after Welcome
    val visibleStep = (currentStep).coerceAtLeast(1)
    val visibleTotal = totalSteps

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..visibleTotal) {
                val isCompleted = i < visibleStep
                val isCurrent = i == visibleStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Langkah $visibleStep dari $visibleTotal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(visibleStep * 100 / visibleTotal)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
