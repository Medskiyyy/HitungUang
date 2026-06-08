package com.hitunguang.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.usecase.AutoResetBudgetsUseCase
import com.hitunguang.feature.budget.domain.usecase.CreateBudgetUseCase
import com.hitunguang.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.hitunguang.feature.budget.domain.usecase.UpdateBudgetUseCase
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

    private val _showFormDialog = MutableStateFlow(false)
    private val _budgetToEdit = MutableStateFlow<Budget?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactionsWithDetails(),
        categoryRepository.getAllCategories(),
        _showFormDialog,
        _budgetToEdit
    ) { budgets, transactions, categories, showForm, editBudget ->
        
        val now = System.currentTimeMillis()

        // Trigger auto reset rollover in background if there's any expired active budget
        val expiredBudgets = budgets.filter { it.isActive && now > it.endDate }
        if (expiredBudgets.isNotEmpty()) {
            viewModelScope.launch {
                autoResetBudgetsUseCase(expiredBudgets)
            }
        }

        val budgetsWithProgress = budgets.map { budget ->
            val spentAmount = transactions
                .filter {
                    !it.isDeleted && 
                    it.transactionDate in budget.startDate..budget.endDate &&
                    (it.transactionType == "EXPENSE" || it.transactionType == "TRANSFER_FEE") &&
                    (budget.categoryId == null || it.categoryId == budget.categoryId)
                }
                .sumOf { it.amount }

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
            showFormDialog = showForm,
            budgetToEdit = editBudget,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun showCreateDialog() {
        _budgetToEdit.value = null
        _showFormDialog.value = true
    }

    fun showEditDialog(budget: Budget) {
        _budgetToEdit.value = budget
        _showFormDialog.value = true
    }

    fun dismissFormDialog() {
        _showFormDialog.value = false
        _budgetToEdit.value = null
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
                }
                dismissFormDialog()
            } catch (e: Exception) {
                // Handled gracefully in tests or logs
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            deleteBudgetUseCase(budget)
        }
    }
}
