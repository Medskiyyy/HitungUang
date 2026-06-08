package com.hitunguang.feature.transaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.domain.usecase.AddAttachmentUseCase
import com.hitunguang.feature.transaction.domain.usecase.ClearDraftUseCase
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteAttachmentUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.GetAttachmentsUseCase
import com.hitunguang.feature.transaction.domain.usecase.GetDraftUseCase
import com.hitunguang.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.hitunguang.feature.transaction.domain.usecase.SaveDraftUseCase
import com.hitunguang.feature.transaction.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransactionUiState(
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDetailsDialog: Boolean = false,
    val transactionToEdit: TransactionWithDetails? = null,
    val transactionToDelete: TransactionWithDetails? = null,
    val transactionDetails: TransactionWithDetails? = null,
    val attachments: List<Attachment> = emptyList(),
    val pendingAttachmentUris: List<android.net.Uri> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getAttachmentsUseCase: GetAttachmentsUseCase,
    private val addAttachmentUseCase: AddAttachmentUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val getDraftUseCase: GetDraftUseCase,
    private val clearDraftUseCase: ClearDraftUseCase
) : ViewModel() {

    private var attachmentsJob: kotlinx.coroutines.Job? = null

    val transactions: StateFlow<List<TransactionWithDetails>> = getTransactionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactionDraft: StateFlow<TransactionDraft?> = getDraftUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    fun showCreateDialog(initialCategoryId: String? = null, initialType: String? = null) {
        viewModelScope.launch {
            if (initialCategoryId != null || initialType != null) {
                val draft = TransactionDraft(
                    transactionType = initialType ?: "EXPENSE",
                    accountId = accounts.value.firstOrNull()?.id ?: "",
                    categoryId = initialCategoryId,
                    title = "",
                    note = null,
                    amount = null,
                    updatedAt = System.currentTimeMillis()
                )
                saveDraftUseCase(draft)
            }
            _uiState.update { it.copy(showCreateDialog = true, pendingAttachmentUris = emptyList(), attachments = emptyList(), error = null) }
        }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, pendingAttachmentUris = emptyList()) }
        clearDraft()
    }

    fun showEditDialog(transaction: TransactionWithDetails) {
        _uiState.update { it.copy(showEditDialog = true, transactionToEdit = transaction, showDetailsDialog = false, error = null) }
        loadAttachments(transaction.id)
    }

    fun hideEditDialog() {
        attachmentsJob?.cancel()
        _uiState.update { it.copy(showEditDialog = false, transactionToEdit = null, attachments = emptyList()) }
    }

    fun showDeleteDialog(transaction: TransactionWithDetails) {
        _uiState.update { it.copy(showDeleteDialog = true, transactionToDelete = transaction, showDetailsDialog = false, error = null) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, transactionToDelete = null) }
    }

    fun showDetailsDialog(transaction: TransactionWithDetails) {
        _uiState.update { it.copy(showDetailsDialog = true, transactionDetails = transaction, error = null) }
        loadAttachments(transaction.id)
    }

    fun hideDetailsDialog() {
        attachmentsJob?.cancel()
        _uiState.update { it.copy(showDetailsDialog = false, transactionDetails = null, attachments = emptyList()) }
    }

    fun createTransaction(
        accountId: String,
        categoryId: String?,
        transactionType: String,
        title: String,
        note: String?,
        amount: Long,
        transactionDate: Long
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val transactionId = UUID.randomUUID().toString()
                val transaction = Transaction(
                    id = transactionId,
                    accountId = accountId,
                    categoryId = categoryId,
                    receiptId = null,
                    transactionType = transactionType,
                    title = title,
                    note = note,
                    amount = amount,
                    transactionDate = transactionDate,
                    isDeleted = false,
                    deletedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
                createTransactionUseCase(transaction)

                // Save pending attachments
                val pendingUris = _uiState.value.pendingAttachmentUris
                pendingUris.forEach { uri ->
                    addAttachmentUseCase(transactionId, uri)
                }

                clearDraft()
                hideCreateDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTransaction(
        transactionId: String,
        accountId: String,
        categoryId: String?,
        transactionType: String,
        title: String,
        note: String?,
        amount: Long,
        transactionDate: Long,
        createdAt: Long
    ) {
        val oldDetails = _uiState.value.transactionToEdit ?: return
        viewModelScope.launch {
            try {
                val oldTransaction = Transaction(
                    id = oldDetails.id,
                    accountId = oldDetails.accountId,
                    categoryId = oldDetails.categoryId,
                    receiptId = oldDetails.receiptId,
                    transactionType = oldDetails.transactionType,
                    title = oldDetails.title,
                    note = oldDetails.note,
                    amount = oldDetails.amount,
                    transactionDate = oldDetails.transactionDate,
                    isDeleted = oldDetails.isDeleted,
                    deletedAt = oldDetails.deletedAt,
                    createdAt = oldDetails.createdAt,
                    updatedAt = oldDetails.updatedAt
                )

                val newTransaction = oldTransaction.copy(
                    accountId = accountId,
                    categoryId = categoryId,
                    transactionType = transactionType,
                    title = title,
                    note = note,
                    amount = amount,
                    transactionDate = transactionDate,
                    updatedAt = System.currentTimeMillis()
                )

                updateTransactionUseCase(oldTransaction, newTransaction)
                hideEditDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTransaction() {
        val details = _uiState.value.transactionToDelete ?: return
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = details.id,
                    accountId = details.accountId,
                    categoryId = details.categoryId,
                    receiptId = details.receiptId,
                    transactionType = details.transactionType,
                    title = details.title,
                    note = details.note,
                    amount = details.amount,
                    transactionDate = details.transactionDate,
                    isDeleted = details.isDeleted,
                    deletedAt = details.deletedAt,
                    createdAt = details.createdAt,
                    updatedAt = details.updatedAt
                )
                deleteTransactionUseCase(transaction)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Attachment Management

    private fun loadAttachments(transactionId: String) {
        attachmentsJob?.cancel()
        attachmentsJob = viewModelScope.launch {
            getAttachmentsUseCase(transactionId).collect { list ->
                _uiState.update { it.copy(attachments = list) }
            }
        }
    }

    fun addPendingAttachment(uri: android.net.Uri) {
        _uiState.update {
            if (it.pendingAttachmentUris.size < 5) {
                it.copy(pendingAttachmentUris = it.pendingAttachmentUris + uri)
            } else {
                it
            }
        }
    }

    fun removePendingAttachment(uri: android.net.Uri) {
        _uiState.update {
            it.copy(pendingAttachmentUris = it.pendingAttachmentUris - uri)
        }
    }

    fun addAttachmentToExistingTransaction(transactionId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                addAttachmentUseCase(transactionId, uri)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteAttachmentFromExistingTransaction(attachment: Attachment) {
        viewModelScope.launch {
            try {
                deleteAttachmentUseCase(attachment)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveDraft(draft: TransactionDraft) {
        viewModelScope.launch {
            saveDraftUseCase(draft)
        }
    }

    fun clearDraft() {
        viewModelScope.launch {
            clearDraftUseCase()
        }
    }
}
