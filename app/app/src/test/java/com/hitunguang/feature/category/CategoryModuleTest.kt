package com.hitunguang.feature.category

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.category.domain.usecase.DeleteCategoryUseCase
import com.hitunguang.feature.category.domain.usecase.GetCategoriesUseCase
import com.hitunguang.feature.category.domain.usecase.SaveCategoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var repository: CategoryRepository
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var saveCategoryUseCase: SaveCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CategoryRepositoryImpl(db.categoryDao())
        getCategoriesUseCase = GetCategoriesUseCase(repository)
        saveCategoryUseCase = SaveCategoryUseCase(repository)
        deleteCategoryUseCase = DeleteCategoryUseCase(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateAndUpdateCategory() = runBlocking {
        val category = Category(
            id = "cat-1",
            name = "Gaji",
            categoryType = "INCOME",
            icon = "salary",
            isDefault = false,
            isPinned = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        saveCategoryUseCase(category, isEdit = false)

        val created = repository.getCategoryById("cat-1").first()
        assertNotNull(created)
        assertEquals("Gaji", created?.name)

        val updated = created!!.copy(name = "Bonus")
        saveCategoryUseCase(updated, isEdit = true)

        val readUpdated = repository.getCategoryById("cat-1").first()
        assertEquals("Bonus", readUpdated?.name)
    }

    @Test
    fun testPinnedCategoriesSortingOrder() = runBlocking {
        val cat1 = Category("cat-1", "B_Category", "INCOME", null, false, false, 1000L, 1000L)
        val cat2 = Category("cat-2", "A_Category", "INCOME", null, false, true, 1000L, 1000L)
        val cat3 = Category("cat-3", "C_Category", "INCOME", null, false, false, 1000L, 1000L)

        repository.insertCategory(cat1)
        repository.insertCategory(cat2) // Pinned! Should be first.
        repository.insertCategory(cat3)

        val list = getCategoriesUseCase().first()
        assertEquals(3, list.size)
        // cat-2 should be first because isPinned = true
        assertEquals("cat-2", list[0].id)
        // Then sorted alphabetically by name: A_Category (cat-2), B_Category (cat-1), C_Category (cat-3)
        assertEquals("cat-1", list[1].id)
        assertEquals("cat-3", list[2].id)
    }

    @Test
    fun testDefaultCategoryConstraints() = runBlocking {
        val defaultCategory = Category(
            id = "cat-default",
            name = "Lain-lain",
            categoryType = "EXPENSE",
            icon = "other",
            isDefault = true,
            isPinned = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.insertCategory(defaultCategory)

        // 1. Deleting default category should succeed (marks as soft-deleted)
        deleteCategoryUseCase(defaultCategory)
        val deleted = db.categoryDao().getCategoryByIdDirect("cat-default")
        assertNotNull(deleted)
        assertTrue(deleted!!.isDeleted)

        // 2. Try to update default category name/type
        val updatedName = defaultCategory.copy(name = "Ubah Nama")
        try {
            saveCategoryUseCase(updatedName, isEdit = true)
            fail("Default category name update should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("default tidak dapat diubah") == true)
        }

        // 3. Pinning/unpinning default category is allowed
        val updatedPin = defaultCategory.copy(isPinned = true)
        saveCategoryUseCase(updatedPin, isEdit = true)
        val read = repository.getCategoryById("cat-default").first()
        assertNotNull(read)
        assertTrue(read!!.isPinned)
    }

    @Test
    fun testRestoreDefaultCategories() = runBlocking {
        val defaultCategory = Category(
            id = "default_expense_makanan",
            name = "Makanan",
            categoryType = "EXPENSE",
            icon = "restaurant",
            isDefault = true,
            isPinned = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.insertCategory(defaultCategory)
        deleteCategoryUseCase(defaultCategory)

        val deleted = db.categoryDao().getCategoryByIdDirect("default_expense_makanan")
        assertNotNull(deleted)
        assertTrue(deleted!!.isDeleted)

        repository.restoreDefaultCategories()

        val restored = db.categoryDao().getCategoryByIdDirect("default_expense_makanan")
        assertNotNull(restored)
        assertFalse(restored!!.isDeleted)
    }

    @Test
    fun testDeleteCategoryWithTransactionsThrowsException() = runBlocking {
        val category = Category("cat-tx", "Makanan", "EXPENSE", null, false, false, 1000L, 1000L)
        repository.insertCategory(category)

        // Insert account first to avoid foreign key constraint violation
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )

        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx-1",
                accountId = "acc-1",
                categoryId = "cat-tx",
                receiptId = null,
                transactionType = "EXPENSE",
                title = "Makan Siang",
                note = null,
                amount = 15000L,
                transactionDate = 1000L,
                isDeleted = false,
                deletedAt = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        try {
            deleteCategoryUseCase(category, forceDelete = false)
            fail("Should throw Exception because category has active transactions and forceDelete = false")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("memiliki transaksi dan memerlukan konfirmasi") == true)
        }
    }

    @Test
    fun testDeleteCategoryWithTransactionsSuccess() = runBlocking {
        val category = Category("cat-tx", "Makanan", "EXPENSE", null, false, false, 1000L, 1000L)
        repository.insertCategory(category)

        // Insert account with initial 100,000 and current 85,000 (after 15,000 expense)
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet", "CASH", null, 100000L, 85000L, 1000L, 1000L)
        )

        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx-1",
                accountId = "acc-1",
                categoryId = "cat-tx",
                receiptId = null,
                transactionType = "EXPENSE",
                title = "Makan Siang",
                note = null,
                amount = 15000L,
                transactionDate = 1000L,
                isDeleted = false,
                deletedAt = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val now = 2000L
        deleteCategoryUseCase(category, forceDelete = true, now = now)

        // 1. Category should be deleted
        val readCat = repository.getCategoryById("cat-tx").first()
        assertNull(readCat)

        // 2. Transaction should NOT be deleted physically but marked as deleted and categoryId is NULL
        val tx = db.transactionDao().getTransactionById("tx-1").first()
        assertNotNull(tx)
        assertNull(tx?.categoryId)
        assertTrue(tx?.isDeleted == true)
        assertEquals(now, tx?.deletedAt)

        // 3. Log should be written to recycle bin
        val deletedItems = db.recycleBinDao().getAllDeletedItems().first()
        assertEquals(2, deletedItems.size)
        val hasTx = deletedItems.any { it.entityType == "TRANSACTION" && it.entityId == "tx-1" }
        val hasCat = deletedItems.any { it.entityType == "CATEGORY" && it.entityId == "cat-tx" }
        assertTrue(hasTx)
        assertTrue(hasCat)

        // 4. Account balance should be adjusted (reverted 15,000 expense: 85,000 + 15,000 = 100,000)
        val account = db.accountDao().getAccountById("acc-1").first()
        assertNotNull(account)
        assertEquals(100000L, account?.currentBalance)
    }
}
