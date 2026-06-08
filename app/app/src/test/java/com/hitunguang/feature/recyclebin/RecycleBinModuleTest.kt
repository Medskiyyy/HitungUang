package com.hitunguang.feature.recyclebin

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.AttachmentEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.recyclebin.data.repository.RecycleBinRepositoryImpl
import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import com.hitunguang.feature.recyclebin.domain.usecase.CleanupExpiredItemsUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.GetDeletedItemsUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.PermanentDeleteTransactionUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.RestoreTransactionUseCase
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase
import com.hitunguang.feature.budget.domain.model.Budget
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecycleBinModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var recycleBinRepository: RecycleBinRepository
    
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    
    private lateinit var getDeletedItemsUseCase: GetDeletedItemsUseCase
    private lateinit var restoreTransactionUseCase: RestoreTransactionUseCase
    private lateinit var permanentDeleteTransactionUseCase: PermanentDeleteTransactionUseCase
    private lateinit var cleanupExpiredItemsUseCase: CleanupExpiredItemsUseCase
    
    private lateinit var fileManager: AttachmentFileManager
    private lateinit var context: Context

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fileManager = AttachmentFileManager(context)
        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        recycleBinRepository = RecycleBinRepositoryImpl(
            database = db,
            recycleBinDao = db.recycleBinDao(),
            transactionDao = db.transactionDao(),
            attachmentDao = db.attachmentDao(),
            fileManager = fileManager
        )

        val dummyBudgetRepo = object : BudgetRepository {
            override fun getAllBudgets() = flowOf(emptyList<Budget>())
            override fun getActiveBudgets() = flowOf(emptyList<Budget>())
            override suspend fun insertBudget(budget: Budget) {}
            override suspend fun updateBudget(budget: Budget) {}
            override suspend fun deleteBudget(budget: Budget) {}
        }
        val checkBudgetThresholdUseCase = CheckBudgetThresholdUseCase(dummyBudgetRepo, context)
        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, db.accountDao(), checkBudgetThresholdUseCase)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository)

        getDeletedItemsUseCase = GetDeletedItemsUseCase(recycleBinRepository)
        restoreTransactionUseCase = RestoreTransactionUseCase(recycleBinRepository)
        permanentDeleteTransactionUseCase = PermanentDeleteTransactionUseCase(recycleBinRepository)
        cleanupExpiredItemsUseCase = CleanupExpiredItemsUseCase(recycleBinRepository)

        // Setup accounts and categories
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testTransactionSoftDeleteAndRecycleBinFlow() = runBlocking {
        val tx = Transaction(
            id = "t-1",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Nasi Goreng",
            note = null,
            amount = 20000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // 1. Insert transaction
        createTransactionUseCase(tx)
        var account = db.accountDao().getAccountById("acc-1").first()
        assertNotNull(account)
        assertEquals(80000L, account!!.currentBalance) // 100000L - 20000L

        // 2. Soft delete transaction
        deleteTransactionUseCase(tx)

        // Verify soft-deleted state
        val deletedTx = db.transactionDao().getTransactionEntityById("t-1")
        assertNotNull(deletedTx)
        assertTrue(deletedTx!!.isDeleted)

        // Verify balance is reverted
        account = db.accountDao().getAccountById("acc-1").first()
        assertEquals(100000L, account!!.currentBalance) // reverted to 100k

        // Verify exists in recycle bin
        val deletedItems = getDeletedItemsUseCase().first()
        assertEquals(1, deletedItems.size)
        assertEquals("t-1", deletedItems[0].entityId)
        assertEquals("TRANSACTION", deletedItems[0].entityType)
        assertEquals("Nasi Goreng", deletedItems[0].transactionDetails?.title)

        // 3. Restore transaction
        restoreTransactionUseCase("t-1")

        // Verify restored state
        val restoredTx = db.transactionDao().getTransactionEntityById("t-1")
        assertNotNull(restoredTx)
        assertFalse(restoredTx!!.isDeleted)
        assertNull(restoredTx.deletedAt)

        // Verify balance adjusted back
        account = db.accountDao().getAccountById("acc-1").first()
        assertEquals(80000L, account!!.currentBalance) // 80k

        // Verify removed from recycle bin
        val deletedItemsAfterRestore = getDeletedItemsUseCase().first()
        assertTrue(deletedItemsAfterRestore.isEmpty())
    }

    @Test
    fun testPermanentDeleteAndFileCleanup() = runBlocking {
        val tx = Transaction(
            id = "t-2",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "INCOME",
            title = "Gaji",
            note = null,
            amount = 50000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        createTransactionUseCase(tx)

        // Create a dummy attachment file
        val attachmentDir = fileManager.attachmentsDir
        val dummyFile = File(attachmentDir, "dummy_attach.jpg")
        dummyFile.writeText("Dummy Image Data")
        assertTrue(dummyFile.exists())

        // Insert attachment into DB
        val attachmentEntity = AttachmentEntity(
            id = "att-1",
            transactionId = "t-2",
            filePath = dummyFile.absolutePath,
            mimeType = "image/jpeg",
            fileSize = dummyFile.length(),
            createdAt = 1000L
        )
        db.attachmentDao().insertAttachment(attachmentEntity)

        // Soft delete transaction
        deleteTransactionUseCase(tx)

        // Verify it is soft-deleted and in recycle bin
        var deletedItems = getDeletedItemsUseCase().first()
        assertEquals(1, deletedItems.size)

        // Permanently delete transaction
        permanentDeleteTransactionUseCase("t-2")

        // Verify transaction is deleted from DB
        val dbTx = db.transactionDao().getTransactionEntityById("t-2")
        assertNull(dbTx)

        // Verify attachment is deleted from DB (cascaded)
        val dbAttachments = db.attachmentDao().getAttachmentsByTransactionIdDirect("t-2")
        assertTrue(dbAttachments.isEmpty())

        // Verify physical file is deleted from disk
        assertFalse(dummyFile.exists())

        // Verify removed from recycle bin
        deletedItems = getDeletedItemsUseCase().first()
        assertTrue(deletedItems.isEmpty())
    }

    @Test
    fun testAutoCleanupExpiredItems() = runBlocking {
        val tx1 = Transaction(
            id = "t-exp",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Expired",
            note = null,
            amount = 10000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val tx2 = Transaction(
            id = "t-keep",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Keep",
            note = null,
            amount = 15000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        createTransactionUseCase(tx1)
        createTransactionUseCase(tx2)

        // Soft delete both
        deleteTransactionUseCase(tx1)
        deleteTransactionUseCase(tx2)

        // Manipulate expire_at for tx1 to be in the past, and tx2 in the future
        val now = System.currentTimeMillis()
        db.runInTransaction {
            db.openHelper.writableDatabase.execSQL("UPDATE recycle_bin SET expire_at = ? WHERE entity_id = 't-exp'", arrayOf(now - 5000L))
            db.openHelper.writableDatabase.execSQL("UPDATE recycle_bin SET expire_at = ? WHERE entity_id = 't-keep'", arrayOf(now + 100000L))
        }

        // Run cleanup
        cleanupExpiredItemsUseCase(now)

        // Verify t-exp is permanently deleted
        val txExp = db.transactionDao().getTransactionEntityById("t-exp")
        assertNull(txExp)

        // Verify t-keep remains
        val txKeep = db.transactionDao().getTransactionEntityById("t-keep")
        assertNotNull(txKeep)

        // Verify recycle bin contains only t-keep
        val deletedItems = getDeletedItemsUseCase().first()
        assertEquals(1, deletedItems.size)
        assertEquals("t-keep", deletedItems[0].entityId)
    }
}
