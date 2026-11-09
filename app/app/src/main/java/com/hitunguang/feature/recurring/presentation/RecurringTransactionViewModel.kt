package com.hitunguang.feature.recurring.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.usecase.CreateRecurringTransactionUseCase
import com.hitunguang.feature.recurring.domain.usecase.DeleteRecurringTransactionUseCase
import com.hitunguang.feature.recurring.domain.usecase.GetRecurringTransactionsUseCase
import com.hitunguang.feature.recurring.domain.usecase.ProcessDueRecurringUseCase
import com.hitunguang.feature.recurring.domain.usecase.SetRecurringTransactionActiveUseCase
import com.hitunguang.feature.recurring.domain.usecase.UpdateRecurringTransactionUseCase
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecurringTransactionViewModel @Inject constructor(
    getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val createRecurringTransactionUseCase: CreateRecurringTransactionUseCase,
    private val updateRecurringTransactionUseCase: UpdateRecurringTransactionUseCase,
    private val setRecurringTransactionActiveUseCase: SetRecurringTransactionActiveUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val processDueRecurringUseCase: ProcessDueRecurringUseCase
) : ViewModel() {

    private data class FormState(
        val showFormDialog: Boolean = false,
        val recurringToEdit: RecurringTransaction? = null,
        val error: String? = null
    )

    private val formState = MutableStateFlow(FormState())

    val uiState: StateFlow<RecurringTransactionUiState> = combine(
        getRecurringTransactionsUseCase(),
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories(),
        formState
    ) { recurringList, accounts, categories, form ->
        RecurringTransactionUiState(
            items = recurringList.map { recurring ->
                RecurringTransactionItem(
                    recurring = recurring,
                    accountName = accounts.find { it.id == recurring.accountId }?.name,
                    categoryName = recurring.categoryId?.let { id -> categories.find { it.id == id }?.name }
                )
            },
            accounts = accounts,
            categories = categories,
            showFormDialog = form.showFormDialog,
            recurringToEdit = form.recurringToEdit,
            error = form.error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringTransactionUiState(isLoading = true)
    )

    fun showCreateDialog() {
        formState.value = FormState(showFormDialog = true)
    }

    fun showEditDialog(recurring: RecurringTransaction) {
        formState.value = FormState(showFormDialog = true, recurringToEdit = recurring)
    }

    fun dismissFormDialog() {
        formState.value = FormState()
    }

    fun saveRecurring(
        type: String,
        title: String,
        note: String?,
        amount: Long,
        accountId: String,
        categoryId: String?,
        rule: RecurrenceRule,
        startDate: LocalDate
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val nextRunAt = RecurrenceSchedule.firstRunAt(rule, startDate)
                val currentEdit = formState.value.recurringToEdit
                if (currentEdit != null) {
                    updateRecurringTransactionUseCase(
                        currentEdit.copy(
                            accountId = accountId,
                            categoryId = categoryId,
                            transactionType = type,
                            title = title.trim(),
                            note = note,
                            amount = amount,
                            recurrenceRule = rule,
                            nextRunAt = nextRunAt,
                            updatedAt = now
                        )
                    )
                } else {
                    createRecurringTransactionUseCase(
                        RecurringTransaction(
                            id = UUID.randomUUID().toString(),
                            accountId = accountId,
                            categoryId = categoryId,
                            transactionType = type,
                            title = title.trim(),
                            note = note,
                            amount = amount,
                            recurrenceRule = rule,
                            nextRunAt = nextRunAt,
                            isActive = true,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                dismissFormDialog()
                processDue()
            } catch (e: Exception) {
                formState.value = formState.value.copy(error = e.message ?: "")
            }
        }
    }

    fun setActive(recurring: RecurringTransaction, active: Boolean) {
        viewModelScope.launch {
            setRecurringTransactionActiveUseCase(recurring, active)
            if (active) processDue()
        }
    }

    fun deleteRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch {
            deleteRecurringTransactionUseCase(recurring)
        }
    }

    // A rule starting today (or earlier) is posted right away instead of waiting for the daily worker
    private suspend fun processDue() {
        try {
            processDueRecurringUseCase()
        } catch (e: Exception) {
            // Retried by the daily worker
        }
    }
}
