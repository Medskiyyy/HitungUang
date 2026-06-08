package com.hitunguang.feature.dashboard.presentation

import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

data class BudgetProgress(
    val budget: Budget,
    val categoryName: String?,
    val spentAmount: Long,
    val progressPercent: Float
)

data class DashboardUiState(
    val userName: String = "",
    val totalBalance: Long = 0L,
    val hideBalance: Boolean = false,
    val selectedPeriod: String = "WEEKLY", // TODAY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netDifference: Long = 0L,
    val quickAddCategories: List<Category> = emptyList(),
    val budgetProgressList: List<BudgetProgress> = emptyList(),
    val expenseCategoriesDistribution: Map<Category, Long> = emptyMap(),
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val topExpenseCategory: Category? = null,
    val topExpenseAmount: Long = 0L,
    val savingsRateMessage: String? = null,
    val previousTotalIncome: Long = 0L,
    val previousTotalExpense: Long = 0L,
    val periodComparisonMessage: String? = null,
    val isExpenseIncreased: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
