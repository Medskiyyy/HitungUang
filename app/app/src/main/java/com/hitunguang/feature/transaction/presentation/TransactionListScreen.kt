package com.hitunguang.feature.transaction.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.designsystem.components.HUTransactionCard
import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.presentation.components.AttachmentPreviewDialog
import com.hitunguang.feature.transaction.presentation.components.DeleteTransactionDialog
import com.hitunguang.feature.transaction.presentation.components.TransactionDetailDialog
import com.hitunguang.feature.transaction.presentation.components.TransactionFormDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onSearchClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    showSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val customDateRange by viewModel.customDateRange.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var activePreviewAttachment by remember { mutableStateOf<Attachment?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }


    val groupedTransactions = remember(transactions) {
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
        val todayStr = formatter.format(Date())
        val yesterdayStr = formatter.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))

        transactions.groupBy {
            val dateStr = formatter.format(Date(it.transactionDate))
            when (dateStr) {
                todayStr -> "Hari Ini"
                yesterdayStr -> "Kemarin"
                else -> dateStr
            }
        }
    }

    val periods = listOf(
        "ALL" to "Semua",
        "TODAY" to "Hari Ini",
        "WEEKLY" to "Minggu Ini",
        "MONTHLY" to "Bulan Ini",
        "YEARLY" to "Tahun Ini",
        "CUSTOM" to "Pilih Tanggal"
    )

    val sorts = listOf(
        "NEWEST" to "Terbaru",
        "OLDEST" to "Terlama",
        "HIGHEST" to "Tertinggi",
        "LOWEST" to "Terendah"
    )

    val sdf = remember { SimpleDateFormat("dd/MM", Locale("in", "ID")) }
    val customRangeLabel = if (selectedPeriod == "CUSTOM" && customDateRange != null) {
        "Pilih Tanggal (${sdf.format(Date(customDateRange!!.first))} - ${sdf.format(Date(customDateRange!!.second))})"
    } else {
        "Pilih Tanggal"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari"
                        )
                    }
                }
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
                    contentDescription = "Tambah Transaksi"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.large)
        ) {
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.large)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.small),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentPeriodLabel = periods.find { it.first == selectedPeriod }?.second ?: "Semua"
                val currentSortLabel = sorts.find { it.first == selectedSort }?.second ?: "Terbaru"
                val filterText = if (selectedPeriod == "ALL" && selectedSort == "NEWEST") {
                    "Filter ▼"
                } else {
                    "Filter: $currentPeriodLabel • $currentSortLabel ▼"
                }

                Button(
                    onClick = { showFilterBottomSheet = true },
                    shape = RoundedCornerShape(Radius.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(text = filterText, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(Spacing.massive)
                    )
                    Spacer(modifier = Modifier.height(Spacing.large))
                    Text(
                        text = "Belum ada transaksi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedTransactions.forEach { (dateHeader, dailyTxs) ->
                        item(key = dateHeader) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.medium, bottom = Spacing.small),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val net = dailyTxs.sumOf {
                                    if (it.transactionType == "INCOME") it.amount else -it.amount
                                }
                                val isPositive = net >= 0
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.full))
                                        .background(
                                            if (isPositive) IncomeGreen.copy(alpha = 0.1f)
                                            else ExpenseRed.copy(alpha = 0.1f)
                                        )
                                        .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.format(net, showSign = true),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPositive) IncomeGreen else ExpenseRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(dailyTxs, key = { it.id }) { tx ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    when (dismissValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            viewModel.showEditDialog(tx)
                                            false
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            viewModel.showDeleteDialog(tx)
                                            false
                                        }
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                        SwipeToDismissBoxValue.Settled -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                    val alignment = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        SwipeToDismissBoxValue.Settled -> Alignment.Center
                                    }
                                    val icon = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                        SwipeToDismissBoxValue.Settled -> null
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(Radius.medium))
                                            .background(color)
                                            .padding(horizontal = Spacing.doubleLarge),
                                        contentAlignment = alignment
                                    ) {
                                        if (icon != null) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = when (dismissState.dismissDirection) {
                                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                },
                                content = {
                                    HUTransactionCard(
                                        transaction = tx,
                                        onClick = { viewModel.showDetailsDialog(tx) }
                                    )
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(Spacing.massive * 2))
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = end
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calendar.set(java.util.Calendar.MINUTE, 59)
                            calendar.set(java.util.Calendar.SECOND, 59)
                            calendar.set(java.util.Calendar.MILLISECOND, 999)
                            viewModel.setCustomDateRange(start, calendar.timeInMillis)
                            viewModel.setPeriod("CUSTOM")
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (uiState.showCreateDialog) {
        val draft by viewModel.transactionDraft.collectAsState()
        TransactionFormDialog(
            transaction = null,
            accounts = accounts,
            categories = categories,
            attachments = emptyList(),
            pendingAttachmentUris = uiState.pendingAttachmentUris,
            draft = draft,
            onDraftChanged = { viewModel.saveDraft(it) },
            onAddAttachment = { viewModel.addPendingAttachment(it) },
            onDeleteAttachment = { attachment ->
                viewModel.removePendingAttachment(android.net.Uri.parse(attachment.filePath))
            },
            onAttachmentClick = { activePreviewAttachment = it },
            onDismiss = { viewModel.hideCreateDialog() },
            onManageCategoriesClick = onManageCategoriesClick,
            onSave = { accountId, categoryId, type, title, note, amount, date ->
                viewModel.createTransaction(accountId, categoryId, type, title, note, amount, date)
                showSnackbar("Transaksi berhasil disimpan")
            }
        )
    }

    if (uiState.showEditDialog && uiState.transactionToEdit != null) {
        TransactionFormDialog(
            transaction = uiState.transactionToEdit,
            accounts = accounts,
            categories = categories,
            attachments = uiState.attachments,
            pendingAttachmentUris = emptyList(),
            draft = null,
            onDraftChanged = {},
            onAddAttachment = { uri ->
                viewModel.addAttachmentToExistingTransaction(uiState.transactionToEdit!!.id, uri)
            },
            onDeleteAttachment = { attachment ->
                viewModel.deleteAttachmentFromExistingTransaction(attachment)
            },
            onAttachmentClick = { activePreviewAttachment = it },
            onDismiss = { viewModel.hideEditDialog() },
            onManageCategoriesClick = onManageCategoriesClick,
            onSave = { accountId, categoryId, type, title, note, amount, date ->
                viewModel.updateTransaction(
                    transactionId = uiState.transactionToEdit!!.id,
                    accountId = accountId,
                    categoryId = categoryId,
                    transactionType = type,
                    title = title,
                    note = note,
                    amount = amount,
                    transactionDate = date,
                    createdAt = uiState.transactionToEdit!!.createdAt
                )
                showSnackbar("Transaksi berhasil diperbarui")
            }
        )
    }

    if (uiState.showDetailsDialog && uiState.transactionDetails != null) {
        TransactionDetailDialog(
            transaction = uiState.transactionDetails!!,
            attachments = uiState.attachments,
            onAttachmentClick = { activePreviewAttachment = it },
            onDismiss = { viewModel.hideDetailsDialog() },
            onEdit = { viewModel.showEditDialog(uiState.transactionDetails!!) },
            onDelete = { viewModel.showDeleteDialog(uiState.transactionDetails!!) }
        )
    }

    if (uiState.showDeleteDialog && uiState.transactionToDelete != null) {
        DeleteTransactionDialog(
            transaction = uiState.transactionToDelete!!,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = {
                viewModel.deleteTransaction()
                showSnackbar("Transaksi berhasil dihapus")
            }
        )
    }

    if (activePreviewAttachment != null) {
        AttachmentPreviewDialog(
            attachment = activePreviewAttachment!!,
            onDismissRequest = { activePreviewAttachment = null }
        )
    }

    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large)
            ) {
                Text(
                    text = "Filter Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Spacing.large)
                )

                // Period Section
                Text(
                    text = "Periode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        periods.take(3).forEach { (chipValue, label) ->
                            val isSelected = selectedPeriod == chipValue
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (chipValue == "CUSTOM") {
                                        showDateRangePicker = true
                                    } else {
                                        viewModel.setPeriod(chipValue)
                                    }
                                },
                                label = { Text(label) },
                                shape = RoundedCornerShape(Radius.medium),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        val remainingPeriods = periods.drop(3)
                        remainingPeriods.forEach { (chipValue, label) ->
                            val isSelected = selectedPeriod == chipValue
                            val displayLabel = if (chipValue == "CUSTOM" && customDateRange != null) {
                                "${sdf.format(Date(customDateRange!!.first))} - ${sdf.format(Date(customDateRange!!.second))}"
                            } else label
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (chipValue == "CUSTOM") {
                                        showDateRangePicker = true
                                    } else {
                                        viewModel.setPeriod(chipValue)
                                    }
                                },
                                label = { Text(displayLabel) },
                                shape = RoundedCornerShape(Radius.medium),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                // Sort Section
                Text(
                    text = "Urutkan Berdasarkan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        sorts.take(2).forEach { (chipValue, label) ->
                            val isSelected = selectedSort == chipValue
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSort(chipValue) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(Radius.medium),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        sorts.drop(2).forEach { (chipValue, label) ->
                            val isSelected = selectedSort == chipValue
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSort(chipValue) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(Radius.medium),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.extraHuge))

                Button(
                    onClick = { showFilterBottomSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.medium)
                ) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
