package com.hitunguang.feature.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hitunguang.R
import com.hitunguang.feature.account.domain.model.Account

@Composable
fun AccountFormDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, icon: String, initialBalance: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.accountType ?: "CASH") }
    var icon by remember { mutableStateOf(account?.icon ?: "wallet") }
    var initialBalanceStr by remember { mutableStateOf(account?.initialBalance?.toString() ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val isEditMode = account != null
    val nameRequiredText = stringResource(R.string.account_err_name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) stringResource(R.string.account_form_edit) else stringResource(R.string.account_form_add))
        },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) nameRequiredText else null
                    },
                    label = { Text(stringResource(R.string.account_form_name)) },
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(stringResource(R.string.account_form_type), style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = type == "CASH",
                            onClick = { type = "CASH"; icon = "wallet" }
                        )
                        Text(stringResource(R.string.account_type_cash))
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        RadioButton(
                            selected = type == "BANK",
                            onClick = { type = "BANK"; icon = "bank" }
                        )
                        Text(stringResource(R.string.account_type_bank))
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        RadioButton(
                            selected = type == "E_WALLET",
                            onClick = { type = "E_WALLET"; icon = "e_wallet" }
                        )
                        Text(stringResource(R.string.account_type_ewallet))
                    }
                }

                if (!isEditMode) {
                    OutlinedTextField(
                        value = initialBalanceStr,
                        onValueChange = { initialBalanceStr = it },
                        label = { Text(stringResource(R.string.account_form_initial_balance)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = nameRequiredText
                    } else {
                        val balance = initialBalanceStr.toLongOrNull() ?: 0L
                        onSave(name, type, icon, balance)
                    }
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
