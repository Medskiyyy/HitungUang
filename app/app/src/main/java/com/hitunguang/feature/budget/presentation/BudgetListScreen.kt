package com.hitunguang.feature.budget.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.budget.presentation.components.BudgetFormDialog
import com.hitunguang.feature.category.presentation.components.CategoryIconHelper
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
                title = { Text("Anggaran", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Radius.medium)
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
                    text = { Text("Aktif", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Riwayat", fontWeight = FontWeight.Bold) }
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
                            .padding(Spacing.large),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(Spacing.massive)
                        )
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        Text(
                            text = if (selectedTabIndex == 0) "Belum ada anggaran aktif" else "Belum ada riwayat anggaran",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.large),
                        contentPadding = PaddingValues(
                            start = Spacing.large,
                            end = Spacing.large,
                            top = Spacing.medium,
                            bottom = 80.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                                        .padding(bottom = Spacing.small),
                                    shape = RoundedCornerShape(Radius.extraLarge),
                                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.large),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.large)
                                    ) {
                                        // Left side: Circular Gauge
                                        Box(
                                            modifier = Modifier.size(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val gaugeColor = when {
                                                totalProgress > 0.8f -> MaterialTheme.colorScheme.error
                                                totalProgress > 0.6f -> MaterialTheme.colorScheme.tertiary
                                                else -> IncomeGreen
                                            }

                                            CircularProgressIndicator(
                                                progress = { totalProgress.coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxSize(),
                                                strokeWidth = 10.dp,
                                                color = gaugeColor,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${(totalProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Terpakai",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Right side: Statistics Column
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
                                        ) {
                                            Text(
                                                text = "Total Anggaran Aktif",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Limit: Rp ${formatter.format(totalLimit)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Terpakai: Rp ${formatter.format(totalSpent)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Sisa: Rp ${formatter.format(totalRemaining)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (totalRemaining <= 0L && totalLimit > 0) MaterialTheme.colorScheme.error else IncomeGreen
                                            )

                                            Spacer(modifier = Modifier.height(Spacing.extraSmall))

                                            val healthSummaryList = mutableListOf<String>()
                                            if (safeCount > 0) healthSummaryList.add("$safeCount Aman")
                                            if (warningCount > 0) healthSummaryList.add("$warningCount Warning")
                                            if (overCount > 0) healthSummaryList.add("$overCount Over")
                                            val healthSummary = if (healthSummaryList.isEmpty()) "Tidak ada anggaran" else healthSummaryList.joinToString(" • ")

                                            Text(
                                                text = healthSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(displayList, key = { it.budget.id }) { budgetProgress ->
                            BudgetProgressCard(
                                budgetProgress = budgetProgress,
                                categories = uiState.categories,
                                onEditClick = { viewModel.showEditDialog(it) },
                                onDeleteClick = { viewModel.deleteBudget(it) }
                            )
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
    categories: List<com.hitunguang.feature.category.domain.model.Category> = emptyList(),
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

    val category = remember(budget.categoryId, categories) {
        categories.find { it.id == budget.categoryId }
    }

    val progressValue = (budgetProgress.progressPercent / 100f).coerceIn(0f, 1f)

    val progressColor = when {
        budgetProgress.isOverBudget -> MaterialTheme.colorScheme.error
        budgetProgress.isThresholdReached -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when {
                        budget.budgetType == "GLOBAL" -> Icons.Default.BarChart
                        category != null -> CategoryIconHelper.getIconByName(category.icon)
                        else -> Icons.Default.BarChart
                    }
                    val iconColor = when {
                        budgetProgress.isOverBudget -> MaterialTheme.colorScheme.error
                        budgetProgress.isThresholdReached -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(iconColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.medium))

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
                            Spacer(modifier = Modifier.width(Spacing.small))

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
                                    .clip(RoundedCornerShape(Radius.extraSmall))
                                    .background(badgeBgColor)
                                    .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        Text(
                            text = dateRangeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

            Spacer(modifier = Modifier.height(Spacing.medium))

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

            Spacer(modifier = Modifier.height(Spacing.small))

            LinearProgressIndicator(
                progress = { progressValue },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.small)
                    .clip(RoundedCornerShape(Radius.small))
            )

            Spacer(modifier = Modifier.height(Spacing.small))

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
