package com.hitunguang.feature.transaction.presentation.components

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
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hapus Transaksi")
        },
        text = {
            Text("Apakah Anda yakin ingin menghapus transaksi \"${transaction.title}\"? Nominal saldo dompet Anda akan disesuaikan kembali.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Hapus", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        modifier = modifier
    )
}
