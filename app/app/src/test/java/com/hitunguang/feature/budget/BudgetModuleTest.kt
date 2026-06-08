package com.hitunguang.feature.budget

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.usecase.AutoResetBudgetsUseCase
import com.hitunguang.feature.budget.domain.usecase.CreateBudgetUseCase
import com.hitunguang.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.hitunguang.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.hitunguang.feature.budget.presentation.BudgetViewModel
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BudgetModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var createBudgetUseCase: CreateBudgetUseCase
    private lateinit var updateBudgetUseCase: UpdateBudgetUseCase
    private lateinit var deleteBudgetUseCase: DeleteBudgetUseCase
    private lateinit var autoResetBudgetsUseCase: AutoResetBudgetsUseCase

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        budgetRepository = BudgetRepositoryImpl(db.budgetDao())
        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        categoryRepository = CategoryRepositoryImpl(db.categoryDao())

        createBudgetUseCase = CreateBudgetUseCase(budgetRepository)
        updateBudgetUseCase = UpdateBudgetUseCase(budgetRepository)
        deleteBudgetUseCase = DeleteBudgetUseCase(budgetRepository)
        autoResetBudgetsUseCase = AutoResetBudgetsUseCase(budgetRepository)

        // Setup common account and category
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-2", "Transportasi", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-3", "Gaji", "INCOME", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testCreateBudgetValidation() = runBlocking {
        val validBudget = Budget(
            id = "b-1",
            categoryId = "cat-1",
            budgetType = "CATEGORY",
            amount = 50000L,
            thresholdPercent = 80,
            startDate = 1000L,
            endDate = 2000L,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // 1. Invalid amount (<= 0)
        try {
            createBudgetUseCase(validBudget.copy(amount = 0L))
            fail("Should throw IllegalArgumentException for amount <= 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Nominal budget harus lebih besar dari 0") == true)
        }

        // 2. Invalid threshold (< 1 or > 100)
        try {
            createBudgetUseCase(validBudget.copy(thresholdPercent = 0))
            fail("Should throw IllegalArgumentException for threshold < 1")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Threshold harus berada di antara 1 dan 100%") == true)
        }
        try {
            createBudgetUseCase(validBudget.copy(thresholdPercent = 101))
            fail("Should throw IllegalArgumentException for threshold > 100")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Threshold harus berada di antara 1 dan 100%") == true)
        }

        // 3. StartDate > EndDate
        try {
            createBudgetUseCase(validBudget.copy(startDate = 2000L, endDate = 1000L))
            fail("Should throw IllegalArgumentException for startDate > endDate")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Tanggal mulai tidak boleh melebihi tanggal selesai") == true)
        }

        // 4. CATEGORY type with null categoryId
        try {
            createBudgetUseCase(validBudget.copy(categoryId = null))
            fail("Should throw IllegalArgumentException for CATEGORY budget with null categoryId")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Kategori harus dipilih untuk budget per kategori") == true)
        }

        // 5. GLOBAL type with non-null categoryId
        try {
            createBudgetUseCase(validBudget.copy(budgetType = "GLOBAL", categoryId = "cat-1"))
            fail("Should throw IllegalArgumentException for GLOBAL budget with non-null categoryId")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Kategori harus kosong untuk budget global") == true)
        }

        // 6. Valid budget should succeed
        createBudgetUseCase(validBudget)
        val read = budgetRepository.getAllBudgets().first()
        assertEquals(1, read.size)
        assertEquals("b-1", read[0].id)
    }

    @Test
    fun testUpdateBudgetValidation() = runBlocking {
        val validBudget = Budget(
            id = "b-2",
            categoryId = "cat-1",
            budgetType = "CATEGORY",
            amount = 50000L,
            thresholdPercent = 80,
            startDate = 1000L,
            endDate = 2000L,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        budgetRepository.insertBudget(validBudget)

        // 1. Invalid amount (<= 0)
        try {
            updateBudgetUseCase(validBudget.copy(amount = -100L))
            fail("Should throw IllegalArgumentException for amount <= 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Nominal budget harus lebih besar dari 0") == true)
        }

        // 2. Invalid threshold (< 1 or > 100)
        try {
            updateBudgetUseCase(validBudget.copy(thresholdPercent = 0))
            fail("Should throw IllegalArgumentException for threshold < 1")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Threshold harus berada di antara 1 dan 100%") == true)
        }

        // 3. StartDate > EndDate
        try {
            updateBudgetUseCase(validBudget.copy(startDate = 2000L, endDate = 1000L))
            fail("Should throw IllegalArgumentException for startDate > endDate")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Tanggal mulai tidak boleh melebihi tanggal selesai") == true)
        }

        // 4. Valid update should succeed
        val updated = validBudget.copy(amount = 60000L)
        updateBudgetUseCase(updated)
        val read = budgetRepository.getAllBudgets().first()
        assertEquals(60000L, read[0].amount)
    }

    @Test
    fun testDeleteBudget() = runBlocking {
        val budget = Budget(
            id = "b-3",
            categoryId = null,
            budgetType = "GLOBAL",
            amount = 100000L,
            thresholdPercent = 90,
            startDate = 1000L,
            endDate = 2000L,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        budgetRepository.insertBudget(budget)
        assertEquals(1, budgetRepository.getAllBudgets().first().size)

        deleteBudgetUseCase(budget)
        assertEquals(0, budgetRepository.getAllBudgets().first().size)
    }

    @Test
    fun testAutoResetRollover() = runBlocking {
        // Create an expired active budget
        val expiredBudget = Budget(
            id = "b-expired",
            categoryId = "cat-1",
            budgetType = "CATEGORY",
            amount = 50000L,
            thresholdPercent = 80,
            startDate = 1000L,
            endDate = 2000L, // Expired
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        budgetRepository.insertBudget(expiredBudget)

        // Invoke autoResetBudgetsUseCase
        autoResetBudgetsUseCase(listOf(expiredBudget))

        val allBudgets = budgetRepository.getAllBudgets().first()
        // Should have 2 budgets: 1 inactive (old) and 1 active (new rollover)
        assertEquals(2, allBudgets.size)

        val oldBudget = allBudgets.find { it.id == "b-expired" }
        assertNotNull(oldBudget)
        assertFalse(oldBudget!!.isActive)

        val newBudget = allBudgets.find { it.id != "b-expired" }
        assertNotNull(newBudget)
        assertTrue(newBudget!!.isActive)
        assertEquals("cat-1", newBudget.categoryId)
        assertEquals("CATEGORY", newBudget.budgetType)
        assertEquals(50000L, newBudget.amount)
        assertEquals(80, newBudget.thresholdPercent)
        
        // Verify the new dates are a valid shift of the old dates
        val duration = 1000L
        val step = 1001L
        val startShift = newBudget.startDate - 1000L
        val endShift = newBudget.endDate - 2000L
        assertEquals(startShift, endShift)
        assertEquals(0L, startShift % step)
        assertTrue(newBudget.endDate >= System.currentTimeMillis() - 5000L)
    }

    @Test
    fun testViewModelCalculationsAndAutoReset() = runBlocking {
        // Setup budget range: 10000L to 20000L
        val budget = Budget(
            id = "b-vm",
            categoryId = "cat-1",
            budgetType = "CATEGORY",
            amount = 100000L,
            thresholdPercent = 80,
            startDate = 10000L,
            endDate = 20000L,
            isActive = true,
            createdAt = 10000L,
            updatedAt = 10000L
        )
        budgetRepository.insertBudget(budget)

        // Insert transactions:
        // 1. Matching Expense (within range, correct category): 30,000
        db.transactionDao().insertTransaction(
            TransactionEntity("t-1", "acc-1", "cat-1", null, "EXPENSE", "Lunch", null, 30000L, 15000L, false, null, 15000L, 15000L)
        )
        // 2. Matching Transfer Fee (within range, correct category): 5,000
        db.transactionDao().insertTransaction(
            TransactionEntity("t-2", "acc-1", "cat-1", null, "TRANSFER_FEE", "Fee", null, 5000L, 16000L, false, null, 16000L, 16000L)
        )
        // 3. Non-matching category Expense: 10,000
        db.transactionDao().insertTransaction(
            TransactionEntity("t-3", "acc-1", "cat-2", null, "EXPENSE", "Transport", null, 10000L, 17000L, false, null, 17000L, 17000L)
        )
        // 4. Non-matching date range Expense: 20,000
        db.transactionDao().insertTransaction(
            TransactionEntity("t-4", "acc-1", "cat-1", null, "EXPENSE", "Dinner", null, 20000L, 25000L, false, null, 25000L, 25000L)
        )
        // 5. Income transaction (ignored): 50,000
        db.transactionDao().insertTransaction(
            TransactionEntity("t-5", "acc-1", "cat-1", null, "INCOME", "Refund", null, 50000L, 15000L, false, null, 15000L, 15000L)
        )
        // 6. Soft-deleted Expense: 15,000
        db.transactionDao().insertTransaction(
            TransactionEntity("t-6", "acc-1", "cat-1", null, "EXPENSE", "Snack", null, 15000L, 15000L, true, 16000L, 15000L, 16000L)
        )

        // Initialize ViewModel
        val viewModel = BudgetViewModel(
            budgetRepository,
            transactionRepository,
            categoryRepository,
            createBudgetUseCase,
            updateBudgetUseCase,
            deleteBudgetUseCase,
            autoResetBudgetsUseCase
        )

        // Collect state and wait for auto-reset to process and emit
        val state = viewModel.uiState.first { !it.isLoading && it.activeBudgets.isNotEmpty() }
        val completed = state.completedBudgets
        val active = state.activeBudgets

        // Verify the old budget is in completed list
        val oldBudgetProgress = completed.find { it.budget.id == "b-vm" }
        assertNotNull(oldBudgetProgress)
        // spentAmount = 30,000 (Lunch) + 5,000 (Transfer Fee) = 35,000
        assertEquals(35000L, oldBudgetProgress!!.spentAmount)
        assertEquals(65000L, oldBudgetProgress.remainingAmount)
        assertEquals(35f, oldBudgetProgress.progressPercent, 0.01f)
        assertFalse(oldBudgetProgress.isOverBudget)
        assertFalse(oldBudgetProgress.isThresholdReached)
        assertEquals("Makanan", oldBudgetProgress.categoryName)

        // Verify that a new active budget was created due to auto-reset
        assertEquals(1, active.size)
        val newBudgetProgress = active[0]
        assertEquals("cat-1", newBudgetProgress.budget.categoryId)
        assertTrue(newBudgetProgress.budget.startDate > 20000L)
        assertEquals(0L, newBudgetProgress.spentAmount)
        assertEquals(100000L, newBudgetProgress.remainingAmount)
        assertEquals(0f, newBudgetProgress.progressPercent)
    }
}
