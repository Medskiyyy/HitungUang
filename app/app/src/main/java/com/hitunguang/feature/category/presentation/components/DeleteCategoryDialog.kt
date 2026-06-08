package com.hitunguang.feature.category.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hitunguang.feature.category.domain.model.Category

@Composable
fun DeleteCategoryDialog(
    category: Category,
    hasTransactions: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (forceDelete: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hapus Kategori: ${category.name}")
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasTransactions) {
                    Text(
                        text = "Kategori ini memiliki riwayat transaksi aktif. Menghapus kategori ini juga akan menghapus (soft-delete) seluruh transaksi di dalamnya dan memindahkannya ke Keranjang Sampah (Recycle Bin).\n\nApakah Anda tetap ingin menghapus?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus kategori ini?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(hasTransactions) }
            ) {
                Text(
                    text = if (hasTransactions) "Hapus Tetap" else "Hapus",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
