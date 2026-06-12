package com.hitunguang.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.NumberFormat
import java.util.Locale

import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.dashboard.presentation.components.BalanceCard
import com.hitunguang.feature.dashboard.presentation.components.BudgetSummaryCard
import com.hitunguang.feature.dashboard.presentation.components.ExpenseChartCard
import com.hitunguang.feature.dashboard.presentation.components.QuickActionsSection
import com.hitunguang.feature.dashboard.presentation.components.RecentTransactionsSection
import com.hitunguang.feature.transfer.presentation.components.TransferDialog
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.presentation.components.TransactionDetailDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddTransactionClick: (type: String) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Halo, ${uiState.userName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.large)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.doubleLarge)
            ) {
                // Balance Card (Redesigned Hero Balance Card)
                BalanceCard(
                    totalBalance = uiState.totalBalance,
                    hideBalance = uiState.hideBalance,
                    onToggleHideBalance = { viewModel.toggleHideBalance() },
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    netDifference = uiState.netDifference
                )


                // Quick Actions Grid
                QuickActionsSection(
                    onAddExpenseClick = { onAddTransactionClick("EXPENSE") },
                    onAddIncomeClick = { onAddTransactionClick("INCOME") },
                    onTransferClick = { showTransferDialog = true },
                    onScanClick = onScanClick
                )

                // Budget Utilization Card
                BudgetSummaryCard(
                    budgetProgressList = uiState.budgetProgressList,
                    onAddBudgetClick = onNavigateToBudgets
                )

                // Expense Distribution Chart
                ExpenseChartCard(
                    expenseCategoriesDistribution = uiState.expenseCategoriesDistribution
                )

                // Recent Transactions
                RecentTransactionsSection(
                    transactions = uiState.recentTransactions,
                    onViewAllClick = onNavigateToTransactions,
                    onTransactionClick = { selectedTransactionForDetail = it }
                )

                Spacer(modifier = Modifier.height(Spacing.huge)) // Extra space for bottom nav
            }
        }
    }

    if (selectedTransactionForDetail != null) {
        TransactionDetailDialog(
            transaction = selectedTransactionForDetail!!,
            attachments = emptyList(), // View only, attachments can be viewed in transaction detail screen
            onAttachmentClick = {},
            onDismiss = { selectedTransactionForDetail = null },
            onEdit = {
                selectedTransactionForDetail = null
                onNavigateToTransactions()
            },
            onDelete = {
                selectedTransactionForDetail = null
                onNavigateToTransactions()
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            onDismiss = { showTransferDialog = false }
        )
    }
}


@Composable
fun FinancialSummaryInfo(
    totalIncome: Long,
    totalExpense: Long,
    netDifference: Long,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getIntegerInstance(Locale("in", "ID"))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pemasukan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rp ${formatter.format(totalIncome)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pengeluaran",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rp ${formatter.format(totalExpense)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selisih",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (netDifference >= 0) "+" else ""}Rp ${formatter.format(netDifference)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netDifference >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
