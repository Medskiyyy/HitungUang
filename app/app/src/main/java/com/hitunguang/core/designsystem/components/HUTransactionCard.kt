package com.hitunguang.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.category.presentation.components.CategoryIconHelper
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HUTransactionCard(
    transaction: TransactionWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.transactionType == "EXPENSE" || transaction.transactionType == "TRANSFER_FEE"
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("in", "ID")) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large, vertical = Spacing.medium), // More compact vertically
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // Slightly smaller icon container
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Neutral background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIconByName(transaction.categoryIcon),
                        contentDescription = transaction.categoryName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, // Neutral icon color
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (transaction.categoryName != null) "${transaction.accountName} • ${transaction.categoryName}" else transaction.accountName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.small))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}${CurrencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) ExpenseRed else IncomeGreen,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormatter.format(Date(transaction.transactionDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
