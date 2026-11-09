package com.hitunguang.feature.settings.presentation.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.feature.settings.presentation.PendingSecurityAction
import com.hitunguang.feature.settings.presentation.SecurityUiState
import com.hitunguang.feature.settings.presentation.SecurityViewModel
import com.hitunguang.feature.settings.presentation.SetupStep
import androidx.compose.ui.res.stringResource
import com.hitunguang.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPinEnabled = uiState.securitySettings?.pinHash != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.security_screen_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.security_enable_pin_title), fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(stringResource(R.string.security_enable_pin_desc)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isPinEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        viewModel.startPinSetup()
                                    } else {
                                        viewModel.openVerifyCurrentPin(PendingSecurityAction.DISABLE_PIN)
                                    }
                                }
                            )
                        }
                    )

                    if (isPinEnabled) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.security_enable_fingerprint_title), fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(stringResource(R.string.security_enable_fingerprint_desc)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = uiState.securitySettings?.biometricEnabled ?: false,
                                    onCheckedChange = { checked ->
                                        viewModel.setBiometricEnabled(checked)
                                    }
                                )
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.security_change_pin_title), fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            trailingContent = {
                                TextButton(onClick = { viewModel.openVerifyCurrentPin(PendingSecurityAction.CHANGE_PIN) }) {
                                    Text(stringResource(R.string.common_change))
                                }
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.security_new_code_title), fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(stringResource(R.string.security_new_code_desc)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            trailingContent = {
                                TextButton(onClick = { viewModel.openVerifyCurrentPin(PendingSecurityAction.VIEW_RECOVERY_CODE) }) {
                                    Text(stringResource(R.string.security_new_code_action))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // PIN Verification Dialog before sensitive actions
    if (uiState.verifyCurrentPinOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeVerifyCurrentPin() },
            title = { Text(stringResource(R.string.security_verify_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.security_verify_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.verifyCurrentPinInput,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it in '0'..'9' }) {
                                viewModel.onVerifyCurrentPinInputChanged(input)
                            }
                        },
                        label = { Text(stringResource(R.string.security_current_pin_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = uiState.verifyCurrentPinError != null,
                        supportingText = {
                            uiState.verifyCurrentPinError?.let {
                                Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitVerifyCurrentPin() },
                    enabled = uiState.verifyCurrentPinInput.length >= 4
                ) {
                    Text(stringResource(R.string.common_next))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeVerifyCurrentPin() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    PinSetupDialogs(uiState = uiState, viewModel = viewModel)
}

@Composable
fun PinSetupDialogs(
    uiState: SecurityUiState,
    viewModel: SecurityViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val codeCopiedText = stringResource(R.string.security_code_copied)

    if (uiState.setupStep != SetupStep.INACTIVE) {
        when (uiState.setupStep) {
            SetupStep.ENTER_NEW_PIN, SetupStep.CONFIRM_NEW_PIN -> {
                val isConfirm = uiState.setupStep == SetupStep.CONFIRM_NEW_PIN
                AlertDialog(
                    onDismissRequest = { viewModel.finishPinSetup() },
                    title = {
                        Text(
                            text = if (isConfirm) stringResource(R.string.security_confirm_pin_title) else stringResource(R.string.security_enter_pin_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = if (isConfirm) stringResource(R.string.security_confirm_pin_desc) 
                                else stringResource(R.string.security_enter_pin_desc)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = if (isConfirm) uiState.confirmPinInput else uiState.newPinInput,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { it in '0'..'9' }) {
                                        viewModel.onSetupPinInputChanged(input)
                                    }
                                },
                                label = { Text(if (isConfirm) stringResource(R.string.security_confirm_pin_label) else stringResource(R.string.security_new_pin_label)) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                isError = uiState.setupError != null,
                                supportingText = {
                                    uiState.setupError?.let {
                                        Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.submitSetupPin() },
                            enabled = if (isConfirm) uiState.confirmPinInput.length >= 4 
                            else uiState.newPinInput.length >= 4
                        ) {
                            Text(stringResource(R.string.common_next))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.finishPinSetup() }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
            SetupStep.SHOW_RECOVERY_CODE -> {
                AlertDialog(
                    onDismissRequest = { viewModel.finishPinSetup() },
                    title = { Text(stringResource(R.string.security_save_code_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.security_save_code_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.generatedRecoveryCode ?: "",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            uiState.generatedRecoveryCode?.let {
                                                clipboardManager.setText(AnnotatedString(it))
                                                Toast.makeText(context, codeCopiedText, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.security_copy_cd))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.finishPinSetup() }
                        ) {
                            Text(stringResource(R.string.security_code_saved))
                        }
                    }
                )
            }
            else -> {}
        }
    }
}
