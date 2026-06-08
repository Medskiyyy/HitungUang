package com.hitunguang.feature.dashboard.presentation

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.BudgetEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.database.entity.UserProfileEntity
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.onboarding.data.repository.UserProfileRepositoryImpl
import com.hitunguang.feature.settings.data.repository.SettingsRepositoryImpl
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var db: HitungUangDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Setup in-memory Room database
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // 2. Setup in-memory DataStore
        val datastoreFile = File(tmpFolder.newFolder(), "test_preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)

        // 3. Setup Repositories
        val userProfileRepo = UserProfileRepositoryImpl(db.userProfileDao())
        val accountRepo = AccountRepositoryImpl(db.accountDao())
        val categoryRepo = CategoryRepositoryImpl(db.categoryDao())
        val transactionRepo = TransactionRepositoryImpl(db.transactionDao())
        val budgetRepo = BudgetRepositoryImpl(db.budgetDao())
        val settingsRepo = SettingsRepositoryImpl(settingsDataStore)

        // 4. Initialize ViewModel
        viewModel = DashboardViewModel(
            userProfileRepository = userProfileRepo,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            transactionRepository = transactionRepo,
            budgetRepository = budgetRepo,
            settingsRepository = settingsRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testDashboardInitialState() = runBlocking {
        val states = mutableListOf<DashboardUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        kotlinx.coroutines.delay(200)
        val state = states.last()
        assertEquals("Pengguna", state.userName)
        assertEquals(0L, state.totalBalance)
        assertFalse(state.hideBalance)
        assertEquals("WEEKLY", state.selectedPeriod)

        job.cancel()
    }

    @Test
    fun testDashboard_BalanceCalculationAndHideShowToggle() = runBlocking {
        val states = mutableListOf<DashboardUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        val now = System.currentTimeMillis()
        // Insert user profile
        db.userProfileDao().insertOrUpdate(
            UserProfileEntity("user-1", "Ahmad", "Developer", now, now)
        )
        // Insert accounts
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 100000L, now, now)
        )
        db.accountDao().insertAccount(
            AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 500000L, 500000L, now, now)
        )

        kotlinx.coroutines.delay(200)
        var state = states.last()
        assertEquals("Ahmad", state.userName)
        assertEquals(600000L, state.totalBalance)
        assertFalse(state.hideBalance)

        // Toggle hide balance
        viewModel.toggleHideBalance()
        kotlinx.coroutines.delay(200)
        state = states.last()
        assertTrue(state.hideBalance)

        job.cancel()
    }

    @Test
    fun testDashboard_PeriodFiltering_IncomeAndExpenseAggregation() = runBlocking {
        val states = mutableListOf<DashboardUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val localNow = LocalDateTime.now(zoneId)

        // Let's insert accounts & categories
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet", "CASH", "wallet", 200000L, 200000L, now, now)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-income", "Gaji", "INCOME", "salary", isDefault = true, isPinned = true, now, now)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-expense", "Makanan", "EXPENSE", "food", isDefault = true, isPinned = true, now, now)
        )

        val startOfWeek = localNow.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // 1. Transaction within current week (Monday)
        val mondayTime = startOfWeek.with(LocalTime.NOON)
            .atZone(zoneId).toInstant().toEpochMilli()
        db.transactionDao().insertTransaction(
            TransactionEntity("tx-1", "acc-1", "cat-income", null, "INCOME", "Gaji Pokok", null, 50000L, mondayTime, isDeleted = false, null, now, now)
        )

        // 2. Transaction within current week (Wednesday)
        val wednesdayTime = startOfWeek.plusDays(2).with(LocalTime.NOON)
            .atZone(zoneId).toInstant().toEpochMilli()
        db.transactionDao().insertTransaction(
            TransactionEntity("tx-2", "acc-1", "cat-expense", null, "EXPENSE", "Makan Siang", null, 15000L, wednesdayTime, isDeleted = false, null, now, now)
        )

        // 3. Transaction outside current week (Last week)
        val lastWeekTime = startOfWeek.minusDays(3).with(LocalTime.NOON)
            .atZone(zoneId).toInstant().toEpochMilli()
        db.transactionDao().insertTransaction(
            TransactionEntity("tx-3", "acc-1", "cat-expense", null, "EXPENSE", "Makan Minggu Lalu", null, 30000L, lastWeekTime, isDeleted = false, null, now, now)
        )

        kotlinx.coroutines.delay(200)

        // Assert weekly period (default)
        viewModel.setDashboardPeriod("WEEKLY")
        kotlinx.coroutines.delay(200)
        var state = states.last()
        assertEquals(50000L, state.totalIncome)
        assertEquals(15000L, state.totalExpense)
        assertEquals(35000L, state.netDifference)

        // Change period to MONTHLY (which should include last week's transaction as well, assuming it's in the same month)
        // Let's verify if last week's time is in the same month.
        if (localNow.minusWeeks(1).month == localNow.month) {
            viewModel.setDashboardPeriod("MONTHLY")
            kotlinx.coroutines.delay(200)
            state = states.last()
            assertEquals(50000L, state.totalIncome)
            assertEquals(45000L, state.totalExpense) // 15000 + 30000
        }

        job.cancel()
    }

    @Test
    fun testDashboard_QuickAddChipsSorting() = runBlocking {
        val states = mutableListOf<DashboardUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        val now = System.currentTimeMillis()

        // Insert categories: some pinned, some unpinned
        db.categoryDao().insertCategory(CategoryEntity("cat-1", "Makanan", "EXPENSE", "food", isDefault = false, isPinned = true, now, now))
        db.categoryDao().insertCategory(CategoryEntity("cat-2", "Belanja", "EXPENSE", "shopping", isDefault = false, isPinned = false, now, now))
        db.categoryDao().insertCategory(CategoryEntity("cat-3", "Gaji", "INCOME", "salary", isDefault = false, isPinned = true, now, now))
        db.categoryDao().insertCategory(CategoryEntity("cat-4", "Hiburan", "EXPENSE", "entertainment", isDefault = false, isPinned = false, now, now))

        // Create some transaction history to test usage frequency sorting
        // Let's make cat-2 have more transactions than cat-4
        db.accountDao().insertAccount(AccountEntity("acc-1", "Dompet", "CASH", "wallet", 10000L, 10000L, now, now))
        db.transactionDao().insertTransaction(TransactionEntity("tx-1", "acc-1", "cat-2", null, "EXPENSE", "Belanja 1", null, 1000L, now, isDeleted = false, null, now, now))
        db.transactionDao().insertTransaction(TransactionEntity("tx-2", "acc-1", "cat-2", null, "EXPENSE", "Belanja 2", null, 1000L, now, isDeleted = false, null, now, now))
        db.transactionDao().insertTransaction(TransactionEntity("tx-3", "acc-1", "cat-4", null, "EXPENSE", "Nonton Film", null, 1000L, now, isDeleted = false, null, now, now))

        kotlinx.coroutines.delay(200)
        val state = states.last()

        assertTrue(state.quickAddCategories.size >= 4)
        // Pinned categories ("cat-1" and "cat-3") must appear first, sorted alphabetically by DAO query ("Gaji" before "Makanan")
        assertEquals("Gaji", state.quickAddCategories[0].name)
        assertEquals("Makanan", state.quickAddCategories[1].name)

        // Then, unpinned categories sorted by usage ("cat-2" used twice, "cat-4" used once)
        assertEquals("Belanja", state.quickAddCategories[2].name)
        assertEquals("Hiburan", state.quickAddCategories[3].name)

        job.cancel()
    }

    @Test
    fun testDashboard_BudgetProgressCalculation() = runBlocking {
        val states = mutableListOf<DashboardUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        val now = System.currentTimeMillis()

        db.accountDao().insertAccount(AccountEntity("acc-1", "Dompet", "CASH", "wallet", 100000L, 100000L, now, now))
        db.categoryDao().insertCategory(CategoryEntity("cat-food", "Makanan", "EXPENSE", "food", isDefault = true, isPinned = true, now, now))

        // Active global budget: amount = 100.000
        db.budgetDao().insertBudget(
            BudgetEntity("budget-global", null, "GLOBAL", 100000L, 80, now - 10000L, now + 100000L, isActive = true, now, now)
        )

        // Add expense transaction within budget window: amount = 25.000
        db.transactionDao().insertTransaction(
            TransactionEntity("tx-1", "acc-1", "cat-food", null, "EXPENSE", "Soto Ayam", null, 25000L, now, isDeleted = false, null, now, now)
        )

        kotlinx.coroutines.delay(200)
        val state = states.last()

        assertEquals(1, state.budgetProgressList.size)
        val progress = state.budgetProgressList[0]
        assertEquals("budget-global", progress.budget.id)
        assertEquals(25000L, progress.spentAmount)
        assertEquals(25f, progress.progressPercent)

        job.cancel()
    }

    @Test
    fun testDashboard_PeriodComparisonCalculation() = runBlocking {
        val states = mutableListOf<DashboardUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val localNow = LocalDateTime.now(zoneId)

        // Insert accounts & categories
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet", "CASH", "wallet", 200000L, 200000L, now, now)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-expense", "Makanan", "EXPENSE", "food", isDefault = true, isPinned = true, now, now)
        )

        val startOfWeek = localNow.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // 1. Transaction in current week (Wednesday) -> amount = 50000
        val wednesdayTime = startOfWeek.plusDays(2).with(LocalTime.NOON)
            .atZone(zoneId).toInstant().toEpochMilli()
        db.transactionDao().insertTransaction(
            TransactionEntity("tx-current", "acc-1", "cat-expense", null, "EXPENSE", "Makan Siang", null, 50000L, wednesdayTime, isDeleted = false, null, now, now)
        )

        // 2. Transaction in previous week -> amount = 40000
        val lastWeekWednesdayTime = startOfWeek.minusWeeks(1).plusDays(2).with(LocalTime.NOON)
            .atZone(zoneId).toInstant().toEpochMilli()
        db.transactionDao().insertTransaction(
            TransactionEntity("tx-previous", "acc-1", "cat-expense", null, "EXPENSE", "Makan Siang Lalu", null, 40000L, lastWeekWednesdayTime, isDeleted = false, null, now, now)
        )

        kotlinx.coroutines.delay(200)

        // Set period to WEEKLY
        viewModel.setDashboardPeriod("WEEKLY")
        kotlinx.coroutines.delay(200)

        val state = states.last()
        assertEquals(50000L, state.totalExpense)
        assertEquals(40000L, state.previousTotalExpense)
        assertTrue(state.isExpenseIncreased)
        assertNotNull(state.periodComparisonMessage)
        assertTrue(state.periodComparisonMessage!!.contains("naik 25.0%"))

        job.cancel()
    }
}
