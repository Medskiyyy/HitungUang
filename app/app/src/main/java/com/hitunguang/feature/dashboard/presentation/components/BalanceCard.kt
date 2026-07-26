package com.hitunguang.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.designsystem.theme.AutoResizeText
import com.hitunguang.core.designsystem.theme.BalanceGradientEndDark
import com.hitunguang.core.designsystem.theme.BalanceGradientEndLight
import com.hitunguang.core.designsystem.theme.BalanceGradientStartDark
import com.hitunguang.core.designsystem.theme.BalanceGradientStartLight
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

@Composable
fun BalanceCard(
    totalBalance: Long,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    totalIncome: Long,
    totalExpense: Long,
    netDifference: Long,
    modifier: Modifier = Modifier
) {
    val formattedBalance = CurrencyFormatter.format(totalBalance)
    val isDark = isSystemInDarkTheme()

    val gradientStart = if (isDark) BalanceGradientStartDark else BalanceGradientStartLight
    val gradientEnd = if (isDark) BalanceGradientEndDark else BalanceGradientEndLight

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    ),
                    shape = RoundedCornerShape(Radius.extraLarge)
                )
                .padding(Spacing.large + Spacing.small) // slightly more breathing room
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top row: label + toggle visibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Saldo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    // Slightly more prominent eye button
                    IconButton(
                        onClick = onToggleHideBalance,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideBalance) "Tampilkan Saldo" else "Sembunyikan Saldo",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                // Large balance text
                if (hideBalance) {
                    Text(
                        text = "••••••",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    AutoResizeText(
                        text = formattedBalance,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                // Income & Expense summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Pemasukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        Text(
                            text = if (hideBalance) "••••••" else CurrencyFormatter.format(totalIncome),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen.copy(alpha = 0.95f) // slightly differentiated green on gradient
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        Text(
                            text = if (hideBalance) "••••••" else CurrencyFormatter.format(totalExpense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
