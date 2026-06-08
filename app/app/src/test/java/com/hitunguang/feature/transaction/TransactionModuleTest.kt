package com.hitunguang.feature.transaction

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.hitunguang.feature.transaction.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var getTransactionsUseCase: GetTransactionsUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepositoryImpl(db.transactionDao())
        val budgetRepository = com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl(db.budgetDao())
        val checkBudgetThresholdUseCase = com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase(budgetRepository, context)
        createTransactionUseCase = CreateTransactionUseCase(repository, db.accountDao(), checkBudgetThresholdUseCase)
        getTransactionsUseCase = GetTransactionsUseCase(repository)
        updateTransactionUseCase = UpdateTransactionUseCase(repository, db.accountDao())
        deleteTransactionUseCase = DeleteTransactionUseCase(repository)

        // Setup common account and category
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        db.accountDao().insertAccount(
            AccountEntity("acc-2", "Tabungan Bank", "BANK", null, 200000L, 200000L, 1000L, 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-2", "Gaji", "INCOME", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateIncomeAndExpenseTransactions() = runBlocking {
        // 1. Create Income of 50,000 on Account 1
        val income = Transaction(
            id = "tx-income",
            accountId = "acc-1",
            categoryId = "cat-2",
            receiptId = null,
            transactionType = "INCOME",
            title = "Gaji Bulanan",
            note = null,
            amount = 50000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(income)

        // Account 1 current balance: 100,000 + 50,000 = 150,000
        val acc1 = db.accountDao().getAccountById("acc-1").first()
        assertEquals(150000L, acc1?.currentBalance)

        // 2. Create Expense of 20,000 on Account 1
        val expense = Transaction(
            id = "tx-expense",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Makan Siang",
            note = null,
            amount = 20000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(expense)

        // Account 1 current balance: 150,000 - 20,000 = 130,000
        val acc1AfterExpense = db.accountDao().getAccountById("acc-1").first()
        assertEquals(130000L, acc1AfterExpense?.currentBalance)
    }

    @Test
    fun testUpdateTransactionAmountAndAccount() = runBlocking {
        // Create an Expense of 30,000 on Account 1
        val transaction = Transaction(
            id = "tx-1",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Belanja",
            note = null,
            amount = 30000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(transaction)

        // Account 1 balance: 100,000 - 30,000 = 70,000
        val acc1Initial = db.accountDao().getAccountById("acc-1").first()
        assertEquals(70000L, acc1Initial?.currentBalance)

        // Update transaction: change account to Account 2 and increase amount to 50,000
        val updated = transaction.copy(
            accountId = "acc-2",
            amount = 50000L,
            updatedAt = 2000L
        )
        updateTransactionUseCase(transaction, updated)

        // 1. Account 1 balance should be reverted: 70,000 + 30,000 = 100,000
        val acc1Updated = db.accountDao().getAccountById("acc-1").first()
        assertEquals(100000L, acc1Updated?.currentBalance)

        // 2. Account 2 balance should be adjusted: 200,000 - 50,000 = 150,000
        val acc2Updated = db.accountDao().getAccountById("acc-2").first()
        assertEquals(150000L, acc2Updated?.currentBalance)
    }

    @Test
    fun testSoftDeleteTransaction() = runBlocking {
        // Create an Expense of 25,000 on Account 1
        val transaction = Transaction(
            id = "tx-del",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Kopi",
            note = null,
            amount = 25000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(transaction)

        // Account 1 balance: 100,000 - 25,000 = 75,000
        val acc1Initial = db.accountDao().getAccountById("acc-1").first()
        assertEquals(75000L, acc1Initial?.currentBalance)

        // Delete the transaction
        deleteTransactionUseCase(transaction)

        // 1. Transaction should be soft-deleted (is_deleted = true, deleted_at is set)
        val readTx = repository.getTransactionById("tx-del").first()
        assertNotNull(readTx)
        assertTrue(readTx?.isDeleted == true)
        assertNotNull(readTx?.deletedAt)

        // 2. Account 1 balance should be reverted: 75,000 + 25,000 = 100,000
        val acc1AfterDelete = db.accountDao().getAccountById("acc-1").first()
        assertEquals(100000L, acc1AfterDelete?.currentBalance)

        // 3. Written to recycle bin
        val deletedItems = db.recycleBinDao().getAllDeletedItems().first()
        assertEquals(1, deletedItems.size)
        assertEquals("TRANSACTION", deletedItems[0].entityType)
        assertEquals("tx-del", deletedItems[0].entityId)
    }

    @Test
    fun testGetTransactionsWithDetailsJoin() = runBlocking {
        val transaction = Transaction(
            id = "tx-detail",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Jajan",
            note = null,
            amount = 10000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(transaction)

        val list = getTransactionsUseCase().first()
        assertEquals(1, list.size)
        assertEquals("tx-detail", list[0].id)
        assertEquals("Dompet Tunai", list[0].accountName)
        assertEquals("Makanan", list[0].categoryName)
    }
}
