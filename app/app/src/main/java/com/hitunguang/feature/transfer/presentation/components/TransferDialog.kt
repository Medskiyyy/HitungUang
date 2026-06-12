package com.hitunguang.feature.transfer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.feature.transfer.presentation.TransferViewModel
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    onDismiss: () -> Unit,
    showSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            showSnackbar("Transfer berhasil disimpan")
            onDismiss()
        }
    }

    LaunchedEffect(uiState.error) {
        showErrorDialog = uiState.error != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Transfer Antar Akun", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // From Account Dropdown
                Column {
                    Text("Dari Dompet *", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { fromAccountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(uiState.fromAccount?.name ?: "Pilih Dompet Asal")
                        }
                        DropdownMenu(
                            expanded = fromAccountExpanded,
                            onDismissRequest = { fromAccountExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (Saldo: Rp ${acc.currentBalance})") },
                                    onClick = {
                                        viewModel.onFromAccountSelected(acc)
                                        fromAccountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // To Account Dropdown
                Column {
                    Text("Ke Dompet *", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { toAccountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(uiState.toAccount?.name ?: "Pilih Dompet Tujuan")
                        }
                        DropdownMenu(
                            expanded = toAccountExpanded,
                            onDismissRequest = { toAccountExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (Saldo: Rp ${acc.currentBalance})") },
                                    onClick = {
                                        viewModel.onToAccountSelected(acc)
                                        toAccountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    label = { Text("Nominal Transfer *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Admin Fee
                OutlinedTextField(
                    value = uiState.adminFee,
                    onValueChange = { viewModel.onAdminFeeChanged(it) },
                    label = { Text("Biaya Admin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker Button
                Column {
                    Text("Tanggal Transfer", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateFormatter.format(Date(uiState.transferDate)))
                    }
                }

                // Note
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    label = { Text("Catatan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.executeTransfer() },
                enabled = !uiState.isSaving
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.transferDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDateChanged(datePickerState.selectedDateMillis ?: uiState.transferDate)
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Peringatan", fontWeight = FontWeight.Bold) },
            text = { Text(uiState.error ?: "Terjadi kesalahan") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}
