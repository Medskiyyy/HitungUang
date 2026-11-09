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
                title = { Text("Keamanan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
                text = "Atur cara mengunci aplikasi supaya data keuanganmu aman dari orang lain.",
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
                        headlineContent = { Text("Aktifkan PIN Keamanan", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Pakai PIN buat mengunci aplikasi") },
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
                            headlineContent = { Text("Aktifkan sidik jari", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Buka aplikasi pakai sidik jari") },
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
                            headlineContent = { Text("Ubah PIN Keamanan", fontWeight = FontWeight.SemiBold) },
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
                                    Text("Ubah")
                                }
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Buat kode baru", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Kode lama nggak berlaku lagi setelah kamu buat yang baru.") },
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
                                    Text("Buat")
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
            title = { Text("Masukkan PIN saat ini", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Masukkan PIN kamu saat ini buat melanjutkan.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.verifyCurrentPinInput,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it in '0'..'9' }) {
                                viewModel.onVerifyCurrentPinInputChanged(input)
                            }
                        },
                        label = { Text("PIN saat ini") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = uiState.verifyCurrentPinError != null,
                        supportingText = {
                            uiState.verifyCurrentPinError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
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
                    Text("Lanjut")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeVerifyCurrentPin() }) {
                    Text("Batal")
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

    if (uiState.setupStep != SetupStep.INACTIVE) {
        when (uiState.setupStep) {
            SetupStep.ENTER_NEW_PIN, SetupStep.CONFIRM_NEW_PIN -> {
                val isConfirm = uiState.setupStep == SetupStep.CONFIRM_NEW_PIN
                AlertDialog(
                    onDismissRequest = { viewModel.finishPinSetup() },
                    title = {
                        Text(
                            text = if (isConfirm) "Ulangi PIN baru" else "Masukkan PIN baru",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = if (isConfirm) "Ketik PIN baru kamu sekali lagi." 
                                else "Masukkan 4 sampai 6 angka PIN baru untuk mengunci aplikasi."
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = if (isConfirm) uiState.confirmPinInput else uiState.newPinInput,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { it in '0'..'9' }) {
                                        viewModel.onSetupPinInputChanged(input)
                                    }
                                },
                                label = { Text(if (isConfirm) "Ulangi PIN" else "PIN baru") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                isError = uiState.setupError != null,
                                supportingText = {
                                    uiState.setupError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error)
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
                            Text("Lanjut")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.finishPinSetup() }) {
                            Text("Batal")
                        }
                    }
                )
            }
            SetupStep.SHOW_RECOVERY_CODE -> {
                AlertDialog(
                    onDismissRequest = { viewModel.finishPinSetup() },
                    title = { Text("Simpan kode pemulihan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    text = {
                        Column {
                            Text(
                                text = "Simpan kode pemulihan ini di tempat aman. Kode ini dipakai buat membuka aplikasi kalau kamu lupa PIN.",
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
                                                Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.finishPinSetup() }
                        ) {
                            Text("Sudah kusimpan")
                        }
                    }
                )
            }
            else -> {}
        }
    }
}
