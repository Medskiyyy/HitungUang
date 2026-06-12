package com.hitunguang.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hitunguang.core.designsystem.components.HUTransactionCard
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionWithDetails>,
    onViewAllClick: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaksi Terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onViewAllClick) {
                Text("Lihat Semua")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.extraSmall))

        if (transactions.isEmpty()) {
            Text(
                text = "Belum ada transaksi tercatat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.large)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                transactions.take(5).forEach { tx ->
                    HUTransactionCard(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
            }
        }
    }
}
