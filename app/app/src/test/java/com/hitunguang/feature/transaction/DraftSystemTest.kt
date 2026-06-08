package com.hitunguang.feature.transaction

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.transaction.data.repository.AttachmentRepositoryImpl
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import com.hitunguang.feature.transaction.domain.usecase.*
import com.hitunguang.feature.transaction.presentation.TransactionViewModel
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DraftSystemTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var db: HitungUangDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    
    private lateinit var saveDraftUseCase: SaveDraftUseCase
    private lateinit var getDraftUseCase: GetDraftUseCase
    private lateinit var clearDraftUseCase: ClearDraftUseCase
    
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup in-memory Room Database
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        // Setup in-memory DataStore
        val datastoreFile = File(tmpFolder.newFolder(), "test_preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)

        // Setup usecases
        saveDraftUseCase = SaveDraftUseCase(settingsDataStore)
        getDraftUseCase = GetDraftUseCase(settingsDataStore)
        clearDraftUseCase = ClearDraftUseCase(settingsDataStore)

        val txRepository = TransactionRepositoryImpl(db.transactionDao())
        val accRepository = AccountRepositoryImpl(db.accountDao())
        val catRepository = CategoryRepositoryImpl(db.categoryDao())
        val attRepository = AttachmentRepositoryImpl(db.attachmentDao())
        
        val getTransactionsUseCase = GetTransactionsUseCase(txRepository)
        val dummyBudgetRepo = object : BudgetRepository {
            override fun getAllBudgets() = flowOf(emptyList<Budget>())
            override fun getActiveBudgets() = flowOf(emptyList<Budget>())
            override suspend fun insertBudget(budget: Budget) {}
            override suspend fun updateBudget(budget: Budget) {}
            override suspend fun deleteBudget(budget: Budget) {}
        }
        val checkBudgetThresholdUseCase = CheckBudgetThresholdUseCase(dummyBudgetRepo, context)
        val createTransactionUseCase = CreateTransactionUseCase(txRepository, db.accountDao(), checkBudgetThresholdUseCase)
        val updateTransactionUseCase = UpdateTransactionUseCase(txRepository, db.accountDao())
        val deleteTransactionUseCase = DeleteTransactionUseCase(txRepository)
        
        // Mock fileManager dependencies since we don't test physical attachments here
        val fileManager = com.hitunguang.core.filemanager.AttachmentFileManager(context)
        val getAttachmentsUseCase = GetAttachmentsUseCase(attRepository)
        val addAttachmentUseCase = AddAttachmentUseCase(attRepository, fileManager)
        val deleteAttachmentUseCase = DeleteAttachmentUseCase(attRepository, fileManager)

        // Initialize ViewModel
        viewModel = TransactionViewModel(
            accountRepository = accRepository,
            categoryRepository = catRepository,
            getTransactionsUseCase = getTransactionsUseCase,
            createTransactionUseCase = createTransactionUseCase,
            updateTransactionUseCase = updateTransactionUseCase,
            deleteTransactionUseCase = deleteTransactionUseCase,
            getAttachmentsUseCase = getAttachmentsUseCase,
            addAttachmentUseCase = addAttachmentUseCase,
            deleteAttachmentUseCase = deleteAttachmentUseCase,
            saveDraftUseCase = saveDraftUseCase,
            getDraftUseCase = getDraftUseCase,
            clearDraftUseCase = clearDraftUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testDraftUseCases() {
        runBlocking {
            val draft = TransactionDraft(
                transactionType = "INCOME",
                accountId = "acc-1",
                categoryId = "cat-1",
                title = "Salary",
                note = "Monthly",
                amount = 5000000L,
                updatedAt = 1000L
            )

            // Save Draft
            saveDraftUseCase(draft)

            // Get Draft
            val read = getDraftUseCase().first()
            assertNotNull(read)
            assertEquals("Salary", read?.title)
            assertEquals("INCOME", read?.transactionType)
            assertEquals(5000000L, read?.amount)

            // Clear Draft
            clearDraftUseCase()
            val cleared = getDraftUseCase().first()
            assertNull(cleared)
        }
    }

    @Test
    fun testViewModel_SaveAndClearDraft() {
        runBlocking {
            val draft = TransactionDraft(
                transactionType = "EXPENSE",
                accountId = "acc-2",
                title = "Dinner",
                amount = 45000L,
                updatedAt = 2000L
            )

            // Save via ViewModel
            viewModel.saveDraft(draft)
            kotlinx.coroutines.delay(200)

            // Verify saved in DataStore
            val read = getDraftUseCase().first()
            assertNotNull(read)
            assertEquals("Dinner", read?.title)

            // Clear via ViewModel
            viewModel.clearDraft()
            kotlinx.coroutines.delay(200)
            val cleared = getDraftUseCase().first()
            assertNull(cleared)
        }
    }

    @Test
    fun testViewModel_ClearDraftOnTransactionSave() {
        runBlocking {
            // Setup an account so balance checks pass
            db.accountDao().insertAccount(
                AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
            )

            val draft = TransactionDraft(
                transactionType = "EXPENSE",
                accountId = "acc-1",
                title = "Lunch",
                amount = 15000L,
                updatedAt = 3000L
            )

            // Pre-save a draft
            viewModel.saveDraft(draft)
            kotlinx.coroutines.delay(200)
            assertNotNull(getDraftUseCase().first())

            // Create transaction (this triggers draft cleanup)
            viewModel.createTransaction(
                accountId = "acc-1",
                categoryId = null,
                transactionType = "EXPENSE",
                title = "Lunch",
                note = null,
                amount = 15000L,
                transactionDate = System.currentTimeMillis()
            )
            kotlinx.coroutines.delay(200)

            // Verify draft is cleared
            val cleared = getDraftUseCase().first()
            assertNull(cleared)
        }
    }

    @Test
    fun testViewModel_ClearDraftOnCancel() {
        runBlocking {
            val draft = TransactionDraft(
                transactionType = "EXPENSE",
                accountId = "acc-1",
                title = "Water Bill",
                amount = 50000L,
                updatedAt = 4000L
            )

            // Pre-save a draft
            viewModel.saveDraft(draft)
            kotlinx.coroutines.delay(200)
            assertNotNull(getDraftUseCase().first())

            // Cancel create dialog
            viewModel.hideCreateDialog()
            kotlinx.coroutines.delay(200)

            // Verify draft is cleared
            val cleared = getDraftUseCase().first()
            assertNull(cleared)
        }
    }
}
