package com.hitunguang.feature.budget.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.feature.budget.presentation.components.BudgetFormDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val idLocale = remember { Locale("in", "ID") }
    val formatter = remember { NumberFormat.getIntegerInstance(idLocale) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anggaran Saya", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Anggaran"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Anggaran Aktif") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Riwayat Anggaran") }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val displayList = if (selectedTabIndex == 0) uiState.activeBudgets else uiState.completedBudgets
                if (displayList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = if (selectedTabIndex == 0) "Belum ada anggaran aktif" else "Belum ada riwayat anggaran",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (selectedTabIndex == 0 && uiState.activeBudgets.isNotEmpty()) {
                            item(key = "budget_summary") {
                                val totalLimit = uiState.activeBudgets.sumOf { it.budget.amount }
                                val totalSpent = uiState.activeBudgets.sumOf { it.spentAmount }
                                val totalRemaining = uiState.activeBudgets.sumOf { it.remainingAmount }
                                val totalProgress = if (totalLimit > 0) totalSpent.toFloat() / totalLimit.toFloat() else 0f

                                val safeCount = uiState.activeBudgets.count { !it.isOverBudget && !it.isThresholdReached }
                                val warningCount = uiState.activeBudgets.count { !it.isOverBudget && it.isThresholdReached }
                                val overCount = uiState.activeBudgets.count { it.isOverBudget }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "Total Anggaran Aktif",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Rp ${formatter.format(totalLimit)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Terpakai",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "Rp ${formatter.format(totalSpent)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Sisa",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "Rp ${formatter.format(totalRemaining)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (totalRemaining == 0L && totalLimit > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        LinearProgressIndicator(
                                            progress = { totalProgress.coerceIn(0f, 1f) },
                                            color = if (totalSpent > totalLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val healthSummaryList = mutableListOf<String>()
                                        if (safeCount > 0) healthSummaryList.add("$safeCount Aman")
                                        if (warningCount > 0) healthSummaryList.add("$warningCount Mendekati Limit")
                                        if (overCount > 0) healthSummaryList.add("$overCount Melebihi Limit")
                                        val healthSummary = if (healthSummaryList.isEmpty()) "Tidak ada anggaran" else healthSummaryList.joinToString(" • ")

                                        Text(
                                            text = healthSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }

                        items(displayList, key = { it.budget.id }) { budgetProgress ->
                            BudgetProgressCard(
                                budgetProgress = budgetProgress,
                                onEditClick = { viewModel.showEditDialog(it) },
                                onDeleteClick = { viewModel.deleteBudget(it) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (uiState.showFormDialog) {
        BudgetFormDialog(
            budget = uiState.budgetToEdit,
            categories = uiState.categories,
            onDismiss = { viewModel.dismissFormDialog() },
            onSave = { categoryId, budgetType, amount, threshold, start, end ->
                viewModel.saveBudget(categoryId, budgetType, amount, threshold, start, end)
            }
        )
    }
}

@Composable
fun BudgetProgressCard(
    budgetProgress: BudgetWithProgress,
    onEditClick: (com.hitunguang.feature.budget.domain.model.Budget) -> Unit,
    onDeleteClick: (com.hitunguang.feature.budget.domain.model.Budget) -> Unit,
    modifier: Modifier = Modifier
) {
    val idLocale = Locale("in", "ID")
    val formatter = NumberFormat.getIntegerInstance(idLocale)
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", idLocale)

    val budget = budgetProgress.budget
    val title = if (budget.budgetType == "GLOBAL") "Anggaran Global" else budgetProgress.categoryName ?: "Kategori Kustom"
    val dateRangeText = "${dateFormatter.format(Date(budget.startDate))} - ${dateFormatter.format(Date(budget.endDate))}"

    val progressValue = (budgetProgress.progressPercent / 100f).coerceIn(0f, 1f)

    val progressColor = when {
        budgetProgress.isOverBudget -> MaterialTheme.colorScheme.error
        budgetProgress.isThresholdReached -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        val badgeText = when {
                            budgetProgress.isOverBudget -> "Over Budget"
                            budgetProgress.isThresholdReached -> "Warning"
                            else -> "Aman"
                        }
                        val badgeBgColor = when {
                            budgetProgress.isOverBudget -> MaterialTheme.colorScheme.errorContainer
                            budgetProgress.isThresholdReached -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val badgeTextColor = when {
                            budgetProgress.isOverBudget -> MaterialTheme.colorScheme.onErrorContainer
                            budgetProgress.isThresholdReached -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateRangeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEditClick(budget) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteClick(budget) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai: Rp ${formatter.format(budgetProgress.spentAmount)} / Rp ${formatter.format(budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format(Locale.US, "%.0f", budgetProgress.progressPercent)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressValue },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (budgetProgress.isOverBudget) {
                    "Melebihi anggaran sebesar Rp ${formatter.format(budgetProgress.spentAmount - budget.amount)}!"
                } else {
                    "Sisa limit: Rp ${formatter.format(budgetProgress.remainingAmount)}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (budgetProgress.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
