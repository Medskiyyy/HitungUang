package com.hitunguang.feature.receipt.presentation.components

import com.hitunguang.R
import androidx.compose.ui.res.stringResource
import com.hitunguang.core.common.util.CurrencyFormatter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hitunguang.feature.receipt.presentation.ReceiptDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptDetailDialog(
    receiptId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    val receiptState by remember(receiptId) { viewModel.getReceipt(receiptId) }.collectAsState(initial = null)
    val itemsState by remember(receiptId) { viewModel.getItems(receiptId) }.collectAsState(initial = emptyList())

    val receipt = receiptState
    val items = itemsState

    if (receipt == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.receipt_detail_loading)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {}
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = receipt.merchantName?.ifBlank { stringResource(R.string.receipt_detail_title) } ?: stringResource(R.string.receipt_detail_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Struk image preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        AsyncImage(
                            model = receipt.imagePath,
                            contentDescription = stringResource(R.string.receipt_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Metadata
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val dateFormatter = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("id", "ID"))
                        val dateStr = receipt.receiptDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(R.string.receipt_no_date)
                        Text(
                            text = stringResource(R.string.receipt_detail_date, dateStr),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.receipt_detail_created, dateFormatter.format(Date(receipt.createdAt))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Items List
                    if (items.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.receipt_detail_items),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items.forEach { item ->
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val qtyStr = if (item.quantity != null) "${item.quantity}x " else ""
                                        Text(
                                            text = "${item.itemName} (${qtyStr}${CurrencyFormatter.format(item.unitPrice)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(item.subtotal),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Summary
                    Divider()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        receipt.subtotal?.let {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.receipt_detail_subtotal), style = MaterialTheme.typography.bodyMedium)
                                Text(CurrencyFormatter.format(it), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        receipt.tax?.let {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.receipt_detail_tax), style = MaterialTheme.typography.bodyMedium)
                                Text(CurrencyFormatter.format(it), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.receipt_detail_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(CurrencyFormatter.format(receipt.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}
