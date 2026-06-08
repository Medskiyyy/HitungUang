package com.hitunguang.feature.transaction

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.SearchTransactionsUseCase
import com.hitunguang.feature.transaction.domain.usecase.UpdateTransactionUseCase
import com.hitunguang.feature.transaction.presentation.SearchViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var searchTransactionsUseCase: SearchTransactionsUseCase
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        searchTransactionsUseCase = SearchTransactionsUseCase(transactionRepository)
        
        val accountRepository = AccountRepositoryImpl(db.accountDao())
        val dummyBudgetRepo = object : BudgetRepository {
            override fun getAllBudgets() = flowOf(emptyList<Budget>())
            override fun getActiveBudgets() = flowOf(emptyList<Budget>())
            override suspend fun insertBudget(budget: Budget) {}
            override suspend fun updateBudget(budget: Budget) {}
            override suspend fun deleteBudget(budget: Budget) {}
        }
        val checkBudgetThresholdUseCase = CheckBudgetThresholdUseCase(dummyBudgetRepo, context)
        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, db.accountDao(), checkBudgetThresholdUseCase)
        updateTransactionUseCase = UpdateTransactionUseCase(transactionRepository, db.accountDao())
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository)

        // Setup common account and categories
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-2", "Transportasi", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testUseCaseQuerySanitization() = runBlocking {
        // Create matching transactions
        val tx = Transaction(
            id = "t-1",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Makan Siang Enak",
            note = "Beli nasi padang",
            amount = 25000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(tx)

        // 1. Empty query should yield empty list
        val emptyResults = searchTransactionsUseCase("").first()
        assertTrue(emptyResults.isEmpty())

        // 2. Exact match search
        val exactResults = searchTransactionsUseCase("Siang").first()
        assertEquals(1, exactResults.size)
        assertEquals("t-1", exactResults[0].id)

        // 3. Prefix search (e.g. "Maka" matches "Makan")
        val prefixResults = searchTransactionsUseCase("maka").first()
        assertEquals(1, prefixResults.size)

        // 4. Multi-word prefix search (e.g. "beli nasi")
        val multiWordResults = searchTransactionsUseCase("beli nasi").first()
        assertEquals(1, multiWordResults.size)
    }

    @Test
    fun testSearchFieldMatching() = runBlocking {
        // Insert transactions with notes and different categories
        val tx1 = Transaction(
            id = "t-1",
            accountId = "acc-1",
            categoryId = "cat-1", // Makanan
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Kopi pagi",
            note = "Starbucks caffe",
            amount = 50000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val tx2 = Transaction(
            id = "t-2",
            accountId = "acc-1",
            categoryId = "cat-2", // Transportasi
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Gojek ke kantor",
            note = "Kembalian tunai",
            amount = 15000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(tx1)
        createTransactionUseCase(tx2)

        // 1. Match by note
        val resultsNote = searchTransactionsUseCase("caffe").first()
        assertEquals(1, resultsNote.size)
        assertEquals("t-1", resultsNote[0].id)

        // 2. Match by category name
        val resultsCategory = searchTransactionsUseCase("makanan").first()
        assertEquals(1, resultsCategory.size)
        assertEquals("t-1", resultsCategory[0].id)

        val resultsCategory2 = searchTransactionsUseCase("transportasi").first()
        assertEquals(1, resultsCategory2.size)
        assertEquals("t-2", resultsCategory2[0].id)

        // 3. Match by title prefix
        val resultsTitle = searchTransactionsUseCase("gojek").first()
        assertEquals(1, resultsTitle.size)
        assertEquals("t-2", resultsTitle[0].id)
    }

    @Test
    fun testInstantCrudSynchronization() = runBlocking {
        val tx = Transaction(
            id = "t-sync",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Bensin Pertamax",
            note = null,
            amount = 100000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // 1. Initial search (no results)
        val initialResults = searchTransactionsUseCase("Pertamax").first()
        assertTrue(initialResults.isEmpty())

        // 2. Insert transaction (should auto-index)
        createTransactionUseCase(tx)
        val afterInsert = searchTransactionsUseCase("Pertamax").first()
        assertEquals(1, afterInsert.size)

        // 3. Update transaction title (should update FTS index)
        val updated = tx.copy(title = "Bensin Shell", updatedAt = 2000L)
        updateTransactionUseCase(tx, updated)

        val searchShell = searchTransactionsUseCase("Shell").first()
        assertEquals(1, searchShell.size)
        assertEquals("t-sync", searchShell[0].id)

        val searchPertamax = searchTransactionsUseCase("Pertamax").first()
        assertTrue(searchPertamax.isEmpty())

        // 4. Soft delete transaction (should de-index)
        deleteTransactionUseCase(updated)
        val afterDelete = searchTransactionsUseCase("Shell").first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testRebuildSearchIndex() = runBlocking {
        // Insert directly into DB bypass triggers/usecase hooks if any to simulate legacy unindexed data
        val entity = TransactionEntity(
            id = "t-legacy",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Belanja Bulanan",
            note = "Supermarket",
            amount = 300000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.transactionDao().insertTransaction(entity)

        // Verify it is not indexed yet because we bypassed the usecase/DAO hook
        val initialSearch = searchTransactionsUseCase("Bulanan").first()
        assertTrue(initialSearch.isEmpty())

        // Rebuild index
        transactionRepository.rebuildSearchIndex()

        // Verify it can now be searched
        val afterRebuild = searchTransactionsUseCase("Bulanan").first()
        assertEquals(1, afterRebuild.size)
        assertEquals("t-legacy", afterRebuild[0].id)
    }

    @Test
    fun testViewModelSearchTriggerAndRebuild() = runBlocking {
        // Setup transaction
        val tx = Transaction(
            id = "t-vm",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Sate Ayam",
            note = "Bumbu kacang",
            amount = 20000L,
            transactionDate = 1000L,
            isDeleted = false,
            deletedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        createTransactionUseCase(tx)

        val categoryRepository = CategoryRepositoryImpl(db.categoryDao())
        val viewModel = SearchViewModel(
            searchTransactionsUseCase = searchTransactionsUseCase,
            transactionRepository = transactionRepository
        )

        // Search via query change
        viewModel.onQueryChanged("sate")
        val results = viewModel.searchResults.first { it.isNotEmpty() }
        assertEquals(1, results.size)
        assertEquals("t-vm", results[0].id)
    }
}
