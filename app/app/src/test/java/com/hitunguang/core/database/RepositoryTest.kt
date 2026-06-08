package com.hitunguang.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.account.domain.usecase.CreateAccountUseCase
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transfer.data.repository.TransferRepositoryImpl
import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.repository.TransferRepository
import com.hitunguang.feature.transfer.domain.usecase.ExecuteTransferUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var transferRepository: TransferRepository

    private lateinit var createAccountUseCase: CreateAccountUseCase
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var executeTransferUseCase: ExecuteTransferUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        accountRepository = AccountRepositoryImpl(db.accountDao())
        categoryRepository = CategoryRepositoryImpl(db.categoryDao())
        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        transferRepository = TransferRepositoryImpl(db.transferDao())

        createAccountUseCase = CreateAccountUseCase(accountRepository)
        val budgetRepository = com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl(db.budgetDao())
        val checkBudgetThresholdUseCase = com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase(budgetRepository, context)
        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, db.accountDao(), checkBudgetThresholdUseCase)
        executeTransferUseCase = ExecuteTransferUseCase(transferRepository, db.accountDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateAccountUseCase() = runBlocking {
        val account = Account(
            id = "acc-99",
            name = "Tabungan",
            accountType = "BANK",
            icon = "bank",
            initialBalance = 500000L,
            currentBalance = 500000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )

        createAccountUseCase(account)

        val readAccount = accountRepository.getAccountById("acc-99").first()
        assertNotNull(readAccount)
        assertEquals("Tabungan", readAccount?.name)
        assertEquals(500000L, readAccount?.currentBalance)
    }

    @Test
    fun testCreateTransactionUseCase() = runBlocking {
        val account = Account(
            id = "acc-tx",
            name = "Cash Wallet",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 200000L,
            currentBalance = 200000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        accountRepository.insertAccount(account)

        val category = Category(
            id = "cat-tx",
            name = "Belanja",
            categoryType = "EXPENSE",
            icon = "shopping",
            isDefault = false,
            isPinned = true,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        categoryRepository.insertCategory(category)

        val transaction = Transaction(
            id = "tx-99",
            accountId = "acc-tx",
            categoryId = "cat-tx",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Beli Baju",
            note = "Kaos polo",
            amount = 75000L,
            transactionDate = 123456789L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )

        // Deduct 75000 from 200000 -> 125000
        createTransactionUseCase(transaction)

        val readAccount = accountRepository.getAccountById("acc-tx").first()
        assertEquals(125000L, readAccount?.currentBalance)

        val readTx = transactionRepository.getTransactionById("tx-99").first()
        assertNotNull(readTx)
        assertEquals("Beli Baju", readTx?.title)
    }

    @Test
    fun testExecuteTransferUseCase() = runBlocking {
        val fromAccount = Account(
            id = "acc-from",
            name = "Dompet Asal",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 100000L,
            currentBalance = 100000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        val toAccount = Account(
            id = "acc-to",
            name = "Rekening Tujuan",
            accountType = "BANK",
            icon = "bank",
            initialBalance = 10000L,
            currentBalance = 10000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        accountRepository.insertAccount(fromAccount)
        accountRepository.insertAccount(toAccount)

        val transfer = Transfer(
            id = "tr-99",
            fromAccountId = "acc-from",
            toAccountId = "acc-to",
            amount = 40000L,
            adminFee = 0L,
            note = "Kirim jajan",
            transferDate = 123456789L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )

        executeTransferUseCase(transfer)

        val readFrom = accountRepository.getAccountById("acc-from").first()
        val readTo = accountRepository.getAccountById("acc-to").first()

        assertEquals(60000L, readFrom?.currentBalance)
        assertEquals(50000L, readTo?.currentBalance)
    }
}
