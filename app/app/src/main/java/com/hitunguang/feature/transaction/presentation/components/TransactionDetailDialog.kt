package com.hitunguang.feature.transaction.presentation.components

import com.hitunguang.R
import androidx.compose.ui.res.stringResource
import com.hitunguang.core.common.util.CurrencyFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    transaction: TransactionWithDetails,
    attachments: List<Attachment>,
    onAttachmentClick: (Attachment) -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
    val isExpense = transaction.transactionType == "EXPENSE" || transaction.transactionType == "TRANSFER_FEE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.txn_detail_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(stringResource(R.string.txn_label_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(transaction.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text(stringResource(R.string.txn_label_amount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${if (isExpense) "-" else "+"}${CurrencyFormatter.format(transaction.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(stringResource(R.string.txn_label_type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isExpense) stringResource(R.string.txn_type_expense) else stringResource(R.string.txn_type_income), style = MaterialTheme.typography.bodyMedium)
                }

                Column {
                    Text(stringResource(R.string.txn_label_wallet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(transaction.accountName, style = MaterialTheme.typography.bodyMedium)
                }

                Column {
                    Text(stringResource(R.string.txn_label_category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(transaction.categoryName ?: stringResource(R.string.txn_no_category), style = MaterialTheme.typography.bodyMedium)
                }

                Column {
                    Text(stringResource(R.string.txn_label_date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateFormatter.format(Date(transaction.transactionDate)), style = MaterialTheme.typography.bodyMedium)
                }

                if (!transaction.note.isNullOrBlank()) {
                    Column {
                        Text(stringResource(R.string.txn_label_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(transaction.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (attachments.isNotEmpty()) {
                    AttachmentGrid(
                        attachments = attachments,
                        onAddClick = {},
                        onDeleteClick = {},
                        onAttachmentClick = onAttachmentClick,
                        isEditable = false
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text(stringResource(R.string.common_edit))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}
