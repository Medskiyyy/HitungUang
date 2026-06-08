package com.hitunguang.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
        budgetRepository.getActiveBudgets()
    ) { categories, transactions, budgets ->
        Triple(categories, transactions, budgets)
    }

    val uiState: StateFlow<DashboardUiState> = combine(flow1, flow2) { f1, f2 ->
        val (profile, appSettings, accounts) = f1
        val (categories, transactions, budgets) = f2

        val userName = profile?.name ?: "Pengguna"
        val hideBalance = appSettings?.hideBalance ?: false
        val selectedPeriod = appSettings?.dashboardPeriod ?: "WEEKLY"

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

        val totalExpense = periodTransactions
            .filter { it.transactionType == "EXPENSE" || it.transactionType == "TRANSFER_FEE" }
            .sumOf { it.amount }

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

        val previousTotalExpense = prevPeriodTransactions
            .filter { it.transactionType == "EXPENSE" || it.transactionType == "TRANSFER_FEE" }
            .sumOf { it.amount }

        val periodLabel = when (selectedPeriod) {
            "TODAY" -> "kemarin"
            "WEEKLY" -> "minggu lalu"
            "MONTHLY" -> "bulan lalu"
            "YEARLY" -> "tahun lalu"
            else -> "periode lalu"
        }

        var isExpenseIncreased = false
        val periodComparisonMessage = if (previousTotalExpense > 0L) {
            val diff = totalExpense - previousTotalExpense
            val diffPercent = (diff.toFloat() / previousTotalExpense.toFloat()) * 100f
            val formattedPercent = String.format(java.util.Locale.US, "%.1f", java.lang.Math.abs(diffPercent))
            
            if (diff > 0) {
                isExpenseIncreased = true
                "Pengeluaranmu naik $formattedPercent% dibandingkan dengan $periodLabel."
            } else if (diff < 0) {
                isExpenseIncreased = false
                "Pengeluaranmu turun $formattedPercent% dibandingkan dengan $periodLabel. Kinerja keuanganmu membaik!"
            } else {
                isExpenseIncreased = false
                "Pengeluaranmu sama persis dengan $periodLabel."
            }
        } else if (totalExpense > 0L) {
            isExpenseIncreased = true
            val formatter = java.text.NumberFormat.getIntegerInstance(java.util.Locale("in", "ID"))
            "Ada pengeluaran Rp ${formatter.format(totalExpense)} periode ini, sedangkan $periodLabel tidak ada pengeluaran sama sekali."
        } else {
            isExpenseIncreased = false
            "Belum ada catatan pengeluaran pada periode ini maupun $periodLabel."
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

        // Budgets Progress
        val budgetProgressList = budgets.map { budget ->
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

        // Expense distribution
        val expenseTransactions = periodTransactions.filter { it.transactionType == "EXPENSE" || it.transactionType == "TRANSFER_FEE" }
        val categoryMap = expenseTransactions.groupBy { it.categoryId }
        val expenseCategoriesDistribution = categoryMap.mapNotNull { (catId, txs) ->
            val category = categories.find { it.id == catId } ?: Category(
                id = catId ?: "unknown",
                name = "Lainnya",
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
            .take(10)

        // Top Expense Category & Amount
        val topExpenseEntry = expenseCategoriesDistribution.maxByOrNull { it.value }
        val topExpenseCategory = topExpenseEntry?.key
        val topExpenseAmount = topExpenseEntry?.value ?: 0L

        // Savings rate message
        val savingsRateMessage = if (totalIncome > 0) {
            val rate = (netDifference.toFloat() / totalIncome.toFloat()) * 100f
            if (rate > 0) {
                "Kamu berhasil menghemat ${String.format(java.util.Locale.US, "%.1f", rate)}% dari pemasukanmu periode ini!"
            } else {
                "Pengeluaran melebihi pemasukan. Ayo perketat budget pengeluaranmu!"
            }
        } else if (totalExpense > 0) {
            "Belum ada pemasukan. Ayo batasi pengeluaran periode ini!"
        } else {
            "Belum ada catatan keuangan untuk periode ini."
        }

        DashboardUiState(
            userName = userName,
            totalBalance = totalBalance,
            hideBalance = hideBalance,
            selectedPeriod = selectedPeriod,
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
            isLoading = false
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
                dashboardPeriod = "WEEKLY",
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
                dashboardPeriod = "WEEKLY",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val updated = currentSettings.copy(dashboardPeriod = period, updatedAt = System.currentTimeMillis())
            settingsRepository.saveAppSettings(updated)
        }
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
