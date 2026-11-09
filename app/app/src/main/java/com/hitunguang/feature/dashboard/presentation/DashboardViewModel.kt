package com.hitunguang.feature.dashboard.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.R
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.model.TransferFee
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.usecase.AutoResetBudgetsUseCase
import com.hitunguang.feature.budget.domain.util.BudgetDates
import com.hitunguang.feature.budget.domain.util.BudgetSpending
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import com.hitunguang.feature.recurring.domain.usecase.ProcessDueRecurringUseCase
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val autoResetBudgetsUseCase: AutoResetBudgetsUseCase,
    private val processDueRecurringUseCase: ProcessDueRecurringUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private data class DashboardData(
        val categories: List<Category>,
        val transactions: List<TransactionWithDetails>,
        val budgets: List<Budget>,
        val transferFees: List<TransferFee>
    )

    init {
        // Roll over expired budgets once per dashboard load (the daily worker covers the rest)
        viewModelScope.launch {
            try {
                autoResetBudgetsUseCase()
            } catch (e: Exception) {
                // Rollover is retried by the daily worker
            }
        }
        // Post due recurring transactions once per dashboard load (the daily worker covers the rest)
        viewModelScope.launch {
            try {
                processDueRecurringUseCase()
            } catch (e: Exception) {
                // Retried by the daily worker
            }
        }
    }

    // Emits the local date now and again at every local midnight so period bounds never go stale
    private val currentDate: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(ZoneId.systemDefault()))
            delay(BudgetDates.millisUntilNextMidnight(System.currentTimeMillis()) + 1L)
        }
    }.distinctUntilChanged()

    private val flow1 = combine(
        userProfileRepository.getUserProfile(),
        settingsRepository.getAppSettings(),
        accountRepository.getAllAccounts()
    ) { profile, appSettings, accounts ->
        Triple(profile, appSettings, accounts)
    }

    private val flow2 = combine(
        categoryRepository.getAllCategories(),
        transactionRepository.getAllTransactionsWithDetails(),
        budgetRepository.getActiveBudgets(),
        budgetRepository.getTransferFees()
    ) { categories, transactions, budgets, transferFees ->
        DashboardData(categories, transactions, budgets, transferFees)
    }

    val uiState: StateFlow<DashboardUiState> = combine(flow1, flow2, currentDate) { f1, f2, _ ->
        val (profile, appSettings, accounts) = f1
        val (categories, transactions, budgets, transferFees) = f2

        val userName = profile?.name ?: context.getString(R.string.dashboard_default_user_name)
        val hideBalance = appSettings?.hideBalance ?: false
        val selectedPeriod = appSettings?.dashboardPeriod ?: "MONTHLY"

        val totalBalance = accounts.sumOf { it.currentBalance }

        // Get time bounds
        val bounds = getPeriodBounds(selectedPeriod)
        val startTime = bounds.first
        val endTime = bounds.second

        // Filter transactions for period
        val periodTransactions = transactions.filter {
            !it.isDeleted && it.transactionDate in startTime..endTime
        }

        val totalIncome = periodTransactions
            .filter { it.transactionType == "INCOME" }
            .sumOf { it.amount }

        val totalExpense = BudgetSpending.totalExpense(transactions, transferFees, startTime, endTime)

        val netDifference = totalIncome - totalExpense

        // Calculate previous period stats for period comparison
        val prevBounds = getPreviousPeriodBounds(selectedPeriod)
        val prevStartTime = prevBounds.first
        val prevEndTime = prevBounds.second

        val prevPeriodTransactions = transactions.filter {
            !it.isDeleted && it.transactionDate in prevStartTime..prevEndTime
        }

        val previousTotalIncome = prevPeriodTransactions
            .filter { it.transactionType == "INCOME" }
            .sumOf { it.amount }

        val previousTotalExpense = BudgetSpending.totalExpense(transactions, transferFees, prevStartTime, prevEndTime)

        val periodLabel = when (selectedPeriod) {
            "TODAY" -> context.getString(R.string.dashboard_prev_period_yesterday)
            "WEEKLY" -> context.getString(R.string.dashboard_prev_period_week)
            "MONTHLY" -> context.getString(R.string.dashboard_prev_period_month)
            "YEARLY" -> context.getString(R.string.dashboard_prev_period_year)
            else -> context.getString(R.string.dashboard_prev_period_other)
        }

        var isExpenseIncreased = false
        val periodComparisonMessage = if (previousTotalExpense > 0L) {
            val diff = totalExpense - previousTotalExpense
            val diffPercent = (diff.toFloat() / previousTotalExpense.toFloat()) * 100f
            val formattedPercent = String.format(java.util.Locale.US, "%.1f", java.lang.Math.abs(diffPercent))
            
            if (diff > 0) {
                isExpenseIncreased = true
                context.getString(R.string.dashboard_comparison_up, formattedPercent, periodLabel)
            } else if (diff < 0) {
                isExpenseIncreased = false
                context.getString(R.string.dashboard_comparison_down, formattedPercent, periodLabel)
            } else {
                isExpenseIncreased = false
                context.getString(R.string.dashboard_comparison_same, periodLabel)
            }
        } else if (totalExpense > 0L) {
            isExpenseIncreased = true
            val formatter = java.text.NumberFormat.getIntegerInstance(java.util.Locale("in", "ID"))
            context.getString(R.string.dashboard_comparison_only_current, formatter.format(totalExpense), periodLabel)
        } else {
            isExpenseIncreased = false
            context.getString(R.string.dashboard_comparison_none, periodLabel)
        }

        // Quick Add Categories (6 chips)
        // Count category usage in transactions
        val categoryUsageCount = transactions.filter { !it.isDeleted }.groupingBy { it.categoryId }.eachCount()
        val quickAddCategories = categories
            .sortedWith(
                compareByDescending<Category> { it.isPinned }
                    .thenByDescending { categoryUsageCount[it.id] ?: 0 }
            )
            .take(6)

        // Budgets Progress: only budgets whose period contains now
        val now = System.currentTimeMillis()
        val budgetProgressList = budgets.filter { now in it.startDate..it.endDate }.map { budget ->
            val spentAmount = BudgetSpending.spentAmount(budget, transactions, transferFees)

            val progressPercent = if (budget.amount > 0) {
                (spentAmount.toFloat() / budget.amount.toFloat()) * 100f
            } else {
                0f
            }

            val categoryName = budget.categoryId?.let { catId ->
                categories.find { it.id == catId }?.name
            }

            BudgetProgress(
                budget = budget,
                categoryName = categoryName,
                spentAmount = spentAmount,
                progressPercent = progressPercent
            )
        }

        // Expense distribution (category-only: transfer admin fees have no category)
        val expenseTransactions = periodTransactions.filter { it.transactionType == "EXPENSE" }
        val categoryMap = expenseTransactions.groupBy { it.categoryId }
        val expenseCategoriesDistribution = categoryMap.mapNotNull { (catId, txs) ->
            val category = categories.find { it.id == catId } ?: Category(
                id = catId ?: "unknown",
                name = context.getString(R.string.dashboard_category_other),
                categoryType = "EXPENSE",
                icon = "help",
                isDefault = false,
                isPinned = false,
                createdAt = 0L,
                updatedAt = 0L
            )
            Pair(category, txs.sumOf { it.amount })
        }.toMap()

        // Accounts list
        val recentTransactions = transactions
            .filter { !it.isDeleted }
            .sortedByDescending { it.transactionDate }
            .take(5)

        // Top Expense Category & Amount
        val topExpenseEntry = expenseCategoriesDistribution.maxByOrNull { it.value }
        val topExpenseCategory = topExpenseEntry?.key
        val topExpenseAmount = topExpenseEntry?.value ?: 0L

        // Savings rate message
        val savingsRateMessage = if (totalIncome > 0) {
            val rate = (netDifference.toFloat() / totalIncome.toFloat()) * 100f
            if (rate > 0) {
                context.getString(R.string.dashboard_savings_positive, String.format(java.util.Locale.US, "%.1f", rate))
            } else {
                context.getString(R.string.dashboard_savings_overspent)
            }
        } else if (totalExpense > 0) {
            context.getString(R.string.dashboard_savings_no_income)
        } else {
            context.getString(R.string.dashboard_savings_no_records)
        }

        DashboardUiState(
            userName = userName,
            totalBalance = totalBalance,
            hideBalance = hideBalance,
            selectedPeriod = selectedPeriod,
            currentPeriodLabel = currentPeriodLabel(selectedPeriod),
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netDifference = netDifference,
            quickAddCategories = quickAddCategories,
            budgetProgressList = budgetProgressList,
            expenseCategoriesDistribution = expenseCategoriesDistribution,
            accounts = accounts,
            recentTransactions = recentTransactions,
            topExpenseCategory = topExpenseCategory,
            topExpenseAmount = topExpenseAmount,
            savingsRateMessage = savingsRateMessage,
            previousTotalIncome = previousTotalIncome,
            previousTotalExpense = previousTotalExpense,
            periodComparisonMessage = periodComparisonMessage,
            isExpenseIncreased = isExpenseIncreased,
            isLoading = false,
            // Checked against all data, not the selected period: a transaction recorded
            // last month still counts as "already done".
            hasWallet = accounts.isNotEmpty(),
            hasTransaction = transactions.any { !it.isDeleted },
            hasBudget = budgets.isNotEmpty()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true)
        )

    fun toggleHideBalance() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getAppSettings().firstOrNull() ?: AppSettings(
                id = "app_settings",
                themeMode = "SYSTEM",
                hideBalance = false,
                receiptAutoDeleteDays = 30,
                dashboardPeriod = "MONTHLY",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val updated = currentSettings.copy(hideBalance = !currentSettings.hideBalance, updatedAt = System.currentTimeMillis())
            settingsRepository.saveAppSettings(updated)
        }
    }

    fun setDashboardPeriod(period: String) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getAppSettings().firstOrNull() ?: AppSettings(
                id = "app_settings",
                themeMode = "SYSTEM",
                hideBalance = false,
                receiptAutoDeleteDays = 30,
                dashboardPeriod = "MONTHLY",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val updated = currentSettings.copy(dashboardPeriod = period, updatedAt = System.currentTimeMillis())
            settingsRepository.saveAppSettings(updated)
        }
    }

    private fun currentPeriodLabel(period: String): String = when (period) {
        "TODAY" -> context.getString(R.string.dashboard_period_today)
        "WEEKLY" -> context.getString(R.string.dashboard_period_week)
        "MONTHLY" -> context.getString(R.string.dashboard_period_month)
        "YEARLY" -> context.getString(R.string.dashboard_period_year)
        else -> context.getString(R.string.dashboard_period_other)
    }

    private fun getPeriodBounds(period: String): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)

        return when (period) {
            "TODAY" -> {
                val start = now.with(LocalTime.MIN)
                val end = now.with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            "WEEKLY" -> {
                val start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN)
                val end = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            "MONTHLY" -> {
                val start = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN)
                val end = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            "YEARLY" -> {
                val start = now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN)
                val end = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX) // Wait! YEARLY should end at lastDayOfYear.
                // Let's correct end date for YEARLY:
                // now.with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX)
                val endYear = now.with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), endYear.atZone(zoneId).toInstant().toEpochMilli())
            }
            else -> {
                val start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN)
                val end = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
        }
    }

    private fun getPreviousPeriodBounds(period: String): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)

        return when (period) {
            "TODAY" -> {
                val yesterday = now.minusDays(1)
                val start = yesterday.with(LocalTime.MIN)
                val end = yesterday.with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            "WEEKLY" -> {
                val lastWeek = now.minusWeeks(1)
                val start = lastWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN)
                val end = lastWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            "MONTHLY" -> {
                val lastMonth = now.minusMonths(1)
                val start = lastMonth.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN)
                val end = lastMonth.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            "YEARLY" -> {
                val lastYear = now.minusYears(1)
                val start = lastYear.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN)
                val end = lastYear.with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
            else -> {
                val lastWeek = now.minusWeeks(1)
                val start = lastWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN)
                val end = lastWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX)
                Pair(start.atZone(zoneId).toInstant().toEpochMilli(), end.atZone(zoneId).toInstant().toEpochMilli())
            }
        }
    }
}
