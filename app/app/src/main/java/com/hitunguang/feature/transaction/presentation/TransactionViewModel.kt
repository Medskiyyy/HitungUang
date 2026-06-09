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

    private val _selectedPeriod = MutableStateFlow("ALL")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedSort = MutableStateFlow("NEWEST")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = kotlinx.coroutines.flow.combine(
        getTransactionsUseCase(),
        _selectedPeriod,
        _selectedSort,
        _customDateRange
    ) { txList, period, sort, dateRange ->
        var result = txList

        // 1. Filter by period
        if (period != "ALL") {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = now

            val startMs: Long
            val endMs: Long

            when (period) {
                "TODAY" -> {
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    startMs = calendar.timeInMillis

                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    endMs = calendar.timeInMillis
                }
                "WEEKLY" -> {
                    calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    startMs = calendar.timeInMillis

                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 6)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    endMs = calendar.timeInMillis
                }
                "MONTHLY" -> {
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    startMs = calendar.timeInMillis

                    calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    endMs = calendar.timeInMillis
                }
                "YEARLY" -> {
                    calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    startMs = calendar.timeInMillis

                    calendar.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 31)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    endMs = calendar.timeInMillis
                }
                "CUSTOM" -> {
                    startMs = dateRange?.first ?: 0L
                    endMs = dateRange?.second ?: Long.MAX_VALUE
                }
                else -> {
                    startMs = 0L
                    endMs = Long.MAX_VALUE
                }
            }

            result = result.filter { it.transactionDate in startMs..endMs }
        }

        // 2. Sort
        result = when (sort) {
            "NEWEST" -> result.sortedWith(compareByDescending<TransactionWithDetails> { it.transactionDate }.thenByDescending { it.createdAt })
            "OLDEST" -> result.sortedWith(compareBy<TransactionWithDetails> { it.transactionDate }.thenBy { it.createdAt })
            "HIGHEST" -> result.sortedByDescending { it.amount }
            "LOWEST" -> result.sortedBy { it.amount }
            else -> result
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setSort(sort: String) {
        _selectedSort.value = sort
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = start to end
    }

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
