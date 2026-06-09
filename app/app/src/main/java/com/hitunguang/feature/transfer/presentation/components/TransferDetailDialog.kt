package com.hitunguang.feature.transfer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hitunguang.feature.transfer.presentation.TransferWithAccountNames
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferDetailDialog(
    transferWithNames: TransferWithAccountNames,
    onDismiss: () -> Unit,
    onRevertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRevertConfirm by remember { mutableStateOf(false) }
    val transfer = transferWithNames.transfer
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("in", "ID"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Detail Transfer", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Visual flow: Source -> Destination
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dari",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transferWithNames.fromAccountName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ke",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transferWithNames.toAccountName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Details list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Nominal Transfer", value = "Rp ${transfer.amount}")
                    if (transfer.adminFee > 0L) {
                        DetailRow(label = "Biaya Admin", value = "Rp ${transfer.adminFee}")
                        DetailRow(
                            label = "Total Potongan", 
                            value = "Rp ${transfer.amount + transfer.adminFee}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow(label = "Tanggal", value = dateFormatter.format(Date(transfer.transferDate)))
                    if (!transfer.note.isNullOrBlank()) {
                        DetailRow(label = "Catatan", value = transfer.note)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { showRevertConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.width(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Batalkan Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )

    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            title = { Text("Batalkan Transfer?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin membatalkan transfer ini? Saldo dari kedua dompet akan dikembalikan ke kondisi semula dan catatan transfer ini akan dihapus permanen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRevertClick()
                        showRevertConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
