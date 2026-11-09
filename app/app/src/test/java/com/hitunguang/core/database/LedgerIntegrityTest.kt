package com.hitunguang.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.balance.BalanceValidator
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.RecycleBinEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.database.entity.TransferEntity
import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.account.domain.usecase.DeleteAccountUseCase
import com.hitunguang.feature.account.data.mapper.AccountMapper
import com.hitunguang.feature.recyclebin.data.repository.RecycleBinRepositoryImpl
import com.hitunguang.feature.transaction.domain.usecase.SearchTransactionsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
class LedgerIntegrityTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var recycleBin: RecycleBinRepositoryImpl

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recycleBin = RecycleBinRepositoryImpl(
            database = db,
            recycleBinDao = db.recycleBinDao(),
            transactionDao = db.transactionDao(),
            attachmentDao = db.attachmentDao(),
            fileManager = AttachmentFileManager(context)
        )
        db.accountDao().insertAccount(AccountEntity("acc-1", "Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L))
        db.accountDao().insertAccount(AccountEntity("acc-2", "Bank", "BANK", null, 500000L, 500000L, 1000L, 1000L))
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tx(id: String, accountId: String = "acc-1", categoryId: String? = "cat-1", amount: Long = 20000L, receiptId: String? = null) =
        TransactionEntity(id, accountId, categoryId, receiptId, "EXPENSE", "Nasi $id", null, amount, 1000L, false, null, 1000L, 1000L)

    private suspend fun balance(id: String) = db.accountDao().getAccountBalance(id)

    private suspend fun insertExpense(t: TransactionEntity) =
        db.transactionDao().insertTransactionAndUpdateBalance(t, -t.amount)

    @Test
    fun deleteWalletWithReplacement_thenRestore_doesNotDuplicateMoney() = runBlocking {
        insertExpense(tx("t-1"))
        assertEquals(80000L, balance("acc-1"))
        val repo = AccountRepositoryImpl(db.accountDao())
        val stale = AccountMapper.toDomain(db.accountDao().getAccountById("acc-1").first()!!).copy(currentBalance = 999L)

        DeleteAccountUseCase(repo)(stale, replacementAccountId = "acc-2")

        assertEquals(580000L, balance("acc-2"))
        assertEquals(0L, balance("acc-1"))
        assertEquals(0L, db.accountDao().getInitialBalance("acc-1"))

        recycleBin.restoreItem("acc-1", "WALLET")
        val total = db.accountDao().getAllAccounts().first().sumOf { it.currentBalance }
        assertEquals(580000L, total)
    }

    @Test
    fun updateAccount_doesNotOverwriteBalance() = runBlocking {
        val repo = AccountRepositoryImpl(db.accountDao())
        val snapshot = AccountMapper.toDomain(db.accountDao().getAccountById("acc-1").first()!!)
        insertExpense(tx("t-1"))

        repo.updateAccount(snapshot.copy(name = "Dompet"))

        val read = db.accountDao().getAccountById("acc-1").first()!!
        assertEquals("Dompet", read.name)
        assertEquals(80000L, read.currentBalance)
    }

    @Test
    fun deleteAndRestoreTransaction_areIdempotent() = runBlocking {
        val t = tx("t-1")
        insertExpense(t)
        db.transactionDao().deleteTransactionAndUpdateBalance(t, 20000L)
        db.transactionDao().deleteTransactionAndUpdateBalance(t, 20000L)
        assertEquals(100000L, balance("acc-1"))
        assertEquals(1, db.recycleBinDao().getAllDeletedItems().first().size)

        recycleBin.restoreTransaction("t-1")
        recycleBin.restoreTransaction("t-1")
        assertEquals(80000L, balance("acc-1"))
        assertTrue(db.recycleBinDao().getAllDeletedItems().first().isEmpty())
    }

    @Test
    fun revertTransfer_isIdempotent() = runBlocking {
        val transfer = TransferEntity("tr-1", "acc-1", "acc-2", 30000L, 2000L, null, 5000L, 5000L, 5000L)
        db.transferDao().executeTransfer(transfer)
        db.transferDao().revertTransfer(transfer)
        db.transferDao().revertTransfer(transfer)
        assertEquals(100000L, balance("acc-1"))
        assertEquals(500000L, balance("acc-2"))
    }

    @Test
    fun sumAdminFeesBetween_sumsOnlyInRange() = runBlocking {
        db.transferDao().executeTransfer(TransferEntity("tr-1", "acc-2", "acc-1", 10000L, 2500L, null, 5000L, 5000L, 5000L))
        db.transferDao().executeTransfer(TransferEntity("tr-2", "acc-2", "acc-1", 10000L, 1000L, null, 9000L, 9000L, 9000L))
        assertEquals(2500L, db.transferDao().sumAdminFeesBetweenDirect(4000L, 6000L))
        assertEquals(3500L, db.transferDao().sumAdminFeesBetween(0L, 10000L).first())
    }

    @Test
    fun balanceValidator_countsAdminFeeOnce() = runBlocking {
        db.transferDao().executeTransfer(TransferEntity("tr-1", "acc-1", "acc-2", 30000L, 2000L, null, 5000L, 5000L, 5000L))
        val validator = BalanceValidator(db.accountDao(), db.transactionDao(), db.transferDao())
        val result = validator.validateBalance("acc-1")
        assertEquals(68000L, result.calculatedBalance)
        assertTrue(result.isConsistent)
    }

    @Test
    fun softDeleteCategory_keepsLink_andRestoreBringsItBack() = runBlocking {
        insertExpense(tx("t-1"))
        val category = db.categoryDao().getCategoryByIdDirect("cat-1")!!
        db.categoryDao().deleteCategoryAndSoftDeleteTransactions(category, 2000L)

        val deleted = db.transactionDao().getTransactionEntityById("t-1")!!
        assertTrue(deleted.isDeleted)
        assertEquals("cat-1", deleted.categoryId)

        recycleBin.restoreItem("cat-1", "CATEGORY")
        recycleBin.restoreTransaction("t-1")
        val restored = db.transactionDao().getTransactionEntityById("t-1")!!
        assertFalse(restored.isDeleted)
        assertEquals("cat-1", restored.categoryId)
        assertEquals(80000L, balance("acc-1"))
    }

    @Test
    fun hardDeleteCategory_keepsTransactionsUncategorized() = runBlocking {
        insertExpense(tx("t-active"))
        insertExpense(tx("t-binned"))
        db.transactionDao().deleteTransactionAndUpdateBalance(tx("t-binned"), 20000L)
        db.categoryDao().softDeleteCategory("cat-1", 2000L)

        recycleBin.permanentDeleteItem("cat-1", "CATEGORY")

        assertNull(db.categoryDao().getCategoryByIdDirect("cat-1"))
        assertNull(db.transactionDao().getTransactionEntityById("t-active")!!.categoryId)
        assertNull(db.transactionDao().getTransactionEntityById("t-binned")!!.categoryId)
        assertEquals(80000L, balance("acc-1"))

        recycleBin.restoreTransaction("t-binned")
        val restored = db.transactionDao().getTransactionEntityById("t-binned")!!
        assertFalse(restored.isDeleted)
        assertNull(restored.categoryId)
        assertEquals(60000L, balance("acc-1"))
    }

    @Test
    fun restoreTransaction_missingEntity_removesStaleBinEntry() = runBlocking {
        db.recycleBinDao().addToRecycleBin(RecycleBinEntity("rb-1", "TRANSACTION", "ghost", 1000L, 2000L))
        recycleBin.restoreTransaction("ghost")
        assertTrue(db.recycleBinDao().getAllDeletedItems().first().isEmpty())
    }

    @Test
    fun cleanup_continuesAfterFailingItem() = runBlocking {
        insertExpense(tx("t-1"))
        insertExpense(tx("t-2"))
        db.transactionDao().deleteTransactionAndUpdateBalance(tx("t-2"), 20000L)
        // acc-1 still has t-1, so hard delete is blocked by the FK and must fail
        db.recycleBinDao().addToRecycleBin(RecycleBinEntity("rb-w", "WALLET", "acc-1", 1000L, 0L))
        db.openHelper.writableDatabase.execSQL("UPDATE recycle_bin SET expire_at = 0")

        val result = recycleBin.cleanupExpiredItems(10L)

        assertEquals(1, result.deleted)
        assertEquals(1, result.failed)
        assertNull(db.transactionDao().getTransactionEntityById("t-2"))
        assertNotNull(db.accountDao().getAccountById("acc-1").first())
    }

    @Test
    fun getTransactionByReceiptId_ignoresDeletedTransaction() = runBlocking {
        val t = tx("t-r", receiptId = "rcp-1")
        insertExpense(t)
        assertNotNull(db.transactionDao().getTransactionByReceiptId("rcp-1"))
        db.transactionDao().deleteTransactionAndUpdateBalance(t, 20000L)
        assertNull(db.transactionDao().getTransactionByReceiptId("rcp-1"))
    }

    @Test
    fun search_withFtsSpecialCharacters_doesNotCrash() = runBlocking {
        insertExpense(tx("t-1"))
        for (raw in listOf("\"nasi", "nasi*(", "-nasi", "na:si", "^nasi OR", "\"")) {
            val q = SearchTransactionsUseCase.toFtsQuery(raw) ?: continue
            db.transactionDao().searchTransactions(q).first()
        }
        val hits = db.transactionDao().searchTransactions(SearchTransactionsUseCase.toFtsQuery("\"nasi")!!).first()
        assertEquals(1, hits.size)
    }
}
