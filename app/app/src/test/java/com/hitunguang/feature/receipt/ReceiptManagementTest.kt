package com.hitunguang.feature.receipt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.filemanager.ReceiptFileManager
import com.hitunguang.feature.receipt.data.repository.ReceiptRepositoryImpl
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import com.hitunguang.feature.receipt.domain.usecase.AutoDeleteReceiptsUseCase
import com.hitunguang.feature.receipt.domain.usecase.DeleteReceiptUseCase
import com.hitunguang.feature.receipt.domain.usecase.ParsedItemInput
import com.hitunguang.feature.receipt.domain.usecase.SaveReceiptUseCase
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReceiptManagementTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var fileManager: ReceiptFileManager
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var receiptRepository: ReceiptRepository
    private lateinit var transactionRepository: TransactionRepository
    
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var saveReceiptUseCase: SaveReceiptUseCase
    private lateinit var deleteReceiptUseCase: DeleteReceiptUseCase
    private lateinit var autoDeleteReceiptsUseCase: AutoDeleteReceiptsUseCase

    private lateinit var tempPrefsFile: File

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // In-memory Database
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        // File Manager
        fileManager = ReceiptFileManager(context)
        
        // Settings DataStore
        tempPrefsFile = File(context.cacheDir, "test_settings_${System.currentTimeMillis()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempPrefsFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
        
        // Repositories
        receiptRepository = ReceiptRepositoryImpl(db.receiptDao(), fileManager)
        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        
        // Use Cases
        val budgetRepository = com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl(db.budgetDao())
        val checkBudgetThresholdUseCase = com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase(budgetRepository, context)
        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, db.accountDao(), checkBudgetThresholdUseCase)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository)
        
        saveReceiptUseCase = SaveReceiptUseCase(receiptRepository, fileManager, createTransactionUseCase)
        deleteReceiptUseCase = DeleteReceiptUseCase(receiptRepository, transactionRepository, deleteTransactionUseCase)
        autoDeleteReceiptsUseCase = AutoDeleteReceiptsUseCase(receiptRepository, settingsDataStore)

        // Seed Account and Category
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        db.close()
        if (tempPrefsFile.exists()) {
            tempPrefsFile.delete()
        }
        // Cleanup all receipts dir files
        val files = fileManager.receiptsDir.listFiles()
        files?.forEach { it.delete() }
    }

    private fun createDummyImageUri(): Uri {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "test_receipt_${System.currentTimeMillis()}.jpg")
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return Uri.fromFile(file)
    }

    @Test
    fun testSaveReceiptFlow() = runBlocking {
        val imageUri = createDummyImageUri()
        val date = System.currentTimeMillis()
        val items = listOf(
            ParsedItemInput("Nasi Goreng", 1.0, 15000L, 15000L),
            ParsedItemInput("Es Teh", 2.0, 5000L, 10000L)
        )

        // Execute save receipt (total: 25000L + 2500L tax = 27500L)
        saveReceiptUseCase(
            tempImageUri = imageUri,
            merchantName = "Warteg Barokah",
            receiptDate = date,
            subtotal = 25000L,
            tax = 2500L,
            total = 27500L,
            ocrRawText = "WARTEG BAROKAH\nNasi Goreng 15000\nEs Teh 10000",
            accountId = "acc-1",
            categoryId = "cat-1",
            items = items
        )

        // 1. Verify receipt is saved in database
        val receipts = receiptRepository.getAllReceipts().first()
        assertEquals(1, receipts.size)
        val savedReceipt = receipts[0]
        assertEquals("Warteg Barokah", savedReceipt.merchantName)
        assertEquals(27500L, savedReceipt.total)

        // 2. Verify receipt items are saved
        val savedItems = receiptRepository.getReceiptItems(savedReceipt.id).first()
        assertEquals(2, savedItems.size)
        assertEquals("Nasi Goreng", savedItems[0].itemName)
        assertEquals(15000L, savedItems[0].subtotal)

        // 3. Verify image file is compressed and copied to internal directory
        val imageFile = File(savedReceipt.imagePath)
        assertTrue(imageFile.exists())
        assertEquals(fileManager.receiptsDir.absolutePath, imageFile.parentFile?.absolutePath)

        // 4. Verify financial transaction was created and bound
        val linkedTx = transactionRepository.getTransactionByReceiptId(savedReceipt.id)
        assertNotNull(linkedTx)
        assertEquals("Warteg Barokah", linkedTx?.title)
        assertEquals(27500L, linkedTx?.amount)
        assertEquals("EXPENSE", linkedTx?.transactionType)

        // 5. Verify account balance mutates (100,000 - 27,500 = 72,500)
        val acc = db.accountDao().getAccountById("acc-1").first()
        assertEquals(72500L, acc?.currentBalance)
    }

    @Test
    fun testDeleteReceiptFlow() = runBlocking {
        val imageUri = createDummyImageUri()
        val date = System.currentTimeMillis()
        val items = listOf(
            ParsedItemInput("Bakso", 1.0, 20000L, 20000L)
        )

        saveReceiptUseCase(
            tempImageUri = imageUri,
            merchantName = "Bakso Mas Kumis",
            receiptDate = date,
            subtotal = 20000L,
            tax = 0L,
            total = 20000L,
            ocrRawText = "Bakso 20000",
            accountId = "acc-1",
            categoryId = "cat-1",
            items = items
        )

        val receipts = receiptRepository.getAllReceipts().first()
        val savedReceipt = receipts[0]
        val imageFile = File(savedReceipt.imagePath)
        assertTrue(imageFile.exists())

        // Balance should be: 100,000 - 20,000 = 80,000
        val accInitial = db.accountDao().getAccountById("acc-1").first()
        assertEquals(80000L, accInitial?.currentBalance)

        // Delete receipt
        deleteReceiptUseCase(savedReceipt)

        // 1. Verify receipt is removed from database
        val receiptsAfter = receiptRepository.getAllReceipts().first()
        assertTrue(receiptsAfter.isEmpty())

        // 2. Verify items are removed
        val itemsAfter = receiptRepository.getReceiptItems(savedReceipt.id).first()
        assertTrue(itemsAfter.isEmpty())

        // 3. Verify file is deleted from disk
        assertFalse(imageFile.exists())

        // 4. Verify transaction is deleted/soft-deleted
        val linkedTx = transactionRepository.getTransactionByReceiptId(savedReceipt.id)
        assertTrue(linkedTx == null || linkedTx.isDeleted)

        // 5. Verify balance is restored: 80,000 + 20,000 = 100,000
        val accAfter = db.accountDao().getAccountById("acc-1").first()
        assertEquals(100000L, accAfter?.currentBalance)
    }

    @Test
    fun testAutoDeleteExpiredReceipts() = runBlocking {
        // Save app preferences: 30 days retention
        settingsDataStore.saveAppSettings(
            AppSettings(
                id = "default",
                themeMode = "DARK",
                hideBalance = false,
                receiptAutoDeleteDays = 30, // 30 days
                dashboardPeriod = "MONTHLY",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        val imageUri = createDummyImageUri()
        
        // 1. Save an expired receipt (date is 40 days ago)
        val expiredDate = System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000L
        saveReceiptUseCase(
            tempImageUri = imageUri,
            merchantName = "Old Merchant",
            receiptDate = expiredDate,
            subtotal = 10000L,
            tax = 0L,
            total = 10000L,
            ocrRawText = "Old",
            accountId = "acc-1",
            categoryId = "cat-1",
            items = listOf(ParsedItemInput("Barang", 1.0, 10000L, 10000L))
        )

        // 2. Save a fresh receipt (date is today)
        saveReceiptUseCase(
            tempImageUri = imageUri,
            merchantName = "Fresh Merchant",
            receiptDate = System.currentTimeMillis(),
            subtotal = 5000L,
            tax = 0L,
            total = 5000L,
            ocrRawText = "Fresh",
            accountId = "acc-1",
            categoryId = "cat-1",
            items = listOf(ParsedItemInput("Barang Baru", 1.0, 5000L, 5000L))
        )

        // Total of 2 receipts
        var receipts = receiptRepository.getAllReceipts().first()
        assertEquals(2, receipts.size)

        // Run auto cleanup
        autoDeleteReceiptsUseCase()

        // 1. Verify expired receipt is deleted, but fresh receipt remains
        receipts = receiptRepository.getAllReceipts().first()
        assertEquals(1, receipts.size)
        assertEquals("Fresh Merchant", receipts[0].merchantName)

        // 2. Verify financial transaction records still exist for BOTH transactions!
        val allTx = transactionRepository.getAllTransactions().first()
        val oldTx = allTx.find { it.title == "Old Merchant" }
        val freshTx = allTx.find { it.title == "Fresh Merchant" }
        assertNotNull(oldTx)
        assertNotNull(freshTx)
        assertEquals(10000L, oldTx?.amount)
        assertEquals(5000L, freshTx?.amount)
    }
}
