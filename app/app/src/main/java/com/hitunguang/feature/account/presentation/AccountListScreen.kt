package com.hitunguang.feature.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.presentation.components.AccountFormDialog
import com.hitunguang.feature.account.presentation.components.DeleteAccountDialog
import com.hitunguang.feature.transfer.presentation.components.TransferDialog
import com.hitunguang.core.common.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    onNavigateToTransferHistory: () -> Unit,
    showSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showTransferDialog by remember { mutableStateOf(false) }


    val cashWallets = remember(accounts) { accounts.filter { it.accountType == "CASH" } }
    val bankWallets = remember(accounts) { accounts.filter { it.accountType == "BANK" } }
    val eWalletWallets = remember(accounts) { accounts.filter { it.accountType == "E_WALLET" } }

    var cashExpanded by rememberSaveable { mutableStateOf(true) }
    var bankExpanded by rememberSaveable { mutableStateOf(true) }
    var eWalletExpanded by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Akun", fontWeight = FontWeight.Bold) }
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
                    contentDescription = "Tambah Akun"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = Spacing.large,
                end = Spacing.large,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Wallet Summary Section
            item {
                WalletSummaryCard(
                    totalBalance = accounts.sumOf { it.currentBalance },
                    walletCount = accounts.size
                )
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Button(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = "Transfer Dana",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onNavigateToTransferHistory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = "Riwayat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Collapsible Wallets Groups
            if (accounts.isEmpty()) {
                item {
                    EmptyWalletState()
                }
            } else {
                // Tunai Section
                if (cashWallets.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cashExpanded = !cashExpanded }
                                .padding(vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (cashExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (cashExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = "Tunai",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(cashWallets.sumOf { it.currentBalance }),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (cashExpanded) {
                        items(cashWallets, key = { it.id }) { account ->
                            WalletListItem(
                                account = account,
                                onEdit = { viewModel.showEditDialog(account) },
                                onDelete = { viewModel.showDeleteDialog(account) }
                            )
                        }
                    }
                }

                // Bank Section
                if (bankWallets.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bankExpanded = !bankExpanded }
                                .padding(vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (bankExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (bankExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = "Bank",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(bankWallets.sumOf { it.currentBalance }),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (bankExpanded) {
                        items(bankWallets, key = { it.id }) { account ->
                            WalletListItem(
                                account = account,
                                onEdit = { viewModel.showEditDialog(account) },
                                onDelete = { viewModel.showDeleteDialog(account) }
                            )
                        }
                    }
                }

                // E-Wallet Section
                if (eWalletWallets.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { eWalletExpanded = !eWalletExpanded }
                                .padding(vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (eWalletExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (eWalletExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = "E-Wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(eWalletWallets.sumOf { it.currentBalance }),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (eWalletExpanded) {
                        items(eWalletWallets, key = { it.id }) { account ->
                            WalletListItem(
                                account = account,
                                onEdit = { viewModel.showEditDialog(account) },
                                onDelete = { viewModel.showDeleteDialog(account) }
                            )
                        }
                    }
                }
            }


        }
    }

    if (uiState.showCreateDialog) {
        AccountFormDialog(
            account = null,
            onDismiss = { viewModel.hideCreateDialog() },
            onSave = { name, type, icon, balance ->
                viewModel.createAccount(name, type, icon, balance)
                showSnackbar("Akun berhasil dibuat")
            }
        )
    }

    if (uiState.showEditDialog && uiState.accountToEdit != null) {
        AccountFormDialog(
            account = uiState.accountToEdit,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { name, type, icon, _ ->
                viewModel.updateAccount(uiState.accountToEdit!!, name, type, icon)
                showSnackbar("Akun berhasil diperbarui")
            }
        )
    }

    if (uiState.showDeleteDialog && uiState.accountToDelete != null) {
        DeleteAccountDialog(
            account = uiState.accountToDelete!!,
            hasTransactions = uiState.hasTransactions,
            availableAccounts = accounts,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { replacementId ->
                viewModel.deleteAccount(replacementId)
                showSnackbar("Akun berhasil dihapus")
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            onDismiss = { showTransferDialog = false },
            showSnackbar = showSnackbar
        )
    }
}

@Composable
fun WalletSummaryCard(
    totalBalance: Long,
    walletCount: Int,
    modifier: Modifier = Modifier
) {


    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        shape = RoundedCornerShape(Radius.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(Spacing.extraHuge)
        ) {
            Column {
                Text(
                    text = "Total Saldo Anda",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = CurrencyFormatter.format(totalBalance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = "$walletCount Dompet Terdaftar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun WalletListItem(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {


    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (account.accountType) {
                            "BANK" -> Icons.Default.AccountBalance
                            "E_WALLET" -> Icons.Default.AccountBalanceWallet
                            else -> Icons.Default.Wallet
                        },
                        contentDescription = account.accountType,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.medium))
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Saldo: ${CurrencyFormatter.format(account.currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyWalletState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.extraHuge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Wallet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(Spacing.massive)
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = "Belum ada dompet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}






