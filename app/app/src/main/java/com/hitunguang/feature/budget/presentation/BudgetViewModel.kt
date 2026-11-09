package com.hitunguang.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.usecase.AutoResetBudgetsUseCase
import com.hitunguang.feature.budget.domain.usecase.CreateBudgetUseCase
import com.hitunguang.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.hitunguang.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.hitunguang.feature.budget.domain.util.BudgetSpending
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val autoResetBudgetsUseCase: AutoResetBudgetsUseCase
) : ViewModel() {

    private data class FormState(
        val showFormDialog: Boolean,
        val budgetToEdit: Budget?,
        val error: String?,
        val requestNotificationPermission: Boolean
    )

    private val _showFormDialog = MutableStateFlow(false)
    private val _budgetToEdit = MutableStateFlow<Budget?>(null)
    private val _saveError = MutableStateFlow<String?>(null)
    private val _requestNotificationPermission = MutableStateFlow(false)

    private val formState = combine(
        _showFormDialog,
        _budgetToEdit,
        _saveError,
        _requestNotificationPermission
    ) { showForm, editBudget, error, requestPermission ->
        FormState(showForm, editBudget, error, requestPermission)
    }

    init {
        // Roll over expired budgets once when the screen opens (the daily worker covers the rest)
        viewModelScope.launch {
            try {
                autoResetBudgetsUseCase()
            } catch (e: Exception) {
                // Rollover is retried by the daily worker
            }
        }
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactionsWithDetails(),
        categoryRepository.getAllCategories(),
        budgetRepository.getTransferFees(),
        formState
    ) { budgets, transactions, categories, transferFees, form ->
        
        val now = System.currentTimeMillis()

        val budgetsWithProgress = budgets.map { budget ->
            val spentAmount = BudgetSpending.spentAmount(budget, transactions, transferFees)

            val progressPercent = if (budget.amount > 0) {
                (spentAmount.toFloat() / budget.amount.toFloat()) * 100f
            } else {
                0f
            }

            val remainingAmount = (budget.amount - spentAmount).coerceAtLeast(0L)
            val isOverBudget = spentAmount > budget.amount
            val isThresholdReached = progressPercent >= budget.thresholdPercent

            val categoryName = budget.categoryId?.let { catId ->
                categories.find { it.id == catId }?.name
            }

            BudgetWithProgress(
                budget = budget,
                categoryName = categoryName,
                spentAmount = spentAmount,
                remainingAmount = remainingAmount,
                progressPercent = progressPercent,
                isOverBudget = isOverBudget,
                isThresholdReached = isThresholdReached
            )
        }

        // Active budgets are active AND not expired
        val activeBudgets = budgetsWithProgress.filter { it.budget.isActive && now <= it.budget.endDate }
        // Completed budgets are inactive OR expired
        val completedBudgets = budgetsWithProgress.filter { !it.budget.isActive || now > it.budget.endDate }

        BudgetUiState(
            activeBudgets = activeBudgets,
            completedBudgets = completedBudgets,
            categories = categories,
            showFormDialog = form.showFormDialog,
            budgetToEdit = form.budgetToEdit,
            isLoading = false,
            error = form.error,
            requestNotificationPermission = form.requestNotificationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun showCreateDialog() {
        _saveError.value = null
        _budgetToEdit.value = null
        _showFormDialog.value = true
    }

    fun showEditDialog(budget: Budget) {
        _saveError.value = null
        _budgetToEdit.value = budget
        _showFormDialog.value = true
    }

    fun dismissFormDialog() {
        _showFormDialog.value = false
        _budgetToEdit.value = null
        _saveError.value = null
    }

    fun onNotificationPermissionRequested() {
        _requestNotificationPermission.value = false
    }

    fun saveBudget(
        categoryId: String?,
        budgetType: String,
        amount: Long,
        thresholdPercent: Int,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            try {
                val currentEdit = _budgetToEdit.value
                if (currentEdit != null) {
                    val updated = currentEdit.copy(
                        categoryId = categoryId,
                        budgetType = budgetType,
                        amount = amount,
                        thresholdPercent = thresholdPercent,
                        startDate = startDate,
                        endDate = endDate,
                        // Editing a past budget into a current period reactivates it
                        isActive = System.currentTimeMillis() <= endDate,
                        updatedAt = System.currentTimeMillis()
                    )
                    updateBudgetUseCase(updated)
                } else {
                    val newBudget = Budget(
                        id = UUID.randomUUID().toString(),
                        categoryId = categoryId,
                        budgetType = budgetType,
                        amount = amount,
                        thresholdPercent = thresholdPercent,
                        startDate = startDate,
                        endDate = endDate,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    createBudgetUseCase(newBudget)
                    // Budget alerts need POST_NOTIFICATIONS on Android 13+; the screen asks if not granted
                    _requestNotificationPermission.value = true
                }
                dismissFormDialog()
            } catch (e: Exception) {
                _saveError.value = e.message ?: ""
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            deleteBudgetUseCase(budget)
        }
    }
}
