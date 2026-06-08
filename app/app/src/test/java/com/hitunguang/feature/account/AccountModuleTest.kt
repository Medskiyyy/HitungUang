package com.hitunguang.feature.account

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.database.entity.TransferEntity
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.account.domain.usecase.CreateAccountUseCase
import com.hitunguang.feature.account.domain.usecase.DeleteAccountUseCase
import com.hitunguang.feature.account.domain.usecase.UpdateAccountUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var repository: AccountRepository
    private lateinit var createAccountUseCase: CreateAccountUseCase
    private lateinit var updateAccountUseCase: UpdateAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AccountRepositoryImpl(db.accountDao())
        createAccountUseCase = CreateAccountUseCase(repository)
        updateAccountUseCase = UpdateAccountUseCase(repository)
        deleteAccountUseCase = DeleteAccountUseCase(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateAndUpdateAccount() = runBlocking {
        val account = Account(
            id = "acc-1",
            name = "Dompet Tunai",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 100000L,
            currentBalance = 100000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        createAccountUseCase(account)

        val created = repository.getAccountById("acc-1").first()
        assertNotNull(created)
        assertEquals("Dompet Tunai", created?.name)

        val updated = created!!.copy(name = "Dompet Utama")
        updateAccountUseCase(updated)

        val readUpdated = repository.getAccountById("acc-1").first()
        assertEquals("Dompet Utama", readUpdated?.name)
    }

    @Test
    fun testDeleteAccountWithoutData() = runBlocking {
        val account = Account(
            id = "acc-2",
            name = "Dompet Kosong",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 0L,
            currentBalance = 0L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.insertAccount(account)

        // Directly delete
        deleteAccountUseCase(account)

        val read = repository.getAccountById("acc-2").first()
        assertNull(read)
    }

    @Test
    fun testDeleteAccountWithTransactionsThrowsException() = runBlocking {
        val account = Account(
            id = "acc-3",
            name = "Dompet Aktif",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 50000L,
            currentBalance = 50000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.insertAccount(account)

        // Insert a dummy transaction for this account
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx-1",
                accountId = "acc-3",
                categoryId = null,
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
        )

        try {
            deleteAccountUseCase(account, replacementAccountId = null)
            fail("Should throw IllegalStateException because account has transactions and no replacement account was provided")
        } catch (e: IllegalStateException) {
            // Expected
            assertTrue(e.message?.contains("membutuhkan akun pengganti") == true)
        }
    }

    @Test
    fun testDeleteAccountWithDataAndMigration() = runBlocking {
        val accountToDelete = Account(
            id = "acc-old",
            name = "Dompet Lama",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 50000L,
            currentBalance = 50000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val replacementAccount = Account(
            id = "acc-new",
            name = "Dompet Baru",
            accountType = "BANK",
            icon = "bank",
            initialBalance = 100000L,
            currentBalance = 100000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        repository.insertAccount(accountToDelete)
        repository.insertAccount(replacementAccount)

        // Insert transaction for old account
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx-2",
                accountId = "acc-old",
                categoryId = null,
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
        )

        // Insert transfer from old account
        db.transferDao().insertTransfer(
            TransferEntity(
                id = "tr-1",
                fromAccountId = "acc-old",
                toAccountId = "acc-new",
                amount = 20000L,
                adminFee = 0L,
                note = "Transfer saldo awal",
                transferDate = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Delete old account with replacement
        deleteAccountUseCase(accountToDelete, replacementAccountId = "acc-new")

        // 1. Old account should be deleted
        val readOld = repository.getAccountById("acc-old").first()
        assertNull(readOld)

        // 2. Transaction should be migrated to replacement account
        val tx = db.transactionDao().getTransactionById("tx-2").first()
        assertNotNull(tx)
        assertEquals("acc-new", tx?.accountId)

        // 3. Transfer from account should be migrated to replacement account
        val transfer = db.transferDao().getTransferById("tr-1").first()
        assertNotNull(transfer)
        assertEquals("acc-new", transfer?.fromAccountId)

        // 4. Replacement account balance should be adjusted: 100,000 + 50,000 = 150,000
        val readNew = repository.getAccountById("acc-new").first()
        assertNotNull(readNew)
        assertEquals(150000L, readNew?.currentBalance)
    }
}
