package com.hitunguang.feature.transaction.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import java.io.File
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
    var categoryExpanded by remember { mutableStateOf(false) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) "Edit Transaksi" else "Tambah Transaksi", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        val amount = it.toLongOrNull() ?: 0L
                        amountError = if (amount <= 0L) "Nominal harus lebih besar dari 0" else null
                    },
                    label = { Text("Nominal (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Tipe Transaksi", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = type == "EXPENSE",
                            onClick = { type = "EXPENSE" }
                        )
                        Text("Pengeluaran")

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = type == "INCOME",
                            onClick = { type = "INCOME" }
                        )
                        Text("Pemasukan")
                    }
                }

                // Dompet Dropdown
                Column {
                    Text("Dompet *", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentAccount = accounts.find { it.id == selectedAccountId }
                        OutlinedButton(
                            onClick = { accountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentAccount?.name ?: "Pilih Dompet")
                        }
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Kategori Dropdown
                Column {
                    Text("Kategori", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentCategory = categories.find { it.id == selectedCategoryId }
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentCategory?.name ?: "Pilih Kategori")
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Tanggal Picker Button
                Column {
                    Text("Tanggal Transaksi", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateFormatter.format(Date(dateInMillis)))
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                AttachmentGrid(
                    attachments = allDisplayAttachments,
                    onAddClick = { showAttachmentSourceDialog = true },
                    onDeleteClick = onDeleteAttachment,
                    onAttachmentClick = onAttachmentClick,
                    isEditable = true
                )
            }
        },
        confirmButton = {
            TextButton(
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
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

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
