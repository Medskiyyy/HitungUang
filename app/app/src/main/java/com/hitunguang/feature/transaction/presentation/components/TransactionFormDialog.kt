package com.hitunguang.feature.transaction.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.presentation.components.CategoryIconHelper
import com.hitunguang.feature.category.presentation.components.CategoryPickerDialog
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    transaction: TransactionWithDetails?,
    accounts: List<Account>,
    categories: List<Category>,
    attachments: List<Attachment>,
    pendingAttachmentUris: List<android.net.Uri>,
    draft: TransactionDraft?,
    onDraftChanged: (TransactionDraft) -> Unit,
    onAddAttachment: (android.net.Uri) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    onDismiss: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onSave: (
        accountId: String,
        categoryId: String?,
        type: String,
        title: String,
        note: String?,
        amount: Long,
        date: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var amountStr by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(transaction?.transactionType ?: "EXPENSE") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var dateInMillis by remember { mutableStateOf(transaction?.transactionDate ?: System.currentTimeMillis()) }

    var selectedAccountId by remember { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction?.categoryId ?: "") }

    var hasRestoredDraft by remember { mutableStateOf(false) }

    LaunchedEffect(draft) {
        if (transaction == null && draft != null && !hasRestoredDraft) {
            title = draft.title ?: ""
            amountStr = draft.amount?.toString() ?: ""
            type = draft.transactionType ?: "EXPENSE"
            note = draft.note ?: ""
            if (draft.accountId != null) {
                selectedAccountId = draft.accountId
            }
            if (draft.categoryId != null) {
                selectedCategoryId = draft.categoryId
            }
            hasRestoredDraft = true
        }
    }

    LaunchedEffect(type, selectedAccountId, selectedCategoryId, title, note, amountStr) {
        if (transaction == null) {
            val amount = amountStr.toLongOrNull()
            onDraftChanged(
                TransactionDraft(
                    transactionType = type,
                    accountId = selectedAccountId,
                    categoryId = selectedCategoryId.ifBlank { null },
                    title = title,
                    note = note.ifBlank { null },
                    amount = amount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val dummyAttachments = remember(pendingAttachmentUris) {
        pendingAttachmentUris.map { uri ->
            Attachment(
                id = uri.toString(),
                transactionId = transaction?.id ?: "",
                filePath = uri.toString(),
                mimeType = "image/jpeg",
                fileSize = 0L,
                createdAt = System.currentTimeMillis()
            )
        }
    }
    val allDisplayAttachments = remember(attachments, dummyAttachments) {
        attachments + dummyAttachments
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                onAddAttachment(uri)
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = (5 - allDisplayAttachments.size).coerceAtLeast(1)
        )
    ) { uris ->
        uris.forEach { uri ->
            onAddAttachment(uri)
        }
    }

    var showAttachmentSourceDialog by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }

    var accountExpanded by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val filteredCategories = remember(type, categories) {
        categories.filter { it.categoryType == type }
    }

    LaunchedEffect(type, filteredCategories) {
        if (selectedCategoryId.isEmpty() || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id ?: ""
        }
    }

    val isEditMode = transaction != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val formatAmount: (String) -> String = { input ->
        val amount = input.toLongOrNull() ?: 0L
        NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.large)
            ) {
                Text(
                    text = if (isEditMode) "Edit Transaksi" else "Tambah Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = Spacing.medium)
                )

                // Segmented Type Selector
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.medium),
                    shape = RoundedCornerShape(Radius.medium),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.extraSmall)
                    ) {
                        listOf("EXPENSE" to "Pengeluaran", "INCOME" to "Pemasukan").forEach { (itemType, label) ->
                            val isSelected = type == itemType
                            val backgroundColor = if (isSelected) {
                                if (itemType == "EXPENSE") ExpenseRed else IncomeGreen
                            } else Color.Transparent

                            val contentColor = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(Spacing.extraHuge)
                                    .clip(RoundedCornerShape(Radius.medium))
                                    .background(backgroundColor)
                                    .clickable { type = itemType },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Hero Amount Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rp ${formatAmount(amountStr)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                amountStr = it
                                val amount = it.toLongOrNull() ?: 0L
                                amountError = if (amount <= 0L) "Nominal harus lebih besar dari 0" else null
                            }
                        },
                        placeholder = { Text("0") },
                        label = { Text("Nominal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError != null,
                        supportingText = { amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        shape = RoundedCornerShape(Radius.medium),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen,
                            focusedLabelColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = if (it.isBlank()) "Judul tidak boleh kosong" else null
                    },
                    label = { Text("Judul Transaksi *") },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(Radius.medium),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Horizontal Category Picker
                Column {
                    Text(
                        "Kategori",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        contentPadding = PaddingValues(bottom = Spacing.small)
                    ) {
                        val topCategories = filteredCategories.take(8)
                        items(topCategories) { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIconByName(category.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                shape = RoundedCornerShape(Radius.medium),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (type == "EXPENSE") ExpenseRed.copy(alpha = 0.1f) else IncomeGreen.copy(alpha = 0.1f),
                                    selectedLabelColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen,
                                    selectedLeadingIconColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showCategoryPicker = true },
                                label = { Text("Lainnya") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                shape = RoundedCornerShape(Radius.medium)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Dompet Selector
                Column {
                    Text(
                        "Dompet *",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentAccount = accounts.find { it.id == selectedAccountId }
                        Surface(
                            onClick = { accountExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.medium),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline
                            ),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (currentAccount?.accountType) {
                                        "BANK" -> Icons.Default.AccountBalance
                                        "E_WALLET" -> Icons.Default.AccountBalanceWallet
                                        else -> Icons.Default.Wallet
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Spacing.medium))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        currentAccount?.name ?: "Pilih Dompet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Saldo: Rp ${currentAccount?.currentBalance?.let { NumberFormat.getNumberInstance(Locale("in", "ID")).format(it) } ?: "0"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (acc.accountType) {
                                                    "BANK" -> Icons.Default.AccountBalance
                                                    "E_WALLET" -> Icons.Default.AccountBalanceWallet
                                                    else -> Icons.Default.Wallet
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.medium))
                                            Column {
                                                Text(acc.name, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "Rp ${NumberFormat.getNumberInstance(Locale("in", "ID")).format(acc.currentBalance)}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Tanggal Picker
                Column {
                    Text(
                        "Tanggal Transaksi",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
                    Surface(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.medium),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.medium))
                            Text(dateFormatter.format(Date(dateInMillis)))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (Opsional)") },
                    shape = RoundedCornerShape(Radius.medium),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                AttachmentGrid(
                    attachments = allDisplayAttachments,
                    onAddClick = { showAttachmentSourceDialog = true },
                    onDeleteClick = onDeleteAttachment,
                    onAttachmentClick = onAttachmentClick,
                    isEditable = true
                )
                
                Spacer(modifier = Modifier.height(Spacing.huge))
            }

            // Sticky Bottom Actions
            Surface(
                shadowElevation = Elevation.extraHigh,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val amount = amountStr.toLongOrNull() ?: 0L
                            var hasError = false

                            if (title.isBlank()) {
                                titleError = "Judul tidak boleh kosong"
                                hasError = true
                            }
                            if (amount <= 0L) {
                                amountError = "Nominal harus lebih besar dari 0"
                                hasError = true
                            }
                            if (selectedAccountId.isBlank()) {
                                accountError = "Harus memilih dompet"
                                hasError = true
                            }

                            if (!hasError) {
                                onSave(
                                    selectedAccountId,
                                    selectedCategoryId.ifBlank { null },
                                    type,
                                    title,
                                    note.ifBlank { null },
                                    amount,
                                    dateInMillis
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                        )
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateInMillis = datePickerState.selectedDateMillis ?: dateInMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAttachmentSourceDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentSourceDialog = false },
            title = { Text("Pilih Sumber Gambar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAttachmentSourceDialog = false
                            val uri = getTempPhotoUri(context)
                            tempPhotoUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kamera", style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(
                        onClick = {
                            showAttachmentSourceDialog = false
                            pickMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Galeri", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentSourceDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = filteredCategories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { cat ->
                selectedCategoryId = cat.id
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
            onManageCategoriesClick = {
                onDismiss()
                onManageCategoriesClick()
            }
        )
    }
}

private fun getTempPhotoUri(context: android.content.Context): android.net.Uri {
    val tempFile = File.createTempFile("camera_photo_", ".jpg", context.cacheDir).apply {
        deleteOnExit()
    }
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "com.hitunguang.fileprovider",
        tempFile
    )
}
