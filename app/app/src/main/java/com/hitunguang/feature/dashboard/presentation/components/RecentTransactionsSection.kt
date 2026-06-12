package com.hitunguang.feature.dashboard.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hitunguang.feature.category.presentation.components.CategoryIconHelper
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.core.common.util.CurrencyFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing

@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionWithDetails>,
    onViewAllClick: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val idLocale = Locale("in", "ID")
    val formatter = NumberFormat.getIntegerInstance(idLocale)
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", idLocale)

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
                    val isExpense = tx.transactionType == "EXPENSE" || tx.transactionType == "TRANSFER_FEE"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTransactionClick(tx) },
                        shape = RoundedCornerShape(Radius.medium),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.large),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconVector = CategoryIconHelper.getIconByName(tx.categoryIcon)
                            Box(
                                modifier = Modifier
                                    .size(Spacing.huge)
                                    .clip(CircleShape)
                                    .background(
                                        if (isExpense) ExpenseRed.copy(alpha = 0.1f)
                                        else IncomeGreen.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (isExpense) ExpenseRed else IncomeGreen,
                                    modifier = Modifier.size(Spacing.extraLarge)
                                )
                            }

                            Spacer(modifier = Modifier.width(Spacing.medium))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tx.categoryName != null) {
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = tx.categoryName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                                Text(
                                    text = dateFormatter.format(Date(tx.transactionDate)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.width(Spacing.small))

                            Text(
                                text = "${if (isExpense) "-" else "+"}${CurrencyFormatter.format(tx.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpense) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
