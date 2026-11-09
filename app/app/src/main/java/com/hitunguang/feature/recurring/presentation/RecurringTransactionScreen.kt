package com.hitunguang.feature.recurring.presentation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.presentation.components.RecurringTransactionFormDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var recurringToDelete by remember { mutableStateOf<RecurringTransaction?>(null) }
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Berulang", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
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
                    contentDescription = "Tambah transaksi berulang"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(Spacing.large),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(Spacing.massive)
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text(
                        text = "Belum ada transaksi berulang. Ketuk + buat bikin yang pertama, misalnya bayar kos tiap bulan.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    contentPadding = PaddingValues(
                        start = Spacing.large,
                        end = Spacing.large,
                        top = Spacing.medium,
                        bottom = 80.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(uiState.items, key = { it.recurring.id }) { item ->
                        RecurringTransactionCard(
                            item = item,
                            dateFormatter = dateFormatter,
                            onClick = { viewModel.showEditDialog(item.recurring) },
                            onActiveChange = { viewModel.setActive(item.recurring, it) },
                            onDeleteClick = { recurringToDelete = item.recurring }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showFormDialog) {
        RecurringTransactionFormDialog(
            recurring = uiState.recurringToEdit,
            accounts = uiState.accounts,
            categories = uiState.categories,
            errorMessage = uiState.error,
            onDismiss = { viewModel.dismissFormDialog() },
            onSave = { type, title, note, amount, accountId, categoryId, rule, startDate ->
                viewModel.saveRecurring(type, title, note, amount, accountId, categoryId, rule, startDate)
            }
        )
    }

    recurringToDelete?.let { recurring ->
        AlertDialog(
            onDismissRequest = { recurringToDelete = null },
            title = { Text("Hapus transaksi berulang?", fontWeight = FontWeight.Bold) },
            text = { Text("Hapus transaksi berulang ini? Transaksi yang udah tercatat nggak ikut kehapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecurring(recurring)
                        recurringToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { recurringToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun RecurringTransactionCard(
    item: RecurringTransactionItem,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    val recurring = item.recurring
    val isExpense = recurring.transactionType == "EXPENSE"
    val details = listOfNotNull(item.accountName ?: "Dompet udah dihapus", item.categoryName).joinToString(" • ")
    val schedule = RecurrenceText.describe(recurring.recurrenceRule, recurring.nextRunAt) + " • " +
        if (recurring.isActive) "Berikutnya ${dateFormatter.format(Date(recurring.nextRunAt))}" else "Dijeda"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.large, top = Spacing.large, end = Spacing.small, bottom = Spacing.small)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recurring.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "${if (isExpense) "-" else "+"}${CurrencyFormatter.format(recurring.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) ExpenseRed else IncomeGreen
                )
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.extraSmall)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = recurring.isActive,
                    onCheckedChange = onActiveChange
                )
            }
        }
    }
}
