package com.hitunguang.feature.transfer.presentation.components

import com.hitunguang.R
import androidx.compose.ui.res.stringResource
import com.hitunguang.core.common.util.CurrencyFormatter
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

    val savedMessage = stringResource(R.string.transfer_saved)
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            showSnackbar(savedMessage)
            onDismiss()
        }
    }

    LaunchedEffect(uiState.error) {
        showErrorDialog = uiState.error != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.transfer_dialog_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // From Account Dropdown
                Column {
                    Text(stringResource(R.string.transfer_from_label), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { fromAccountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(uiState.fromAccount?.name ?: stringResource(R.string.transfer_from_placeholder))
                        }
                        DropdownMenu(
                            expanded = fromAccountExpanded,
                            onDismissRequest = { fromAccountExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.transfer_account_balance, acc.name, CurrencyFormatter.format(acc.currentBalance))) },
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
                    Text(stringResource(R.string.transfer_to_label), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { toAccountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(uiState.toAccount?.name ?: stringResource(R.string.transfer_to_placeholder))
                        }
                        DropdownMenu(
                            expanded = toAccountExpanded,
                            onDismissRequest = { toAccountExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.transfer_account_balance, acc.name, CurrencyFormatter.format(acc.currentBalance))) },
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
                    label = { Text(stringResource(R.string.transfer_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Admin Fee
                OutlinedTextField(
                    value = uiState.adminFee,
                    onValueChange = { viewModel.onAdminFeeChanged(it) },
                    label = { Text(stringResource(R.string.transfer_label_fee)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker Button
                Column {
                    Text(stringResource(R.string.transfer_date_label), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
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
                    label = { Text(stringResource(R.string.transfer_note_label)) },
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
                Text(stringResource(R.string.transfer_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
                    Text(stringResource(R.string.common_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(stringResource(R.string.transfer_warning_title), fontWeight = FontWeight.Bold) },
            text = { Text(uiState.error ?: stringResource(R.string.transfer_error_generic)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }
}
