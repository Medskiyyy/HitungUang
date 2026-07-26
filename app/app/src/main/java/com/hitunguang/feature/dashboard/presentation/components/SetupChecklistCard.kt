package com.hitunguang.feature.dashboard.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing

private data class SetupTask(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isDone: Boolean,
    val onClick: () -> Unit
)

/**
 * First-run guidance shown on the dashboard until the user has a wallet, a transaction
 * and a budget. State is derived from real data, so the card disappears on its own and
 * never shows up for an established user.
 */
@Composable
fun SetupChecklistCard(
    hasWallet: Boolean,
    hasTransaction: Boolean,
    hasBudget: Boolean,
    onCreateWalletClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onCreateBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks = listOf(
        SetupTask(
            title = "Buat dompet pertama",
            description = "Tempat mencatat saldo tunai, bank, atau e-wallet.",
            icon = Icons.Default.AccountBalanceWallet,
            isDone = hasWallet,
            onClick = onCreateWalletClick
        ),
        SetupTask(
            title = "Catat transaksi pertama",
            description = "Isi manual atau pindai struk belanja langsung.",
            icon = Icons.Default.ReceiptLong,
            isDone = hasTransaction,
            onClick = onAddTransactionClick
        ),
        SetupTask(
            title = "Atur budget bulanan",
            description = "Tetapkan batas pengeluaran biar tidak kebablasan.",
            icon = Icons.Default.PieChart,
            isDone = hasBudget,
            onClick = onCreateBudgetClick
        )
    )

    val doneCount = tasks.count { it.isDone }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Langkah Awal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$doneCount dari ${tasks.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = "Selesaikan langkah ini supaya HitungUang siap dipakai sehari-hari.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            LinearProgressIndicator(
                progress = { doneCount.toFloat() / tasks.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(Radius.extraSmall)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            tasks.forEachIndexed { index, task ->
                SetupTaskRow(task = task)
                if (index != tasks.lastIndex) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}

@Composable
private fun SetupTaskRow(task: SetupTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.small))
            .clickable(enabled = !task.isDone, onClick = task.onClick)
            .padding(vertical = Spacing.small, horizontal = Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (task.isDone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (task.isDone) Icons.Default.Check else task.icon,
                contentDescription = null,
                tint = if (task.isDone) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.size(Spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.SemiBold,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (!task.isDone) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (!task.isDone) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
