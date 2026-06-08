package com.hitunguang.core.balance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.TransferEntity
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BalanceEngineTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var balanceValidator: BalanceValidator

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        balanceValidator = BalanceValidator(db.accountDao(), db.transactionDao(), db.transferDao())

        // Setup accounts
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        db.accountDao().insertAccount(
            AccountEntity("acc-2", "Tabungan Bank", "BANK", null, 500000L, 500000L, 1000L, 1000L)
        )
        // Setup categories
        db.categoryDao().insertCategory(
            CategoryEntity("cat-e", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-i", "Gaji", "INCOME", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ========================
    // BalanceCalculator Tests
    // ========================

    @Test
    fun testCalculateDifference_income() {
        assertEquals(50000L, BalanceCalculator.calculateDifference("INCOME", 50000L))
    }

    @Test
    fun testCalculateDifference_expense() {
        assertEquals(-30000L, BalanceCalculator.calculateDifference("EXPENSE", 30000L))
    }

    @Test
    fun testCalculateDifference_transferFee() {
        assertEquals(-5000L, BalanceCalculator.calculateDifference("TRANSFER_FEE", 5000L))
    }

    @Test
    fun testCalculateDifference_unknownType() {
        assertEquals(0L, BalanceCalculator.calculateDifference("UNKNOWN", 10000L))
    }

    @Test
    fun testCalculateDifference_zeroAmount() {
        assertEquals(0L, BalanceCalculator.calculateDifference("INCOME", 0L))
        assertEquals(0L, BalanceCalculator.calculateDifference("EXPENSE", 0L))
    }

    @Test
    fun testCalculateReversal_income() {
        // Reversal of INCOME should subtract
        assertEquals(-50000L, BalanceCalculator.calculateReversal("INCOME", 50000L))
    }

    @Test
    fun testCalculateReversal_expense() {
        // Reversal of EXPENSE should add back
        assertEquals(30000L, BalanceCalculator.calculateReversal("EXPENSE", 30000L))
    }

    @Test
    fun testCalculateReversal_transferFee() {
        assertEquals(5000L, BalanceCalculator.calculateReversal("TRANSFER_FEE", 5000L))
    }

    // ========================
    // BalanceValidator Tests
    // ========================

    @Test
    fun testValidateBalance_initialStateIsConsistent() = runBlocking {
        val result = balanceValidator.validateBalance("acc-1")
        assertTrue(result.isConsistent)
        assertEquals(100000L, result.storedBalance)
        assertEquals(100000L, result.calculatedBalance)
    }

    @Test
    fun testValidateBalance_afterTransactions() = runBlocking {
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val createUseCase = createCreateUseCase(repository)

        // Add income of 50,000
        createUseCase(makeTransaction("tx-1", "acc-1", "INCOME", 50000L, "cat-i"))
        // Add expense of 20,000
        createUseCase(makeTransaction("tx-2", "acc-1", "EXPENSE", 20000L, "cat-e"))

        // Expected: 100,000 + 50,000 - 20,000 = 130,000
        val result = balanceValidator.validateBalance("acc-1")
        assertTrue(result.isConsistent)
        assertEquals(130000L, result.storedBalance)
        assertEquals(130000L, result.calculatedBalance)
    }

    @Test
    fun testValidateBalance_afterTransfers() = runBlocking {
        // Transfer 30,000 from acc-1 to acc-2, with 2,000 admin fee
        val transfer = TransferEntity(
            id = "tr-1",
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 30000L,
            adminFee = 2000L,
            note = null,
            transferDate = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.transferDao().executeTransfer(transfer)

        // acc-1 expected: 100,000 - 30,000 - 2,000 = 68,000
        val result1 = balanceValidator.validateBalance("acc-1")
        assertTrue(result1.isConsistent)
        assertEquals(68000L, result1.storedBalance)

        // acc-2 expected: 500,000 + 30,000 = 530,000
        val result2 = balanceValidator.validateBalance("acc-2")
        assertTrue(result2.isConsistent)
        assertEquals(530000L, result2.storedBalance)
    }

    @Test
    fun testValidateBalance_detectsMismatch() = runBlocking {
        // Manually corrupt balance
        db.accountDao().adjustBalance("acc-1", 99999L, 2000L)

        val result = balanceValidator.validateBalance("acc-1")
        assertFalse(result.isConsistent)
        assertEquals(199999L, result.storedBalance)  // corrupted
        assertEquals(100000L, result.calculatedBalance) // correct
    }

    @Test
    fun testRepairBalance_fixesMismatch() = runBlocking {
        // Manually corrupt balance
        db.accountDao().adjustBalance("acc-1", 50000L, 2000L)

        // Stored: 150,000. Calculated: 100,000
        val beforeRepair = balanceValidator.validateBalance("acc-1")
        assertFalse(beforeRepair.isConsistent)

        // Repair
        balanceValidator.repairBalance("acc-1")

        // After repair should be consistent
        val afterRepair = balanceValidator.validateBalance("acc-1")
        assertTrue(afterRepair.isConsistent)
        assertEquals(100000L, afterRepair.storedBalance)
    }

    @Test
    fun testValidateBalance_softDeletedTransactionsNotCounted() = runBlocking {
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val createUseCase = createCreateUseCase(repository)
        val deleteUseCase = DeleteTransactionUseCase(repository)

        // Add income of 40,000
        val tx = makeTransaction("tx-sd", "acc-1", "INCOME", 40000L, "cat-i")
        createUseCase(tx)

        // Balance: 100,000 + 40,000 = 140,000
        val result1 = balanceValidator.validateBalance("acc-1")
        assertEquals(140000L, result1.storedBalance)
        assertTrue(result1.isConsistent)

        // Soft-delete the income
        deleteUseCase(tx)

        // Balance reverted: 140,000 - 40,000 = 100,000
        val result2 = balanceValidator.validateBalance("acc-1")
        assertEquals(100000L, result2.storedBalance)
        assertTrue(result2.isConsistent)
    }

    @Test
    fun testValidateBalance_mixedScenario() = runBlocking {
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val createUseCase = createCreateUseCase(repository)

        // Income 80,000
        createUseCase(makeTransaction("tx-m1", "acc-1", "INCOME", 80000L, "cat-i"))
        // Expense 25,000
        createUseCase(makeTransaction("tx-m2", "acc-1", "EXPENSE", 25000L, "cat-e"))
        // Expense 15,000
        createUseCase(makeTransaction("tx-m3", "acc-1", "EXPENSE", 15000L, "cat-e"))

        // Transfer 20,000 from acc-1 to acc-2, admin fee 1,000
        val transfer = TransferEntity(
            id = "tr-mix",
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 20000L,
            adminFee = 1000L,
            note = null,
            transferDate = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.transferDao().executeTransfer(transfer)

        // acc-1 expected: 100,000 + 80,000 - 25,000 - 15,000 - 20,000 - 1,000 = 119,000
        val result1 = balanceValidator.validateBalance("acc-1")
        assertTrue(result1.isConsistent)
        assertEquals(119000L, result1.storedBalance)

        // acc-2 expected: 500,000 + 20,000 = 520,000
        val result2 = balanceValidator.validateBalance("acc-2")
        assertTrue(result2.isConsistent)
        assertEquals(520000L, result2.storedBalance)
    }

    // ========================
    // Negative Balance Validation Tests
    // ========================

    @Test(expected = IllegalStateException::class)
    fun testCreateTransaction_rejectsNegativeBalance() = runBlocking {
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val createUseCase = createCreateUseCase(repository)

        // Try to spend 200,000 from acc-1 (balance 100,000) — should fail
        createUseCase(makeTransaction("tx-neg", "acc-1", "EXPENSE", 200000L, "cat-e"))
    }

    @Test(expected = IllegalStateException::class)
    fun testUpdateTransaction_rejectsNegativeBalance() = runBlocking {
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val createUseCase = createCreateUseCase(repository)
        val updateUseCase = UpdateTransactionUseCase(repository, db.accountDao())

        // Create a small expense (10,000)
        val tx = makeTransaction("tx-upd", "acc-1", "EXPENSE", 10000L, "cat-e")
        createUseCase(tx)
        // Balance: 100,000 - 10,000 = 90,000

        // Try to update to 200,000 — net change is -190,000 more, should fail
        val updated = tx.copy(amount = 200000L, updatedAt = 2000L)
        updateUseCase(tx, updated)
    }

    // ========================
    // End-to-End Test
    // ========================

    @Test
    fun testEndToEnd_createUpdateDeleteValidate() = runBlocking {
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val createUseCase = createCreateUseCase(repository)
        val updateUseCase = UpdateTransactionUseCase(repository, db.accountDao())
        val deleteUseCase = DeleteTransactionUseCase(repository)

        // 1. Create income 60,000
        val income = makeTransaction("tx-e2e", "acc-1", "INCOME", 60000L, "cat-i")
        createUseCase(income)
        assertEquals(160000L, db.accountDao().getAccountBalance("acc-1"))

        // 2. Update to 80,000
        val updated = income.copy(amount = 80000L, updatedAt = 2000L)
        updateUseCase(income, updated)
        assertEquals(180000L, db.accountDao().getAccountBalance("acc-1"))

        // 3. Delete
        deleteUseCase(updated)
        assertEquals(100000L, db.accountDao().getAccountBalance("acc-1"))

        // 4. Validate
        val result = balanceValidator.validateBalance("acc-1")
        assertTrue(result.isConsistent)
        assertEquals(100000L, result.storedBalance)
    }

    // ========================
    // Helper
    // ========================

    private fun createCreateUseCase(repository: TransactionRepositoryImpl): CreateTransactionUseCase {
        val budgetRepository = com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl(db.budgetDao())
        val checkBudgetThresholdUseCase = com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase(budgetRepository, ApplicationProvider.getApplicationContext())
        return CreateTransactionUseCase(repository, db.accountDao(), checkBudgetThresholdUseCase)
    }

    private fun makeTransaction(
        id: String,
        accountId: String,
        type: String,
        amount: Long,
        categoryId: String
    ): Transaction = Transaction(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        receiptId = null,
        transactionType = type,
        title = "Test $type",
        note = null,
        amount = amount,
        transactionDate = 1000L,
        isDeleted = false,
        deletedAt = null,
        createdAt = 1000L,
        updatedAt = 1000L
    )
}
