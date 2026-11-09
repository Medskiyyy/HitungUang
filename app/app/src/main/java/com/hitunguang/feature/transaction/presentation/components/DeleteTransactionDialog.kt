package com.hitunguang.feature.transaction.presentation.components

import com.hitunguang.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

@Composable
fun DeleteTransactionDialog(
    transaction: TransactionWithDetails,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.txn_delete_title))
        },
        text = {
            Text(stringResource(R.string.txn_delete_message, transaction.title))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isProcessing
            ) {
                Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        modifier = modifier
    )
}
