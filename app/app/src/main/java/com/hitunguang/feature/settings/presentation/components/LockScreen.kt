package com.hitunguang.feature.settings.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.feature.settings.presentation.SecurityViewModel

@Composable
fun LockScreen(
    onBiometricPromptTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBiometricEnabled = uiState.securitySettings?.biometricEnabled == true

    // Auto-trigger biometric prompt on startup if enabled and not in recovery mode
    LaunchedEffect(isBiometricEnabled, uiState.isRecoveryMode) {
        if (isBiometricEnabled && !uiState.isRecoveryMode) {
            onBiometricPromptTrigger()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HitungUang",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Aplikasi Catatan Keuangan Pribadi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = !uiState.isRecoveryMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.isLockedOut) "Terlalu banyak percobaan salah" else "Masukkan PIN Anda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isLockedOut || uiState.pinError != null) MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dots Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Supposed length is 4 or 6, let's render 6 dots if pinInput is longer than 4, otherwise 4
                        val dotsCount = if (uiState.pinInput.length > 4) 6 else 4
                        repeat(dotsCount) { index ->
                            val isFilled = index < uiState.pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isFilled) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.pinError != null && !uiState.isLockedOut) {
                        Text(
                            text = "${uiState.pinError} (Sisa ${uiState.remainingAttempts} percobaan)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (uiState.isLockedOut) {
                        Text(
                            text = "Silakan gunakan Kode Pemulihan Anda untuk membuka kunci.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Keypad
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val rowModifier = Modifier.fillMaxWidth(0.85f)
                        Row(
                            modifier = rowModifier,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            KeypadButton(digit = "1", onClick = { viewModel.onPinDigitEntered("1") })
                            KeypadButton(digit = "2", onClick = { viewModel.onPinDigitEntered("2") })
                            KeypadButton(digit = "3", onClick = { viewModel.onPinDigitEntered("3") })
                        }
                        Row(
                            modifier = rowModifier,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            KeypadButton(digit = "4", onClick = { viewModel.onPinDigitEntered("4") })
                            KeypadButton(digit = "5", onClick = { viewModel.onPinDigitEntered("5") })
                            KeypadButton(digit = "6", onClick = { viewModel.onPinDigitEntered("6") })
                        }
                        Row(
                            modifier = rowModifier,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            KeypadButton(digit = "7", onClick = { viewModel.onPinDigitEntered("7") })
                            KeypadButton(digit = "8", onClick = { viewModel.onPinDigitEntered("8") })
                            KeypadButton(digit = "9", onClick = { viewModel.onPinDigitEntered("9") })
                        }
                        Row(
                            modifier = rowModifier,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left button: Biometric trigger
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                if (isBiometricEnabled) {
                                    IconButton(
                                        onClick = onBiometricPromptTrigger,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Autentikasi Sidik Jari",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            KeypadButton(digit = "0", onClick = { viewModel.onPinDigitEntered("0") })

                            // Right button: Backspace
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { viewModel.onPinBackspace() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(onClick = { viewModel.enterRecoveryMode(true) }) {
                        Text("Lupa PIN?", fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isRecoveryMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pemulihan Keamanan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Masukkan 16-karakter Kode Pemulihan Anda di bawah ini untuk meriset PIN.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.recoveryCodeInput,
                            onValueChange = { input ->
                                if (input.length <= 16) {
                                    viewModel.onRecoveryCodeChanged(input)
                                }
                            },
                            label = { Text("Kode Pemulihan") },
                            placeholder = { Text("KODE16KARAKTER") },
                            singleLine = true,
                            isError = uiState.recoveryCodeError != null,
                            supportingText = {
                                uiState.recoveryCodeError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.enterRecoveryMode(false) }) {
                                Text("Batal")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { viewModel.verifyRecoveryCode() }) {
                                Text("Kirim")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    digit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
