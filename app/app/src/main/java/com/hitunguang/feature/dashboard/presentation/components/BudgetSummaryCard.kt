package com.hitunguang.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hitunguang.core.designsystem.theme.BudgetDanger
import com.hitunguang.core.designsystem.theme.BudgetSafe
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.dashboard.presentation.BudgetProgress
import com.hitunguang.core.common.util.CurrencyFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BudgetSummaryCard(
    budgetProgressList: List<BudgetProgress>,
    onAddBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val idLocale = Locale("in", "ID")
    val formatter = NumberFormat.getIntegerInstance(idLocale)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Budget Terpakai",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        if (budgetProgressList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.doubleLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Belum Ada Budget Aktif",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = "Buat budget baru untuk membantumu melacak pengeluaran secara teratur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.large))
                    OutlinedButton(onClick = onAddBudgetClick) {
                        Text("Buat Budget")
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                budgetProgressList.take(3).forEach { progress ->
                    val budget = progress.budget
                    val isLimitExceeded = progress.spentAmount > budget.amount
                    val progressRatio = (progress.progressPercent / 100f).coerceIn(0f, 1f)

                    val budgetName = if (budget.budgetType == "GLOBAL") {
                        "Budget Bulanan Global"
                    } else {
                        "Budget ${progress.categoryName ?: "Kategori"}"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.medium),
                        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low)
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.large)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = budgetName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${progress.progressPercent.roundToInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitExceeded) BudgetDanger else BudgetSafe
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.small))

                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Spacing.small)
                                    .clip(RoundedCornerShape(Radius.extraSmall)),
                                color = if (isLimitExceeded) BudgetDanger else BudgetSafe,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(Spacing.small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Terpakai: ${CurrencyFormatter.format(progress.spentAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val remaining = budget.amount - progress.spentAmount
                                Text(
                                    text = "Sisa Budget: ${CurrencyFormatter.format(remaining)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (remaining >= 0) BudgetSafe else BudgetDanger,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
