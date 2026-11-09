package com.hitunguang.feature.receipt.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.filemanager.ReceiptFileManager
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.receipt.data.repository.ReceiptRepositoryImpl
import com.hitunguang.feature.receipt.domain.usecase.ParseReceiptUseCase
import com.hitunguang.feature.receipt.domain.usecase.SaveReceiptUseCase
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReceiptReviewViewModelTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var viewModel: ReceiptReviewViewModel

    private lateinit var uriA: Uri
    private val uriB = Uri.parse("file:///b.jpg")

    private val rawWithTotal = """
        TOKO MAJU
        Nasi Goreng 2 x 10.000 20.000
        Es Teh 1 x 5.000 5.000
        PPN 2.500
        TOTAL 27.500
    """.trimIndent()

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        val imageFile = File(context.cacheDir, "review_vm_test.jpg")
        FileOutputStream(imageFile).use {
            Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 80, it)
        }
        uriA = Uri.fromFile(imageFile)
        val fileManager = ReceiptFileManager(context)
        val receiptRepository = ReceiptRepositoryImpl(db.receiptDao(), fileManager)
        val transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        val budgetRepository = BudgetRepositoryImpl(db.budgetDao())
        val createTransaction = CreateTransactionUseCase(
            transactionRepository, db.accountDao(), CheckBudgetThresholdUseCase(budgetRepository, context)
        )
        viewModel = ReceiptReviewViewModel(
            SaveReceiptUseCase(receiptRepository, fileManager, createTransaction, db.accountDao(), db),
            AccountRepositoryImpl(db.accountDao()),
            CategoryRepositoryImpl(db.categoryDao()),
            ParseReceiptUseCase()
        )
        val deadline = System.currentTimeMillis() + 10_000
        while (viewModel.uiState.value.accountId.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun newScanStartsFromFreshState() {
        viewModel.initializeWithOcr(uriA, rawWithTotal)
        viewModel.saveReceipt()
        val deadline = System.currentTimeMillis() + 10_000
        while (!viewModel.uiState.value.isSaved && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        assertTrue(viewModel.uiState.value.errorMessage ?: "not saved", viewModel.uiState.value.isSaved)

        viewModel.initializeWithOcr(uriB, rawWithTotal)
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
        assertEquals(null, state.errorMessage)
        assertEquals(uriB, state.imageUri)
    }

    @Test
    fun samePairIsNoOpAndKeepsEdits() {
        viewModel.initializeWithOcr(uriA, rawWithTotal)
        viewModel.updateMerchantName("Edited")
        viewModel.initializeWithOcr(uriA, rawWithTotal)
        assertEquals("Edited", viewModel.uiState.value.merchantName)
    }

    @Test
    fun pinnedTotalIsNotOverwrittenByEdits() {
        viewModel.initializeWithOcr(uriA, rawWithTotal)
        assertEquals("27500", viewModel.uiState.value.totalStr)
        viewModel.updateItemUnitPrice(1, "6.000")
        viewModel.updateTax("3.000")
        assertEquals("27500", viewModel.uiState.value.totalStr)
        viewModel.removeItem(1)
        assertEquals("27500", viewModel.uiState.value.totalStr)
    }

    @Test
    fun editedTotalBecomesPinned() {
        viewModel.initializeWithOcr(uriA, "TOKO MAJU")
        viewModel.addItem()
        viewModel.updateItemUnitPrice(0, "10.000")
        assertFalse(viewModel.uiState.value.isTotalPinned)
        viewModel.updateTotal("12.345")
        assertTrue(viewModel.uiState.value.isTotalPinned)
        viewModel.updateTax("500")
        assertEquals("12345", viewModel.uiState.value.totalStr)
    }

    @Test
    fun unpinnedTotalFollowsItemsAndTax() {
        viewModel.initializeWithOcr(uriA, "TOKO MAJU")
        viewModel.addItem()
        viewModel.updateItemUnitPrice(0, "10.000")
        assertFalse(viewModel.uiState.value.isTotalPinned)
        viewModel.updateTax("1.000")
        assertEquals("11000", viewModel.uiState.value.totalStr)
    }

    @Test
    fun mismatchIsRecomputedAfterEdits() {
        viewModel.initializeWithOcr(uriA, rawWithTotal)
        assertFalse(viewModel.uiState.value.isAmountMismatch)
        viewModel.updateItemUnitPrice(1, "9.000")
        assertTrue(viewModel.uiState.value.isAmountMismatch)
        viewModel.updateTotal("31.500")
        assertFalse(viewModel.uiState.value.isAmountMismatch)
    }

    @Test
    fun amountInputWithThousandsDotsIsParsed() {
        viewModel.initializeWithOcr(uriA, rawWithTotal)
        viewModel.updateItemUnitPrice(1, "5.000")
        assertEquals(5000L, viewModel.uiState.value.items[1].unitPrice)
        viewModel.updateTax("1.250")
        assertEquals("1250", viewModel.uiState.value.taxStr)
        viewModel.updateTotal("Rp 1.234.567")
        assertEquals("1234567", viewModel.uiState.value.totalStr)
    }
}
