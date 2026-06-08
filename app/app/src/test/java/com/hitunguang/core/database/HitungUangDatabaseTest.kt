package com.hitunguang.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.core.database.dao.CategoryDao
import com.hitunguang.core.database.dao.TransactionDao
import com.hitunguang.core.database.dao.TransferDao
import com.hitunguang.core.database.dao.UserProfileDao
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.database.entity.TransferEntity
import com.hitunguang.core.database.entity.UserProfileEntity
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
class HitungUangDatabaseTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var transferDao: TransferDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userProfileDao = db.userProfileDao()
        accountDao = db.accountDao()
        categoryDao = db.categoryDao()
        transactionDao = db.transactionDao()
        transferDao = db.transferDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadUserProfile() = runBlocking {
        val profile = UserProfileEntity(
            id = "user-1",
            name = "Ahmad",
            occupation = "Freelancer",
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        userProfileDao.insertOrUpdate(profile)
        val readProfile = userProfileDao.getUserProfile().first()
        assertNotNull(readProfile)
        assertEquals("Ahmad", readProfile?.name)
        assertEquals("Freelancer", readProfile?.occupation)
    }

    @Test
    fun writeAndReadAccount() = runBlocking {
        val account = AccountEntity(
            id = "acc-1",
            name = "Dompet Utama",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 100000L,
            currentBalance = 100000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        accountDao.insertAccount(account)
        val readAccount = accountDao.getAccountById("acc-1").first()
        assertNotNull(readAccount)
        assertEquals("Dompet Utama", readAccount?.name)
        assertEquals(100000L, readAccount?.currentBalance)
    }

    @Test
    fun testTransactionAndBalanceSync() = runBlocking {
        val account = AccountEntity(
            id = "acc-1",
            name = "Dompet",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 100000L,
            currentBalance = 100000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        accountDao.insertAccount(account)

        val category = CategoryEntity(
            id = "cat-1",
            name = "Makanan",
            categoryType = "EXPENSE",
            icon = "food",
            isDefault = true,
            isPinned = false,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        categoryDao.insertCategory(category)

        val transaction = TransactionEntity(
            id = "tx-1",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Makan Siang",
            note = "Nasi Padang",
            amount = 15000L,
            transactionDate = 123456789L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )

        // Insert transaction and deduct balance (-15000)
        transactionDao.insertTransactionAndUpdateBalance(transaction, -15000L)

        val updatedAccount = accountDao.getAccountById("acc-1").first()
        assertEquals(85000L, updatedAccount?.currentBalance)

        val txList = transactionDao.getAllTransactions().first()
        assertEquals(1, txList.size)
        assertEquals("Makan Siang", txList[0].title)
    }

    @Test
    fun testTransferAndBalanceSync() = runBlocking {
        val account1 = AccountEntity(
            id = "acc-from",
            name = "Dompet 1",
            accountType = "CASH",
            icon = "wallet",
            initialBalance = 100000L,
            currentBalance = 100000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        val account2 = AccountEntity(
            id = "acc-to",
            name = "Dompet 2",
            accountType = "BANK",
            icon = "bank",
            initialBalance = 50000L,
            currentBalance = 50000L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        accountDao.insertAccount(account1)
        accountDao.insertAccount(account2)

        val transfer = TransferEntity(
            id = "tr-1",
            fromAccountId = "acc-from",
            toAccountId = "acc-to",
            amount = 30000L,
            adminFee = 2500L,
            note = "Kirim uang saku",
            transferDate = 123456789L,
            createdAt = 123456789L,
            updatedAt = 123456789L
        )

        transferDao.executeTransfer(transfer)

        val updatedFrom = accountDao.getAccountById("acc-from").first()
        val updatedTo = accountDao.getAccountById("acc-to").first()

        // From account should be: 100000 - 30000 (amount) - 2500 (fee) = 67500
        assertEquals(67500L, updatedFrom?.currentBalance)
        // To account should be: 50000 + 30000 = 80000
        assertEquals(80000L, updatedTo?.currentBalance)
    }
}
